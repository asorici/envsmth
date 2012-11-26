package com.envsocial.android.features.order;

import com.envsocial.android.api.Annotation;

public interface ISendOrderRequest {
	public void sendOrder(OrderDialogFragment orderDialog);
	public void postSendOrderRequest(String orderRequestType, Annotation orderRequest, boolean success);
}
