package com.envsocial.android.features.order;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.envsocial.android.R;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.EnvivedNotification;
import com.envsocial.android.utils.EnvivedNotificationContents;

public class ResolvedOrderNotification extends EnvivedNotification {
	private static final String TAG = "ResolvedOrderNotification";
	private static int counter = 0;
	
	private int mId;
	private int mIconId;
	private String mTitle;
	private long mWhen;
	private String mMessage;
	
	public ResolvedOrderNotification(Context context, Intent intent,
			EnvivedNotificationContents notificationContents) {
		super(context, intent, notificationContents);
		
		//mId = R.string.resolved_order;
		mId = counter++;
		mIconId = R.drawable.ic_launcher;
		mTitle = mContext.getResources().getString(R.string.resolved_order);
		mWhen = System.currentTimeMillis();
		mMessage = "Your order is on the way!";
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
		Intent launcher = new Intent();
		launcher.setComponent(new ComponentName(mContext,
				com.envsocial.android.features.order.ResolvedOrderDialogActivity.class));
		
		launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
				| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

		// Add extras
		launcher.setData(Uri.parse(Uri.encode(Url.fromUri(mNotificationContents.getResourceUri()))));
		launcher.putExtra(EnvivedNotificationContents.INTENT_EXTRA_PARAMS,
				mNotificationContents.getParams().toString());
		
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, launcher, 
				PendingIntent.FLAG_ONE_SHOT);
		
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
		nm.notify(TAG, mId, notification);
		playNotificationSound(mContext);
	}

}
