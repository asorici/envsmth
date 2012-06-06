package com.envsocial.android.features.program;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.envsocial.android.R;

public class ProgramListAdapter extends BaseAdapter {

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
	
	private void bind(ViewHolder holder, int position) {
		Map<String,String> entry = getItem(position);
		holder.time.setText(
					formatTime(
						entry.get(ProgramDbHelper.COL_ENTRY_START_TIME), 
						entry.get(ProgramDbHelper.COL_ENTRY_END_TIME)
					)
				);
		// TODO
		holder.session.setText("Session 1");
		holder.title.setText(entry.get(ProgramDbHelper.COL_ENTRY_TITLE));
		holder.speakers.setText(entry.get(ProgramDbHelper.COL_ENTRY_SPEAKERS));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.program_entry, parent, false);
			
			holder = new ViewHolder();
			holder.time = (TextView) convertView.findViewById(R.id.time);
			holder.session = (TextView) convertView.findViewById(R.id.session);
			holder.title = (TextView) convertView.findViewById(R.id.title);
			holder.speakers = (TextView) convertView.findViewById(R.id.speakers);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		bind(holder, position);
		
		return convertView;
	}
	
	static class ViewHolder {
		TextView time;
		TextView session;
		TextView title;
		TextView speakers;
	}

}
