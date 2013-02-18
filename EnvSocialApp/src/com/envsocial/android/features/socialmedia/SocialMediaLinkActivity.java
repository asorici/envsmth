package com.envsocial.android.features.socialmedia;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SocialMediaLinkActivity extends SherlockFragmentActivity {
	private String mSocialMediaUrl;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mSocialMediaUrl = getIntent().getStringExtra("social_media_url");
	    
	    WebView webview = new WebView(this);
	    setContentView(webview);
	    
	    webview.getSettings().setJavaScriptEnabled(true);

		final Activity activity = this;
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
	}
}
