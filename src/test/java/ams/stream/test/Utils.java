package ams.stream.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class Utils {
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
}
