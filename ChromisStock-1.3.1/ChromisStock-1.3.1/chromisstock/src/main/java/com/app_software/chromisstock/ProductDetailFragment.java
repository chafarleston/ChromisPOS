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
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.app_software.chromisstock.Data.StockProduct;
import com.google.zxing.integration.android.ScannerIntegrator;
import com.google.zxing.integration.android.ScannerResult;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * A fragment representing a single Product detail screen.
 * This fragment is either contained in a {@link ProductListActivity}
 * in two-pane mode (on tablets) or a {@link ProductDetailActivity}
 * on handsets.
 */
public class ProductDetailFragment extends Fragment implements  DatabaseHandler.DataChangeNotify {
    static final String TAG = "ProductDetailFragment";

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public static final String ARG_ITEM_ID = StockProduct.ID;

    /**
     * The content this fragment is presenting.
     */
    private StockProduct mItem;
    private String m_LastTaxCat;
    private String m_LastCategory;
    private DatabaseHandler m_db;
    private Spinner m_taxSpinner;
    private Spinner m_categorySpinner;
    private ImageView m_image;
    private TextView m_txt_location;

    private EditText m_edit_name;
    private EditText m_edit_reference;
    private EditText m_edit_barcode;
    private EditText m_edit_instock;
    private EditText m_edit_minqty;
    private EditText m_edit_maxqty;
    private EditText m_edit_pricebuy;
    private EditText m_edit_pricesell;

    private TaxCatAdapter m_taxAdaptor;
    private CategoryAdapter m_categoryAdaptor;

    private boolean m_nameChecked = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProductDetailFragment() {
    }

    public void setDatabase( DatabaseHandler db ) {
        m_db = db;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            if( m_db == null ) {
                m_db = DatabaseHandler.getInstance( getActivity() );
            }

            Long id = getArguments().getLong(ARG_ITEM_ID);

            mItem = m_db.getProduct( id, true );
        }

        // We are interested in database changes
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );
        db.addChangeNotify(this);
    }

    @Override
    public void onDestroy() {
        // No longer interested in database changes
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );
        db.removeChangeNotify( this );

        super.onDestroy();
    }

    @Override
    public void NotifyDataChanged(  int action,  String chromisID  ) {
        switch( action ) {
            case DatabaseHandler.CHANGENOTIFY_RESET:
                // Best to die
                getActivity().finish();
            break;
            case DatabaseHandler.CHANGENOTIFY_CHANGEPRODUCT:
                if( mItem.getChromisId().compareTo( chromisID ) == 0 ) {
                    // Refetch our data
                    mItem = m_db.getProduct( chromisID, true );
                    setViewsContent();
                }
                break;
            case DatabaseHandler.CHANGENOTIFY_DELETPRODUCT:
                if( mItem.getChromisId().compareTo( chromisID ) == 0 ) {
                    getActivity().finish();
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_product_detail, container, false);


        m_taxSpinner = ((Spinner) rootView.findViewById(R.id.spn_taxcat));
        m_categorySpinner = ((Spinner) rootView.findViewById(R.id.spn_category));
        m_image = (ImageView) rootView.findViewById(R.id.iv_image);
        m_image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        m_txt_location = (TextView) rootView.findViewById(R.id.txt_location);

        m_edit_name = (EditText) rootView.findViewById(R.id.edit_name);
        m_edit_reference = (EditText) rootView.findViewById(R.id.edit_reference);
        m_edit_barcode = (EditText) rootView.findViewById(R.id.edit_barcode);
        m_edit_instock = (EditText) rootView.findViewById(R.id.edit_instock);
        m_edit_minqty = (EditText) rootView.findViewById(R.id.edit_minqty);
        m_edit_maxqty = (EditText) rootView.findViewById(R.id.edit_maxqty);
        m_edit_pricebuy = (EditText) rootView.findViewById(R.id.edit_pricebuy);
        m_edit_pricesell = (EditText) rootView.findViewById(R.id.edit_pricesell);

        m_taxAdaptor = new TaxCatAdapter( m_db.getTaxCats() );
        m_taxSpinner.setAdapter(m_taxAdaptor);
        m_taxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                DatabaseHandler.TaxCat item = (DatabaseHandler.TaxCat) arg0.getItemAtPosition(arg2);
                if (item != null) {
                    if (TextUtils.isEmpty(m_LastTaxCat) || m_LastTaxCat.compareTo(item.ChromisId) != 0) {
                        m_db.addChange(mItem.getChromisId(), DatabaseHandler.CHANGETYPE_CHANGEVALUE, StockProduct.TAXCAT, item.ChromisId, item.Name);
                        m_LastTaxCat = item.ChromisId;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        m_categoryAdaptor = new CategoryAdapter(m_db.getCategories());
        m_categorySpinner.setAdapter(m_categoryAdaptor);
        m_categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                DatabaseHandler.Category item = (DatabaseHandler.Category) arg0.getItemAtPosition(arg2);
                if (item != null) {
                    if (TextUtils.isEmpty(m_LastCategory) || m_LastCategory.compareTo(item.ChromisId) != 0) {
                        m_db.addChange(mItem.getChromisId(), DatabaseHandler.CHANGETYPE_CHANGEVALUE, StockProduct.CATEGORY, item.ChromisId, item.Name);
                        m_LastCategory = item.ChromisId;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        m_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dispatchTakePictureIntent();
            }
        });

        rootView.findViewById(R.id.ibAddStock).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });

        rootView.findViewById(R.id.ibBarcode).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onScanItem(v);
            }
        });

        m_edit_instock.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        rootView.findViewById(R.id.edit_name).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_reference.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_barcode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_minqty.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_maxqty.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_pricebuy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });
        m_edit_pricesell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onEditItem(v);
            }
        });

        setViewsContent();

        return rootView;
    }

    private void setViewsContent() {

        if (mItem != null) {
            m_edit_name.setText(mItem.getValueString(StockProduct.NAME));
            m_edit_reference.setText(mItem.getValueString(StockProduct.REFERENCE));
            m_edit_barcode.setText(mItem.getValueString(StockProduct.CODE));
            m_edit_instock.setText(String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_INSTOCK)));
            m_edit_minqty.setText( String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MIN)) );
            m_edit_maxqty.setText( String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MAX)) );
            m_edit_pricebuy.setText(String.format("%.2f", mItem.getValueDouble(StockProduct.PRICEBUY)));
            m_edit_pricesell.setText(String.format("%.2f", mItem.getValueDouble(StockProduct.PRICESELL)));

            DatabaseHandler.Location loc = m_db.getLocation(mItem.getValueString(StockProduct.LOCATION ));
            if( loc != null ) {
                m_txt_location.setText(loc.Name);
            }

            m_LastTaxCat = mItem.getValueString(StockProduct.TAXCAT);
            m_taxSpinner.setSelection(m_taxAdaptor.getPosition((m_LastTaxCat)));

            m_LastCategory = mItem.getValueString(StockProduct.CATEGORY);
            m_categorySpinner.setSelection(m_categoryAdaptor.getPosition((m_LastCategory)));

            byte [] image = mItem.getValueByteArray(StockProduct.IMAGE);
            if( image != null ) {
                Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                m_image.setImageBitmap(bmp);
            }

            // Force entry of name field
            if( !m_nameChecked && TextUtils.isEmpty( mItem.getValueString( StockProduct.NAME)) ) {
                m_nameChecked = true;
                onEditItem(m_edit_name);
            }
        }

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bitmap = null;

                if (intent != null) {
                    Bundle extras = intent.getExtras();
                    bitmap = (Bitmap) extras.get("data");
                    m_image.setImageBitmap(bitmap);
                }

                if (bitmap != null) {
                    ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream1);
                    byte[] imageInByte = stream1.toByteArray();

                    m_db.addChange(mItem.getChromisId(), DatabaseHandler.CHANGETYPE_CHANGEVALUEBLOB,
                            StockProduct.IMAGE, imageInByte, getResources().getString(R.string.captured_image));

                }
            } else {

                ScannerResult scanResult = ScannerIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (scanResult != null) {
                    String code = scanResult.getContents();

                    if( !TextUtils.isEmpty(code) ) {
                        // is this an existing barcode ?
                        DatabaseHandler db = DatabaseHandler.getInstance( getContext() );
                        StockProduct product = db.lookupBarcode(code);
                        if (product != null) {
                            // Ask to load the product details activity
                            askDuplicateBarcode(product);
                        } else {
                            // Use the scanned data as the barcode
                            setBarcode(code);
                        }
                    }
                }
            }
       }
    }

    private void setBarcode( String code ) {
        DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
         db.addChange( mItem.getChromisId(),  DatabaseHandler.CHANGETYPE_CHANGEVALUE,
                StockProduct.CODE, code, code);
    }

    private void askDuplicateBarcode( StockProduct product ) {

        final Long duplicateID = product.getID();
        final String barcode = product.getValueString( StockProduct.CODE );

        AlertDialog.Builder alert = new AlertDialog.Builder( getActivity() );
        alert.setTitle(getResources().getString(R.string.dlg_barcode_exists_title));
        alert.setMessage(getResources().getString(R.string.dlg_barcode_exists_message));

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Bring up the existing product
                Intent detailIntent = new Intent(getActivity(), ProductDetailActivity.class);
                detailIntent.putExtra(ProductDetailFragment.ARG_ITEM_ID, duplicateID);
                startActivity(detailIntent);
            }
        });

        alert.setNeutralButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Use the scanned data as the barcode
                        setBarcode(barcode);
                    }
                });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        alert.show();

    }

    private void onScanItem( View v ) {
        ScannerIntegrator scanner = new ScannerIntegrator(this);
        scanner.initiateScan();
    }

    private void onEditItem( View v ) {
        Bundle args = new Bundle();
        boolean bDoDialog = true;

        switch( v.getId() ) {
            case R.id.ibAddStock:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.QTY_INSTOCK);
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_instock));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_ADJUSTVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, "0" );
            break;

            case R.id.edit_instock:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.QTY_INSTOCK);
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_instock));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_INSTOCK)));
                break;

            case R.id.edit_name:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.NAME );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_productname));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, mItem.getValueString(StockProduct.NAME));
                break;

            case R.id.edit_reference:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.REFERENCE );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_reference));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, mItem.getValueString(StockProduct.REFERENCE));
                break;

            case R.id.edit_barcode:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.CODE );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_barcode));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, mItem.getValueString(StockProduct.CODE));
                break;

            case R.id.edit_minqty:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.QTY_MIN );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_minqty));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MIN)));
                break;

            case R.id.edit_maxqty:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.QTY_MAX );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_maxqty));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, String.format("%.0f", mItem.getValueDouble(StockProduct.QTY_MAX)));
                break;

            case R.id.edit_pricebuy:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.PRICEBUY );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_pricebuy));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, String.format("%.2f", mItem.getValueDouble(StockProduct.PRICEBUY)));
                break;

            case R.id.edit_pricesell:
                args.putString(StockChangeDialog.ARG_FIELD, StockProduct.PRICESELL );
                args.putString(StockChangeDialog.ARG_FIELD_LABEL, getResources().getString(R.string.label_pricesell));
                args.putInt(StockChangeDialog.ARG_CHANGETYPE, DatabaseHandler.CHANGETYPE_CHANGEVALUE);
                args.putString(StockChangeDialog.ARG_VALUE, String.format("%.2f", mItem.getValueDouble(StockProduct.PRICESELL)));
                break;

            default:
                bDoDialog = false;
                break;
        }

        if( bDoDialog ) {
            // Create an instance of the change dialog fragment and show it
            StockChangeDialog dialog = new StockChangeDialog();

            args.putLong(StockChangeDialog.ARG_PRODUCTID, mItem.getID());

            dialog.setArguments(args);
            dialog.show(getFragmentManager(), "StockChangeDialog");
        }
    }

    private class TaxCatAdapter extends BaseAdapter implements SpinnerAdapter {

        private final List<DatabaseHandler.TaxCat> m_data;

        public TaxCatAdapter(List<DatabaseHandler.TaxCat> data) {
            this.m_data = data;
        }

        @Override
        public int getCount() {
            return m_data.size();
        }

        public int getPosition( String id ) {
            int pos = 0;

            if( TextUtils.isEmpty( id ) ) {
                return -1;
            }

            while (pos < m_data.size()) {
                if (id.compareTo(m_data.get(pos).ChromisId) == 0) {
                    break;
                }
                ++pos;
            }

            if( pos == m_data.size() ) {
                return -1;
            }

            return pos;
        }

        @Override
        public Object getItem(int position) {
            return m_data.get(position);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View recycle, ViewGroup parent) {
            TextView text;
            if (recycle != null) {
                // Re-use the recycled view here!
                text = (TextView) recycle;
            } else {
                // No recycled view, inflate the "original" from the platform:
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);

                text = (TextView) inflater.inflate(
                        R.layout.simple_list_1, parent, false);
            }

            text.setText(m_data.get(position).Name);
            return text;
        }
    }


    private class CategoryAdapter extends BaseAdapter implements SpinnerAdapter {

        private final List<DatabaseHandler.Category> m_data;

        public CategoryAdapter(List<DatabaseHandler.Category> data) {
            this.m_data = data;
        }

        @Override
        public int getCount() {
            return m_data.size();
        }

        @Override
        public Object getItem(int position) {
            return m_data.get(position);
        }

        public int getPosition( String id ) {
            int pos = 0;

            if( TextUtils.isEmpty( id ) ) {
                return -1;
            }

            while (pos < m_data.size()) {
                if (id.compareTo(m_data.get(pos).ChromisId) == 0) {
                    break;
                }
                ++pos;
            }

            if( pos == m_data.size() ) {
                return -1;
            }

            return pos;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View recycle, ViewGroup parent) {
            TextView text;
            if (recycle != null) {
                // Re-use the recycled view here!
                text = (TextView) recycle;
            } else {
                // No recycled view, inflate the "original" from the platform:
                LayoutInflater inflater = (LayoutInflater)
                        getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);

                text = (TextView) inflater.inflate(
                        R.layout.simple_list_1, parent, false);
            }

            text.setText(m_data.get(position).Name);
            return text;
        }
    }
    }
