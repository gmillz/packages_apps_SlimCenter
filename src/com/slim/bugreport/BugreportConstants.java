package com.slim.bugreport;

import android.os.Environment;

public class BugreportConstants {

    public static final String MAIN_HEADER = "== dumpstate";
    public static final String DMESG_HEADER = "------ KERNEL LOG (dmesg) ------";
    public static final String LOGCAT_HEADER = "SYSTEM LOG";
    public static final String LAST_KMSG_HEADER = "------ LAST KMSG";
    public static final String RADIO_LOG_HEADER = "------ RADIO LOG";
    public static final String LAST_RADIO_LOG_HEADER = ""

    public static final int NOTIFICATION_ID = 1001001001;

    public static final String END_HEADER = "------ ";

    public static final String BUGREPORT_PATH = Environment.getExternalStorageDirectory()
            + "Slim/bugreports";

}
