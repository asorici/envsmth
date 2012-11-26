package com.envsocial.android.features.order;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.utils.EnvivedNotificationContents;

public class ResolvedOrderDialogActivity extends Activity implements OnClickListener {
	private static final String TAG = "ResolvedOrderDialogActivity";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String jsonParams = getIntent().getStringExtra(EnvivedNotificationContents.INTENT_EXTRA_PARAMS);
        
        try {
			JSONObject orderObject = new JSONObject(jsonParams).getJSONObject("order");
			JSONArray orderData = orderObject.getJSONArray("order");
			
			int orderLen = orderData.length();
			String orderEntry = "";
			for (int j = 0; j < orderLen; ++ j) {
				String category = orderData.getJSONObject(j).getString("category");
				String order = orderData.getJSONObject(j).getString("items");
				orderEntry += "<b>" + category + ":</b><br />";
				orderEntry += "&nbsp;&nbsp;&nbsp;&nbsp;";
				orderEntry += order.replace("\n", "<br />&nbsp;&nbsp;&nbsp;&nbsp;");
				orderEntry += "<br />";
			}
			
			setContentView(R.layout.order_resolved_dialog);
			
			TextView orderTextView = (TextView) findViewById(R.id.order_resolved_content);
			Button dismissButton = (Button) findViewById(R.id.order_resolved_button);
			
			orderTextView.setText(Html.fromHtml(orderEntry));
			dismissButton.setOnClickListener(this);
			
		} catch (JSONException e) {
			Log.d(TAG, "Parsing error!", e);
			finish();
			return;
		}
	}

	@Override
	public void onClick(View v) {
		// we just want to finish the activity
		finish();
	}
}
