package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.utils.UIUtils;
import com.viewpagerindicator.TitlePageIndicator;

public class OrderCatalogPagerAdapter extends PagerAdapter 
								implements OnClickListener, OnPageChangeListener, IOrderCatalogAdapter {
	
	private static final String TAG = "OrderCatalogPagerAdapter";
	private static final String LIST_EXPANDED_GROUPS_KEY = "listExpandedGroups";
	private static final String LIST_POSITION_KEY = "listPosition";
	private static final String ITEM_POSITION_KEY = "itemPosition";
	
	private static Map<Integer, String> mCatalogTitleMap;
	static {
		mCatalogTitleMap = new HashMap<Integer, String>();
		mCatalogTitleMap.put(0, OrderFeature.TYPE_DRINKS);
		mCatalogTitleMap.put(1, OrderFeature.TYPE_FOOD);
		mCatalogTitleMap.put(2, OrderFeature.TYPE_DESERT);
	}
	
	private OrderFragment mParentFragment;
	private TitlePageIndicator mTitlePageIndicator;
	private int mCurrentCatalogPage;
	
	private Map<Integer, OrderCatalogListAdapter> mCatalogListAdapterMap;
	private Map<Integer, Bundle> mCatalogListStateMap;
	
	
	public OrderCatalogPagerAdapter(OrderFragment parentFragment) {
		mParentFragment = parentFragment;
		mCatalogListAdapterMap = new HashMap<Integer, OrderCatalogListAdapter>();
		mCatalogListStateMap = new HashMap<Integer, Bundle>();
	}
	
	@Override
	public int getCount() {
		// we know there are 3 type (drinks, food, desert) in this order
		return 3;
	}

	@Override
	public Object instantiateItem(View collection, int position) {
        LayoutInflater inflater = (LayoutInflater) collection.getContext()
        		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.catalog_page, null);
        
        OrderFeature orderFeat = mParentFragment.getOrderFeature(); 
        OrderCatalogListAdapter	adapter = mCatalogListAdapterMap.get(position);
        
        if (adapter == null) {
        	String type = mCatalogTitleMap.get(position);
        	
	        // Create custom expandable list adapter
	        adapter = new OrderCatalogListAdapter(mParentFragment.getActivity(),
	        			orderFeat.getOrderCategories(type),
	     				R.layout.catalog_group,
	     				new String[] { OrderFeature.CATEGORY_NAME },
	     				new int[] { R.id.orderGroup },
	     				orderFeat.getOrderItems(type),
	     				R.layout.catalog_item,
	     				new String[] { OrderFeature.ITEM_NAME },
	     				new int[] { R.id.orderItem }
	     	);
     		
	        mCatalogListAdapterMap.put(position, adapter);
        }
        
        // create the expandable list view
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.catalog_page);
        Context appContext = Envived.getContext();
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        
        int width = metrics.widthPixels;
        listView.setIndicatorBounds(width - UIUtils.getDipsFromPixel(28, appContext), width - UIUtils.getDipsFromPixel(10, appContext));
        
        // Set list adapter
     	listView.setAdapter(adapter);
        
        // restore list state if any was saved
        Bundle listSavedData = mCatalogListStateMap.get(position);
        if (listSavedData != null) {
        	Log.d(TAG, "--- Trying to restore state for position: " + position);
        	
        	ArrayList<Long> expandedListState = (ArrayList<Long>)listSavedData.getSerializable(LIST_EXPANDED_GROUPS_KEY);
        	int listPosition = listSavedData.getInt(ITEM_POSITION_KEY);
        	int listItemPosition = listSavedData.getInt(ITEM_POSITION_KEY);
        	
        	if (expandedListState != null) {
        		restoreExpandedState(expandedListState, listView);
        		Log.d(TAG, ":-) -- list state being restored");
        	}
        	listView.setSelectionFromTop(listPosition, listItemPosition);
        	
        	// at the end clear current state
        	mCatalogListStateMap.remove(position);
        }
        
        
     	// add the whole page view to the collection
        ((ViewPager) collection).addView(view, 0);

        return view;
    }
	
	
	@Override
    public void destroyItem(View collection, int position, Object view) {
        // first save the state of the list view in the page that is about to be removed
		// this will be restored later in the instantiateItem call
		
		// get the list view
		ExpandableListView listView = (ExpandableListView) ((View)view).findViewById(R.id.catalog_page);
		Bundle listSavedData = new Bundle();
		
		Log.d(TAG, "--- Trying to SAVE state for position: " + position);
		
		// save list state
		ArrayList<Long> expandedListState = getExpandedIds(listView);
		listSavedData.putSerializable(LIST_EXPANDED_GROUPS_KEY, expandedListState);
		
		// save position of first visible item
		int listPosition = listView.getFirstVisiblePosition();
		listSavedData.putInt(LIST_POSITION_KEY, listPosition);
		
		// Save scroll position of item
	    View itemView = listView.getChildAt(0);
	    int listItemPosition = itemView == null ? 0 : itemView.getTop();
	    listSavedData.putInt(ITEM_POSITION_KEY, listItemPosition);
		
	    // store saved list state in the map
	    mCatalogListStateMap.put(position, listSavedData);
	    
	    // at the end remove this page's view from the collection
		((ViewPager) collection).removeView((View) view);
    }
	
	
	@Override
    public boolean isViewFromObject(View view, Object obj) {
        return view == ((View) obj);
    }

	@Override
	public CharSequence getPageTitle(int position) {
		return mCatalogTitleMap.get(position);
	}
	
	public void setTitlePageIndicator(TitlePageIndicator titleIndicator) {
		mTitlePageIndicator = titleIndicator;
		mTitlePageIndicator.setOnPageChangeListener(this);
	}
	
	
	@Override
	public List<Map<String, Object>> getOrderSelections() {
		int pageCt = getCount();
		List<Map<String, Object>> orderList = 
				new ArrayList<Map<String,Object>>();
		
		// add the selections from each page to make a single order
		for (int page = 0; page < pageCt; page++) {
			OrderCatalogListAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
			if (catalogAdapter != null) {
				orderList.addAll(catalogAdapter.getOrderSelections());
			}
		}
		
		return orderList;
	}
	

	@Override
	public void clearOrderSelections() {
		int pageCt = getCount();
		for (int page = 0; page < pageCt; page++) {
			OrderCatalogListAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
			if (catalogAdapter != null) {
				catalogAdapter.clearOrderSelections();
			}
		}
	}
	
	
	@Override
	public void onPageScrollStateChanged(int position) {
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int position) {
		mCurrentCatalogPage = position;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private ArrayList<Long> getExpandedIds(ExpandableListView list) {
        ExpandableListAdapter adapter = list.getExpandableListAdapter();
        
        if (adapter != null) {
            int length = adapter.getGroupCount();
            ArrayList<Long> expandedIds = new ArrayList<Long>();
            for(int i=0; i < length; i++) {
                if(list.isGroupExpanded(i)) {
                    expandedIds.add(adapter.getGroupId(i));
                }
            }
            
            return expandedIds;
        } else {
            return null;
        }
    }
	
	private void restoreExpandedState(ArrayList<Long> expandedIds, ExpandableListView list) {
        if (expandedIds != null) {
            ExpandableListAdapter adapter = list.getExpandableListAdapter();
            if (adapter != null) {
                for (int i=0; i < adapter.getGroupCount(); i++) {
                    long id = adapter.getGroupId(i);
                    if (expandedIds.contains(id)) 
                    	list.expandGroup(i);
                }
            }
        }
    }
}
