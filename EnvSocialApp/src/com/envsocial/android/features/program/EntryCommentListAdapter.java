package com.envsocial.android.features.program;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.envsocial.android.R;

public class EntryCommentListAdapter extends BaseAdapter {
	private Context mContext;
	private List<Map<String, String>> mData;
	private LayoutInflater mInflater;
	
	public EntryCommentListAdapter(Context context, LinkedList<Map<String,String>> data) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mData = data;
	}
	
	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void addItem(Map<String,String> data) {
		mData.add(0, data);
		notifyDataSetChanged();
	}
	
	public void removeItem(int position) {
		mData.remove(position);
		notifyDataSetChanged();
	}
	
	public void appendItems(List<Map<String, String>> newItems) {
		mData.addAll(newItems);
		notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.entry_comment_row, parent, false);
			
			// Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
			holder = new ViewHolder();
			holder.author = (TextView)convertView.findViewById(R.id.entry_comment_author);
			holder.date = (TextView)convertView.findViewById(R.id.entry_comment_date);
			holder.text = (TextView)convertView.findViewById(R.id.entry_comment_text);
			
			convertView.setTag(holder);
		}
		else {
			// Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
		}
		
		bind(holder, position);
		
		return convertView;
	}
	
	
	private void bind(ViewHolder holder, int position) {
		Map<String, String> commentData = mData.get(position);
		
		holder.author.setText(commentData.get("author"));
		holder.date.setText(commentData.get("date"));
		holder.text.setText(commentData.get("text"));
	}
	
	static class ViewHolder {
		TextView author;
		TextView date;
		TextView text;
	}
}
