package ams.stream.test;

import com.codeborne.selenide.Configuration;
import io.github.bonigarcia.wdm.managers.ChromeDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;


import java.io.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static ams.stream.test.Utils.getProcessOnPort;
import static ams.stream.test.Utils.killProcess;

public class DashStreamTests {
    private ChromeDriver driver;
    private Thread bgThread;
    private String startTime;
    private BufferedWriter writer;
    private static final Integer PROXY_PORT = 8085;

    @BeforeAll
    public static void setUpAll() {
        Configuration.browserSize = "1920x1080";
        //SelenideLogger.addListener("allure", new AllureSelenide());
    }

    /**
     * Setup
     * First a timestamp is recorded for each test
     * Then the old proxy processes are terminated (keeps running even if Java process is terminated)
     * Then sets up chrome driver with proxy
     * The Proxy is started in a background process and logs all response headers and times
     *
     */
    @BeforeEach
    public void setUp() {
        startTime = new Timestamp(System.currentTimeMillis()).toString();
        System.err.println("Finding and killing processes on proxy port: " + PROXY_PORT);
        Set<String> ids = getProcessOnPort(PROXY_PORT);
        for (String id : ids
        ) {
            killProcess(id);
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
    // proxy: https://github.com/browserup/mitmproxy/blob/main/clients/examples/java/src/test/java/com/javatest/JavaClientTest.java
    @Test
    public void test1() throws IOException {
        System.out.println("Starting test1 at: " + startTime);
        driver.get("https://streaming.stulpinger.at/");
        waitForPageLoad(driver);
        driver.setNetworkConditions(new ChromiumNetworkConditions()); //todo
        WebElement video = driver.findElement(By.tagName("video"));
        StringBuilder build = new StringBuilder();
        File out = new File("out" + File.separator + "Buffer-" + startTime.replace(':', '-') + ".txt");
        out.getParentFile().mkdir();
        out.createNewFile();
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));
        while (true) {
            build.setLength(0);
            JavascriptExecutor js = driver;


            double bufferUpperBound = 0;
            double currentTime = 0;
            try {
                bufferUpperBound = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].buffered.end(0));", video));
                currentTime = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].currentTime);", video));// time
            } catch (Exception e) {
                System.out.println("Buffer of 0 / Error");
            }

            build.append(new Timestamp(System.currentTimeMillis())).append("; ");
            double bufferSize = bufferUpperBound - currentTime;

            if (bufferSize < 0.05) {
                build.append("0.0; buffering");
            } else {
                build.append(bufferSize);
            }
            this.writer.write(build.toString());
            this.writer.newLine();
            this.writer.flush();
            System.out.println(build);

            try {
                Thread.sleep(1000);
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

    private static void waitForPageLoad(WebDriver driver) {
        boolean done = false;
        while (!done) {
            done = ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
        }
    }

    private void startAndLogProxy() {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "mitmdump -s script.py --mode regular@" + PROXY_PORT
        );
        builder.redirectErrorStream(true);
        try {
            File out = new File("out" + File.separator + "Responses-" + startTime.replace(':', '-') + ".txt");
            out.getParentFile().mkdir();
            out.createNewFile();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));
            Process p = builder.start();
            BufferedReader proxyOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String lineBefore = "";
            while (true) {
                line = proxyOutput.readLine();
                if (line == null) {
                    break;
                }
                System.out.println(line);
                if (line.startsWith("Response time:")) {
                    writer.write(transFormString(lineBefore, line));
                    //writer.write(lineBefore);
                    //writer.newLine();
                    //writer.write(line);
                    writer.write("\n----\n");
                    writer.flush();
                    System.out.println(lineBefore);
                    System.out.println(line);
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

    // GET /content/video/chunk-stream9-00003.webm HTTP/2.0
    // Response time: Fri, 20 Jan 2023 17:24:44 GMT
    private String transFormString(String lineBefore, String line){

        String date = line.split(":", 2)[1].trim();
        DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("EEE, dd LLL uuuu HH:mm:ss zzz", Locale.ENGLISH).withZone(ZoneId.of("Etc/UTC"));
        Instant timestamp = Instant.from(formatterInput.parse(date));
        //LocalDateTime dateTime = LocalDateTime.from(formatterInput.parse(date));
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");
        ZoneId z = ZoneId.of("Europe/Paris");
        ZonedDateTime zdt = timestamp.atZone(z);
        return lineBefore + "; Response time: " + zdt.format(formatterOutput); // .atZone(ZoneId.of("Europe/Paris")).
    }


}
