package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;

import com.envsocial.android.R;
import com.viewpagerindicator.TitlePageIndicator;

public class OrderCatalogPagerAdapter extends PagerAdapter 
								implements OnClickListener, OnPageChangeListener, IOrderCatalogAdapter {
	
	private static final String TAG = "OrderCatalogPagerAdapter";
	
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
	
	//private Button mOrderButton;
	//private Button mTabButton;
	
	public OrderCatalogPagerAdapter(OrderFragment parentFragment) {
		mParentFragment = parentFragment;
		mCatalogListAdapterMap = new HashMap<Integer, OrderCatalogListAdapter>();
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
        
     	// set button zone as list footer
     	//LinearLayout buttonLayout = (LinearLayout)inflater.inflate(R.layout.catalog_page_buttons, null);
     	
        //mOrderButton = (Button)view.findViewById(R.id.btn_order);
     	//mTabButton = (Button)view.findViewById(R.id.btn_tab);
     	//mOrderButton.setOnClickListener(this);
     	//mTabButton.setOnClickListener(this);
     	
     	//listView.setFooterDividersEnabled(true);
     	//listView.addFooterView(buttonLayout);
     	
     	// Set list adapter
     	listView.setAdapter(adapter);
        
     	// add the whole page view to the collection
        ((ViewPager) collection).addView(view, 0);

        return view;
    }
	
	
	@Override
    public void destroyItem(View view, int position, Object obj) {
        ((ViewPager) view).removeView((View) obj);
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
		Log.d(TAG, "#### currentCatalogPage: " + mCurrentCatalogPage);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
