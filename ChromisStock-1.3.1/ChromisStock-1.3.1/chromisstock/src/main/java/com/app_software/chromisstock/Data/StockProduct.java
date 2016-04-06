package com.app_software.chromisstock.Data;

import android.database.Cursor;
import android.os.Bundle;

import com.app_software.chromisstock.DatabaseHandler;

import java.util.UUID;

/**
 * Created by John on 18/09/2015.
 */
public class StockProduct {
    public static final String ID = "_id";
    public static final String CHROMISID = "CHROMISID";
    public static final String NAME = "NAME";
    public static final String REFERENCE = "REFERENCE";
    public static final String CODE = "CODE";
    public static final String CATEGORY = "CATEGORY";
    public static final String LOCATION = "LOCATION";
    public static final String PRICEBUY = "PRICEBUY";
    public static final String PRICESELL = "PRICESELL";
    public static final String TAXCAT = "TAXCAT";
    public static final String QTY_INSTOCK = "QTY_INSTOCK";
    public static final String QTY_MAX = "QTY_MAX";
    public static final String QTY_MIN = "QTY_MIN";
    public static final String IMAGE = "IMAGE";
    public static final String HASCHANGES = "HASCHANGES";

    Bundle m_Values;

    // Empty constructor
    public StockProduct(){
        m_Values = new Bundle();
    }
    // constructor
    public StockProduct( Bundle values ){
        m_Values = new Bundle(values);
    }

    public Bundle getValues() {
        return new Bundle( m_Values );
    }

    public String getValueString( String key ) {

        return m_Values.getString( key );
    }

    public Double getValueDouble( String key ) {

        return m_Values.getDouble(key);
    }

    public Boolean getValueBoolean( String key ) {

        return m_Values.getBoolean(key);
    }

    public byte [] getValueByteArray( String key ) {

        return m_Values.getByteArray(key);
    }

    public int getValueInt( String key ) {

        return m_Values.getInt(key);
    }

    public String getChromisId() {
        String chromisid = m_Values.getString( CHROMISID );

        if( chromisid == null ) {
            // No ID so create a new unique uuid
            chromisid = UUID.randomUUID().toString();

            m_Values.putString(CHROMISID, chromisid);
        }

        return chromisid;
    }

    public Long getID() {
        Long id = m_Values.getLong(ID);

        return id;
    }

    public void setValues( Bundle values ){
        m_Values.putAll( values );
    }

    public void setValueString( String key, String value ){
        m_Values.putString(key, value);
    }

    public void setValueDouble( String key, Double value ){
        m_Values.putDouble(key, value);
    }

    public void setValueInt( String key, int value ){
        m_Values.putInt(key, value);
    }

    public void setValueByteArray( String key, byte [] value ) {
        m_Values.putByteArray(key, value);
    }
}
