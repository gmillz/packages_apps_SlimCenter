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
 *  AUTHORS:     fronti90
 *  DESCRIPTION: SlimSizer: manage your apps
 *
 *=========================================================================
 */
package com.slim.sizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.slim.ota.R;
import com.slim.center.SlimCenter;
import com.slim.util.Shell;
import com.slim.util.Utils;

//import static com.slim.center.settings.SettingsProvider.SlimSizer.getBackupApps;

public class SlimSizer extends Fragment {

    private static final String TAG = "SlimSizer";

    private final int STARTUP_DIALOG = 1;
    private final int DELETE_DIALOG = 2;
    private final int DELETE_MULTIPLE_DIALOG = 3;
    private final int REBOOT_DIALOG = 4;
    protected TableRow adapter;
    private ArrayList<String> mSysApp;
    private ArrayList<String> mSelected = new ArrayList<>();
    private ArrayList<ViewHolder> mItems = new ArrayList<>();
    private ArrayList<ListItem> mListItems = new ArrayList<>();
    private boolean startup = true;
    public final String mSystemPath = "/system/app/";
    private static final String mBackupPath =
            Environment.getExternalStorageDirectory().toString() + "/Slim/AppBackups/";
    private String mProfilePath;
    private String mProfileName;
    private String mSelectedProfile;
    //private ListView mListView;
    private String mItem;
    Shell.SH shell;

    private Context mContext;

    private static class ListItem {

        String appName;
        Drawable appIcon;
    }

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.slim_sizer, container, false);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        showSuperuserRequest();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        shell = new Shell().su;

        mContext = getActivity();

        // create arraylist of apps not to be removed
        final ArrayList<String> safetyList = new ArrayList<>();
        safetyList.add("BackupRestoreConfirmation.apk");
        safetyList.add("CertInstaller.apk");
        safetyList.add("Contacts.apk");
        safetyList.add("ContactsProvider.apk");
        safetyList.add("DefaultContainerService.apk");
        safetyList.add("DownloadProvider.apk");
        safetyList.add("DrmProvider.apk");
        safetyList.add("MediaProvider.apk");
        safetyList.add("Mms.apk");
        safetyList.add("PackageInstaller.apk");
        safetyList.add("Phone.apk");
        safetyList.add("Settings.apk");
        safetyList.add("SettingsProvider.apk");
        safetyList.add("SlimCenter.apk");
        safetyList.add("Superuser.apk");
        safetyList.add("SystemUI.apk");
        safetyList.add("TelephonyProvider.apk");

        mProfilePath = getActivity().getFilesDir().toString();

        // create arraylist from /system/app content
        mSysApp = getSystemApps();

        // remove "apps not to be removed" from list and sort list
        mSysApp.removeAll(safetyList);
        Collections.sort(mSysApp);
        new SlimSizer.ListItemPopulator().execute();

        initializeDrawable();

        // populate listview via arrayadapter
        adapter = new TableRow();

        if (getView() == null) return;
        ListView mListView = (ListView) getView().findViewById(R.id.list_view);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setAdapter(adapter);
    }

    private ArrayList<String> getSystemApps() {
        ArrayList<String> apps = new ArrayList<>();
        File system = new File(mSystemPath);
        File[] array = system.listFiles();
        for (File f : array) {
            if (f.isDirectory()) {
                File[] fia = f.listFiles();
                for (File fi : fia) {
                    if (fi.getName().endsWith("apk")) {
                        apps.add(fi.toString());
                    }
                }
            }
        }
        return apps;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!isNavigationDrawerOpen()) {
            inflater.inflate(R.menu.slim_sizer, menu);
        }
    }

    private boolean isNavigationDrawerOpen() {
        SlimCenter sc = (SlimCenter) getActivity();
        return sc.getNavigationDrawerFragment().isDrawerOpen();
    }

    private void unCheckAll() {
        for (ViewHolder item : mItems) {
            item.title.setChecked(false);
            if (mSelected.contains(item.fileName)) {
                mSelected.remove(item.fileName);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.profile_button:
                // call select dialog
                selectDialog();
                return true;
            case R.id.delete_button:
                if (mSelected.isEmpty()) {
                    toast(getResources().getString(
                            R.string.sizer_message_noselect));
                    return false;
                } else {
                    showDialog(DELETE_MULTIPLE_DIALOG);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        // showSuperuserRequest();
    }

    private void showSuperuserRequest() {
        if (this.getUserVisibleHint() && adapter != null && startup) {
            showDialog(STARTUP_DIALOG);
            startup = false;
        }
    }

    private void showDialog(int id) {
        // startup dialog
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        if (id == STARTUP_DIALOG) {
            // create warning dialog
            alert.setMessage(R.string.sizer_message_startup)
                    .setTitle(R.string.caution)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for ok
                                    dialog.cancel();
                                }
                            });
            // delete dialog
        } else if (id == DELETE_DIALOG) {
            alert.setMessage(R.string.sizer_message_delete)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for ok
                                    // call delete
                                    new SlimSizer.SlimDeleter().execute();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for cancel
                                    mItem = null;
                                    dialog.cancel();
                                }
                            });
        } else if (id == DELETE_MULTIPLE_DIALOG) {
            String message;
            if (mSelected.size() == 1) {
                message = getResources().getString(R.string.sizer_message_delete_multi_one);
            } else {
                message = getResources().getString(R.string.sizer_message_delete_multi);
            }
            alert.setMessage(message)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    for (String app : mSelected) {
                                        // remove list entry
                                        adapter.remove(app);
                                    }
                                    adapter.notifyDataSetChanged();
                                    new SlimSizer.SlimDeleter().execute();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for cancel
                                    dialog.cancel();
                                }
                            });
        } else if (id == REBOOT_DIALOG) {
            // create warning dialog
            alert.setMessage(R.string.reboot)
                    .setTitle(R.string.caution)
                    .setCancelable(true)
                    .setPositiveButton(R.string.reboot_ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for ok
                                    shell.runCommand("reboot");
                                }
                            })
                    .setNegativeButton(R.string.reboot_cancel,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    // action for cancel
                                    dialog.cancel();
                                }
                            });
        }
        // show warning dialog
        alert.show();
    }

    private void createProfileDialog() {
        View dialogView = View.inflate(getActivity(), R.layout.create_profile_dialog, null);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        final EditText et = (EditText) dialogView.findViewById(R.id.edit_text);

        dialog.setView(dialogView);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                mProfileName = et.getText().toString();

                try {
                    if (TextUtils.isEmpty(mProfileName))
                        mProfileName = "slimsizer.stf";
                    File newProfile = new File(mProfilePath + "/" + mProfileName + ".stf");
                    Log.d(TAG, newProfile.toString());
                    // create directory if it doesnt exist
                    StringBuilder sb = new StringBuilder();
                    for (String app : mSelected) {
                        sb.append(app).append("\n");
                    }
                    // create string from arraylists
                    String lists = sb.toString();
                    // delete savefile if it exists (overwrite)
                    if (newProfile.exists()) {
                        if (!newProfile.delete()) return;
                    }
                    // create savefile and output lists to it
                    FileWriter outstream = new FileWriter(
                            newProfile);
                    BufferedWriter save = new BufferedWriter(
                            outstream);
                    save.write(lists);
                    save.close();
                    // check for success
                    if (newProfile.exists()) {
                        toast(getResources()
                                .getString(
                                        R.string.sizer_message_filesuccess));
                    } else {
                        toast(getResources()
                                .getString(
                                        R.string.sizer_message_filefail));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        dialog.show();
    }

    private void createProfileSelectorDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        File[] filesArray = new File(mProfilePath).listFiles();
        final CharSequence[] titles = new String[filesArray.length];
        if (filesArray.length > 0) {
            for (int i = 0; i < filesArray.length; i++) {
                titles[i] = filesArray[i].getName().split("\\.")[0];
            }
        } else {
            titles[0] = "No Profiles Found.";
        }
        dialog.setItems(titles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSelectedProfile = titles[i].toString();
                try {
                    File profile = new File(mBackupPath + mSelectedProfile + ".stf");
                    Log.d(TAG, profile.toString());
                    // read savefile and create arraylist
                    BufferedReader reader = new BufferedReader(new FileReader(
                            profile));
                    String line;
                    ArrayList<String> deleteList = new ArrayList<>();
                    while ((line = reader.readLine()) != null) {
                        deleteList.add(line);
                    }
                    reader.close();
                    // delete all entries in deleteList
                    for (String item : deleteList) {
                        if (!mSelected.contains(item)) {
                            mSelected.add(item);
                            adapter.remove(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    new SlimSizer.SlimDeleter().execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.show();
    }

    // profile select dialog
    private void selectDialog() {
        AlertDialog.Builder select = new AlertDialog.Builder(getActivity());
        select.setItems(R.array.slimsizer_profile_array,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        short state = Utils.sdAvailable();
                        File path = getActivity().getFilesDir();
                        Log.d(TAG, path.toString());
                        if (which == 0) {
                            // load profile action
                            if (state >= 1) {
                                createProfileSelectorDialog();

                            } else {
                                toast(getResources().getString(
                                        R.string.sizer_message_sdnoread));
                            }
                        } else if (which == 1) {
                            // save profile action
                            if (state == 2) {
                                createProfileDialog();
                            } else {
                                toast(getResources().getString(
                                        R.string.sizer_message_sdnowrite));
                            }
                        }
                    }
                });
        select.show();
    }

    public void toast(String text) {
        // easy toasts for all!
        Toast toast = Toast.makeText(mContext, text,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public class ListItemPopulator extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... v) {
            for (int i = 0; mSysApp.size() > i; i++) {
                String file = mSysApp.get(i);
                ListItem item = new ListItem();
                Drawable icon = getDrawableFromCache(file);
                CharSequence title = getApkName(file);

                if (title == null) {
                    title = mSysApp.get(i);
                }

                if (icon == null) {
                    icon = getApkDrawable(file);
                }

                if (icon == null) {
                    icon = getResources().getDrawable(
                            android.R.drawable.sym_def_app_icon);
                }

                AppIconManager.cache.put(file, icon);
                item.appIcon = icon;
                item.appName = title.toString();
                mListItems.add(i, item);
                publishProgress(i);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            adapter.add(mSysApp.get(progress[0]));
        }

    }

    public class SlimDeleter extends AsyncTask<String, String, Void> {

        private ProgressDialog progress;
        private boolean singleItem = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (Utils.isSuEnabled()) {
                Shell.remountFileSystem(Shell.SYSTEM_MOUNT, "rw");
            }
            progress = new ProgressDialog(mContext);
            progress.setTitle(getString(R.string.delete_progress_title));
            progress.setMessage(getString(R.string.delete_progress));
            progress.show();
        }

        protected Void doInBackground(String... params) {
            if (mItem != null) {
                if (Utils.isSuEnabled()) {
                    /*if (getBackupApps(getActivity())) {
                        try {
                            Utils.copyFile(
                                    new File(mSystemPath + mItem), new File(mBackupPath + mItem));
                            Utils.zipDataFolder(
                                    getDataDir(mSystemPath + mItem), mBackupPath
                                            + mItem.split("\\.")[0] + ".zip");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }*/
                    Log.d(TAG, "appName=" + mItem);
                    shell.runCommand("rm -rf '" + mItem + "'\n");
                } else {
                    disableApp(mSystemPath + mItem);
                }
                singleItem = true;
                return null;
            }
            for (String appName : mSelected) {
                if (Utils.isSuEnabled()) {
                    Log.d(TAG, "appName=" + appName);
                    shell.runCommand("rm -rf '" + appName + "'\n");
                } else {
                    disableApp(mSystemPath + appName);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            progress.dismiss();
            if (Utils.isSuEnabled()) {
                Shell.remountFileSystem(Shell.SYSTEM_MOUNT, "ro");
                if (!singleItem) {
                    unCheckAll();
                    for (String s : mSelected) {
                        adapter.remove(s);
                    }
                    mSelected.clear();
                } else {
                    adapter.remove(mItem);
                    mItem = null;
                }
                adapter.notifyDataSetChanged();
                // showDialog(REBOOT_DIALOG, null);
            }
        }
    }

    private static class ViewHolder {

        CheckedTextView title;
        ImageView icon;
        String fileName;

        RelativeLayout mLayout;
    }

    public class TableRow extends ArrayAdapter<String> {

        public TableRow() {
            super(getActivity(), R.layout.list_item);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder mViewHolder;
            final String fileName = mSysApp.get(position);

            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item, parent, false);
                mViewHolder = new ViewHolder();
            } else {
                mViewHolder = (ViewHolder) convertView.getTag();
            }
            mViewHolder.fileName = fileName;
            mViewHolder.title = (CheckedTextView) convertView
                    .findViewById(R.id.text1);
            mViewHolder.title.setText(mListItems.get(position).appName);
            mViewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.image1);
            mViewHolder.icon.setImageDrawable(mListItems.get(position).appIcon);
            mViewHolder.mLayout = (RelativeLayout) convertView
                    .findViewById(R.id.item_layout);
            mViewHolder.mLayout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mSelected.contains(fileName)) {
                        mViewHolder.title.setChecked(false);
                        mSelected.remove(fileName);
                    } else {
                        mSelected.add(fileName);
                        mViewHolder.title.setChecked(true);
                    }
                }
            });
            mViewHolder.mLayout.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    // create deletion dialog
                    mItem = fileName;
                    showDialog(DELETE_DIALOG);
                    return true;
                }
            });

            convertView.setTag(mViewHolder);

            mItems.add(mViewHolder);

            return convertView;
        }
    }

    private Drawable getApkDrawable(String filepath) {

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
                PackageManager.GET_ACTIVITIES);

        if (packageInfo != null) {

            final ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = filepath;
            appInfo.publicSourceDir = filepath;

            return appInfo.loadIcon(pm);
        } else {
            return getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }
    }

    private CharSequence getApkName(String filepath) {

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(filepath, PackageManager.GET_ACTIVITIES);

        if (pi != null) {
            final ApplicationInfo ai = pi.applicationInfo;
            ai.sourceDir = filepath;
            ai.publicSourceDir = filepath;
            return pm.getApplicationLabel(ai);
        }
        return null;
    }

    private void disableApp(String path) {
        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);

        if (pi != null) {
            ApplicationInfo ai = pi.applicationInfo;
            ai.sourceDir = path;
            ai.publicSourceDir = path;
            pm.setApplicationEnabledSetting(ai.packageName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
    }

    /*private String getDataDir(String path) {
        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (pi != null) {
            return pi.applicationInfo.dataDir;
        }
        return null;
    }*/

    private static class AppIconManager {

        private static ConcurrentHashMap<String, Drawable> cache;
    }

    private void initializeDrawable() {
        AppIconManager.cache = new ConcurrentHashMap<>();
    }

    private Drawable getDrawableFromCache(String url) {
        if (AppIconManager.cache.containsKey(url)) {
            return AppIconManager.cache.get(url);
        }
        return null;
    }
}