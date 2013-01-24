package com.envsocial.android.utils;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;

public abstract class EnvivedNotificationHandler implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract boolean handleNotification(Context context, Intent intent, 
			EnvivedNotificationContents notificationContents);
}
