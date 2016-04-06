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

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

import com.app_software.chromisstock.Data.StockProduct;

/**
 * An activity representing a single Product detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ProductListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ProductDetailFragment}.
 */
public class ProductDetailActivity extends AppCompatActivity implements ChangesFragment.OnFragmentInteractionListener, DatabaseHandler.DataChangeNotify {
    String TAG = "ProductDetailActivity";

    @Override
    public void onDestroy() {

        // No longer interested in database changes
        DatabaseHandler db = DatabaseHandler.getInstance( this );
        db.removeChangeNotify( this );

        super.onDestroy();
    }

    private void createFragments() {
        Long productID = getIntent().getLongExtra(ProductDetailFragment.ARG_ITEM_ID, 0);

        // Create the detail fragment and add it to the activity
        Bundle detailArgs = new Bundle();
        detailArgs.putLong(ProductDetailFragment.ARG_ITEM_ID, productID );

        ProductDetailFragment newDetail = new ProductDetailFragment();
        newDetail.setArguments(detailArgs);

        if (findViewById(R.id.product_detail_container) != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.product_detail_container, newDetail)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.product_detail_container, newDetail)
                    .commit();
        }

        // Create the changes fragment and add it using a fragment transaction.
        DatabaseHandler db = DatabaseHandler.getInstance( this );

        Bundle changeArgs = new Bundle();
        StockProduct product = db.getProduct(productID, false);
        if( product == null) {
            Log.e( TAG, "Invalid Product ID" );
            return;
        }

        String id = product.getChromisId();

        changeArgs.putString(ChangesFragment.ARG_PRODUCT, id);
        ChangesFragment newChanges = new ChangesFragment();
        newChanges.setArguments(changeArgs);

        if (findViewById(R.id.product_changes_container) != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.product_changes_container, newChanges)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.product_changes_container, newChanges)
                    .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            createFragments();
        }

        // Ensure we get updates about database changes
        DatabaseHandler db = DatabaseHandler.getInstance( this );
        db.addChangeNotify(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, ProductListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Long product) {

    }

    @Override
    public void NotifyDataChanged( int action,  String chromisID ) {
        // Need to recreate the fragments to force a redraw
//  fragments now listen for updates themselves
//      createFragments();
    }
}
