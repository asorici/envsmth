package com.envsocial.android;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.LocationHistory;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class HomeLocationListAdapter extends BaseExpandableListAdapter {
	/*
	 * Hardcoded for now - include just the link to the AIWO environment
	 */
	private static final String URL_BASE = "http://192.168.1.108:8080";
	private static final String ENVIVED_AIWO_LINK = "/envsocial/client/v1/resources/environment/12/";
	private static final String ENVIVED_AIWO_IMAGE_URL = "/envsocial/media/images/aiwo_small.png";
	
	public static final int FEATURED_LOCATIONS_GROUP_ID = 0;
	public static final int LOCATION_HISTORY_GROUP_ID = 1;
	
	private static final String FEATURED_LOCATIONS_LABEL = "Featured Locations";
	private static final String LOCATION_HISTORY_LABEL = "Checkin History";
	
	private List<Location> mLocationHistoryList;
	private Context mContext;
	private ImageFetcher mImageFetcher;
	
	public HomeLocationListAdapter(Context context, ImageFetcher imageFetcher) {
		mContext = context;
		mImageFetcher = imageFetcher;
		
		LocationHistory locationHistory = Envived.getLocationHistory();
		mLocationHistoryList = new LinkedList<Location>(locationHistory.snapshot().values());
		
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		if (groupPosition == FEATURED_LOCATIONS_GROUP_ID) {
			// return the link 
			return ENVIVED_AIWO_LINK;
		}
		else {
			return mLocationHistoryList.get(childPosition);
		}
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		if (groupPosition == FEATURED_LOCATIONS_GROUP_ID) {
			return Long.parseLong(Url.resourceIdFromUrl(ENVIVED_AIWO_LINK));
		}
		else {
			return Long.parseLong(mLocationHistoryList.get(childPosition).getId());
		} 
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		ViewHolder viewHolder;
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.home_location_list_item, parent, false);
			viewHolder = new ViewHolder();
			
			viewHolder.locationImageView = (ImageView) convertView.findViewById(R.id.location_image);
			viewHolder.locationNameView = (TextView) convertView.findViewById(R.id.location_name);
		
			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		// bind data
		bindChildViewData(viewHolder, groupPosition, childPosition);
		
		return convertView;
	}

	private void bindChildViewData(ViewHolder viewHolder, int groupPosition, int childPosition) {
		if (groupPosition == FEATURED_LOCATIONS_GROUP_ID) {
			viewHolder.locationNameView.setText("AIWO 2013");
			
			String imageUrl = URL_BASE + ENVIVED_AIWO_IMAGE_URL;
			mImageFetcher.loadImage(imageUrl, viewHolder.locationImageView);
		}
		else {
			Location location = (Location) getChild(groupPosition, childPosition);
			
			viewHolder.locationNameView.setText(location.getName());
			
			if (location.getImageThumbnailUrl() != null) {
				mImageFetcher.loadImage(location.getImageThumbnailUrl(), viewHolder.locationImageView);
			}
		}
	}
	
	
	@Override
	public int getChildrenCount(int groupPosition) {
		if (groupPosition == FEATURED_LOCATIONS_GROUP_ID) {
			return 1;
		}
		else {
			return mLocationHistoryList.size();
		}
	}

	@Override
	public Object getGroup(int groupPosition) {
		if (groupPosition == FEATURED_LOCATIONS_GROUP_ID) {
			return FEATURED_LOCATIONS_LABEL;
		}
		else {
			return LOCATION_HISTORY_LABEL;
		} 
	}

	@Override
	public int getGroupCount() {
		return 2;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.home_location_list_group, parent, false);
		}
		
		TextView labelView = (TextView) convertView.findViewById(R.id.location_list_label);
		
		String labelText = (String) getGroup(groupPosition);
		labelView.setText(labelText);
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	
	static class ViewHolder {
		ImageView locationImageView;
		TextView locationNameView;
	}
	
	
	@Override
	public void notifyDataSetChanged() {
		LocationHistory locationHistory = Envived.getLocationHistory();
		mLocationHistoryList = new LinkedList<Location>(locationHistory.snapshot().values());
		//Collections.reverse(mLocationHistoryList);
		
		super.notifyDataSetChanged();
	}
}
