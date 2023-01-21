package ams.selenium;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.Response;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class NetworkCondition {
    private int timePoint = -1;
    private int bandwidth = -1;
    private int latency = 0;

    public NetworkCondition() {
    }

    public NetworkCondition(int timePoint, int bandwidth, int latency) {
        this.timePoint = timePoint;
        this.bandwidth = bandwidth;
        this.latency = latency;
    }

    public NetworkCondition(String csv) {
        String[] split = csv.split(";");
        if (split.length != 3)
            throw new IllegalArgumentException("Error parsing network conditions file, wrong number elements");
        this.timePoint = Integer.parseInt(split[0]);
        this.bandwidth = Integer.parseInt(split[1]);
        this.latency = Integer.parseInt(split[2]);
    }

    /**
     * Updates the given ChromiumNetworkConditions with new Values
     * Both the Upload and Download is set since calling getDownloadThroughput() returns the Upload value for some reason
     * The values also need to be multiplied by 1024 since the function takes bps and not Kbps as described in doc
     * @param conditions
     * @return
     */
    public ChromiumNetworkConditions updateChromiumNetworkConditions(ChromiumNetworkConditions conditions){
        conditions.setUploadThroughput(this.bandwidth * 1024);
        conditions.setDownloadThroughput(this.bandwidth * 1024);
        conditions.setLatency(Duration.ofMillis(this.latency));
        return conditions;
    }

    /**
     * Parses the given network conditions file, expects it to be in the root folder under ./network
     * The file has the .csv format and contains timepoints; bitrates; latency
     * @param file
     * @return
     */
    public static LinkedList<NetworkCondition> parseNetworkConditionsFile (String file){
        if (Objects.equals(file, "none")){
            return new LinkedList<>();
        }
        File conditions = new File("network" + File.separator + file);
        LinkedList<NetworkCondition> results = new LinkedList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(conditions));
            String line;
            int count = 0;
            while (true){
                count++;
                line = reader.readLine();
                if (line == null) break;
                try {
                    results.add(new NetworkCondition(line));
                }catch (Exception e){
                    System.err.println("Error parsing conditions file at line: " + count);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Conditions file was not found!");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return results;
    }


    /**
     * This is an alternative way to set the Network conditions of chrome
     * Still the upload and download parameters are mixed up somewhere in this library
     * @param driver
     * @param download
     */
    public void alternativeSetNetworkConditions(ChromeDriver driver, int download){
        CommandExecutor executor = driver.getCommandExecutor();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("offline", false);
        map.put("latency", 5);
        map.put("upload_throughput", download * 1024);
        map.put("download_throughput", download * 1024);

        Response response = null;
        try {
            response = executor.execute(new Command(driver.getSessionId(),"setNetworkConditions", ImmutableMap.of("network_conditions", ImmutableMap.copyOf(map))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(response);
        System.err.println("DL "+driver.getNetworkConditions().getDownloadThroughput());
        System.err.println("UL "+driver.getNetworkConditions().getUploadThroughput());
    }

    public int getTimePoint() {
        return timePoint;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getLatency() {
        return latency;
    }

    @Override
    public String toString() {
        return "{" +
                "timePoint=" + timePoint +
                ", bandwidth=" + bandwidth +
                ", latency=" + latency +
                '}';
    }
}
