package com.slim.bugreport;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Bugreport {

    public static final String TAG = Bugreport.class.getName();

    File path = new File(BugreportConstants.BUGREPORT_PATH + "/temp");

    File screenshot;
    File originalBugreport;
    File logcat;
    File info;
    File last_kmsg;
    File dmesg;
    File radio_log;

    File zip;
    boolean finalized;

    private ArrayList<File> files = new ArrayList<>();

    static Bugreport INSTANCE;

    public static Bugreport getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Bugreport();
        }
        return INSTANCE;
    }

    public Bugreport() {
        if (!path.exists()) {
            if (!path.mkdirs()) return;
        }
        info = new File(path + "/info.txt");
        logcat = new File(path + "/logcat.txt");
        dmesg = new File(path + "/dmesg.txt");
        last_kmsg = new File(path + "/last_kmsg.txt");
        radio_log = new File(path + "/radio_log.txt");
        files.add(info);
        files.add(logcat);
        files.add(dmesg);
        files.add(last_kmsg);
        files.add(radio_log);
    }

    public void setScreenshot(File f) {
        screenshot = f;
        files.add(screenshot);
    }

    public void finalizeReport() {
        if (zipReport()) {
            cleanup();
            finalized = true;
        }
    }

    private void cleanup() {
        deleteRecursive(path);
    }

    private void deleteRecursive(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    deleteRecursive(f);
                }
            } else {
                if (!dir.delete()) Log.d(TAG, "file : " + dir.toString() + " can not be deleted");
            }
        }
    }

    private boolean zipReport() {
        byte[] buf = new byte[1024];
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());
        Date date = new Date();
        String sDate = dateFormat.format(date);

        zip = new File(BugreportConstants.BUGREPORT_PATH
                + "/bugreport-" + sDate + ".zip");
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
            for (File f : files) {
                String sf = f.toString();
                String file = sf.substring(sf.lastIndexOf("/"), sf.length());
                FileInputStream in = new FileInputStream(sf);
                out.putNextEntry(new ZipEntry(file));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
