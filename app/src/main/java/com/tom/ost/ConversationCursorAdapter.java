package com.tom.ost;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ConversationCursorAdapter extends CursorAdapter {
	private final String LOG = "com.tom.ost.ConverationCursorAdapter";
	private final int mItemLayout = R.layout.conversation_view_item;
	private LayoutInflater mCursorInflater;
	
	public ConversationCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mCursorInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		//get views
		ImageView conversationImageView = (ImageView) view.findViewById(R.id.ConversationImageView);
		TextView conversationNameTextView = (TextView) view.findViewById(R.id.ConversationNameTextView);
		TextView conversationLastMessageTextView = (TextView) view.findViewById(R.id.ConversationLastMessageTextView);
		//get data	
		String id = c.getString(c.getColumnIndex(Telephony.ThreadsColumns._ID));
		String rawRecipientId = c.getString(c.getColumnIndex(Telephony.ThreadsColumns.RECIPIENT_IDS));
		String snippet = c.getString(c.getColumnIndex(Telephony.ThreadsColumns.SNIPPET));

		
		String conversationName = new String(rawRecipientId);
		String conversationAddresses = new String();
		String[] recipientIdArray = rawRecipientId.split(" ");
		String address;
        //TODO speed this up, try to do query in the background or use a hash map
		for(int i = 0; i < recipientIdArray.length ; i++){
            address = Utils.getContactAddressFromRecipientId(context, recipientIdArray[i]);
			conversationAddresses += address;
			conversationName += Utils.getContactNameFromAddress(context, address);
			if(i < recipientIdArray.length - 1)
				conversationName += ", ";
		}
		
		
		//set view stuff
		conversationImageView.setImageResource(R.drawable.ic_launcher);
		conversationNameTextView.setText(conversationName);
		conversationLastMessageTextView.setText(snippet);
		
		//save the thread id to get the messages for the conversation detail
		ConversationTag ct = new ConversationTag();
		ct.thread_id = id;
		ct.name = conversationName;
		ct.addresses = conversationAddresses;
		view.setTag(ct);
	}

	@Override
	public View newView(Context context, Cursor c, ViewGroup parent) {
		return mCursorInflater.inflate(mItemLayout, parent, false);
	}

}
