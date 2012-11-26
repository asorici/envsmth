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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.IFeatureAdapter;
import com.envsocial.android.utils.UIUtils;
import com.viewpagerindicator.TitlePageIndicator;

public class OrderCatalogPagerAdapter extends PagerAdapter 
					implements OnClickListener, OnPageChangeListener, IOrderCatalogAdapter, IFeatureAdapter {
	
	private static final String TAG = "OrderCatalogPagerAdapter";
	private static final String LIST_EXPANDED_GROUPS_KEY = "listExpandedGroups";
	private static final String LIST_POSITION_KEY = "listPosition";
	private static final String ITEM_POSITION_KEY = "itemPosition";
	private static final String LIST_DATA_KEY = "listData";
	private static final String PAGER_STATE_KEY = "pagerState";
	
	private static SparseArray<String> mCatalogTitleMap;
	static {
		mCatalogTitleMap = new SparseArray<String>();
		mCatalogTitleMap.put(0, OrderFeature.TYPE_DRINKS);
		mCatalogTitleMap.put(1, OrderFeature.TYPE_FOOD);
		mCatalogTitleMap.put(2, OrderFeature.TYPE_DESERT);
	}
	
	private OrderFragment mParentFragment;
	private TitlePageIndicator mTitlePageIndicator;
	
	
	private SparseArray<OrderCatalogCursorAdapter> mCatalogListAdapterMap;
	private HashMap<Integer, Bundle> mCatalogListStateMap;
	
	
	public OrderCatalogPagerAdapter(OrderFragment parentFragment) {
		mParentFragment = parentFragment;
		mCatalogListAdapterMap = new SparseArray<OrderCatalogCursorAdapter>();
		mCatalogListStateMap = new HashMap<Integer, Bundle>();
	}
	
	
	public OrderFragment getParentFragment() {
		return mParentFragment;
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
        
        /*
        OrderCatalogCursorAdapter adapter = mCatalogListAdapterMap.get(position);
        if (adapter == null) {
        	//String type = mCatalogTitleMap.get(position);
        	String type = mCatalogTitleMap.get(0);
        	
	        // Create custom expandable list adapter
        	adapter = new OrderCatalogCursorAdapter(mParentFragment, 
        			mParentFragment.getActivity(), orderFeat.getOrderCategoryCursor(type), 
        			R.layout.catalog_group, R.layout.catalog_item);
        	
	        mCatalogListAdapterMap.put(position, adapter);
        }
        */
        
        String type = mCatalogTitleMap.get(position);
        OrderCatalogCursorAdapter adapter = new OrderCatalogCursorAdapter(this, position, 
        		orderFeat.getOrderCategoryCursor(type), 
    			R.layout.catalog_group, R.layout.catalog_item);
        mCatalogListAdapterMap.put(position, adapter);
        
        
        // restore list data if any was saved
        Bundle listSavedData = mCatalogListStateMap.get(position);
        if (listSavedData != null) {
        	Bundle listDataBundle = listSavedData.getBundle(LIST_DATA_KEY);
        	adapter.restoreState(listDataBundle);
        }
        
        // create the expandable list view
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.catalog_page);
        
        Context appContext = Envived.getContext();
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        
        int width = metrics.widthPixels;
        listView.setIndicatorBounds(width - UIUtils.getDipsFromPixel(28, appContext), width - UIUtils.getDipsFromPixel(10, appContext));
        
        // Set list adapter
     	listView.setAdapter(adapter);
        
     	// set listeners
     	final int pagePosition = position;
     	final ExpandableListView fListView = listView;
     	
     	listView.setOnGroupExpandListener(new OnGroupExpandListener() {			
			@Override
			public void onGroupExpand(int groupPosition) {
				Bundle listSavedData = saveListState(fListView);
				mCatalogListStateMap.put(pagePosition, listSavedData);
			}
		});
     	
     	listView.setOnGroupCollapseListener(new OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				Bundle listSavedData = saveListState(fListView);
				mCatalogListStateMap.put(pagePosition, listSavedData);
			}
		});
     	
     	
        // restore list state if any was saved
        if (listSavedData != null) {
        	ArrayList<Long> expandedListState = (ArrayList<Long>)listSavedData.getSerializable(LIST_EXPANDED_GROUPS_KEY);
        	int listPosition = listSavedData.getInt(ITEM_POSITION_KEY);
        	int listItemPosition = listSavedData.getInt(ITEM_POSITION_KEY);
        	
        	if (expandedListState != null) {
        		restoreExpandedState(expandedListState, listView);
        	}
        	listView.setSelectionFromTop(listPosition, listItemPosition);
        }
        
        
     	// add the whole page view to the collection
        ((ViewPager) collection).addView(view, 0);
     	//((ViewPager) collection).addView(view);

        return view;
    }
	
	
	@Override
    public void destroyItem(View collection, int position, Object view) {
        // first save the state of the list view in the page that is about to be removed
		// this will be restored later in the instantiateItem call
		
		// get the list view
		ExpandableListView listView = (ExpandableListView) ((View)view).findViewById(R.id.catalog_page);
		Bundle listSavedData = saveListState(listView);
		
	    // store saved list state in the map
	    mCatalogListStateMap.put(position, listSavedData);
	    
	    // at the end remove this page's view from the collection
		((ViewPager) collection).removeView((View) view);
    }
	
	
	@Override
    public boolean isViewFromObject(View view, Object obj) {
        boolean result = view == ((View) obj);
		return result;
    }

	@Override
	public CharSequence getPageTitle(int position) {
		return mCatalogTitleMap.get(position);
	}
	
	public void setTitlePageIndicator(TitlePageIndicator titleIndicator) {
		mTitlePageIndicator = titleIndicator;
		mTitlePageIndicator.setOnPageChangeListener(this);
	}
	
	
	
	// ========================= Order Feature Selection State Management ========================= //
	
	public Bundle onSaveInstanceState() {
		Bundle pagerStateBundle = new Bundle();
		pagerStateBundle.putSerializable(PAGER_STATE_KEY, mCatalogListStateMap);
		
		return pagerStateBundle;
	}
	
	
	public void onRestoreInstanceState(Bundle pagerStateBundle) {
		HashMap<Integer, Bundle> catalogListStateMap = 
				(HashMap<Integer, Bundle>)pagerStateBundle.getSerializable(PAGER_STATE_KEY);
		if (catalogListStateMap != null) {
			mCatalogListStateMap = catalogListStateMap;
		}
	}
	
	
	@Override
	public List<Map<String, Object>> getOrderSelections() {
		int pageCt = getCount();
		List<Map<String, Object>> orderList = new ArrayList<Map<String,Object>>();
		
		// add the selections from each page to make a single order
		for (int page = 0; page < pageCt; page++) {
			
			OrderCatalogCursorAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
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
			OrderCatalogCursorAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
			if (catalogAdapter != null) {
				catalogAdapter.clearOrderSelections();
			}
		}
	}
	
	
	@Override
	public void doCleanup() {
		int pageCt = getCount();
		for (int page = 0; page < pageCt; page++) {
			OrderCatalogCursorAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
			if (catalogAdapter != null) {
				catalogAdapter.doCleanup();
			}
		}
	}
	
	
	public void updateFeature(OrderFeature updatedOrderFeature) {
		// first close all cursors
		doCleanup();
		
		// then see if the new feature can be initialized - db stuff
		try {
			updatedOrderFeature.doUpdate();
			mParentFragment.setOrderFeature(updatedOrderFeature);
		} catch (EnvSocialContentException ex) {
			Log.d(TAG, "[DEBUG] >> OrderFeature update failed. Content could not be parsed.", ex);
		}
		
		// then get the currently set order feature and update the adapters with the right cursors
		OrderFeature currentOrderFeature = mParentFragment.getOrderFeature();
		
		
		int pageCt = getCount();
		for (int page = 0; page < pageCt; page++) {
			OrderCatalogCursorAdapter catalogAdapter = mCatalogListAdapterMap.get(page);
			String type = mCatalogTitleMap.get(page);
			
			if (catalogAdapter != null) {
				catalogAdapter.updateFeature(currentOrderFeature.getOrderCategoryCursor(type));
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
		
	}

	@Override
	public void onClick(View v) {
		
	}
	
	
	private Bundle saveListState(ExpandableListView listView) {
		Bundle listSavedData = new Bundle();
		
		// ---- save list data ----
		OrderCatalogCursorAdapter adapter = (OrderCatalogCursorAdapter)listView.getExpandableListAdapter();
		Bundle listDataBundle = adapter.onSaveInstanceState();
		
		listSavedData.putBundle(LIST_DATA_KEY, listDataBundle);
		
		// ---- save list state ----
		ArrayList<Long> expandedListState = getExpandedIds(listView);
		listSavedData.putSerializable(LIST_EXPANDED_GROUPS_KEY, expandedListState);
		
		// save position of first visible item
		int listPosition = listView.getFirstVisiblePosition();
		listSavedData.putInt(LIST_POSITION_KEY, listPosition);
		
		// Save scroll position of item
	    View itemView = listView.getChildAt(0);
	    int listItemPosition = itemView == null ? 0 : itemView.getTop();
	    listSavedData.putInt(ITEM_POSITION_KEY, listItemPosition);
	    
	    return listSavedData;
	}
	
	
	protected void saveListData(int pagePosition, Bundle listDataBundle) {
		Bundle listSavedData = mCatalogListStateMap.get(pagePosition);
		if (listSavedData != null) {
			listSavedData.putBundle(LIST_DATA_KEY, listDataBundle);
		}
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
