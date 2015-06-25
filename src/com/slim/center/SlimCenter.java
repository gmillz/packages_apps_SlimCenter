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
 *  DESCRIPTION: SlimCenter: manage your ROM
 *
 *=========================================================================
 */
package com.slim.center;

import com.slim.ota.R;
import com.slim.ota.SlimOTA;
import com.slim.ota.settings.About;
import com.slim.performance.PerformanceFragment;
import com.slim.performance.ViewPagerAdapter.E;
import com.slim.sizer.SlimSizer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;

public class SlimCenter extends FragmentActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private static ArrayList<E> ENTRIES = new ArrayList<>();

    static {
        ENTRIES.add(new E(new AboutSlim(), R.string.about_title));
        ENTRIES.add(new E(new SlimOTA(), R.string.ota_title));
        ENTRIES.add(new E(new SlimSizer(), R.string.sizer_title));
        ENTRIES.add(new E(new PerformanceFragment(), R.string.performance));
    }

    public CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slim_center);

        setNavigationDrawerFragment((NavigationDrawerFragment) getFragmentManager()
                .findFragmentById(R.id.navigation_drawer));
        mTitle = getTitle();

        // Set up the drawer
        getNavigationDrawerFragment().setUp(
                R.id.navigation_drawer_layout,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    public static String[] getTitleArray(Context context) {
        String[] sa = new String[ENTRIES.size()];
        for (int i = 0; i < ENTRIES.size(); i++) {
            sa[i] = context.getString(ENTRIES.get(i).title);
        }
        return sa;
    }

    public NavigationDrawerFragment getNavigationDrawerFragment() {
        return mNavigationDrawerFragment;
    }

    public void setNavigationDrawerFragment(NavigationDrawerFragment navigationDrawerFragment) {
        mNavigationDrawerFragment = navigationDrawerFragment;
    }

    public static void switchFragment(Activity activity, Fragment fragment) {
        FragmentManager fm = activity.getFragmentManager();
        fm.beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment = ENTRIES.get(position).fragment;
        switchFragment(this, fragment);
        onSectionAttached(position);
    }

    public void onSectionAttached(int number) {
        mTitle = getString(ENTRIES.get(number).title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.slim_center, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int resId = item.getItemId();
        /*if (resId == android.R.id.home) {
                // app icon in action bar clicked; go home
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
        } else*/ if (resId == R.id.ab_about) {
                Intent intentAbout = new Intent(this, About.class);
                startActivity(intentAbout);
                return true;
        } else {
                return super.onOptionsItemSelected(item);
        }
    }
}
