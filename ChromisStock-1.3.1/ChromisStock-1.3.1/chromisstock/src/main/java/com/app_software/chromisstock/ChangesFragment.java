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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class ChangesFragment extends ListFragment implements AbsListView.OnItemClickListener, DatabaseHandler.DataChangeNotify{

    // the fragment initialization parameters
    public static final String ARG_PRODUCT = "product";

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;
    private String      m_Product;

    public static ChangesFragment newInstance( String product ) {
        ChangesFragment fragment = new ChangesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT, product);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChangesFragment() {
    }

    private void setNewListAdaptor() {
        ChangeListCursorAdaptor adaptor = null;
        DatabaseHandler db = DatabaseHandler.getInstance( getActivity() );

        Cursor curs = db.getChangesCursor( m_Product );
        if( curs == null ) {
            Toast.makeText(getContext(), "Database error - rebuilding", Toast.LENGTH_LONG).show();

            db.ReBuildProductTable( getContext() );
        } else {

            adaptor = new ChangeListCursorAdaptor(getActivity(), curs);
            setListAdapter(adaptor);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            m_Product = getArguments().getString(ARG_PRODUCT);
        }

        setNewListAdaptor();

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_changes, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction( new Long(id) );
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void NotifyDataChanged(int type, String chromisID) {
        switch( type ) {
            case DatabaseHandler.CHANGENOTIFY_RESET:
            case DatabaseHandler.CHANGENOTIFY_DELETPRODUCT:
                // we will be killed - ignore here
                break;
            case DatabaseHandler.CHANGENOTIFY_CHANGEPRODUCT:
                if( m_Product.compareTo( chromisID ) == 0 ) {
                    // Refetch our data
                    setNewListAdaptor();
                }
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Long product);
    }

}
