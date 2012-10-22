package com.envsocial.android.features.order;

import java.util.List;
import java.util.Map;

public interface IOrderCatalogAdapter {
	public List<Map<String, Object>> getOrderSelections();
	public void clearOrderSelections();
	public void doCleanup();
}
