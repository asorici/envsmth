package com.envsocial.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.api.Url;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class HomeFeaturedLocationsAdapter extends BaseAdapter {
	/*
	 * Hardcoded for now - include just the link to the AIWO environment
	 */
	private static final String URL_BASE = Url.HTTP + Url.HOSTNAME;
	private static final String AIWO_ENVIRONMENT = "environment/12/";
	private static final String ENVIVED_AIWO_LINK = Url.RESOURCE_RELATIVE_URL + AIWO_ENVIRONMENT;
	
	//private static final String ENVIVED_AIWO_IMAGE_URL = "/envsocial/media/images/aiwo_small_white_background.png";
	private static final String ENVIVED_AIWO_IMAGE_URL = "/envived/media/images/aiwo_small_white_background.png";
	
	private Context mContext;
	private ImageFetcher mImageFetcher;
	
	public HomeFeaturedLocationsAdapter(Context context, ImageFetcher imageFetcher) {
		mContext = context;
		mImageFetcher = imageFetcher;
	}
	
	@Override
	public int getCount() {
		return 1;
	}

	@Override
	public Object getItem(int arg0) {
		// return the link 
		return ENVIVED_AIWO_LINK;
	}

	@Override
	public long getItemId(int position) {
		return Long.parseLong(Url.resourceIdFromUrl(ENVIVED_AIWO_LINK));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.home_featured_locations_row, parent, false);
		}
		
		TextView locationNameView = (TextView) convertView.findViewById(R.id.location_name);
		ImageView locationImageView = (ImageView) convertView.findViewById(R.id.location_image);
		//locationImageView.setAlpha(50);
		
		locationNameView.setText("AIWO 2013");
		
		String imageUrl = URL_BASE + ENVIVED_AIWO_IMAGE_URL;
		mImageFetcher.loadImage(imageUrl, locationImageView);
		
		return convertView;
	}

}
