package com.slim.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public class Utils {

    public static final boolean DEBUG = false;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String getProperty(String name) {
        String value = null;
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method m = c.getMethod("get", String.class);
            value = (String) m.invoke(null, name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String getGappsVersion() {
        Properties prop = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("/system/etc/gapp.prop");
            prop.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop.getProperty("ro.gapps.version", "-1");
    }

    public static short sdAvailable() {
        // check if sdcard is available
        // taken from developer.android.com
        short mExternalStorageAvailable;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = 2;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = 1;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = 0;
        }
        return mExternalStorageAvailable;
    }

    public static boolean isSuEnabled() {
        int value = 0;
        try {
            value = Integer.valueOf(getProperty("persist.sys.root_access"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return (value == 1 || value == 3);
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new
                FileInputStream(in).getChannel();
        FileChannel outChannel = new
                FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(),
                    outChannel);
        } finally {
            inChannel.close();
            outChannel.close();
        }
    }

    public static void zipDataFolder(String path, String out) {
        zipIt(path, out);
    }

    public static void zipIt(String source, String zipFile) {
        byte[] buffer = new byte[1024];

        String sourceFolder = new File(source).getAbsolutePath();

        FileOutputStream fos;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            FileInputStream in = null;

            for (String file : generateFileList(new File(source))) {
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(sourceFolder + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    if (in != null) in.close();
                }
            }

            zos.closeEntry();
            System.out.println("Folder successfully compressed");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (zos != null) zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<String> generateFileList(File node) {

        ArrayList<String> fileList = new ArrayList<String>();

        // add file only
        if (node.isFile()) {
            fileList.add(node.getName());
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }

        return fileList;
    }

    public static boolean connectivityAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnectedOrConnecting() &&
                (netInfo.getType() == ConnectivityManager.TYPE_MOBILE ||
                        netInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }

    public static boolean isUpdate(String newFilename, String currentFilename) {
        String newVersion = newFilename.split("build")[1].substring(1).split("-")[0];
        String currentVersion = currentFilename.split("build")[1].substring(1).split("-")[0];
        try {
            return Double.valueOf(newVersion) > Double.valueOf(currentVersion);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return true;
    }
}