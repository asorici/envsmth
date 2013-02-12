package com.envsocial.android;

import java.util.List;

import org.codeandmagic.android.TagListView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.api.Location;
import com.envsocial.android.api.Location.AreaInfo;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class BrowseLocationsListAdapter extends BaseAdapter {
	private Context mContext;
	private Location mLocation;
	private ImageFetcher mImageFetcher;
	
	
	public BrowseLocationsListAdapter(Context context, Location location, ImageFetcher imageFetcher) {
		super();
		this.mContext = context;
		this.mLocation = location;
		this.mImageFetcher = imageFetcher;
	}
	
	
	@Override
	public int getCount() {
		return mLocation.getAreaInfoList().size();
	}

	@Override
	public Object getItem(int position) {
		return mLocation.getAreaInfoList().get(position);
	}

	@Override
	public long getItemId(int position) {
		String areaResourceUrl = mLocation.getAreaInfoList().get(position).getResourceUrl();
		return Long.parseLong(Url.resourceIdFromUrl(areaResourceUrl));
	}
	
	@Override
	public boolean isEnabled(int position) {
		return true;
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		View rowView = convertView;
		
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.browse_locations_row, parent, false);
			viewHolder = new ViewHolder();
			
			viewHolder.areaImageView = (ImageView) rowView.findViewById(R.id.location_image);
			viewHolder.areaNameView = (TextView) rowView.findViewById(R.id.location_name);
			viewHolder.areaCheckinCountView = (TextView) rowView.findViewById(R.id.location_checked_in_count);
			viewHolder.areaTagsView = (TagListView) rowView.findViewById(R.id.location_tag_list);
			
			rowView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder) rowView.getTag();
		}
		
		bindViewData(viewHolder, position);
		
		return rowView;
	}
	
	
	private void bindViewData(ViewHolder viewHolder, int position) {
		AreaInfo areaInfo = mLocation.getAreaInfoList().get(position);
		
		viewHolder.areaNameView.setText(areaInfo.getName());
		viewHolder.areaCheckinCountView.setText(
			mContext.getResources().getString(R.string.locations_checked_in_count, areaInfo.getPersonCount()));
	
		// retrieve image
		String imageUrl = areaInfo.getImageUrl();
		if (imageUrl != null) {
			mImageFetcher.loadImage(imageUrl, viewHolder.areaImageView);
		}
		
		
		List<String> tagList = areaInfo.getTags();
		if (tagList != null) {
			viewHolder.areaTagsView.removeAllViews();
			viewHolder.areaTagsView.setTags(tagList);
		}
		
	}
	
	static class ViewHolder {
		ImageView areaImageView;
		TextView areaNameView;
		TextView areaCheckinCountView;
		TagListView areaTagsView;
	}
	
}
