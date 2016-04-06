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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.app_software.chromisstock.Data.StockProduct;


/**
 * A list fragment representing a list of Products. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ProductDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ProductListFragment extends ListFragment implements  DatabaseHandler.DataChangeNotify
{

    private static String TAG = "ProductListFragment";

    public static final String ARG_SEARCH = "SEARCH";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private String m_Search;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProductListFragment() {
    }

    public void setSearch( String search ) {
        m_Search = search;
        Log.v( TAG, "New Search: " + search );

        setNewListAdaptor();
    }

    public String getSearch() {
        return m_Search;
    }

    private void setNewListAdaptor() {
        ProductListCursorAdaptor adaptor = null;
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );

        String select = null;
        String [] args = null;
        String partial = "%" + m_Search + "%";

        if ( !TextUtils.isEmpty( m_Search ) ) {
            select =  StockProduct.CODE + " = ? OR " + StockProduct.REFERENCE + " LIKE ? OR " + StockProduct.NAME + " LIKE ?";
            args = new String[]{m_Search, partial, partial};
        }

        Cursor curs = db.getProductListCursor(select, args, StockProduct.HASCHANGES + " DESC," + StockProduct.NAME);
        if( curs == null ) {
            Toast.makeText( getContext(), "Database error - rebuilding", Toast.LENGTH_LONG).show();

            db.ReBuildProductTable(getContext());
        } else {

            adaptor = new ProductListCursorAdaptor(getActivity(), curs);
            setListAdapter(adaptor);
        }

        String noItems = null;
        if( adaptor == null ) {
            noItems = getString(R.string.no_items);
        } else if( adaptor.getCount() == 0 ) {
            // Set an empty list
            if( !TextUtils.isEmpty( m_Search ) ) {
                noItems = getString(R.string.no_match) + " " + m_Search;
            } else {
                if( db.isFetchingData()) {
                    noItems = getString(R.string.fetching_data);
                } else {
                    noItems = getString(R.string.no_items);
                }
            }
        }

        if( !TextUtils.isEmpty( noItems ) ) {
            setListAdapter(new ArrayAdapter(getActivity(), R.layout.product_listitem));
            setEmptyText(noItems);
        }

    }

    @Override
    public void NotifyDataChanged( int action,  String chromisID ) {

        // Recreate list if database changed
        setNewListAdaptor();
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected( Long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sCallbacks = new Callbacks() {
        @Override
        public void onItemSelected( Long id) {
        }
    };

    @Override
    public void onDestroy() {
        // No longer interested in database changes
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );
        db.removeChangeNotify( this );

        super.onDestroy();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            m_Search = savedInstanceState.getString("m_Search");
            Log.d(TAG, "onCreate: Restored search " + m_Search);
        } else {

            if (getArguments() != null) {
                if (getArguments().containsKey(ARG_SEARCH)) {
                    m_Search = getArguments().getString(ARG_SEARCH);
                }
            }
        }

        // We are interested in database changes
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );
        db.addChangeNotify(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        mCallbacks.onItemSelected(new Long(id));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }

        outState.putString("m_Search", m_Search);
        Log.d(TAG, "Saved search " + m_Search);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            m_Search = savedInstanceState.getString("m_Search");
            Log.d(TAG, "onActivityCreated: Restored search " + m_Search);
        }

        setNewListAdaptor();
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        if( getListAdapter() != null ) {
            // When setting CHOICE_MODE_SINGLE, ListView will automatically
            // give items the 'activated' state when touched.
            getListView().setChoiceMode(activateOnItemClick
                    ? ListView.CHOICE_MODE_SINGLE
                    : ListView.CHOICE_MODE_NONE);
        }
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
