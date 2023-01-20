package ams.stream.test;

import com.browserup.proxy.api.BrowserUpProxyApi;
import com.browserup.proxy_client.ApiException;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.managers.ChromeDriverManager;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v102.network.Network;
import org.openqa.selenium.devtools.v102.network.model.Headers;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.io.IOException;
import java.sql.SQLOutput;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

public class MainPageTest {
    //MainPage mainPage = new MainPage();
    private ChromeDriver driver;
    private static final Integer PROXY_PORT = 8080;

    @BeforeAll
    public static void setUpAll() throws IOException {
        Configuration.browserSize = "1920x1080";
        //Process p = Runtime.getRuntime().exec("mitmdump -s script2.py > out2.txt");
        //new Thread((Runnable) p.);// todo
        //SelenideLogger.addListener("allure", new AllureSelenide());
        /*
        try {
            new BrowserUpProxyApi().resetHarLog();
        }catch (ApiException e){
            e.printStackTrace();
        }

         */
    }

    @BeforeEach
    public void setUp() {
        ChromeDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--proxy-server=http://localhost:" + PROXY_PORT);
        /*
        var options = new ChromeOptions().addArguments(
                //"--headless",
                "--disable-extensions",
                "--proxy-server=http://localhost:" + PROXY_PORT
                );

         */
        driver = new ChromeDriver(options);
    }

    // detect buffering: https://stackoverflow.com/questions/21399872/how-to-detect-whether-html5-video-has-paused-for-buffering
    // video element reference: https://www.w3schools.com/tags/ref_av_dom.asp
    // performance of browser : https://stackoverflow.com/questions/45847035/using-selenium-how-to-get-network-request
    // proxy: https://github.com/browserup/mitmproxy/blob/main/clients/examples/java/src/test/java/com/javatest/JavaClientTest.java
    @Test
    public void test1() {

        driver.get("https://streaming.stulpinger.at/");
        driver.setNetworkConditions(new ChromiumNetworkConditions()); //todo
        WebElement video = driver.findElement(By.tagName("video"));
        StringBuilder build = new StringBuilder();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            build.setLength(0);
            JavascriptExecutor js = driver;

            double bufferUpperBound = 0;
            double currentTime = 0;
            try {
                bufferUpperBound = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].buffered.end(0));", video));
                currentTime = Double.parseDouble((String) js.executeScript("return JSON.stringify(arguments[0].currentTime);", video));// time
            }catch (Exception e){
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
