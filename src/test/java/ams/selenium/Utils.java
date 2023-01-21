package ams.selenium;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Utils {
    /**
     * Returns a set of processes that run on the given port
     * Used to find the process of the mitmproxy that was executed in the run/test before the current one
     * @param p port
     * @return process IDs
     */
    public static Set<String> getProcessOnPort(int p){
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

    /**
     * Terminated the process with the given ID
     * @param id Process ID
     */
    public static void killProcess (String id) {
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

    public static String transformString(String lineBefore, String line) {
        return getDateTime(line) + "; " + getBitrateAndFileIdFromResponse(lineBefore); // .atZone(ZoneId.of("Europe/Paris")).
    }

    /**
     * Parses the String from the HTTP Request/Response for specific segments
     * @param s
     * @return
     */
    // GET /content/video/chunk-stream9-00003.webm HTTP/2.0
    public static String getBitrateAndFileIdFromResponse(String s) {
        String[] split = s.split("-");
        if (split.length < 3) throw new IllegalArgumentException("Bad input string!");
        String bitrate = split[1];
        bitrate = switch (bitrate) {
            case "stream0" -> "video; 300; ";
            case "stream1" -> "audio; 64; ";
            case "stream2" -> "video; 600; ";
            case "stream3" -> "audio; 128; ";
            case "stream4" -> "video; 1000; ";
            case "stream5" -> "audio; 192; ";
            case "stream6" -> "video; 1500; ";
            case "stream7" -> "audio; 256; ";
            case "stream8" -> "video; 2200; ";
            case "stream9" -> "audio; 320; ";
            case "stream10" -> "video; 3200; ";
            case "stream12" -> "video; 4600; ";
            default -> "parse error!";
        };
        String id = split[2].split("\\.")[0];
        return bitrate + id;
    }

    /**
     * Parses the date of the HTTP Response and converts it to the correct time zone
     * @param s
     * @return
     */
    // Response time: Fri, 20 Jan 2023 17:24:44 GMT
    public static String getDateTime(String s) {
        String date = s.split(":", 2)[1].trim();
        DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("EEE, dd LLL uuuu HH:mm:ss zzz", Locale.ENGLISH).withZone(ZoneId.of("Etc/UTC"));
        Instant timestamp = Instant.from(formatterInput.parse(date));
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");
        ZoneId z = ZoneId.of("Europe/Paris");
        ZonedDateTime zdt = timestamp.atZone(z);
        return zdt.format(formatterOutput);
    }
}
