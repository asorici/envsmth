package com.envsocial.android.features.order;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.envsocial.android.R;

public class OrderCatalogItemDescriptionFragment extends DialogFragment {
	/**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static OrderCatalogItemDescriptionFragment newInstance(String itemName, String itemDescription, float itemUsageRanking) {
    	OrderCatalogItemDescriptionFragment f = new OrderCatalogItemDescriptionFragment();

        Bundle args = new Bundle();
        args.putString("item_name", itemName);
        args.putString("item_description", itemDescription);
        args.putFloat("item_usage_ranking", itemUsageRanking);
        f.setArguments(args);

        return f;
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(STYLE_NO_TITLE, R.style.FeatureOrderDialogTheme);
	}
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	String itemName = getArguments().getString("item_name");
    	String itemDescription = getArguments().getString("item_description");
        float itemUsageRanking = getArguments().getFloat("item_usage_ranking");
    	
    	View v = inflater.inflate(R.layout.catalog_item_details, container, false);
    	
    	TextView itemNameView = (TextView)v.findViewById(R.id.item_name_text);
    	itemNameView.setText(itemName);
    	
    	if(!itemDescription.equals("")) {
    		TextView itemDescriptionTextView = (TextView)v.findViewById(R.id.item_description_text);
        	itemDescriptionTextView.setText(itemDescription);
        }
        
        RatingBar itemUsageRankingView = (RatingBar)v.findViewById(R.id.item_usage_ratingbar);
        itemUsageRankingView.setRating(itemUsageRanking);
        
        Button dismissButton = (Button)v.findViewById(R.id.item_description_ok_button);
        
        // Watch for button click
        dismissButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	dismiss();
            }
        });

        return v;
    }
}
