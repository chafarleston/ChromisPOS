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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.app_software.chromisstock.Data.StockProduct;

/**
 * Created by John on 21/09/2015.
 */
public class ProductListCursorAdaptor extends CursorAdapter {

        Context m_Context;

        public ProductListCursorAdaptor(Context context, Cursor cursor ) {
            super(context, cursor, 0);
            m_Context = context;
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.product_listitem, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvID = (TextView) view.findViewById(R.id.tvID);
            TextView tvName = (TextView) view.findViewById(R.id.tvName);

            // Extract properties from cursor
            Long id = cursor.getLong(cursor.getColumnIndexOrThrow(StockProduct.ID));

            // get the rest from the database
            DatabaseHandler db = DatabaseHandler.getInstance(m_Context);
            StockProduct product = db.getProduct(id, true);

            if( product == null ) {
                tvName.setText( "DATABASE READ ERROR");
            } else {
                // Populate fields with extracted properties
                tvID.setText( product.getValueString(StockProduct.REFERENCE));
                tvName.setText(product.getValueString(StockProduct.NAME));

                int c = m_Context.getResources().getColor(R.color.normallistitemcolour);
                if( product.getValueBoolean( StockProduct.HASCHANGES ) ) {
                    c = m_Context.getResources().getColor(R.color.changedlistitemcolour);
                    if( db.isNewProduct(product.getChromisId()) ) {
                        c = m_Context.getResources().getColor(R.color.newlistitemcolour);
                    }
                }
                tvID.setTextColor(c);
                tvName.setTextColor(c);
            }
        }
    }
