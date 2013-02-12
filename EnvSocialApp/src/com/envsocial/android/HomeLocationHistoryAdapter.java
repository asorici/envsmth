package com.envsocial.android;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.api.Location;
import com.envsocial.android.utils.LocationHistory;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class HomeLocationHistoryAdapter extends BaseAdapter {
	
	private List<Location> mLocationHistoryList;
	private Context mContext;
	private ImageFetcher mImageFetcher;
	
	public HomeLocationHistoryAdapter(Context context, ImageFetcher imageFetcher) {
		mContext = context;
		mImageFetcher = imageFetcher;
		
		LocationHistory locationHistory = Envived.getLocationHistory();
		mLocationHistoryList = new LinkedList<Location>(locationHistory.snapshot().values());
		Collections.reverse(mLocationHistoryList);
	}
	
	@Override
	public int getCount() {
		return mLocationHistoryList.size();
	}

	@Override
	public Object getItem(int position) {
		return mLocationHistoryList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return Long.parseLong(mLocationHistoryList.get(position).getId());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.home_checkin_history_row, parent, false);
			viewHolder = new ViewHolder();
			
			viewHolder.locationImageView = (ImageView) convertView.findViewById(R.id.location_image);
			viewHolder.locationNameView = (TextView) convertView.findViewById(R.id.location_name);
		
			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		// bind data
		bindViewData(viewHolder, position);
		
		return convertView;
	}

	
	private void bindViewData(ViewHolder viewHolder, int position) {
		Location location = (Location) getItem(position);
		
		viewHolder.locationNameView.setText(location.getName());
		mImageFetcher.loadImage(location.getImageThumbnailUrl(), viewHolder.locationImageView);
	
	}

	
	static class ViewHolder {
		ImageView locationImageView;
		TextView locationNameView;
	}
	
	@Override
	public void notifyDataSetChanged() {
		LocationHistory locationHistory = Envived.getLocationHistory();
		mLocationHistoryList = new LinkedList<Location>(locationHistory.snapshot().values());
		Collections.reverse(mLocationHistoryList);
		
		super.notifyDataSetChanged();
	}
}
