package com.envsocial.android.features.program;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
	
	private GestureDetector mGestureDetector;
	private View.OnTouchListener mGestureListener;
	
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
		
		mGestureDetector = new GestureDetector(new ProgramOnGestureListener(null));
		mGestureListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		};
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
	
	private void bind(ViewHolder holder, int position) {
		Map<String,String> entry = getItem(position);
		List<Map<String,String>> overlapping = 
			mProgramDb.getOverlappingEntries(entry.get(ProgramDbHelper.COL_ENTRY_ID));
		System.out.println("[DEBUG] >> Overlapping etries: " + overlapping);
		holder.bind(entry, overlapping);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.program_row, parent, false);
			holder = new ViewHolder(mInflater, mGestureListener, DEFAULT_ENTRIES_PER_ROW);
			holder.flipper = (ViewFlipper) convertView.findViewById(R.id.flipper);
			
			final GestureDetector gestureDetector = new GestureDetector(new ProgramOnGestureListener(holder.flipper));
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
		private View.OnTouchListener mGestureListener;
		
		ViewFlipper flipper;
		List<EntryHolder> entries;
		private int mEntries;
		
		ViewHolder(LayoutInflater inflater, View.OnTouchListener gestureListener, int n) {
			mInflater = inflater;
			mGestureListener = gestureListener;
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
			
//			holder.layout.setOnTouchListener(mGestureListener);
			
			entries.add(holder);
		}
		
		// Bind data to view, adding all necessary entries to the flipper
		public void bind(Map<String,String> entry, List<Map<String,String>> overlapping) {
			// Make sure to clear flipper before binding data
			flipper.removeAllViews();
			
			System.out.println("[DEBUG] >> Binding entries to ViewHolder");
			// Make sure we have enough layouts
			int layouts = entries.size();
			int diff = (overlapping.size() + 1) - layouts;
			System.out.println("[DEBUG] >> We need to inflate #layouts: " + diff);
			for (int i = 0; i < diff; ++ i) {
				System.out.println("[DEBUG] >> Inflating layout!");
				inflateEntryLayout();
			}
			
			// Bind data to layouts
			mEntries = 0;
			System.out.println("[DEBUG] >> Binding data for first entry");
			EntryHolder eh = entries.get(mEntries ++); 
			eh.bind(entry);
			flipper.addView(eh.layout);
			for (Map<String,String> e : overlapping) {
				System.out.println("[DEBUG] >> Binding data for alternative #" + mEntries);
				eh = entries.get(mEntries ++);
				eh.bind(e);
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
		
		public void bind(Map<String,String> entry) {
			time.setText(
					formatTime(
						entry.get(ProgramDbHelper.COL_ENTRY_START_TIME), 
						entry.get(ProgramDbHelper.COL_ENTRY_END_TIME)
					)
				);
			// TODO
			session.setText("Session 1");
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
		
		private ViewFlipper mFlipper;
		
		ProgramOnGestureListener(ViewFlipper flipper) {
			mFlipper = flipper;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, 
				float velocityX, float velocityY) {
			
			System.out.println("[DEBUG] >> onFilng velocityX: " + velocityX);
			
			if (velocityX > 0) {
//				Toast.makeText(mContext, "Swipe left", Toast.LENGTH_SHORT).show();
				mFlipper.showPrevious();
			} else {
//				Toast.makeText(mContext, "Swipe right", Toast.LENGTH_SHORT).show();
				mFlipper.showNext();
			}
			
			return false;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
	}

}
