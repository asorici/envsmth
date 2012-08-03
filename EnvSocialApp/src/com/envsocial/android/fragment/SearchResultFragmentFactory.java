package com.envsocial.android.fragment;

import java.util.HashMap;
import java.util.Map;

import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.order.OrderSearchResultFragment;
import com.envsocial.android.features.program.ProgramSearchResultFragment;

public class SearchResultFragmentFactory {
	private static Map<String, SherlockFragment> fragmentMap = new HashMap<String, SherlockFragment>();
	
	public static Fragment newInstance(String featureCategory, String featureQuery) {
		/*
		SherlockFragment frag = fragmentMap.get(featureCategory);
		
		if (frag == null) {
			// swith after the featureCategory string
			if (featureCategory.equals(Feature.ORDER)) {
				frag = new OrderSearchResultFragment();
				fragmentMap.put(featureCategory, frag);
			}
			else if (featureCategory.equals(Feature.PROGRAM)) {
				frag = new ProgramSearchResultFragment();
				fragmentMap.put(featureCategory, frag);
			}
			else {
				// no search results are expected for this category
				return null;
			}
		}
		*/
		
		if (featureCategory.equals(Feature.ORDER)) {
			return new OrderSearchResultFragment();
		}
		else if (featureCategory.equals(Feature.PROGRAM)) {
			return new ProgramSearchResultFragment();
		}
		else {
			// no search results are expected for this category
			return null;
		}
		
	}
}
