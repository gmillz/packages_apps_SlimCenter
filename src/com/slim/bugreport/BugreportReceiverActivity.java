package com.slim.bugreport;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BugreportReceiverActivity extends Activity {

    private ArrayList<Uri> mAttachments;

    Bugreport mBugreport = Bugreport.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if ("application/vnd.android.bugreport".equals(type)) {
                mAttachments = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
        }
        if (mAttachments == null) {
            mAttachments = new ArrayList<>();
        }
        parseBugreport();
    }

    private void parseBugreport() {
        for (Uri u : mAttachments) {
            Log.d("TEST", "uri=" + u.toString());
            Log.d("TEST1", "uri=" + u.getEncodedPath());
            File f = new File(u.getPath());
            Log.d("TEST", "file=" + f.toString());
            if (f.exists()) {
                if (f.toString().endsWith("txt")) {
                    mBugreport.originalBugreport = f;
                } else if (f.toString().endsWith("png")) {
                    mBugreport.screenshot = f;
                }
            }
        }
        parseLog();
    }

    private void parseLog() {
        File file = mBugreport.originalBugreport;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            mBugreport.dmesg = getDmesgFromLog(br);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("TEST", mBugreport.dmesg);
    }

    private String getDmesgFromLog(BufferedReader br) throws IOException {
        String line;
        boolean readLineToLog = false;
        StringBuilder log = new StringBuilder();
        while ((line = br.readLine()) != null) {
            //Log.d("TEST", "line=" + line);
            if (line.contains(BugreportConstants.DMESG_HEADER)) {
                readLineToLog = true;
                continue;
            }
            if (readLineToLog) {
                if (line.startsWith(BugreportConstants.END_HEADER)) break;
                log.append(line);
                log.append("\n");
            }
        }
        return log.toString();
    }
}
