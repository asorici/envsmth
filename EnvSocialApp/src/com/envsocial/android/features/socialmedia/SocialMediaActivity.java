package com.envsocial.android.features.socialmedia;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.EnvivedFeatureActivity;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.socialmedia.SocialMediaFeature.SocialMediaLink;

public class SocialMediaActivity extends EnvivedFeatureActivity {
	private static final String TAG = "SocialMediaActivity";
	
	private TabHost mTabHost;
	private SocialMediaFeature mSocialMediaFeature;
	private ProgressDialog mWebViewLoadingDialog;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        setContentView(R.layout.social_media_tab_layout);        
        getSupportActionBar().setTitle("Social@" + getFeatureLocation().getName());
        
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
    }
	
	public void onDestroy() {
		super.onDestroy();
		
		if (mWebViewLoadingDialog != null) {
			mWebViewLoadingDialog.cancel();
			mWebViewLoadingDialog = null;
		}
	}
	
	
	private ProgressDialog createWebViewLoadingDialog(Context context, String message) {
		ProgressDialog pd = new ProgressDialog(new ContextThemeWrapper(context, R.style.ProgressDialogWhiteText));
		pd.setIndeterminate(true);
		pd.setMessage(message);
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		
		return pd;
	}
	
	
	private void bindSocialMediaViewData() {
		List<SocialMediaLink> socialMediaLinks = mSocialMediaFeature.getSocialMediaLinks();
		
		int len = socialMediaLinks.size();
		for (int i = 0; i < len; i++) {
			SocialMediaLink link = socialMediaLinks.get(i);
			
			int viewId = (i + 1);
			String url = link.getUrl(); 
			String tabName = link.getName();
			String tabTag = tabName.toLowerCase(Locale.US);
			Drawable iconDrawable = getResources().getDrawable(link.getIconDrawable());
			
			WebView webView = createWebView(url, (i + 1));
			
			//Log.d(TAG, "data for link: " + tabName + ", " + tabTag + ", " + link.getIconDrawable());
			mTabHost.getTabContentView().addView(webView);
			mTabHost.addTab(mTabHost.newTabSpec(tabTag).setIndicator(tabName, iconDrawable).setContent(viewId));
		}
		
		TabWidget tabWidget = mTabHost.getTabWidget();
		for (int i = 0; i < len; i++) {
			RelativeLayout tabLayout = (RelativeLayout) tabWidget.getChildAt(i);
			tabLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.feature_social_media_tab_indicator));
		}
	}

	private WebView createWebView(String url, final int id) {
		WebView webview = new WebView(this);
		webview.setId(id);
		
	    webview.getSettings().setJavaScriptEnabled(true);
	    webview.getSettings().setDomStorageEnabled(true);

		final Activity activity = this;
		webview.setWebChromeClient(new WebChromeClient() {
		   public void onProgressChanged(WebView view, int progress) {
		     // Activities and WebViews measure progress with different scales.
		     // The progress meter will automatically disappear when we reach 100%
		     activity.setProgress(progress * 1000);
		   }
		});
		
		webview.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (id == 1 && mWebViewLoadingDialog == null) {
					mWebViewLoadingDialog = createWebViewLoadingDialog(activity, "Loading Page...");
					mWebViewLoadingDialog.show();
				}
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				if (id  == mSocialMediaFeature.getNumLinks() && mWebViewLoadingDialog != null) {
					mWebViewLoadingDialog.cancel();
					mWebViewLoadingDialog = null;
				}
			}
			
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Toast.makeText(activity, "Oh no! " + description,
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
			
			@Override
			public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
			    handler.proceed(); // Ignore SSL certificate errors
			}
		});
		
		webview.loadUrl(url);
		
		return webview;
	}
	
	@Override
	protected Feature getLocationFeature(Location location) throws EnvSocialContentException {
		return location.getFeature(Feature.SOCIAL_MEDIA);
	}

	@Override
	protected void onFeatureDataInitialized(Feature newFeature, boolean success) {
		if (success) {
			mSocialMediaFeature = (SocialMediaFeature) newFeature;
			bindSocialMediaViewData();
		}
	}

	

	@Override
	protected void onFeatureDataUpdated(Feature updatedFeature, boolean success) {
		if (success) {
			mSocialMediaFeature = (SocialMediaFeature) updatedFeature;
			bindSocialMediaViewData();
		}
	}

	@Override
	protected String getActiveUpdateDialogMessage() {
		return  "The URLs for the social media links have changed. This update will refresh your view. " +
				"Please select YES if you wish to perform the update. Select NO if, you " +
				"want to keep your current browsing session. The update will be performed next time you " +
				"start this activity.";
	}

}
