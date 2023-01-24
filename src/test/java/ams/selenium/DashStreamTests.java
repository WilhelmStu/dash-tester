package ams.selenium;

import io.github.bonigarcia.wdm.managers.ChromeDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;


import java.io.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ams.selenium.NetworkCondition.parseNetworkConditionsFile;
import static ams.selenium.Utils.transformString;

public class DashStreamTests {
    private static final Integer PROXY_PORT = 8087;
    private static final Integer CYCLE_TIME = 500;
    private ChromeDriver driver;
    private Thread bgThread;
    private Timestamp startTime;
    private BufferedWriter writer;
    private double currentTime = 0;


    @BeforeAll
    public static void setUpAll() {
        //Configuration.browserSize = "1920x1080";
    }

    /**
     * Setup
     * First a timestamp is recorded for each test
     * Then the old proxy processes are terminated (keeps running even if Java process is terminated)
     * Then sets up chrome driver with proxy
     * The Proxy is started in a background process and logs all response headers and times
     */
    @BeforeEach
    public void setUp() {
        startTime = new Timestamp(System.currentTimeMillis());
        System.err.println("Finding and killing processes on proxy port: " + PROXY_PORT);
        Set<String> ids = Utils.getProcessOnPort(PROXY_PORT);
        for (String id : ids
        ) {
            Utils.killProcess(id);
        }

        ChromeDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--proxy-server=http://localhost:" + PROXY_PORT);
        driver = new ChromeDriver(options);

        Runnable r = this::startAndLogProxy;
        bgThread = new Thread(r);
        bgThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    // detect buffering: https://stackoverflow.com/questions/21399872/how-to-detect-whether-html5-video-has-paused-for-buffering
    // video element reference: https://www.w3schools.com/tags/ref_av_dom.asp
    // performance of browser : https://stackoverflow.com/questions/45847035/using-selenium-how-to-get-network-request
    // proxy: https://github.com/mitmproxy/mitmproxy

    /**
     * The main test class that is run once for each file defined in @ValueSource
     * The whole test takes 10 minutes and outputs the current buffer of the video at specific points in time
     * @param networkConditionsFile test files
     * @throws IOException
     */
    @ParameterizedTest
    @ValueSource(strings = {"conditions1.csv", "conditions2.csv"})
    public void test1(String networkConditionsFile) throws IOException {
        this.startTime = new Timestamp(System.currentTimeMillis());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");
        System.out.println("Starting test1 at: " + startTime + ", with network conditions: " + networkConditionsFile);
        LinkedList<NetworkCondition> networkConditions = parseNetworkConditionsFile(networkConditionsFile);
        NetworkCondition currentConditions = new NetworkCondition(0, 5000, 0);
        NetworkCondition nextConditions = networkConditions.removeFirst();

        ChromiumNetworkConditions cond = new ChromiumNetworkConditions();
        driver.setNetworkConditions(currentConditions.updateChromiumNetworkConditions(cond));
        DevTools devTools = driver.getDevTools();
        devTools.createSession();

        System.out.println("Now testing with network condition: " + currentConditions);
        driver.get("https://streaming.stulpinger.at/");
        devTools.send(Network.setCacheDisabled(true));
        waitForPageLoad(driver);
        WebElement video = driver.findElement(By.tagName("video"));
        StringBuilder build = new StringBuilder();
        File out = new File("out" + File.separator + "Buffer-" + networkConditionsFile + "-" + startTime.toString().replace(':', '-') + ".txt");
        out.getParentFile().mkdir();
        out.createNewFile();
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));
        writer.write("time; buffer(s); video seconds; throttling bandwidth; latency");
        writer.newLine();
        while (true) {
            build.setLength(0);
            JavascriptExecutor js = driver;
            long time = (System.currentTimeMillis() - startTime.getTime()) / 1000;
            if (time > 600) break;
            System.out.println("Time: " + time + "s");
            //System.out.println("Download: " + driver.getNetworkConditions().getDownloadThroughput() / 1000 + "Kbps, Latency: " + driver.getNetworkConditions().getLatency());

            if (nextConditions != null && nextConditions.getTimePoint() < time) {
                currentConditions = nextConditions;
                if (!networkConditions.isEmpty()) {
                    nextConditions = networkConditions.removeFirst();
                } else {
                    nextConditions = null;
                }
                driver.setNetworkConditions(currentConditions.updateChromiumNetworkConditions(cond));
                System.out.println("Now testing with network condition: " + currentConditions);
                System.out.println("Download: " + driver.getNetworkConditions().getDownloadThroughput() / 1000 + "Kbps, Latency: " + driver.getNetworkConditions().getLatency());
            }


            double bufferUpperBound = 0;
            try {
                bufferUpperBound = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].buffered.end(0));", video));
                this.currentTime = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].currentTime);", video));// time
            } catch (Exception e) {
                System.out.println("Buffer of 0 / Error");
            }

            build.append(LocalDateTime.now().format(format)).append("; ");
            double bufferSize = bufferUpperBound - this.currentTime;

            if (bufferSize < 0.03) {
                build.append("0.0; ");
            } else {
                build.append(bufferSize).append("; ");
            }
            build.append(this.currentTime).append(", ").append(currentConditions.getBandwidth()).append("; ").append(currentConditions.getLatency());
            this.writer.write(build.toString());
            this.writer.newLine();
            this.writer.flush();
            //System.out.println(build);

            try {
                Thread.sleep(CYCLE_TIME);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterEach
    public void tearDown() throws IOException {
        driver.quit();
        bgThread.interrupt();
        writer.flush();
        writer.close();
    }

    /**
     * Makes sure the page is loaded before the video and logging starts
     * @param driver
     */
    private static void waitForPageLoad(WebDriver driver) {
        boolean done = false;
        while (!done) {
            done = ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
        }
    }

    /**
     * This is the Mitmproxy thread that runs in the background and intercepts the HTTP traffic
     * If the application is terminated, this cmd process will keep running in the background and block the Proxy port
     * The output of the proxy is filtered with script.py only showing the request path and response timestamp
     */
    private void startAndLogProxy() {
        ProcessBuilder builder = new ProcessBuilder( // http/2 needs to be disabled for better performance
                "cmd.exe", "/c", "mitmdump -s script.py --set stream_large_bodies=250k --set http2=false --mode regular@" + PROXY_PORT
        );
        builder.redirectErrorStream(true);
        try {
            File out = new File("out" + File.separator + "Responses-" + startTime.toString().replace(':', '-') + ".txt");
            out.getParentFile().mkdir();
            out.createNewFile();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));
            Process p = builder.start();
            BufferedReader proxyOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String lineBefore = "";
            writer.write("time; type; bitrate Kbps; id; video seconds");
            writer.newLine();
            while (true) {
                line = proxyOutput.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("Response time:")) {
                    writer.write(transformString(lineBefore, line));
                    writer.write("; " + currentTime);
                    writer.newLine();
                    writer.flush();
                } else {
                    lineBefore = line;
                }
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Ending Proxy thread");
                    writer.flush();
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
