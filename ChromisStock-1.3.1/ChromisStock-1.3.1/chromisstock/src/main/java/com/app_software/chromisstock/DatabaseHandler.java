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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.app_software.chromisstock.Data.StockProduct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Created by John on 18/09/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper implements DownloadResultReceiver.Receiver {

    String TAG = "DatabaseHandler";

    // Database Version
    private static final int DATABASE_VERSION = 6;

    // Database Name
    private static final String DATABASE_NAME = "ChromisStock";

    // Table names
    public static final String TABLE_PRODUCTS = "PRODUCTS";
    public static final String TABLE_CHANGES = "CHANGES";
    public static final String TABLE_LOCATIONS = "LOCATIONS";
    public static final String TABLE_CATEGORIES = "CATEGORIES";
    public static final String TABLE_TAXES = "TAXES";

    public static final String CHANGES_ID = "_id";
    public static final String CHANGES_PRODUCT = "PRODUCTID";
    public static final String CHANGES_TYPE = "CHANGETYPE";
    public static final String CHANGES_FIELD = "FIELD";
    public static final String CHANGES_DISPLAY = "DISPLAY";
    public static final String CHANGES_TEXTVALUE = "TEXTVALUE";
    public static final String CHANGES_BLOBVALUE = "BLOBVALUE";

    public static final int CHANGETYPE_NONE  = 0;
    public static final int CHANGETYPE_ADJUSTVALUE = 1;
    public static final int CHANGETYPE_CHANGEVALUE = 2;
    public static final int CHANGETYPE_CHANGEVALUEBLOB = 3;
    public static final int CHANGETYPE_NEWVALUE = 4;
    public static final int CHANGETYPE_NEWVALUEBLOB = 5;
    public static final int CHANGETYPE_NEWRECORD = 6;

    public static final String LOCATION_ID = "_id";
    public static final String LOCATION_CHROMISID = "CHROMISID";
    public static final String LOCATION_NAME = "NAME";

    public static final String CATEGORY_ID = "_id";
    public static final String CATEGORY_CHROMISID = "CHROMISID";
    public static final String CATEGORY_NAME = "NAME";

    public static final String TAXES_ID = "_id";
    public static final String TAXES_CHROMISID = "CHROMISID";
    public static final String TAXES_NAME = "NAME";

    private Context m_Context;
    private DownloadResultReceiver m_Receiver;
    private Toast m_toaster;
    private boolean bFetchingData;

    public boolean isFetchingData() {
        return bFetchingData;
    }

    public static final int CHANGENOTIFY_RESET = 1;
    public static final int CHANGENOTIFY_NEWPRODUCT = 2;
    public static final int CHANGENOTIFY_CHANGEPRODUCT = 3;
    public static final int CHANGENOTIFY_DELETPRODUCT = 4;

    public interface DataChangeNotify {
        public void NotifyDataChanged( int type, String chromisID );
    }

    private static DatabaseHandler mInstance = null;

    public static DatabaseHandler getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DatabaseHandler(ctx.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private DatabaseHandler(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        m_Context = ctx;
        bFetchingData = false;
    }


    private List<DataChangeNotify> m_NotifyList = new ArrayList<DataChangeNotify>();

    public void addChangeNotify(DataChangeNotify receiver) {
        m_NotifyList.remove(receiver);  // Avoid multiple registrations
        m_NotifyList.add( receiver );
    }

    public void removeChangeNotify(DataChangeNotify receiver) {
        m_NotifyList.remove(receiver);
    }

    protected void NotifyDataChanged( int action, String chromisID  ) {
        Iterator<DataChangeNotify> iterator = m_NotifyList.iterator();
        while (iterator.hasNext()) {
            iterator.next().NotifyDataChanged(action, chromisID);
        }
    }

    public interface DownloadProgressReceiver {
        public void DownloadProgressReceiver( String Msg, boolean bFinished );
    }

    private List<DownloadProgressReceiver> m_ProgressReceivers = new ArrayList<DownloadProgressReceiver>();
    public void addDownloadProgressReceiver(DownloadProgressReceiver receiver) {
        m_ProgressReceivers.add( receiver );
    }

    protected void NotifyDownloadProgress( String msg, boolean bfinished ) {
        Iterator<DownloadProgressReceiver> iterator = m_ProgressReceivers.iterator();
        while (iterator.hasNext()) {
            iterator.next().DownloadProgressReceiver(msg, bfinished);
        }
    }

    String[] m_ProductFields = new String [] {
            StockProduct.ID,
            StockProduct.CHROMISID,
            StockProduct.NAME,
            StockProduct.REFERENCE,
            StockProduct.CATEGORY,
            StockProduct.CODE,
            StockProduct.LOCATION,
            StockProduct.TAXCAT,
            StockProduct.PRICEBUY,
            StockProduct.PRICESELL,
            StockProduct.QTY_INSTOCK,
            StockProduct.QTY_MIN,
            StockProduct.QTY_MAX,
            StockProduct.IMAGE,
            StockProduct.HASCHANGES
    };


    private Bundle ProductFieldsToBundle( Cursor cursor ) {
        Bundle values = new Bundle();
        int index = 0;
        values.putLong(StockProduct.ID, cursor.getLong(index++));
        String chromisID = cursor.getString(index++);
        values.putString(StockProduct.CHROMISID, chromisID);
        values.putString(StockProduct.NAME, cursor.getString(index++));
        values.putString(StockProduct.REFERENCE, cursor.getString(index++));
        values.putString(StockProduct.CATEGORY, cursor.getString(index++));
        values.putString(StockProduct.CODE, cursor.getString(index++));
        values.putString(StockProduct.LOCATION, cursor.getString(index++));
        values.putString(StockProduct.TAXCAT, cursor.getString(index++));
        values.putDouble(StockProduct.PRICEBUY, cursor.getDouble(index++));
        values.putDouble(StockProduct.PRICESELL, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_INSTOCK, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_MIN, cursor.getDouble(index++));
        values.putDouble(StockProduct.QTY_MAX, cursor.getDouble(index++));
        values.putByteArray(StockProduct.IMAGE, cursor.getBlob(index++));
        values.putBoolean(StockProduct.HASCHANGES, cursor.getInt(index++) == 0 ? false : true);

        return values;
    }

    private ContentValues ProductBundleToContentValues( Bundle bundle ) {
        ContentValues values = new ContentValues();

        values.put(StockProduct.ID, bundle.getLong(StockProduct.ID));
        values.put(StockProduct.CHROMISID, bundle.getString(StockProduct.CHROMISID));
        values.put(StockProduct.NAME, bundle.getString(StockProduct.NAME));
        values.put(StockProduct.REFERENCE, bundle.getString(StockProduct.REFERENCE));
        values.put(StockProduct.CATEGORY, bundle.getString(StockProduct.CATEGORY));
        values.put(StockProduct.CODE, bundle.getString(StockProduct.CODE));
        values.put(StockProduct.LOCATION, bundle.getString(StockProduct.LOCATION));
        values.put(StockProduct.TAXCAT, bundle.getString(StockProduct.TAXCAT));
        values.put(StockProduct.PRICEBUY, bundle.getDouble(StockProduct.PRICEBUY));
        values.put(StockProduct.PRICESELL, bundle.getDouble(StockProduct.PRICESELL));
        values.put(StockProduct.QTY_INSTOCK, bundle.getDouble(StockProduct.QTY_INSTOCK));
        values.put(StockProduct.QTY_MIN, bundle.getDouble(StockProduct.QTY_MIN));
        values.put(StockProduct.QTY_MAX, bundle.getDouble(StockProduct.QTY_MAX));
        values.put(StockProduct.IMAGE, bundle.getByteArray(StockProduct.IMAGE));
        // NOTE that StockProduct.HASCHANGES not included - this field is generate on the fly

        return values;
    }

     // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Old versions - just recreate the tables
        if (oldVersion < DATABASE_VERSION) {
            Log.v(TAG, "Database upgrading from version " + oldVersion + " to version " + DATABASE_VERSION);
            dropTables(db);
            createTables(db);
        }
    }

    public void createTables(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        createProductTable( db );
        createChangesTable(db);
        createLocationTable(db);
        createTaxTable( db );
        createCategoryTable(db);
    }

    public void createLocationTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "("
                + LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LOCATION_CHROMISID + " TEXT,"
                + LOCATION_NAME + " TEXT"
                + ")";

        db.execSQL(CREATE_TABLE);

    }

    public void createTaxTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_TABLE = "CREATE TABLE " + TABLE_TAXES + "("
                + TAXES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TAXES_CHROMISID + " TEXT,"
                + TAXES_NAME + " TEXT"
                + ")";

        db.execSQL(CREATE_TABLE);

    }

    public void createCategoryTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CATEGORY_CHROMISID + " TEXT,"
                + CATEGORY_NAME + " TEXT"
                + ")";

        db.execSQL(CREATE_TABLE);

    }

    public void createChangesTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_CHANGES_TABLE = "CREATE TABLE " + TABLE_CHANGES + "("
                + CHANGES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CHANGES_PRODUCT + " TEXT,"
                + CHANGES_TYPE + " INTEGER,"
                + CHANGES_FIELD + " TEXT,"
                + CHANGES_DISPLAY + " TEXT,"
                + CHANGES_TEXTVALUE + " TEXT, "
                + CHANGES_BLOBVALUE + " BLOB "
                + ")";

        db.execSQL(CREATE_CHANGES_TABLE);

    }

    public void createProductTable(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + StockProduct.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"  // Local ID - different from ID used in Chromis db
                + StockProduct.CHROMISID + " TEXT,"
                + StockProduct.NAME + " TEXT,"
                + StockProduct.REFERENCE + " TEXT,"
                + StockProduct.CATEGORY + " TEXT,"
                + StockProduct.CODE + " TEXT,"
                + StockProduct.LOCATION + " TEXT,"
                + StockProduct.TAXCAT + " TEXT,"
                + StockProduct.PRICEBUY + " DOUBLE,"
                + StockProduct.PRICESELL + " DOUBLE,"
                + StockProduct.QTY_INSTOCK + " DOUBLE, "
                + StockProduct.QTY_MIN + " DOUBLE, "
                + StockProduct.QTY_MAX + " DOUBLE, "
                + StockProduct.IMAGE + " BLOB, "
                + StockProduct.HASCHANGES + " INTEGER "
                + ")";

        db.execSQL(CREATE_PRODUCTS_TABLE);

    }

    public void dropTables(SQLiteDatabase db) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        // Drop tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHANGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAXES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
    }

    public void emptyTables( SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        dropTables(db);
        createTables(db);
    }

    public void emptyTables() {
        emptyTables(null);
        NotifyDataChanged(CHANGENOTIFY_RESET, null);
    }

    public void emptyProductTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        createProductTable(db);
    }

    public void emptyLocationTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        createLocationTable(db);
    }

    public void emptyTaxesTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAXES);
        createTaxTable(db);
    }

    public void emptyCategoryTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        createCategoryTable(db);
    }

    private void showToast( String Msg, int duration ) {
        if (m_toaster != null) {
            m_toaster.cancel();
        }

        m_toaster = Toast.makeText( m_Context, Msg, duration );
        m_toaster.show();
    }

    public void ReBuildProductTable( Context ctx,  SQLiteDatabase db ) {

        if( db == null ) {
            db = this.getWritableDatabase();
        }

        showToast("Database Download Started", Toast.LENGTH_SHORT);
        bFetchingData = true;

        emptyProductTable();
        NotifyDataChanged(CHANGENOTIFY_RESET, null);

        m_Receiver = new DownloadResultReceiver(new Handler());
        m_Receiver.setReceiver(this);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
        String connection = SP.getString("database_url", null);
        String user = SP.getString("database_user", null );
        String pwd = SP.getString("database_password", null );
        String location = SP.getString("location", null );

        if( connection == null || user == null || pwd == null ) {
            showToast ("Missing connection settings", Toast.LENGTH_LONG );

            bFetchingData = false;

            // Fire the settings activity
            Intent intent = new Intent( ctx, SettingsActivity.class);
            intent.addFlags( intent.FLAG_ACTIVITY_NEW_TASK );
            m_Context.startActivity( intent );

        } else {
            DownloadStockData.startActionDownloadData( ctx, m_Receiver, connection, user, pwd, location);
        }

    }

    public void testConnection( Context ctx  ) {

        showToast("Testing connection", Toast.LENGTH_SHORT);
        SQLiteDatabase db = this.getWritableDatabase();

        m_Receiver = new DownloadResultReceiver(new Handler());
        m_Receiver.setReceiver(this);


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);
        String connection = SP.getString("database_url", null );
        String user = SP.getString("database_user", null );
        String pwd = SP.getString("database_password", null);

        DownloadStockData.startActionTestConnect(ctx, m_Receiver, connection, user, pwd);
    }

    public void ReBuildProductTable( Context ctx  ) {
        ReBuildProductTable(ctx, null);
    }

        @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        int msgID = R.string.dbstatus_unknown;
        boolean bComplete = false;

        if( resultCode == DownloadStockData.STATUS_DOWNLOAD_FINISHED ) {
            // Need to notify all interested parties the DB content has changed
            msgID = R.string.dbstatus_download_complete;
            bComplete = true;
            bFetchingData = false;
            NotifyDataChanged( CHANGENOTIFY_RESET, null );
        } else if( resultCode == DownloadStockData.STATUS_CONNECTION_OK ) {
            msgID = R.string.dbstatus_connection_ok;
            bComplete = true;
        } else if( resultCode == DownloadStockData.STATUS_ERROR ) {
            msgID = R.string.dbstatus_communication_error;
            Log.e(TAG, "DB Communications failed " + resultData.toString());
            bComplete = true;
            bFetchingData = false;
            NotifyDataChanged( CHANGENOTIFY_RESET, null );
        } else if( resultCode == DownloadStockData.STATUS_RUNNING ) {
            bFetchingData = true;
            msgID = R.string.dbstatus_coomunicating;
        }

        String msg = m_Context.getResources().getString(msgID);
            showToast(msg, Toast.LENGTH_LONG);
        NotifyDownloadProgress(msg, bComplete);
    }

    // Adding new location
    public void addLocation( String chromisId, String name ) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( LOCATION_CHROMISID, chromisId );
        values.put( LOCATION_NAME, name );

        // Inserting Row
        db.insert(TABLE_LOCATIONS, null, values);
        db.close(); // Closing database connection
    }

    // Adding new tax
    public void addTax( String chromisId, String name ) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TAXES_CHROMISID, chromisId);
        values.put(TAXES_NAME, name);

        // Inserting Row
        db.insert(TABLE_TAXES, null, values);
        db.close(); // Closing database connection
    }

    // Adding new category
    public void addCategory( String chromisId, String name ) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( CATEGORY_CHROMISID, chromisId );
        values.put( CATEGORY_NAME, name );

        // Inserting Row
        db.insert(TABLE_CATEGORIES, null, values);
        db.close(); // Closing database connection
    }

    // Adding new product
    public Long createProduct( Bundle values ) {

        if( values == null ) {
            values = new Bundle();
        }

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(m_Context);


        if( values.containsKey(StockProduct.CHROMISID) == false ) {
            values.putString(StockProduct.CHROMISID, UUID.randomUUID().toString() );
        }
        if( values.containsKey(StockProduct.NAME) == false ) {
            values.putString(StockProduct.NAME, "" );
        }
        if( values.containsKey(StockProduct.LOCATION) == false ) {
            values.putString(StockProduct.LOCATION, SP.getString("location", null) );
        }
        if( values.containsKey(StockProduct.REFERENCE) == false ) {
            values.putString(StockProduct.REFERENCE, "" );
        }
        if( values.containsKey(StockProduct.CODE) == false ) {
            values.putString(StockProduct.CODE, "" );
        }
        if( values.containsKey(StockProduct.CATEGORY) == false ) {
            values.putString(StockProduct.CATEGORY, "" );
        }
        if( values.containsKey(StockProduct.PRICEBUY) == false ) {
            values.putDouble(StockProduct.PRICEBUY, (double) 0);
        }
        if( values.containsKey(StockProduct.PRICESELL) == false ) {
            values.putDouble(StockProduct.PRICESELL, (double) 0 );
        }
        if( values.containsKey(StockProduct.QTY_INSTOCK) == false ) {
            values.putDouble(StockProduct.QTY_INSTOCK, (double) 0 );
        }
        if( values.containsKey(StockProduct.QTY_MAX) == false ) {
            values.putDouble(StockProduct.QTY_MAX, (double) 0 );
        }
        if( values.containsKey(StockProduct.QTY_MIN) == false ) {
            values.putDouble(StockProduct.QTY_MIN, (double) 0 );
        }

        StockProduct product = new StockProduct( values );
        addProduct( product, false, false );

        // Get the product ID for the new record
       product = getProduct( product.getChromisId(), false );

        // Add a stock level adjustment change
        addChange(product.getChromisId(), CHANGETYPE_NEWRECORD, StockProduct.ID, product.getID().toString(), "");

        NotifyDataChanged( CHANGENOTIFY_NEWPRODUCT,  product.getChromisId() );

        return product.getID();
    }

    public void addProduct(StockProduct product, boolean bKeepChanges, boolean bNoNotify ) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (bKeepChanges) {
            // We are to retain any change records so apply them to this product before saving
            applyChanges(product);
        } else {
            deleteChanges(product.getValueString(StockProduct.CHROMISID));
        }

        ContentValues values = ProductBundleToContentValues(product.getValues());
        values.remove(StockProduct.ID);
        String chromisID = values.getAsString(StockProduct.CHROMISID);

        // Inserting Row
        db.insert(TABLE_PRODUCTS, null, values);
        db.close(); // Closing database connection

        if (!bNoNotify) {

            // Get the product ID for the new record
            product = getProduct( chromisID, false );

            NotifyDataChanged( CHANGENOTIFY_NEWPRODUCT,  product.getChromisId() );
        }
    }

    // Getting single product by barcode
    public StockProduct lookupBarcode( String barcode ) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.CODE + "=?", new String[]{ barcode }, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(ProductFieldsToBundle(cursor));
            }

            cursor.close();
        }

        return product;
    }

    // Getting single product by database ID
    public StockProduct getProduct( Long id, boolean withChanges) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.ID + "=?", new String[]{id.toString()}, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(ProductFieldsToBundle(cursor));
            }

            cursor.close();

            if( withChanges && product != null ) {
                applyChanges( product );
            }
        }

        return product;
    }

    // Getting single product by Chromis ID
    public StockProduct getProduct( String id, boolean withChanges ) {
        StockProduct product = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, StockProduct.CHROMISID + "=?", new String[]{id}, null, null, null, null);

        if (cursor != null) {

            if( cursor.moveToFirst() ) {
                product = new StockProduct(ProductFieldsToBundle(cursor));
            }

            cursor.close();

            if( withChanges && product != null ) {
                applyChanges( product );
            }
        }

        return product;
    }

    // Getting All Products
    public List<StockProduct> getAllProducts() {

        List<StockProduct> productlist = new ArrayList<StockProduct>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, m_ProductFields, null, null, null, null, null, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    productlist.add(new StockProduct(ProductFieldsToBundle(cursor)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // return product list
        return productlist;
    }

    // Getting a minimal set of product attributes for a product list view
    public Cursor getProductListCursor( String Filter, String [] FilterArgs, String OrderBy ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_PRODUCTS,  new String [] {StockProduct.ID},
                    Filter, FilterArgs, null, null, OrderBy, null);

        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting selected Products
    public Cursor getProductCursor( String Filter, String [] FilterArgs, String OrderBy ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_PRODUCTS, m_ProductFields, Filter, FilterArgs, null, null, OrderBy, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting All Products
    public Cursor getProductCursor( ) {

        return (getProductCursor(null, null, null));
    }

    // Getting products Count
    public int getProductsCount() {
        int count = 0;

        String countQuery = "SELECT  * FROM " + TABLE_PRODUCTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }

        // return count
        return count;
    }

    // Updating single product
    public int updateProduct(StockProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = ProductBundleToContentValues(product.getValues());
        Long id = product.getID();
        values.remove(StockProduct.ID);

        // updating row
        int ret = db.update(TABLE_PRODUCTS, values, StockProduct.ID + " = ?",
                new String[]{id.toString()});

        NotifyDataChanged( CHANGENOTIFY_CHANGEPRODUCT,   product.getChromisId() );

        return ret;
    }

    // Deleting single product
    public void deleteProduct(StockProduct product) {

        SQLiteDatabase db = this.getWritableDatabase();
        Long id = product.getID();

        db.delete(TABLE_PRODUCTS, StockProduct.ID + " = ?",
                new String[]{id.toString()});

        NotifyDataChanged( CHANGENOTIFY_DELETPRODUCT,  product.getChromisId() );

    }

    // Deleting single product
    public void deleteProduct(String chromisID) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_PRODUCTS, StockProduct.CHROMISID + "=?",
                new String[]{chromisID});


        NotifyDataChanged( CHANGENOTIFY_DELETPRODUCT,  chromisID );

    }

    public boolean isNumberField( String field ) {

        if( TextUtils.isEmpty( field ) ) {
            return false;
        }

        if( field.compareTo( StockProduct.QTY_INSTOCK ) == 0 ||
                field.compareTo( StockProduct.PRICEBUY ) == 0 ||
                field.compareTo( StockProduct.QTY_MAX ) == 0 ||
                field.compareTo( StockProduct.QTY_MIN ) == 0 ||
                field.compareTo( StockProduct.PRICESELL ) == 0 ) {
            return true;
        } else {
            return false;
        }
    }

    // Add a stock level adjustment change
    public void addChange( String chromisID, int changeType, String field, String value,  String valueDisplay ) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( CHANGES_PRODUCT, chromisID );
        values.put(CHANGES_TYPE, changeType );
        values.put(CHANGES_FIELD, field );
        values.put(CHANGES_DISPLAY, valueDisplay );
        values.put(CHANGES_TEXTVALUE, value );

        // First delete any existing changes on the same field (over-write)
        db.execSQL( "DELETE FROM " + TABLE_CHANGES + " WHERE "
                + CHANGES_PRODUCT + "='" + chromisID + "' AND "
                + CHANGES_FIELD + "='" + field + "'"  );

        // updating row
        long ret = db.insert(TABLE_CHANGES, null, values);

        // set the HASCHANGES flag in the product table
        db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " +
                StockProduct.HASCHANGES + "=1 WHERE " +
                StockProduct.CHROMISID + "='" + chromisID + "'");

        // Set the HASCHANGES flag in the product table
        updateChangeFlag(chromisID, false, true);
    }

    // Add a stock level adjustment change
    public void addChange( String chromisID, int changeType, String field, byte [] value, String valueDisplay ) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put( CHANGES_PRODUCT, chromisID );
        values.put(CHANGES_TYPE, changeType );
        values.put(CHANGES_FIELD, field );
        values.put(CHANGES_DISPLAY, valueDisplay );
        values.put(CHANGES_BLOBVALUE, value );

        // First delete any existing changes on the same field (over-write)
        db.execSQL( "DELETE FROM " + TABLE_CHANGES + " WHERE "
                + CHANGES_PRODUCT + "='" + chromisID + "' AND "
                + CHANGES_FIELD + "='" + field + "'"  );

        // updating row
        long ret = db.insert(TABLE_CHANGES, null, values);

        // set the HASCHANGES flag in the product table
        db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " +
                StockProduct.HASCHANGES + "=1 WHERE " +
                StockProduct.CHROMISID + "='" + chromisID + "'");

        // Set the HASCHANGES flag in the product table
        updateChangeFlag(chromisID, false, true);
    }

    private double doubleOrNothing( String str ) {
        double dval = (double) 0;

        try {
            if( TextUtils.isEmpty(str) == false ) {
                dval = Double.valueOf(str);
            }
        } catch (NumberFormatException e) {
        }

        return dval;
    }

    // Apply any changes found in the Changes table to the given StockProduct
    public void applyChanges( StockProduct product ) {

        Cursor c = getChangesCursor(product.getValueString(StockProduct.CHROMISID));
        if( c != null ) {

            int colType = c.getColumnIndexOrThrow(CHANGES_TYPE);
            int colField = c.getColumnIndexOrThrow(CHANGES_FIELD);
            int colTextValue = c.getColumnIndexOrThrow(CHANGES_TEXTVALUE);
            int colBlobValue = c.getColumnIndexOrThrow(CHANGES_BLOBVALUE);

            while (c.moveToNext()) {
                int changeType = c.getInt(colType );
                String field =  c.getString( colField );

                switch(changeType ) {
                    case CHANGETYPE_CHANGEVALUE:
                    case CHANGETYPE_NEWVALUE:
                        if( isNumberField( field ) ) {
                            Double value = doubleOrNothing(c.getString(colTextValue));
                            product.setValueDouble( field, value);
                        } else {
                            product.setValueString( field, c.getString(colTextValue) );
                        }
                        break;
                    case CHANGETYPE_CHANGEVALUEBLOB:
                    case CHANGETYPE_NEWVALUEBLOB:
                        product.setValueByteArray(field, c.getBlob(colBlobValue));
                        break;

                    case CHANGETYPE_ADJUSTVALUE:
                        Double value = product.getValueDouble( field );  // Use field name in change record
                        value += doubleOrNothing(c.getString(colTextValue));                 // Use adjustment in change record
                        product.setValueDouble( field, value);
                        break;

                    default:
                        break;
                }
            }
            c.close();
        }
    }

    // Delete a specific change record
    public void deleteChange( Long changeID  ) {
        SQLiteDatabase db = this.getWritableDatabase();
        String chromisID = "";
        int changeType = CHANGETYPE_NONE;

        try {
            Cursor cursor = db.query(TABLE_CHANGES, new String [] { CHANGES_ID, CHANGES_PRODUCT, CHANGES_TYPE }, CHANGES_ID + "=" + changeID, null, null, null, null, null);

            if (cursor.moveToFirst() ) {
                chromisID = cursor.getString( cursor.getColumnIndex(CHANGES_PRODUCT) );
                changeType = cursor.getInt(  cursor.getColumnIndex(CHANGES_TYPE) );
            }
            cursor.close();

            if( changeType == CHANGETYPE_NEWRECORD ) {
                deleteChanges( chromisID );
                deleteProduct( chromisID );
            } else {
                String query = "DELETE FROM " + TABLE_CHANGES + " WHERE " + CHANGES_ID + "=" + changeID;
                db.execSQL(query);
            }
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        // Check the HASCHANGES flag in the product table
        updateChangeFlag(chromisID, true, false);

    }

    // Delete anychange records for this product
    public void deleteChanges( String chromisID  ) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            String query = "DELETE FROM " + TABLE_CHANGES + " WHERE " + CHANGES_PRODUCT + "='" + chromisID + "'";
            db.execSQL(query);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        // unset the HASCHANGES flag in the product table
        updateChangeFlag(chromisID, false, false);

    }

    public boolean isNewProductRecord( Long id ) {
        boolean isNew = false;

        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_CHANGES, new String[]{CHANGES_ID}, CHANGES_ID +
                    "=" + id + " AND " + CHANGES_TYPE + "=" + CHANGETYPE_NEWRECORD,
                    null, null, null, null, null);

            if( cursor.moveToFirst() ) {
                isNew = true;
            }

            cursor.close();
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return isNew;

    }

    public boolean isNewProduct( String chromisID ) {
        boolean isNew = false;

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            Cursor cursor = db.query(TABLE_CHANGES, new String[]{CHANGES_ID},
                    CHANGES_TYPE + "=" + CHANGETYPE_NEWRECORD + " AND " + CHANGES_PRODUCT + "='" + chromisID + "'",
                    null, null, null, null, null);

            if( cursor.moveToFirst() ) {
                isNew = true;
            }

            cursor.close();
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }
        return isNew;
    }

    public void updateChangeFlag( String chromisID, boolean check, boolean setTo ) {
        SQLiteDatabase db = this.getWritableDatabase();

        if( check ) {
            setTo = false;
            try {
                Cursor cursor = db.query(TABLE_CHANGES, new String[]{CHANGES_ID}, CHANGES_PRODUCT + "='" + chromisID + "'", null, null, null, null, null);

                if( cursor.moveToFirst() ) {
                    setTo = true;
                }

                cursor.close();
            } catch ( SQLiteException e) {
                Log.d(TAG, e.toString());
            }
        }

        // set the HASCHANGES flag in the product table
        String flag = setTo ? "1" : "0";

        db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " +
                StockProduct.HASCHANGES + "=" + flag + " WHERE " +
                StockProduct.CHROMISID + "='" + chromisID + "'"  );


        NotifyDataChanged( CHANGENOTIFY_CHANGEPRODUCT,  chromisID );
    }

    String[] m_ChangeFields = new String [] {
            CHANGES_ID,
            CHANGES_PRODUCT,
            CHANGES_TYPE,
            CHANGES_FIELD,
            CHANGES_DISPLAY,
            CHANGES_TEXTVALUE,
            CHANGES_BLOBVALUE
    };

    // Getting a cursor for any changes
    public Cursor getChangesCursor( String Filter, String [] FilterArgs, String OrderBy   ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CHANGES, m_ChangeFields, Filter, FilterArgs, null, null, OrderBy, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    // Getting a cursor for any changes on the given Chromis product id
    public Cursor getChangesCursor( String chromisProductID   ) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CHANGES, m_ChangeFields,
                    CHANGES_PRODUCT + "='" + chromisProductID + "'", null, null, null, null, null);
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return cursor;
    }

    public class Location {

        public Double Id;
        public String Name;
        public String ChromisId;

        public Location( Double id, String name, String chromisid ) {
            Id = id;
            Name = name;
            ChromisId = chromisid;
        }
    }

    public Location getLocation( String chromisID ) {
        Cursor cursor = null;
        Location l = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_LOCATIONS, new String [] { LOCATION_ID, LOCATION_NAME, LOCATION_CHROMISID }, LOCATION_CHROMISID + "='" + chromisID + "'", null, null, null, null, null);

            if( cursor.moveToFirst() ) {
                l = new Location( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
            }

            cursor.close();
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return l;
    }

    public List<Location> getLocations() {
        Cursor cursor = null;

        List<Location> list = new ArrayList<Location>();

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_LOCATIONS, new String [] {
                    LOCATION_ID, LOCATION_NAME, LOCATION_CHROMISID },
                    null, null, null, null, null, null);

            while( cursor.moveToNext() ) {
                Location l = new Location( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
                list.add( l );
            }
            cursor.close();

        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return list;
    }

    public class TaxCat {

        public Double Id;
        public String Name;
        public String ChromisId;

        public TaxCat( Double id, String name, String chromisid ) {
            Id = id;
            Name = name;
            ChromisId = chromisid;
        }
    }

    public TaxCat getTaxCat( String chromisID ) {
        Cursor cursor = null;
        TaxCat l = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_TAXES, new String [] { TAXES_ID, TAXES_NAME, TAXES_CHROMISID },  TAXES_CHROMISID + "='" + chromisID + "'", null, null, null, null, null);

            if( cursor.moveToNext() ) {
                l = new TaxCat( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
            }

            cursor.close();
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return l;
    }

    public List<TaxCat> getTaxCats() {
        Cursor cursor = null;

        List<TaxCat> list = new ArrayList<TaxCat>();

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_TAXES, new String [] {
                    TAXES_ID, TAXES_NAME, TAXES_CHROMISID },
                    null, null, null, null, null, null);

            while( cursor.moveToNext() ) {
                TaxCat l = new TaxCat( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
                list.add( l );
            }
            cursor.close();

        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return list;
    }

    public class Category {

        public Double Id;
        public String Name;
        public String ChromisId;

        public Category( Double id, String name, String chromisid ) {
            Id = id;
            Name = name;
            ChromisId = chromisid;
        }
    }

    public Category getCategory( String chromisID ) {
        Cursor cursor = null;
        Category l = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CATEGORIES, new String [] {
                    CATEGORY_ID, CATEGORY_NAME, CATEGORY_CHROMISID },
                    CATEGORY_CHROMISID + "='" + chromisID + "'", null, null, null, null, null);

            if( cursor.moveToNext() ) {
                l = new Category( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
            }

            cursor.close();
        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return l;
    }

    public List<Category> getCategories() {
        Cursor cursor = null;

        List<Category> list = new ArrayList<Category>();

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_CATEGORIES, new String [] { CATEGORY_ID, CATEGORY_NAME, CATEGORY_CHROMISID }, null, null, null, null, null, null);

            while( cursor.moveToNext() ) {
                Category l = new Category( cursor.getDouble(0), cursor.getString(1), cursor.getString(2) );
                list.add( l );
            }
            cursor.close();

        } catch ( SQLiteException e) {
            Log.d(TAG, e.toString());
        }

        return list;
    }
}
