package com.envsocial.android.features.socialmedia;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;

public class SocialMediaFragment extends SherlockFragment {
	private String mSocialMediaUrl;
	private WebView mSocialMediaView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mSocialMediaUrl = getArguments().getString("social_media_url");
	    
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		WebView webview = new WebView(getActivity());
		getActivity().getWindow().requestFeature(Window.FEATURE_PROGRESS);
		
		webview.getSettings().setJavaScriptEnabled(true);

		final Activity activity = getActivity();
		webview.setWebChromeClient(new WebChromeClient() {
		   public void onProgressChanged(WebView view, int progress) {
		     // Activities and WebViews measure progress with different scales.
		     // The progress meter will automatically disappear when we reach 100%
		     activity.setProgress(progress * 1000);
		   }
		 });
		
		if (mSocialMediaUrl != null) {
			webview.loadUrl(mSocialMediaUrl);
		}
		
		return webview;
	}
}
