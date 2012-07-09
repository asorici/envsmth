package com.envsocial.android.features.program;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.envsocial.android.R;

public class ProgramListAdapter extends BaseAdapter {

	static final int DEFAULT_ENTRIES_PER_ROW = 0;
	
	private Context mContext;
	private LayoutInflater mInflater;
	private ProgramDbHelper mProgramDb;
	
	private List<String> days;
	private Map<String,Map<String,String>> sessions;
	private List<List<Map<String,String>>> entries;
	
	private int currentDay = 0;
	
	public ProgramListAdapter(Context context, ProgramDbHelper programDb) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mProgramDb = programDb;
		
		sessions = programDb.getAllSessions();
		
		days = programDb.getDays();
		entries = new ArrayList<List<Map<String,String>>>();
		for (String d : days) {
			entries.add(programDb.getEntriesByDay(d));
		}
	}
	
	@Override
	public int getCount() {
		return entries.get(currentDay).size();
	}

	@Override
	public Map<String,String> getItem(int position) {
		return entries.get(currentDay).get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public boolean setDay(int d) {
		if (d < 0 || d >= days.size()) {
			return false;
		}
		
		if (currentDay != d) {
			currentDay = d;
			notifyDataSetChanged();
		}
		
		return true;
	}
	
	private void bind(ViewHolder holder, int position) {
		Map<String,String> entry = getItem(position);
		List<Map<String,String>> overlapping = 
			mProgramDb.getOverlappingEntries(entry.get(ProgramDbHelper.COL_ENTRY_ID));
//		System.out.println("[DEBUG] >> Overlapping etries: " + overlapping);
		
		holder.bind(entry, overlapping, sessions);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
//		System.out.println("[DEBUG] >> getView called for position: " + position);
		
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.program_row, parent, false);
			holder = new ViewHolder(mInflater, DEFAULT_ENTRIES_PER_ROW);
			holder.flipper = (ViewFlipper) convertView.findViewById(R.id.flipper);

			final GestureDetector gestureDetector = 
				new GestureDetector(
						new ProgramOnGestureListener(mContext, holder.flipper)
					);
			View.OnTouchListener gestureListener = new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return gestureDetector.onTouchEvent(event);
				}
			};
			
			holder.flipper.setOnTouchListener(gestureListener);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		bind(holder, position);
		
		return convertView;
	}
	
	static class ViewHolder {
		
		private LayoutInflater mInflater;
		private int mEntries;
		
		ViewFlipper flipper;
		List<EntryHolder> entries;
		
		ViewHolder(LayoutInflater inflater, int n) {
			mInflater = inflater;
			entries = new ArrayList<EntryHolder>();
			mEntries = 0;
			
			for (int i = 0; i < n; ++ i) {
				inflateEntryLayout();
			}
		}
		
		private void inflateEntryLayout() {
			View view = mInflater.inflate(R.layout.program_row_layout, null, false);
			
			EntryHolder holder = new EntryHolder();
			holder.layout = (LinearLayout) view.findViewById(R.id.entry_layout);
			holder.time = (TextView) view.findViewById(R.id.time);
			holder.session = (TextView) view.findViewById(R.id.session);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.speakers = (TextView) view.findViewById(R.id.speakers);
			
			entries.add(holder);
		}
		
		// Bind data to view, adding all necessary entries to the flipper
		public void bind(Map<String,String> entry, 
				List<Map<String,String>> overlapping, Map<String,Map<String,String>> sessions) {
			// Make sure to clear flipper before binding data
			flipper.removeAllViews();
			
//			System.out.println("[DEBUG] >> Binding entries to ViewHolder");
			// Make sure we have enough layouts
			int layouts = entries.size();
			int diff = (overlapping.size() + 1) - layouts;
//			System.out.println("[DEBUG] >> We need to inflate #layouts: " + diff);
			for (int i = 0; i < diff; ++ i) {
//				System.out.println("[DEBUG] >> Inflating layout!");
				inflateEntryLayout();
			}
			
			// Bind data to layouts
			mEntries = 0;
//			System.out.println("[DEBUG] >> Binding data for first entry");
			EntryHolder eh = entries.get(mEntries ++); 
			eh.bind(entry, sessions);
			flipper.addView(eh.layout);
			for (Map<String,String> e : overlapping) {
//				System.out.println("[DEBUG] >> Binding data for alternative #" + mEntries);
				eh = entries.get(mEntries ++);
				eh.bind(e, sessions);
				flipper.addView(eh.layout);
			}
		}
	}
	
	static class EntryHolder {
		LinearLayout layout;
		TextView time;
		TextView session;
		TextView title;
		TextView speakers;
		
		
		public void bind(Map<String,String> entry, Map<String,Map<String,String>> sessions) {
			String id = entry.get(ProgramDbHelper.COL_ENTRY_ID);
			layout.setTag(id);
			time.setText(
					formatTime(
						entry.get(ProgramDbHelper.COL_ENTRY_START_TIME), 
						entry.get(ProgramDbHelper.COL_ENTRY_END_TIME)
					)
				);
			Map<String,String> ses = sessions.get(entry.get(ProgramDbHelper.COL_ENTRY_SESSIONID));
			session.setText(ses.get(ProgramDbHelper.COL_SESSION_TITLE));
			title.setText(entry.get(ProgramDbHelper.COL_ENTRY_TITLE));
			speakers.setText(entry.get(ProgramDbHelper.COL_ENTRY_SPEAKERS));
		}
		
		private String formatTime(String startTime, String endTime) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date;
			
			try {
				date = formatter.parse(startTime);
				String start = 
					String.format("%02d", date.getHours()) + ":" + 
					String.format("%02d", date.getMinutes());
				date = formatter.parse(endTime);
				String end = 
					String.format("%02d", date.getHours()) + ":" + 
					String.format("%02d", date.getMinutes());
				return start + " - " + end;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			return startTime + " - " + endTime;
		}
	}
	
	class ProgramOnGestureListener extends SimpleOnGestureListener {
		
//		private final static int SWIPE_MIN_DISTANCE = 120;
//		private final static int SWIPE_MAX_OFF_PATH = 250;
//		private final static int SWIPE_THRESHOLD_VELOCITY = 200;
		
		private Context mContext;
		private ViewFlipper mFlipper;
		
		ProgramOnGestureListener(Context context, ViewFlipper flipper) {
			mContext = context;
			mFlipper = flipper;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, 
				float velocityX, float velocityY) {
			
/*			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}
			
			if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				System.out.println("[DEBUG] >> Swipe RIGHT: " + mFlipper.getDisplayedChild() + " / " + mFlipper.getChildCount());
				if  (mFlipper.getDisplayedChild() < mFlipper.getChildCount() - 1) {
					mFlipper.showNext();
				}
			}  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				System.out.println("[DEBUG] >> Swipe LEFT: " + mFlipper.getDisplayedChild() + " / " + mFlipper.getChildCount());
				if (mFlipper.getDisplayedChild() > 0) {
					mFlipper.showPrevious();
				}
			}*/
			
			if(velocityX < 0) {
				System.out.println("[DEBUG] >> Swipe RIGHT: " + mFlipper.getDisplayedChild() + " / " + mFlipper.getChildCount());
				if  (mFlipper.getDisplayedChild() < mFlipper.getChildCount() - 1) {
					mFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_in_left));
					mFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_out_left));
					mFlipper.showNext();
				}
			}  else {
				System.out.println("[DEBUG] >> Swipe LEFT: " + mFlipper.getDisplayedChild() + " / " + mFlipper.getChildCount());
				if (mFlipper.getDisplayedChild() > 0) {
					mFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_in_right));
					mFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext, R.anim.slide_out_right));
					mFlipper.showPrevious();
				}
			}
			
			return false;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			String id = (String) mFlipper
									.getCurrentView()
									.getTag();
			
			Intent intent = new Intent(mContext, EntryDetailsActivity.class);
			intent.putExtra(ProgramDbHelper.COL_ENTRY_ID, id);
			mContext.startActivity(intent);
			
			return true;
		}
	}
}
