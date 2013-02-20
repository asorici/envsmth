package com.envsocial.android.features.description;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.Url;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.SimpleCursorLoader;

public class BoothDescriptionProductsFragment extends SherlockFragment 
	implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
	
	private static final String TAG = "BoothDescriptionProductsFragment";
	private static final int PRODUCTS_LOADER = 1;
	private static boolean active = true;
	
	private BoothDescriptionActivity mParentActivity;
	private BoothDescriptionFeature mDescriptionFeature;
	private ProgressDialog mDescriptionLoaderDialog;
	
	private TextView mBoothNameView;
	private ListView mBoothProductListView;
	private SimpleCursorAdapter mBoothProductsListAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mParentActivity = (BoothDescriptionActivity) getActivity();
	    mDescriptionFeature = (BoothDescriptionFeature) mParentActivity.getFeature();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.description_booth_products, container, false);
		
		mBoothNameView = (TextView) view.findViewById(R.id.description_details_location);
		mBoothNameView.setText(mParentActivity.getFeatureLocation().getName());
		
		mBoothProductListView = (ListView) view.findViewById(R.id.description_product_list);
		
		// Create and set adapter
		String[] from = new String[] { 
				BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_NAME,
				BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_DESCRIPTION,
				BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_VOTES
		};

		int[] to = new int[] { 
				R.id.description_booth_product_name,
				R.id.description_booth_product_description_short,
				R.id.description_booth_product_votes
		};

		mBoothProductsListAdapter = new BoothDescriptionProductListAdapter(getActivity(),
				R.layout.description_booth_products_row, null, from, to, 0);
		mBoothProductListView.setAdapter(mBoothProductsListAdapter);
		mBoothProductListView.setOnItemClickListener(this);
		
		return view;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    
	    // start setup for data
	 	getLoaderManager().initLoader(PRODUCTS_LOADER, null, this);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		active = true;
	}
	
	@Override
	public void onPause() {
		super.onResume();
		active = false;
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy(); 
		
		if (mDescriptionLoaderDialog != null) {
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
	}
	
	private ProgressDialog getProgressDialogInstance(Context context) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setTitle("Retrieving data ...");
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		
		return dialog;
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		if (mDescriptionLoaderDialog == null && active) {
			//Log.d(TAG, "CREATING LOADER AND PROGRESS DIALOG");
			mDescriptionLoaderDialog = getProgressDialogInstance(getActivity());
			mDescriptionLoaderDialog.show();
		}
		
		return new BoothProductsCursorLoader(getActivity(), mDescriptionFeature);
	}
	
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mBoothProductsListAdapter.swapCursor(cursor);
		
		if (mDescriptionLoaderDialog != null) {
			//Log.d(TAG, "FINISHING LOADER AND PROGRESS DIALOG");
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mBoothProductsListAdapter.swapCursor(null);
		
		if (mDescriptionLoaderDialog != null) {
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
	}


	@Override
	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
		Cursor cursor = (Cursor) mBoothProductsListAdapter.getItem(position);
		
		if (cursor != null) {
			int productIdIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_ID);
			int productId = cursor.getInt(productIdIndex);
			
			Intent intent = new Intent(getActivity(), BoothDescriptionProductDetailsActivity.class);
			intent.putExtra("location", mParentActivity.getFeatureLocation());
			intent.putExtra(Feature.BOOTH_DESCRIPTION, mDescriptionFeature);
			intent.putExtra("product_id", productId);
			
			startActivity(intent);
		}
	}
	
	
	// ##################################### Helper static classes #####################################

	static class BoothProductsCursorLoader extends SimpleCursorLoader {
		private BoothDescriptionFeature mDescriptionFeature;

		public BoothProductsCursorLoader(Context context, BoothDescriptionFeature descriptionFeature) {
			super(context);
			mDescriptionFeature = descriptionFeature;
		}

		@Override
		public Cursor loadInBackground() {
			String descriptionFeatureUri = mDescriptionFeature.getResourceUri();
			int boothId = Integer.parseInt(Url.resourceIdFromUrl(descriptionFeatureUri));
			
			Cursor cursor = mDescriptionFeature.getAllProducts(boothId);
			return cursor;
		}
	}
}
