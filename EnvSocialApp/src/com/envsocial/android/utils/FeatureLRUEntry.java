package com.envsocial.android.utils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

public class FeatureLRUEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String featureCategory;
	private String featureCacheFileName;
	private String locationUrl;
	private boolean featureVirtualAccess;
	
	private Calendar featureTimestamp;
	private Calendar lastAccessTime;
	
	public FeatureLRUEntry(String featureCategory, String featureCacheFileName, 
			String locationUrl, boolean featureVirtualAccess, Calendar featureTimestamp) {
		this.featureCategory = featureCategory;
		this.featureCacheFileName = featureCacheFileName;
		this.featureTimestamp = featureTimestamp;
		this.locationUrl = locationUrl;
		this.featureVirtualAccess = featureVirtualAccess;
		
		updateAccessTime();
	}
	
	public String getFeatureCategory() {
		return featureCategory;
	}

	public String getFeatureCacheFileName() {
		return featureCacheFileName;
	}

	public String getFeatureLocationUrl() {
		return locationUrl;
	}
	
	public boolean hasFeatureVirtualAccess() {
		return featureVirtualAccess;
	}
	
	public Calendar getFeatureTimestamp() {
		return featureTimestamp;
	}
	
	public Calendar getLastAccessTime() {
		return lastAccessTime;
	}
	
	public void updateAccessTime() {
		lastAccessTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	public int hashCode() {
		return featureCacheFileName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureLRUEntry)) {
			return false;
		}
		
		final FeatureLRUEntry other = (FeatureLRUEntry)obj;
		return other.getFeatureCacheFileName().equals(featureCacheFileName);
	}
}
