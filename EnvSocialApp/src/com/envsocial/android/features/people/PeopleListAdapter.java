package com.envsocial.android.features.people;

import java.util.List;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.user.ResearchSubProfile;
import com.envsocial.android.api.user.User;
import com.envsocial.android.api.user.UserProfileConfig.UserSubProfileType;

public class PeopleListAdapter extends BaseAdapter {
	private Context mContext;
	private List<User> mPeople;
	private LayoutInflater mInflater;
	
	public PeopleListAdapter(Context context, List<User> people) {
		mContext = context;
		mPeople = people;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		if (mPeople != null) {
			return mPeople.size();
		}
		return 0;
	}

	@Override
	public User getItem(int position) {
		if (mPeople != null) {
			return mPeople.get(position);
		}
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.people_entry, parent, false);
			
			// Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
			holder = new ViewHolder();
			holder.name = (TextView)convertView.findViewById(R.id.name);
			holder.affiliation = (TextView)convertView.findViewById(R.id.affiliation);
			holder.researchInterests = (TextView)convertView.findViewById(R.id.research_interests);
			
			convertView.setTag(holder);
		}
		else {
			// Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
		}
		
		bind(holder, position);
		
		return convertView;
	}

	
	private void bind(ViewHolder holder, int position) {
		User u = mPeople.get(position);
		if (u != null) {
			holder.name.setText(u.getUserData().getLastName().toUpperCase() + ", " + u.getUserData().getFirstName());
			ResearchSubProfile subProfile = (ResearchSubProfile)u.getUserData().getSubProfile(UserSubProfileType.researchprofile);
			
			if (subProfile != null) {
				holder.affiliation.setText(subProfile.getAffiliation());
				
				String researchInterestString = "";
				for (int i = 0; i < subProfile.getResearchInterests().length; i++) {
					researchInterestString += subProfile.getResearchInterests()[i] + ", ";
				}
				researchInterestString = researchInterestString.substring(0, researchInterestString.length());
				
				holder.researchInterests.setText(researchInterestString);
			}
			else {
				holder.affiliation.setText("n.a.");
				holder.researchInterests.setText("n.a.");
			}
		}
	}


	static class ViewHolder {
		TextView name;
		TextView affiliation;
		TextView researchInterests;
	}
}
