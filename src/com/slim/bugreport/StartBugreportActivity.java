package com.slim.bugreport;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.slim.ota.R;

import java.io.DataOutputStream;
import java.io.IOException;

public class StartBugreportActivity extends Activity {

    public static final String START_BUGREPORT_ACTION =
            "com.slim.bugreport.START_BUGREPORT_ACTION";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;
        if (intent.getAction().equals(START_BUGREPORT_ACTION)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_notification_slimota);
            builder.setContentTitle(getString(R.string.slim_bugreport));
            builder.setProgress(100, 50, true);
            manager.notify(BugreportConstants.NOTIFICATION_ID, builder.build());
            Settings.System.putInt(getContentResolver(), "slim_bugreport", 1);
            try {
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("am bug-report\n");
                os.writeBytes("exit\n");
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        finish();
    }
}
