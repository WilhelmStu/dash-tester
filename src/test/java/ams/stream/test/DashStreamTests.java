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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Condition.attribute;

public class DashStreamTests {
    //MainPage mainPage = new MainPage();
    private ChromeDriver driver;
    private BufferedReader proxyOutput;
    private  Thread bgThread;
    private String startTime;
    private static final Integer PROXY_PORT = 8085;

    @BeforeAll
    public static void setUpAll() {
        Configuration.browserSize = "1920x1080";
        //SelenideLogger.addListener("allure", new AllureSelenide());
    }

    @BeforeEach
    public void setUp() {
        startTime = new Timestamp(System.currentTimeMillis()).toString();
        Set<String> ids = getProcessOnPort(PROXY_PORT);
        for (String id: ids
        ) {
            System.err.println(id);
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
    public void test1(){
        System.out.println("Starting a test at: " + startTime);
        driver.get("https://streaming.stulpinger.at/");
        waitForPageLoad(driver);
        driver.setNetworkConditions(new ChromiumNetworkConditions()); //todo
        WebElement video = driver.findElement(By.tagName("video"));
        StringBuilder build = new StringBuilder();
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

            System.out.println(build);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
        bgThread.interrupt();
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
            proxyOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String lineBefore = "";
            while (true) {
                line = proxyOutput.readLine();
                if (line == null) {
                    break;
                }
                System.out.println(line);
                if (line.startsWith("Response time:")) {
                    writer.write(lineBefore);
                    writer.newLine();
                    writer.write(line);
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

    private static Set<String> getProcessOnPort(int p){
        Set<String> processesOnPort = new HashSet<>();
        ProcessBuilder pb = new ProcessBuilder
                ("cmd.exe", "/c", "netstat -ano | findstr :" + p);
        Process process;
        try {
            process = pb.start();
            process.waitFor();

            BufferedReader processes = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while (true) {
                line = processes.readLine();
                if (line == null) break;
                System.err.println(line);
                processesOnPort.add(line.substring(line.length()-5).trim());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return processesOnPort;
    }

    private static void killProcess (String id) {
        ProcessBuilder pb = new ProcessBuilder
                ("cmd.exe", "/c", "taskkill /F /PID " + id);
        Process process;
        try {
            process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Could not kill process with id:" + id);
        }
    }
}

    /*
    @Test
    public void search() {
        mainPage.searchButton.click();

        $("[data-test='search-input']").sendKeys("Selenium");
        $("button[data-test='full-search-button']").click();

        $("input[data-test='search-input']").shouldHave(attribute("value", "Selenium"));
    }

    @Test
    public void toolsMenu() {
        mainPage.toolsMenu.click();

        $("div[data-test='main-submenu']").shouldBe(visible);
    }

    @Test
    public void navigationToAllTools() {
        mainPage.seeAllToolsButton.click();

        $("#products-page").shouldBe(visible);

        assertEquals("All Developer Tools and Products by JetBrains", Selenide.title());
    }
}
     */
