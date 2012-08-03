package com.envsocial.android.features.order;

import java.util.Calendar;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class OrderFragment extends SherlockFragment implements OnClickListener {
	private static final String TAG = "OrderFragment";
	
	public static final int DIALOG_REQUEST = 0;
	
	private Location mLocation;
	
	private OrderFeature mOrderFeat;
	private Button mBtnOrder;
	OrderCatalogListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	/*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] ------------- onActivityCreated called -----------------");
		super.onActivityCreated(savedInstanceState);
	}
	*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] onCreateView called.");
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.catalog, container, false);
		
		mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
		mOrderFeat = (OrderFeature)mLocation.getFeature(Feature.ORDER);

		// Create custom expandable list adapter
		mAdapter = new OrderCatalogListAdapter(getActivity(),
				mOrderFeat.getOrderCategories(),
				R.layout.catalog_group,
				new String[] { OrderFeature.CATEGORY_NAME },
				new int[] { R.id.orderGroup },
				mOrderFeat.getOrderItems(),
				R.layout.catalog_item,
				new String[] { OrderFeature.ITEM_NAME },
				new int[] { R.id.orderItem }
				);
		
		
		// Set adapter
		ExpandableListView listView = (ExpandableListView) v.findViewById(R.id.catalog);
		listView.setAdapter(mAdapter);

		
		mBtnOrder = (Button) v.findViewById(R.id.btn_order);
		mBtnOrder.setOnClickListener(this);
	    
	    return v;
	}
	
	
	public void onClick(View v) {
		if (v == mBtnOrder) {
			OrderDialogFragment summaryDialog = 
				OrderDialogFragment.newInstance(mOrderFeat.getOrderCategories(), 
						mOrderFeat.getOrderItems(), mAdapter.getCounter());
			summaryDialog.setTargetFragment(this, DIALOG_REQUEST);
			summaryDialog.show(getFragmentManager(), "dialog");
		}
	}
	
	
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		dialog.dismiss();
		
		Annotation order = new Annotation(mLocation, 
				Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderTask(getActivity(), order).execute();
		
	}
	
}
