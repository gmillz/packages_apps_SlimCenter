package com.slim.performance;

import android.util.Log;

import com.slim.util.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils implements Paths {

    public static Shell.SH shell = new Shell().su;

    public static String readOneLine(String file) {
        boolean useSu = false;
        BufferedReader reader;
        String line = "0";
        try {
            reader = new BufferedReader(new FileReader(file), 512);
            try {
                line = reader.readLine();
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            useSu = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (useSu) {
            line = shell.run("cat " + file + "\n").output;
            Log.d("SlimCenter", "OUT=" + line);
        }
        return line;
    }

    public static boolean writeValue(String filename, String value) {
        boolean useSu = false;
        if (new File(filename).exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(new File(filename));
                fos.write(value.getBytes());
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                useSu = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (useSu) {
                shell.run("echo " + value + " > " + filename + "\n");
            }
            return readOneLine(filename).equals(value);
        }
        return false;
    }

    public static boolean fileExists(String file) {
        return new File(file).exists();
    }

    public static int getNumOfCpus() {
        int numOfCpu = 1;
        String numOfCpus = readOneLine(CPU_NUM_FILE);
        String[] cpuCount = numOfCpus.split("-");
        if (cpuCount.length > 1) {
            try {
                int cpuStart = Integer.parseInt(cpuCount[0]);
                int cpuEnd = Integer.parseInt(cpuCount[1]);

                numOfCpu = cpuEnd - cpuStart + 1;

                if (numOfCpu < 0)
                    numOfCpu = 1;
            } catch (NumberFormatException ex) {
                numOfCpu = 1;
            }
        }
        return numOfCpu;
    }

    public static String toMHz(String mhzString) {
        return String.valueOf(Integer.parseInt(mhzString) / 1000) + " MHz";
    }

    public static String getCurrentIOScheduler() {
        String scheduler = null;
        String[] schedulers = null;
        String line = readOneLine(IO_SCHEDULER_PATH[0]);
        if (line != null) {
            schedulers = line.split(" ");
        }
        if (schedulers != null) {
            for (String s : schedulers) {
                if (s.charAt(0) == '[') {
                    scheduler = s.substring(1, s.length() - 1);
                    break;
                }
            }
        }
        return scheduler;
    }

    public static String[] getAvailableIOSchedulers() {
        String[] schedulers = null;
        String[] avail = null;
        String line = readOneLine(IO_SCHEDULER_PATH[0]);
        if (line != null) {
            avail = line.split(" ");
        }
        if (avail != null) {
            schedulers = new String[avail.length];
            for (int i = 0; i < avail.length; i++) {
                if (avail[i].charAt(0) == '[') {
                    schedulers[i] = avail[i].substring(1, avail[i].length() - 1);
                } else {
                    schedulers[i] = avail[i];
                }
            }
        }
        return schedulers;
    }

    public static String getCurrentGovernor() {
        return readOneLine(GOV_FILE);
    }

    public static String getGovernorControlPath() {
        return GOVERNOR_CONTROL + File.separator + getCurrentGovernor();
    }

    public static List<String> getGovernorControl() {
        List<String> files = new ArrayList<>();
        File folder = new File(GOVERNOR_CONTROL + File.separator + getCurrentGovernor());
        for (File f : folder.listFiles()) {
            files.add(f.getName());
        }
        return files;
    }

    public static List<String> getGovernorControlValues() {
        List<String> values = new ArrayList<>();
        List<String> files = getGovernorControl();
        for (String s : files) {
            values.add(readOneLine(s));
        }
        return values;
    }
}
