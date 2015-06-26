package com.slim.bugreport;

import java.io.File;

public class Bugreport {

    File screenshot;
    File originalBugreport;
    File logcat;
    File info;

    String dmesg;

    static Bugreport INSTANCE;

    public static Bugreport getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Bugreport();
        }
        return INSTANCE;
    }
}
