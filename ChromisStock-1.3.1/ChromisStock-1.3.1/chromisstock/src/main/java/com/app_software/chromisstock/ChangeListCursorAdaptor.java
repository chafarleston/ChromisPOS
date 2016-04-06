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


import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.app_software.chromisstock.Data.StockProduct;

/**
 * Created by John on 21/09/2015.
 */
public class ChangeListCursorAdaptor extends CursorAdapter {

    private Context m_Context;

        public ChangeListCursorAdaptor(Context context, Cursor cursor) {
            super(context, cursor, 0);
            m_Context = context;
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.change_listitem, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvType = (TextView) view.findViewById(R.id.tvType);
            TextView tvField = (TextView) view.findViewById(R.id.tvField);
            TextView tvValue = (TextView) view.findViewById(R.id.tvValue);
            ImageButton ibDelete = (ImageButton) view.findViewById(R.id.ibDeleteChange);

            ibDelete.setTag( cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_ID)) );

            // Extract properties from cursor
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_TYPE));
            String field = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_FIELD));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHandler.CHANGES_DISPLAY));
            int sid = R.string.changetype_none;

            switch( type ) {
                case DatabaseHandler.CHANGETYPE_NEWRECORD:
                    sid = R.string.changetype_newrecord;
                    field = "";
                    value = "";
                    break;

                case DatabaseHandler.CHANGETYPE_CHANGEVALUEBLOB:
                case DatabaseHandler.CHANGETYPE_CHANGEVALUE:
                    sid = R.string.changetype_changevalue;
                    break;
                case DatabaseHandler.CHANGETYPE_NEWVALUEBLOB:
                case DatabaseHandler.CHANGETYPE_NEWVALUE:
                    sid = R.string.changetype_newvalue;
                    break;
                case DatabaseHandler.CHANGETYPE_ADJUSTVALUE:
                    sid = R.string.changetype_adjustvalue;
                    break;
            }

            if( field.compareTo( StockProduct.NAME) == 0 ) {
                field =  context.getResources().getString(R.string.change_name);
            } else if( field.compareTo( StockProduct.CODE) == 0 ) {
                field =  context.getResources().getString(R.string.change_barcode);
            } else if( field.compareTo( StockProduct.REFERENCE) == 0 ) {
                field =  context.getResources().getString(R.string.change_reference);
            } else if( field.compareTo( StockProduct.PRICEBUY) == 0 ) {
                field =  context.getResources().getString(R.string.change_buy);
            } else if( field.compareTo( StockProduct.PRICESELL) == 0 ) {
                field =  context.getResources().getString(R.string.change_sell);
            } else if( field.compareTo( StockProduct.QTY_MIN ) == 0 ) {
                field =  context.getResources().getString(R.string.change_min);
            } else if( field.compareTo( StockProduct.QTY_MAX) == 0 ) {
                field =  context.getResources().getString(R.string.change_max);
            } else if( field.compareTo( StockProduct.QTY_INSTOCK) == 0 ) {
                field =  context.getResources().getString(R.string.change_stock);
            } else if( field.compareTo( StockProduct.IMAGE) == 0 ) {
                field =  context.getResources().getString(R.string.change_image);
            } else if( field.compareTo( StockProduct.TAXCAT) == 0 ) {
                field =  context.getResources().getString(R.string.change_taxcat);
            } else if( field.compareTo( StockProduct.CATEGORY) == 0 ) {
                field =  context.getResources().getString(R.string.change_category);
            }

            ibDelete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    askDeleteRecord((Long) v.getTag());
                }
            });

            // Populate fields with extracted properties
            tvType.setText( context.getResources().getString(sid) );
            tvField.setText(field);
            tvValue.setText(value);
        }

        private Long m_currentRecord;

        private void askDeleteRecord( Long recordID ) {
            m_currentRecord = recordID;

            DatabaseHandler db = DatabaseHandler.getInstance(m_Context);

            if (db.isNewProductRecord(recordID)) {

                AlertDialog.Builder alert = new AlertDialog.Builder(m_Context);
                alert.setTitle(m_Context.getResources().getString(R.string.dlg_delrec_title));
                alert.setMessage(m_Context.getResources().getString(R.string.dlg_delrec_message));

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DatabaseHandler db = DatabaseHandler.getInstance(m_Context);
                        db.deleteChange(m_currentRecord);
                    }
                });

                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });

                alert.show();
            } else {
                db.deleteChange( m_currentRecord );
            }

        }
}
