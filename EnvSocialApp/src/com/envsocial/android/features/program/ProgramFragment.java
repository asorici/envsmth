package com.envsocial.android.features.program;

import org.json.JSONException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class ProgramFragment extends Fragment {
	
	private Location mLocation;
	private Program mProgram;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.program, container, false);
		
		try {
			mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
			String menuJSON = mLocation.getFeatureData(Feature.ORDER);
			mProgram= new Program(menuJSON);
			
			// Create custom expandable list adapter
/*			CatalogListAdapter adapter = new CatalogListAdapter(getActivity(),
		    		mOrderMenu.getCategoryData(),
		    		R.layout.catalog_group,
		    		new String[] { OrderMenu.CATEGORY },
		    		new int[] { R.id.orderGroup },
		    		mOrderMenu.getItemData(),
		    		R.layout.catalog_item,
		    		new String[] { OrderMenu.ITEM_NAME },
		    		new int[] { R.id.orderItem },
		    		mOrderMenu.getCounter()
		    		);*/
			
		    // Set adapter
		    ListView listView = (ListView) v.findViewById(R.id.program);
//		    listView.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return v;
	}
	
	
	public static class Program {
		
		Program(String jsonString) throws JSONException {
			
		}
	}
}
