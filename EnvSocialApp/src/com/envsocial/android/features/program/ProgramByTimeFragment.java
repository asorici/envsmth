package com.envsocial.android.features.program;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.SimpleCursorLoader;

public class ProgramByTimeFragment extends ProgramFragment 
		implements OnClickListener, OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "ProgramByTimeFragment";
	
	private ProgramActivity mParentActivity;
	private ProgramFeature mProgramFeature;
	
	private int mCurrentDayIndex = 0;
	
	private SimpleCursorAdapter mProgramAdapter;
	private LinearLayout mDayScroll;
	private ListView mProgramListView;
	private ProgressDialog mProgramLoaderDialog;
	private List<String> mDistinctProgramDays;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mParentActivity = (ProgramActivity) getActivity();
	    mProgramFeature = (ProgramFeature) mParentActivity.getFeature();
	    mProgramDisplayType = ProgramFragment.TIME_DISPLAY_TYPE;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] onCreateView called.");
		
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.program_by_time, container, false);
		
		// get day scroll and list views
		mDayScroll = (LinearLayout) view.findViewById(R.id.dayScroll);
		mProgramListView = (ListView) view.findViewById(R.id.program_by_time);
		
		// Create and set adapter
		String[] from = new String[] {	
				ProgramDbHelper.COL_PRESENTATION_TITLE, 
				ProgramDbHelper.COL_PRESENTATION_START_TIME,
				ProgramDbHelper.COL_PRESENTATION_END_TIME,
				ProgramDbHelper.COL_SESSION_LOCATION_NAME,
				ProgramFeature.SESSION };
		
		int[] to = new int[] {
				R.id.program_presentation_title,
				R.id.program_presentation_start_hour,
				R.id.program_presentation_end_hour,
				R.id.program_presentation_locationName,
				R.id.program_presentation_session };
		
		mProgramAdapter = new SimpleCursorAdapter(getActivity(), R.layout.program_row, null, from, to, 0);
		mProgramListView.setAdapter(mProgramAdapter);
		mProgramListView.setOnItemClickListener(this);
		
		
		return view;
	}
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	    
	    // start setup for data - get the distinct days and load data for first day
	 	loadData();
	}
	
	
	private void loadData() {
		// ======== get distinct program days ========
		if (mDistinctProgramDays == null) {
			mDistinctProgramDays = mProgramFeature.getDistinctDays();
			
			Log.d(TAG, "distinct program days: " + mDistinctProgramDays);
			
			int k = 0;
			int numDays = mDistinctProgramDays.size();
			
			if (numDays > 1) {
				for (int i = 0; i < numDays; i++) {
					String d = mDistinctProgramDays.get(i);
					TextView dayView = new TextView(getActivity());
		
					try {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						Date date = formatter.parse(d);
						formatter = new SimpleDateFormat("EEE, MMM d");
						dayView.setText(formatter.format(date));
					} catch (ParseException e) {
						e.printStackTrace();
						dayView.setText(d);
					}
					
					LinearLayout.LayoutParams dayViewParams = 
							new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
					//dayView.setLayoutParams(dayViewParams);
					
					dayView.setTag(k ++);
					dayView.setOnClickListener(this);
					dayView.setPadding(15, 15, 15, 15);
					dayView.setTextColor(getResources().getColor(R.color.envived_order_text_dark_green));
					dayView.setBackgroundDrawable(getResources().getDrawable(R.drawable.envived_default_green_tab_indicator_ab));
					mDayScroll.addView(dayView, dayViewParams);
				}
			
				mDayScroll.getChildAt(mCurrentDayIndex).setBackgroundDrawable(getResources().getDrawable(R.drawable.envived_default_actionbar_tab_selected_pressed));
			}
		}
		
		
		// ======== start the loader days ========
		Bundle loaderArgs = new Bundle();
		loaderArgs.putSerializable(Feature.PROGRAM, mProgramFeature);
		loaderArgs.putInt("selected_program_day", mCurrentDayIndex);
		
		getLoaderManager().initLoader(mProgramDisplayType, null, this);
	}
	

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		if (mProgramLoaderDialog == null) {
			mProgramLoaderDialog = getProgressDialogInstance(getActivity());
			mProgramLoaderDialog.show();
		}
		else {
			if (!mProgramLoaderDialog.isShowing()) {
				mProgramLoaderDialog.show();
			}
		}
		
		return new ProgramCursorLoader(getActivity(), mProgramFeature, mDistinctProgramDays, mCurrentDayIndex);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mProgramAdapter.swapCursor(cursor);
		//mProgramAdapter.notifyDataSetChanged();
		
		if (mProgramLoaderDialog != null && mProgramLoaderDialog.isShowing()) {
			mProgramLoaderDialog.cancel();
			mProgramLoaderDialog = null;
		}
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mProgramAdapter.swapCursor(null);
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
		Cursor c = (Cursor)mProgramAdapter.getItem(position);
		
		int presentationIdColumnIndex = c.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_ID);
		int presentationId = c.getInt(presentationIdColumnIndex);
		
		Intent i = new Intent(getActivity(), PresentationDetailsActivity.class);
		Bundle extras = new Bundle();
		extras.putInt(ProgramFeature.PRESENTATION_ID, presentationId);
		extras.putSerializable("program_feature", mProgramFeature);
		extras.putSerializable("location", mParentActivity.getFeatureLocation());
		i.putExtras(extras);
		
		startActivity(i);
	}
	
	@Override
	public void onClick(View v) {
		if (v instanceof TextView) {
			mDayScroll.getChildAt(mCurrentDayIndex).setBackgroundDrawable(getResources().getDrawable(R.drawable.envived_default_green_tab_indicator_ab));
			v.setBackgroundDrawable(getResources().getDrawable(R.drawable.envived_default_actionbar_tab_selected_pressed));
			
			mCurrentDayIndex = (Integer) v.getTag();
			getLoaderManager().restartLoader(mProgramDisplayType, null, this);
		}
	}

	@Override
	protected void handleProgramUpdate(ProgramFeature updatedProgramFeature) {
		// TODO - restart loader
		mProgramFeature = updatedProgramFeature;
		
		if (mProgramLoaderDialog != null) {
			mProgramLoaderDialog.cancel();
			mProgramLoaderDialog.show();
		}
		else {
			mProgramLoaderDialog = getProgressDialogInstance(getActivity());
			mProgramLoaderDialog.show();
		}
		
		getLoaderManager().restartLoader(mProgramDisplayType, null, this);
	}
	
	
	private ProgressDialog getProgressDialogInstance(Context context) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setTitle("Retrieving program data ...");
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		
		return dialog;
	}
	
	
	// ##################################### Helper static classes #####################################
	
	static class ProgramCursorLoader extends SimpleCursorLoader {
		private ProgramFeature mProgramFeature;
		private int mSelectedDayIndex;
		private List<String> mDistinctProgramDays;
		
		public ProgramCursorLoader(Context context, ProgramFeature programFeature,
				List<String> distinctProgramDays, int selectedDayIndex) {
			super(context);
			mProgramFeature = programFeature;
			mSelectedDayIndex = selectedDayIndex;
			mDistinctProgramDays = distinctProgramDays;
		}

		@Override
		public Cursor loadInBackground() {
			String selectedDayString = mDistinctProgramDays.get(mSelectedDayIndex);
			Cursor cursor = mProgramFeature.getPresentationsByDay(selectedDayString);

			return cursor;
		}
	}
}
