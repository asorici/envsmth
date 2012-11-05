package com.envsocial.android.features.description;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.DetailsActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.LocationContextManager;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageFetcher;
import com.envsocial.android.utils.imagemanager.ImageWorker;

public class DescriptionFragment extends SherlockFragment {
	private static final String TAG = "DescriptionFragment";
	private Location mData;
	private TextView mDescriptionPeopleCount;
	private ImageView mLogoImageView;
	
	private ImageFetcher mImageFetcher;
	private CheckedInCountTask mPeopleCountTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mData = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.description, container, false);
		mLogoImageView = (ImageView) v.findViewById(R.id.description_image);
		TextView descriptionDetails = (TextView) v.findViewById(R.id.description_details);
		mDescriptionPeopleCount = (TextView) v.findViewById(R.id.description_people_count);
		TextView descriptionNewInfo = (TextView) v.findViewById(R.id.description_new_info);
		
		DescriptionFeature descriptionFeature = (DescriptionFeature) mData.getFeature(Feature.DESCRIPTION);
		descriptionDetails.setText(descriptionFeature.getDescriptionText());
		mDescriptionPeopleCount.setText(descriptionFeature.getPeopleCountText());
		descriptionNewInfo.setText(descriptionFeature.getNewestInfoText());
		
	    return v;
	}
	
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (DetailsActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((DetailsActivity) getActivity()).getImageFetcher();
        }
        
        DescriptionFeature descriptionFeature = (DescriptionFeature) mData.getFeature(Feature.DESCRIPTION);
        String logoImageUrl = descriptionFeature.getLogoImageUri();
        
        if (logoImageUrl != null && mImageFetcher != null) {
        	mImageFetcher.loadImage(logoImageUrl, mLogoImageView);
        }
    }
	
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart in DescriptionFragment");
		
		mPeopleCountTask = new CheckedInCountTask(); 
		mPeopleCountTask.execute(mData);
		mPeopleCountTask = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mPeopleCountTask != null) {
			mPeopleCountTask.cancel(true);
			mPeopleCountTask = null;
		}
		
		if (mLogoImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mLogoImageView);
            mLogoImageView.setImageDrawable(null);
        }
	}
	
	private class CheckedInCountTask extends  AsyncTask<Location, Void, ResponseHolder> {
		
		@Override
		protected ResponseHolder doInBackground(Location... params) {
			// we only pass in one parameter
			Location currentLocation = params[0];
			LocationContextManager locationContextMgr = currentLocation.getContextManager();
			
			if (locationContextMgr != null) {
				return locationContextMgr.getUserCount(getActivity());
			}
			
			return null;
		}
		
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			if (holder != null && !holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					Context context = getActivity();
					
					try {
						JSONObject dataJSON = holder.getJsonContent();
						
						// response is just the integer count
						int peopleCount = dataJSON.getInt("response");
						
						// Display a toast message and update the view
						mDescriptionPeopleCount.setText("Checked in with " + peopleCount + " other users.");
						
						Toast toast = Toast.makeText(context,
								"Checked in users counter updated.", Toast.LENGTH_LONG);
						toast.show();
					} catch (JSONException e) {
						Log.d(TAG, holder.getResponseBody(), e);
					} 
				}
				// else fail silently - no update
			}
			else {
				Log.d(TAG, holder.getResponseBody(), holder.getError());
			}
		}
	}
}
