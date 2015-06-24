package com.slim.center.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.slim.ota.R;

public final class SettingsProvider {

    public static final String SETTINGS_KEY = "com.slim.ota_preferences";
    public static final String UPDATE_KEY = "com.slim.ota_updater";
    public static final String BACKUP_KEY = "com.slim.ota_backup";

    private static final String LAST_INTERVAL = "last_interval";
    public static final String INCLUDE_WEEKLY = "include_weekly";

    private static final String SHA_STORE = "sha_store";

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(SETTINGS_KEY, 0);
    }

    public static SharedPreferences.Editor put(Context context) {
        return context.getSharedPreferences(SETTINGS_KEY, 0).edit();
    }

    public static String getString(Context context, String key, String def) {
        return get(context).getString(key, def);
    }

    public static void putString(Context context, String key, String value) {
        put(context).putString(key, value).commit();
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        return get(context).getBoolean(key, def);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        put(context).putBoolean(key, value).commit();
    }

    // UpdateChecker
    public static SharedPreferences getUpdateCheckerPrefs(Context context) {
        return context.getSharedPreferences(UPDATE_KEY, 0);
    }

    public static void putFilename(Context context, String value) {
        getUpdateCheckerPrefs(context).edit().putString("Filename", value).apply();
    }

    public static String getFilename(Context context) {
        return getUpdateCheckerPrefs(context).getString("Filename", "");
    }

    public static void putURL(Context context, String url) {
        getUpdateCheckerPrefs(context).edit().putString("URL", url).apply();
    }

    public static String getUrl(Context context) {
        return getUpdateCheckerPrefs(context).getString("URL", null);
    }

    public static Long getLastInterval(Context context) {
        return get(context).getLong(LAST_INTERVAL, 0);
    }

    public static void putLastInterval(Context context, Long value) {
        put(context).putLong(LAST_INTERVAL, value).commit();
    }

    // SHA Check
    public static boolean sameSHA(Context context) {
        String lastSHA = get(context).getString(SHA_STORE, null);
        if (lastSHA == null)
            return false;

        HttpURLConnection urlCon = null;
        boolean result = false;
        try {
            URL url = new URL("");
            urlCon = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    urlCon.getInputStream()));
            String newSHA = in.readLine();
            if (newSHA.equals(lastSHA)) {
                putString(context, SHA_STORE, newSHA);
                result = true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlCon != null)
                urlCon.disconnect();
        }
        return result;
    }

    public static final class SlimSizer {

        public static void setBackupApps(Context ctx, boolean b) {
            get(ctx).edit().putBoolean("should_backup_apps", b);
        }

        public static boolean getBackupApps(Context ctx) {
            return get(ctx).getBoolean("should_backup_apps", false);
        }

    }

    public static final class Backup {

        public static SharedPreferences get(Context context) {
            return context.getSharedPreferences(BACKUP_KEY, Context.MODE_MULTI_PROCESS);
        }

        public static SharedPreferences.Editor put(Context context) {
            return context.getSharedPreferences(BACKUP_KEY, Context.MODE_MULTI_PROCESS).edit();
        }

        public static final void putString(Context context, String key, String value) {
            put(context).putString(key, value).commit();
        }

        public static final String getString(Context context, String key) {
            return get(context).getString(key, null);
        }

        public static final void copyBackupToSdCard(Context context, String name) {
            File backupFile = new File("/data/data/" +
                    context.getPackageName() + "/" + BACKUP_KEY + "xml");
            if (!name.endsWith(".xml")) {
                name = name + ".xml";
            }
            File sdcardFile = new File(Environment.getExternalStorageDirectory()
                    + "/Slim/backup" + name);

            try {
                InputStream in = new FileInputStream(backupFile);
                OutputStream out = new FileOutputStream(sdcardFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}