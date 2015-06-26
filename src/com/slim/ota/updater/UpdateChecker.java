/*=========================================================================
 *
 *  PROJECT:  SlimRoms
 *            Team Slimroms (http://www.slimroms.net)
 *
 *  COPYRIGHT Copyright (C) 2013 Slimroms http://www.slimroms.net
 *            All rights reserved
 *
 *  LICENSE   http://www.gnu.org/licenses/gpl-2.0.html GNU/GPL
 *
 *  AUTHORS:     fronti90, mnazim, tchaari, kufikugel
 *  DESCRIPTION: SlimOTA keeps our rom up to date
 *
 *=========================================================================
 */

package com.slim.ota.updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.URLUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.slim.center.SlimCenter;
import com.slim.center.settings.SettingsProvider;
import com.slim.ota.R;
import com.slim.ota.settings.Settings;
import com.slim.util.Utils;

public class UpdateChecker extends AsyncTask<Context, Integer, String> {
    private static final String TAG = "UpdateChecker";

    private static final int MSG_CREATE_DIALOG = 0;
    private static final int MSG_DISPLAY_MESSAGE = 1;
    private static final int MSG_SET_PROGRESS = 2;
    private static final int MSG_CLOSE_DIALOG = 3;

    private String mDevice;
    private String mSlimCurrentVersion;
    private Context mContext;
    private int mId = 1000001;

    private boolean mNoLog = true;

    public ProgressDialog mProgressDialog;

    final Handler mHandler = new Handler() {

        public void createWaitDialog(){
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle(mContext.getString(R.string.title_update));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setMessage(mContext.getString(R.string.toast_text));
            mProgressDialog.show();
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE_DIALOG:
                    createWaitDialog();
                    break;
                case MSG_DISPLAY_MESSAGE:
                    if (mProgressDialog == null) createWaitDialog();
                    if (mProgressDialog.isShowing()) {
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.setProgress(mProgressDialog.getMax());
                        mProgressDialog.setMessage((String) msg.obj);
                    }
                    break;
                case MSG_SET_PROGRESS:
                    if (mProgressDialog != null) mProgressDialog.setProgress(((Integer) msg.obj));
                    break;
                case MSG_CLOSE_DIALOG:
                    if (mProgressDialog != null) mProgressDialog.dismiss();
                    break;
                default: // should never happen
                    break;
            }
        }
    };

    public void getDeviceTypeAndVersion() {
        mDevice = Utils.getProperty("ro.slim.device");
        mSlimCurrentVersion = Utils.getProperty("slim.ota.version");
    }

    @Override
    protected String doInBackground(Context... arg) {
        mContext = arg[0];
        Message msg;
        if (mContext != null && mContext.toString().contains("SlimCenter")) {
            msg = mHandler.obtainMessage(MSG_CREATE_DIALOG);
            mHandler.sendMessage(msg);
        }
        HttpURLConnection urlConnection = null;
        if (!connectivityAvailable(mContext)) return "connectivityNotAvailable";
        try {
            getDeviceTypeAndVersion();
            if (!mNoLog) Log.d(TAG, "strDevice=" + mDevice + "   slimCurVer=" + mSlimCurrentVersion);
            if (mDevice == null || mSlimCurrentVersion == null) return null;
            URL url = new URL(mContext.getString(R.string.json_url) + mDevice + ".json");
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            JSONObject object = new JSONObject(sb.toString());
            JSONObject data = object.getJSONObject("data");
            String version = data.getString("version");
            String urlString = data.getString("url");
            String result = data.getString("status");
            if (!result.equals("success")) return null;
            SettingsProvider.putFilename(mContext, version);
            SettingsProvider.putURL(mContext, urlString);
            return urlString;
        } catch(Exception e) {
            Log.e(TAG, "error while connecting to server", e);
            return null;
        } finally {
            if (urlConnection !=null) urlConnection.disconnect();
        }
    }

    public static boolean connectivityAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnectedOrConnecting() && (netInfo.getType() == ConnectivityManager.TYPE_MOBILE ||
            netInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (!mNoLog) Log.d("\r\n"+TAG, "result= "+result+"\n context="+mContext.toString()+"\r\n");
        if (mContext != null && mContext.toString().contains("SlimCenter")) {
            Message msg = mHandler.obtainMessage(MSG_CLOSE_DIALOG);
            mHandler.sendMessage(msg);
        } else if (result == null) {
            if (!mNoLog) Log.d(TAG, "onPostExecute() - no new Update detected!" );
        } else {
            if (!mNoLog) Log.d(TAG, "new Update available here: " + result);
            if (!URLUtil.isValidUrl(result))
                showInvalidLink();
            else
                showNotification();
        }
    }

    private void showNotification() {
        Notification.Builder mBuilder = new Notification.Builder(mContext)
            .setContentTitle(mContext.getString(R.string.title_update))
            .setContentText(mContext.getString(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_notification_slimota)
            .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_slimota));

        Intent intent = new Intent(mContext, SlimCenter.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                    0, intent, PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = mBuilder.build();
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(mId, notif);
    }

    private void showInvalidLink() {
        if (mContext != null && mContext.toString().contains("SlimCenter")) {
            Message msg = mHandler.obtainMessage(MSG_DISPLAY_MESSAGE, mContext.getString(R.string.bad_url));
            mHandler.sendMessage(msg);
        } else {
            android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(mContext);
            // Setting Dialog Title
            alertDialog.setTitle(mContext.getString(R.string.title_update));
            // Setting Dialog Message
            alertDialog.setMessage(mContext.getString(R.string.bad_url));
            // Setting Positive "OK" Button
            alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    dialog.cancel();
                }
            });
            // Showing Alert Message
            alertDialog.show();
        }
    }
}
