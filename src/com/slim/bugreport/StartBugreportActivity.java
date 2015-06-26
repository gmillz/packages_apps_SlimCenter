package com.slim.bugreport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

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
