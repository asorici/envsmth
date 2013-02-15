package com.envsocial.android.features.program;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.features.program.PresentationCommentsActivity.PresentationComment;
import com.envsocial.android.utils.Utils;

public class PresentationCommentListAdapter extends BaseAdapter {
	private Context mContext;
	private List<PresentationComment> mCommentList;
	private LayoutInflater mInflater;
	
	public PresentationCommentListAdapter(Context context, LinkedList<PresentationComment> commentList) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mCommentList = commentList;
	}
	
	@Override
	public int getCount() {
		return mCommentList.size();
	}

	@Override
	public Object getItem(int position) {
		return mCommentList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void addItem(PresentationComment comment, boolean append) {
		if (append) {
			mCommentList.add(comment);
		}
		else {
			mCommentList.add(0, comment);
		}
		notifyDataSetChanged();
	}
	
	public void removeItem(int position) {
		mCommentList.remove(position);
		notifyDataSetChanged();
	}
	
	public void addAllItems(List<PresentationComment> newComments, boolean append) {
		if (append) {
			mCommentList.addAll(newComments);
		}
		else {
			mCommentList.addAll(0, newComments);
		}
		notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.program_presentation_comment_row, parent, false);
			
			// Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
			holder = new ViewHolder();
			holder.wrapperLayout = (LinearLayout) convertView.findViewById(R.id.presentation_comment_row_wrapper);
			holder.author = (TextView)convertView.findViewById(R.id.presentation_comment_author);
			holder.date = (TextView)convertView.findViewById(R.id.presentation_comment_date);
			holder.text = (TextView)convertView.findViewById(R.id.presentation_comment_text);
			
			convertView.setTag(holder);
		}
		else {
			// Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
		}
		
		Resources r = mContext.getResources();
		int marginPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, r.getDisplayMetrics());
		
		LinearLayout.LayoutParams marginLayoutParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT);
		if (position % 2 == 0) {
			// to the left
			marginLayoutParams.rightMargin = marginPx;
			marginLayoutParams.gravity = Gravity.LEFT;
		}
		else {
			// to the right
			marginLayoutParams.leftMargin = marginPx;
			marginLayoutParams.gravity = Gravity.RIGHT;
		}
		
		holder.wrapperLayout.setLayoutParams(marginLayoutParams);
		
		bind(holder, position);
		
		return convertView;
	}
	
	
	private void bind(ViewHolder holder, int position) {
		PresentationComment commentData = mCommentList.get(position);
		
		holder.author.setText(commentData.getCommentOwner());
		holder.text.setText(commentData.getCommentContent());
		
		Calendar timestamp = commentData.getCommentTimestamp();
		String timestampString = Utils.calendarToString(timestamp, "dd MMM, HH:mm");
		holder.date.setText(timestampString);
		
	}
	
	static class ViewHolder {
		LinearLayout wrapperLayout;
		
		TextView author;
		TextView date;
		TextView text;
	}
}
