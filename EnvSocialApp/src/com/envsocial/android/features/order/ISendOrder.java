package com.envsocial.android.features.order;

public interface ISendOrder {
	public void sendOrder(OrderDialogFragment orderDialog);
	public void postSendOrder(boolean success);
}
