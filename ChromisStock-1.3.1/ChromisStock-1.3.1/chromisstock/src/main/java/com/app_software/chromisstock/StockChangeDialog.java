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

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.app_software.chromisstock.Data.StockProduct;

public class StockChangeDialog extends DialogFragment {
    private static final String TAG = "StockChangeDialog";

    public static final String ARG_PRODUCTID = StockProduct.ID;
    public static final String ARG_FIELD = DatabaseHandler.CHANGES_FIELD;
    public static final String ARG_FIELD_LABEL = DatabaseHandler.CHANGES_FIELD + "_LABEL";
    public static final String ARG_CHANGETYPE = DatabaseHandler.CHANGES_TYPE;
    public static final String ARG_VALUE = DatabaseHandler.CHANGES_TEXTVALUE;

    private Long m_ProductID;
    private String m_Field;
    private String m_FieldLabel;
    private String m_Value;
    private int m_ChangeType;
    private boolean m_bAllowAdjust;
    private boolean m_bIsNumber;
    private String m_ChromisID;

    private TextView m_txtProductName;
    private TextView m_txtField;
    private RadioButton m_rbIncrease;
    private RadioButton m_rbDecrease;
    private RadioButton m_rbSetValue;
    private EditText m_editNewValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if( args != null ) {
            setArguments( args );
        }
    }

    @Override
    public void setArguments(Bundle args) {

        if (args.containsKey(ARG_PRODUCTID)) {
            m_ProductID = args.getLong(ARG_PRODUCTID);
        }
        if (args.containsKey(ARG_FIELD)) {
            m_Field = args.getString(ARG_FIELD);
            m_bAllowAdjust = (m_Field.compareTo( StockProduct.QTY_INSTOCK ) == 0);

            m_bIsNumber = m_bAllowAdjust ||
                    (m_Field.compareTo( StockProduct.PRICEBUY ) == 0) ||
                    (m_Field.compareTo( StockProduct.PRICESELL ) == 0) ||
                    (m_Field.compareTo( StockProduct.QTY_MAX ) == 0) ||
                    (m_Field.compareTo( StockProduct.QTY_MIN ) == 0) ;

        }
        if (args.containsKey(ARG_FIELD_LABEL)) {
            m_FieldLabel = args.getString(ARG_FIELD_LABEL);

        }
        if (args.containsKey(ARG_CHANGETYPE)) {
            m_ChangeType = args.getInt(ARG_CHANGETYPE);
        }
        if (args.containsKey(ARG_VALUE)) {
            m_Value = args.getString(ARG_VALUE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View v = inflater.inflate(R.layout.dialog_stock_change, null);

        // Set information given in the Intent
        m_txtProductName = (TextView) v.findViewById(R.id.txtProductName);
        m_txtField = (TextView) v.findViewById(R.id.txtField);
        m_editNewValue = (EditText) v.findViewById(R.id.editNewValue);
        m_rbIncrease = (RadioButton) v.findViewById(R.id.rbIncrease);
        m_rbDecrease = (RadioButton) v.findViewById(R.id.rbDecrease);
        m_rbSetValue = (RadioButton) v.findViewById(R.id.rbSetValue);

        m_rbIncrease.setEnabled( m_bAllowAdjust );
        m_rbDecrease.setEnabled( m_bAllowAdjust );

        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );
        StockProduct product = db.getProduct(m_ProductID, true);
        if( product == null ) {
            Log.e(TAG, "Product not found");
            m_txtProductName.setText("DATABASE ERROR!!");
        } else {

            m_ChromisID = product.getChromisId();
            String name;
            if( TextUtils.isEmpty(product.getValueString(StockProduct.NAME)) ) {
                name = getResources().getString( R.string.change_newproduct );
            } else {
                name = product.getValueString(StockProduct.NAME);
            }
            m_txtProductName.setText(name);
            m_txtField.setText(m_FieldLabel);

            if (m_ChangeType == DatabaseHandler.CHANGETYPE_ADJUSTVALUE) {
                Double d = new Double(m_Value);
                if (d < 0) {
                    m_rbDecrease.setChecked(true);
                    d = d * -1;
                    m_Value = String.format("%.0f", d);
                } else {
                    m_rbIncrease.setChecked(true);
                }
            } else {
                m_rbSetValue.setChecked(true);
            }

            if (m_bIsNumber) {
                m_editNewValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else {
                m_editNewValue.setInputType(InputType.TYPE_CLASS_TEXT);
                m_editNewValue.setText(m_Value);
            }
            m_editNewValue.setHint(m_Value);
        }

        // Add action buttons
        builder.setView(v).setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Create a new change record
                createChangeRecord();
            }
        })
                .setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        return builder.create();
    }

    private void createChangeRecord() {
        DatabaseHandler db = DatabaseHandler.getInstance(getActivity());

        String value = m_editNewValue.getText().toString();

        int type = DatabaseHandler.CHANGETYPE_CHANGEVALUE;

        if (m_rbIncrease.isChecked()) {
            type = DatabaseHandler.CHANGETYPE_ADJUSTVALUE;
        } else if (m_rbDecrease.isChecked()) {
            type = DatabaseHandler.CHANGETYPE_ADJUSTVALUE;
            Double dv = new Double(m_editNewValue.getText().toString());
            dv = dv * -1;
            value = String.format("%.0f", dv);
        }

        db.addChange(m_ChromisID, type, m_Field, value, value);
    }
}
