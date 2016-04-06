package com.app_software.chromisstock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class StockContentProvider extends ContentProvider {
    private static final UriMatcher sURIMatcher = new UriMatcher( UriMatcher.NO_MATCH );

    private static final int URI_CHANGES = 1;
    private static final int URI_CHANGES_ID = 2;

    private static String URI_AUTHORITY = "com.app_software.chromisstock.provider";
    private static String URI_CHANGESPATH = "changes";

    static
    {
        sURIMatcher.addURI( URI_AUTHORITY, URI_CHANGESPATH, URI_CHANGES);
        sURIMatcher.addURI( URI_AUTHORITY, URI_CHANGESPATH + "/#", URI_CHANGES_ID);
    }

    public StockContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri url)
    {
        int match = sURIMatcher.match(url);
        switch (match)
        {
            case URI_CHANGES:
                return "vnd.android.cursor.dir/vnd.com.app_software.chromisstock.provider.changes";
            case URI_CHANGES_ID:
                return "vnd.android.cursor.item/vnd.com.app_software.chromisstock.provider.changes";
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Choose the query and a sort order based on the code returned for the incoming URI.
        switch (sURIMatcher.match(uri)) {
            case URI_CHANGES:
                if (TextUtils.isEmpty(sortOrder)) sortOrder = DatabaseHandler.CHANGES_ID + " ASC";
                break;

            case URI_CHANGES_ID:
            /*
             * Because this URI was for a single row, the _ID value part is
             * present. Get the last path segment from the URI; this is the _ID value.
             * Then, append the value to the WHERE clause for the query
             */
                selection = selection + DatabaseHandler.CHANGES_ID + " = " + uri.getLastPathSegment();
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented for URI " + uri.toString() );
        }

        // call the code to actually do the query
        DatabaseHandler db = DatabaseHandler.getInstance( this.getContext() );
        return db.getChangesCursor( selection, null, sortOrder );

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
