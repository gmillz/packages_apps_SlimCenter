package com.slim.bugreport;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.slim.ota.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        new ParseBugreportTask().execute();
    }

    private class ParseBugreportTask extends AsyncTask<Void, Void, Void> {

        NotificationManager manager;

        protected Void doInBackground(Void... params) {
            parseBugreport();
            return null;
        }

        protected void onPostExecute(Void v) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(BugreportConstants.NOTIFICATION_ID);
            mBugreport.finalizeReport();
            notifyUser();
        }
    }

    private void parseBugreport() {
        for (Uri u : mAttachments) {
            File f = new File(u.getPath());
            if (f.exists()) {
                if (f.toString().endsWith("txt")) {
                    mBugreport.originalBugreport = f;
                } else if (f.toString().endsWith("png")) {
                    mBugreport.setScreenshot(f);
                }
            }
        }
        parseLog();
    }

    private void parseLog() {
        File file = mBugreport.originalBugreport;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            // order matters
            getLogFromReader(br, BugreportConstants.MAIN_HEADER, mBugreport.info);
            getLogFromReader(br, BugreportConstants.DMESG_HEADER, mBugreport.dmesg);
            getLogFromReader(br, BugreportConstants.LOGCAT_HEADER, mBugreport.logcat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getLogFromReader(BufferedReader br, String tag, File file) throws IOException {
        String line;
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        boolean readLineToLog = false;
        while ((line = br.readLine()) != null) {
            if (line.contains(tag)) {
                readLineToLog = true;
                continue;
            }
            if (readLineToLog) {
                if (line.startsWith(BugreportConstants.END_HEADER))  break;
                writer.write(line);
                writer.write("\n");
            }
        }
        writer.close();
    }

    private void notifyUser() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_slimota)
                .setContentTitle(getString(R.string.slim_bugreport))
                .setContentText(mBugreport.zip.toString());
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(BugreportConstants.NOTIFICATION_ID, builder.build());
    }
}
