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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class uploadIntegrator {
    private static final String TAG = uploadIntegrator.class.getSimpleName();

    private static final String UPLOAD_PACKAGE = "com.app_software.chromisstock.chromisuploader";
    public static final String EXTRA_CONNECTION = "com.app_software.chromisstock.chromisuploader.extra.CONNECTION";
    public static final String EXTRA_USERNAME = "com.app_software.chromisstock.chromisuploader.extra.USERNAME";
    public static final String EXTRA_CHROMISUSER = "com.app_software.chromisstock.chromisuploader.extra.CHROMISUSER";
    public static final String EXTRA_PASSWORD = "com.app_software.chromisstock.chromisuploader.extra.PASSWORD";
    public static final String EXTRA_LOCATION = "com.app_software.chromisstock.chromisuploader.extra.LOCATION";

    private final Activity activity;

    public uploadIntegrator(Activity activity) {
        this.activity = activity;
    }

    public final AlertDialog initiateUpload() {
        Intent intent = new Intent( Intent.ACTION_SEND );
        intent.setType("text/plain");
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(activity);
        intent.putExtra(EXTRA_CONNECTION, SP.getString("database_url", null));
        intent.putExtra(EXTRA_USERNAME, SP.getString("database_user", null));
        intent.putExtra(EXTRA_PASSWORD, SP.getString("database_password", null));
        intent.putExtra(EXTRA_CHROMISUSER, SP.getString("chromis_user", null));
        intent.putExtra(EXTRA_LOCATION, SP.getString("location", null));

        if( checkUploader() ) {
            PackageManager pm = activity.getPackageManager();
            List<ResolveInfo> activityList = pm.queryIntentActivities(intent, 0);
            for (final ResolveInfo app : activityList) {
                if ((app.activityInfo.name).contains(UPLOAD_PACKAGE)) {
                    final ActivityInfo info = app.activityInfo;
                    final ComponentName name = new ComponentName(info.applicationInfo.packageName, info.name);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setComponent(name);
                    activity.startActivity(intent);
                    break;
                }
            }
        }

        return null;
    }

    private boolean checkUploader() {
        PackageManager pm = activity.getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(UPLOAD_PACKAGE);

        if( i == null) {
            showDownloadDialog();
            return false;
        } else {
            return true;
        }
    }

    private AlertDialog showDownloadDialog() {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle( activity.getResources().getString( R.string.dlg_install_uploader_title) );
        downloadDialog.setMessage( activity.getResources().getString( R.string.dlg_install_uploader_message ));

        downloadDialog.setPositiveButton( activity.getResources().getString( R.string.label_yes ),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://details?id=" + UPLOAD_PACKAGE);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    Toast.makeText( activity, "Google Play is not installed", Toast.LENGTH_LONG ).show();
                    Log.w(TAG, "Google Play is not installed; cannot install " + UPLOAD_PACKAGE);
                }
            }
        });
        downloadDialog.setNegativeButton(activity.getResources().getString(R.string.label_no), null);
        downloadDialog.setCancelable(true);
        return downloadDialog.show();
    }
}
