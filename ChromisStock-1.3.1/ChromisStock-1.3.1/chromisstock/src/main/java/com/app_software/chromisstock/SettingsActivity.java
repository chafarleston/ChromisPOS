//    ChromisStock
//    Copyright (c) 2015 John Barrett
//    http://www.app-software.com
//    http://www.chromis.co.uk
//
//    This file is part of Chromis Stock
//    An Android based system that works with Chromis POS and some versions of Unicenta POS
//
//    ChromisStock is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    ChromisStock does not include the changes uploader. This is a seperate, bolt on application
//    that can be downloaded from Google Play.
//
//    ChromisStock is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Chromis POS.

package com.app_software.chromisstock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import java.util.List;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements DatabaseHandler.DownloadProgressReceiver {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    private  List<DatabaseHandler.Location> m_Locations;
    private static Boolean m_bDBChanged;
    private static Preference m_PrefDbURL;
    private static Preference m_PrefDbUser;
    private static Preference m_PrefDbPassword;
    private static Preference m_PrefDbLocation;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        m_bDBChanged = false;

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // If we have a valid DB connection we may already have location information
        addLocations();

        m_PrefDbURL = findPreference("database_url");
        m_PrefDbUser = findPreference("database_user");
        m_PrefDbPassword = findPreference("database_password");
        m_PrefDbLocation = findPreference("location");

        bindPreferenceSummaryToValue(m_PrefDbURL);
        bindPreferenceSummaryToValue(m_PrefDbUser);
//        bindPreferenceSummaryToValue(m_PrefDbPassword);
        bindPreferenceSummaryToValue(m_PrefDbLocation);
        bindPreferenceSummaryToValue(findPreference("chromis_user"));

        Preference button = (Preference) findPreference("test_connect");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                askTestConnect();
                return true;
            }
        });

        m_bDBChanged = false;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            if( preference.compareTo( m_PrefDbURL ) == 0 || preference.compareTo( m_PrefDbUser ) == 0 ||
                    preference.compareTo( m_PrefDbPassword ) == 0 || preference.compareTo( m_PrefDbLocation ) == 0 ) {
                m_bDBChanged = true;
            }

            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            bindPreferenceSummaryToValue(findPreference("database_url"));
            bindPreferenceSummaryToValue(findPreference("database_user"));
            bindPreferenceSummaryToValue(findPreference("database_password"));
            bindPreferenceSummaryToValue(findPreference("chromis_user"));
            bindPreferenceSummaryToValue(findPreference("location"));

        }
    }

    private void addLocations() {

        ListPreference location = (ListPreference) findPreference("location");

        DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext());
        m_Locations = db.getLocations();
        if( m_Locations != null && m_Locations.size() > 0 ) {
            String[] options = new String[m_Locations.size()];
            String[] optionvalues = new String[m_Locations.size()];
            for (int i = 0; i < m_Locations.size(); ++i) {
                options[i] = m_Locations.get(i).Name;
                optionvalues[i] = m_Locations.get(i).ChromisId;
            }
            location.setEntryValues( optionvalues );
            location.setEntries(options);
        } else {
            String[] options = new String[1];
            options[0] = "Default Location";
            String[] optionvalues = new String[1];
            optionvalues[0] = "0";
            location.setEntryValues( optionvalues );
            location.setEntries(options );
        }
    }

    @Override
    public void DownloadProgressReceiver(String Msg, boolean bFinished) {
        findPreference("test_connect").setSummary( Msg );

        if( bFinished ) {
            addLocations();
        }
    }

    private void doTestConnect() {
        DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext());
        db.addDownloadProgressReceiver( this );
        db.testConnection(getApplicationContext());
    }

    private void ClearDatabase() {
        DatabaseHandler db = DatabaseHandler.getInstance( getApplicationContext() );
        db.ReBuildProductTable(getApplicationContext());
    }

    private void askTestConnect() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getResources().getString(R.string.dlg_connect_title));
        alert.setMessage(  getResources().getString(R.string.dlg_connect_message) );

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                doTestConnect();
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        alert.show();

    }
}
