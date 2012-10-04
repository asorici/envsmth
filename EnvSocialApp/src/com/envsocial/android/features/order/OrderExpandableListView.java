package com.envsocial.android.features.order;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

public class OrderExpandableListView extends ExpandableListView {

	public OrderExpandableListView(Context context) {
		super(context);
	}
	
	public OrderExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OrderExpandableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
	}
	
	public Parcelable onSaveInstanceState() {
		// we can do this because we know it won't be used in any other place / way
		OrderCatalogListAdapter adapter = (OrderCatalogListAdapter)getExpandableListAdapter();
		
		return null;
	}
}
