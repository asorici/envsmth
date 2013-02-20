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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.utils.SimpleCursorLoader;

public class ProgramBySessionFragment extends ProgramFragment 
	implements OnClickListener, OnChildClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String TAG = "ProgramBySessionFragment";
	
	private ProgramActivity mParentActivity;
	private ProgramFeature mProgramFeature;
	
	private int mCurrentDayIndex = 0;
	private List<String> mDistinctProgramDays;
	
	private ProgramBySessionAdapter mSessionAdapter;
	private LinearLayout mDayScroll;
	private ExpandableListView mSessionListView;
	
	private ProgressDialog mProgramLoaderDialog;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mParentActivity = (ProgramActivity) getActivity();
	    mProgramFeature = (ProgramFeature) mParentActivity.getFeature();
	    mProgramDisplayType = ProgramFragment.SESSION_DISPLAY_TYPE;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.program_by_session, container, false);
		
		// get day scroll and list views
		mSessionListView = (ExpandableListView) view.findViewById(R.id.program_by_session);
		mDayScroll = (LinearLayout) view.findViewById(R.id.dayScroll);
		
		mSessionListView.setOnChildClickListener(this);
		
		return view;
	}
	
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    
	    loadData();
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy(); 
		
		if (mProgramLoaderDialog != null) {
			mProgramLoaderDialog.cancel();
			mProgramLoaderDialog = null;
		}
	}
	
	
	private void loadData() {
		if (mProgramFeature != null && mProgramFeature.isInitialized()) {
			// ======== get distinct program days ========
			if (mDistinctProgramDays == null) {
				mDistinctProgramDays = mProgramFeature.getDistinctDays();
				
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
			
			// Create and set adapter
			int groupLayout = R.layout.program_session_group;
			int childLayout = R.layout.program_session_item;
			
			String[] groupFrom = new String[] {
				ProgramDbHelper.COL_SESSION_TITLE
			};
			
			int[] groupTo = new int[] {
				R.id.program_session_title
			};
			
			
			String[] childFrom = new String[] {	
				ProgramDbHelper.COL_PRESENTATION_TITLE, 
				ProgramDbHelper.COL_PRESENTATION_START_TIME,
				ProgramDbHelper.COL_PRESENTATION_END_TIME,
				ProgramDbHelper.COL_SESSION_LOCATION_NAME,
			};
			
			int[] childTo = new int[] {
				R.id.program_presentation_title,
				R.id.program_presentation_start_hour,
				R.id.program_presentation_end_hour,
				R.id.program_presentation_locationName,
			};
			
			String selectedDayString = mDistinctProgramDays.get(mCurrentDayIndex);
			mSessionAdapter = new ProgramBySessionAdapter(mProgramFeature, selectedDayString, getActivity(), 
					null, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
			mSessionListView.setAdapter(mSessionAdapter);
			
			// ======== start the loader days ========
			getLoaderManager().initLoader(mProgramDisplayType, null, this);
		}
	}
	
	
	@Override
	protected void handleProgramInit(ProgramFeature initProgramFeature) {
		mProgramFeature = initProgramFeature;
		
		// start setup for data - get the distinct days and load data for first day
	    loadData();
	}
	
	
	@Override
	protected void handleProgramUpdate(ProgramFeature updatedProgramFeature) {
		mProgramFeature = updatedProgramFeature;

		if (mProgramLoaderDialog != null) {
			mProgramLoaderDialog.cancel();
			mProgramLoaderDialog = null;
		} 
		
		getLoaderManager().restartLoader(mProgramDisplayType, null, this);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (mProgramLoaderDialog == null && active) {
			mProgramLoaderDialog = getProgressDialogInstance(getActivity());
			mProgramLoaderDialog.show();
		}
		
		return new ProgramCursorLoader(getActivity(), mProgramFeature, mDistinctProgramDays, mCurrentDayIndex);
	}

	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mSessionAdapter.changeCursor(cursor);
		
		if (mProgramLoaderDialog != null) {
			mProgramLoaderDialog.cancel();
			mProgramLoaderDialog = null;
		}
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mSessionAdapter.changeCursor(null);
	}
	
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Cursor c = mSessionAdapter.getChild(groupPosition, childPosition);
		
		int presentationIdColumnIndex = c.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_ID);
		int presentationId = c.getInt(presentationIdColumnIndex);
		
		Intent i = new Intent(getActivity(), PresentationDetailsActivity.class);
		Bundle extras = new Bundle();
		extras.putInt(ProgramFeature.PRESENTATION_ID, presentationId);
		extras.putSerializable("program_feature", mProgramFeature);
		extras.putSerializable("location", mParentActivity.getFeatureLocation());
		i.putExtras(extras);
		
		startActivity(i);
		
		return true;
	}
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
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
			Cursor cursor = mProgramFeature.getSessionsByDay(selectedDayString);

			return cursor;
		}
	}
}
