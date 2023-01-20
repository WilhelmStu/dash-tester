package ams.stream.test;

// Import classes:
import com.browserup.proxy_client.ApiClient;
import com.browserup.proxy_client.ApiException;
import com.browserup.proxy_client.Configuration;
import com.browserup.proxy_client.Counter;
import com.browserup.proxy.api.BrowserUpProxyApi;

/**
 * Does not work...
 */
public class BrowserupMain {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8088");

        BrowserUpProxyApi apiInstance = new BrowserUpProxyApi(defaultClient);
        Counter counter = new Counter(); // Counter | Receives a new counter to add. The counter is stored, under the hood, in an array in the har under the _counters key
        try {
            apiInstance.addCounter(counter);
        } catch (ApiException e) {
            System.err.println("Exception when calling BrowserUpProxyApi#addCounter");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
