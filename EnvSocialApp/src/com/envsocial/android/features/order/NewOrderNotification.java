package com.envsocial.android.features.order;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.envsocial.android.GCMIntentService;
import com.envsocial.android.R;
import com.envsocial.android.utils.EnvivedNotification;
import com.envsocial.android.utils.EnvivedNotificationContents;

public class NewOrderNotification extends EnvivedNotification {
	
	private int mId;
	private int mIconId;
	private String mTitle;
	private long mWhen;
	private String mMessage;
	
	public NewOrderNotification(Context context, Intent intent,
			EnvivedNotificationContents notificationContents) {
		super(context, intent, notificationContents);
		
		mId = R.string.incoming_order;
		mIconId = R.drawable.ic_launcher;
		mTitle = mContext.getResources().getString(R.string.incoming_order);
		mWhen = System.currentTimeMillis();
		
		mMessage = "You have new orders!";
	}

	@Override
	public int getNotificationId() {
		return mId;
	}

	@Override
	public int getNotificationIcon() {
		return mIconId;
	}

	@Override
	public String getNotificationTitle() {
		return mTitle;
	}

	@Override
	public long getNotificationWhen() {
		return mWhen;
	}

	@Override
	public String getNotificationMessage() {
		return mMessage;
	}

	@Override
	public void sendNotification() {
		// Create launcher intent
		Intent launcher = new Intent(Intent.ACTION_MAIN);
		launcher.setComponent(new ComponentName(mContext, 
				com.envsocial.android.EnvSocialAppActivity.class));
		launcher.addCategory(Intent.CATEGORY_LAUNCHER);
		launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		// Add extras
		launcher.putExtra(GCMIntentService.NOTIFICATION, true);
		launcher.putExtra(EnvivedNotificationContents.LOCATION_URI, mNotificationContents.getLocationUri());
		launcher.putExtra(EnvivedNotificationContents.FEATURE, mNotificationContents.getFeature());
		launcher.putExtra(EnvivedNotificationContents.RESOURCE_URI, mNotificationContents.getResourceUri());
		launcher.putExtra(EnvivedNotificationContents.PARAMS, mNotificationContents.getParams().toString());
		
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, launcher, 0);
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		builder.setContentIntent(pendingIntent)
				.setAutoCancel(true)
				.setContentTitle(mTitle)
				.setContentText(mMessage)
				.setSmallIcon(mIconId)
				.setWhen(mWhen)
				.setTicker(mTitle);
		
		Notification notification = builder.getNotification();
		
		NotificationManager nm = 
				(NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(mId, notification);
		playNotificationSound(mContext);
	}

}
