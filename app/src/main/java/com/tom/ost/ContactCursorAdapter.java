package com.tom.ost;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactCursorAdapter extends CursorAdapter {
	private final String LOG = "com.tom.ost.ContactsCursorAdapter";
	private final int mItemLayout = R.layout.contacts_view_item;
	private LayoutInflater mCursorInflater;

	public ContactCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mCursorInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		// get views
		ImageView contactImageView = (ImageView) view.findViewById(R.id.ContactImageView);
		TextView contactNameTextView = (TextView) view.findViewById(R.id.ContactNameTextView);
		TextView contactNumberTextView = (TextView) view.findViewById(R.id.ContactNumberTextView);
		// get data
		String id = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
		String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		String number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
		
//		String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
//		String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//		String number = "0";//c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
//		Cursor pCur = null;
//		try {
//			pCur = context.getContentResolver().query(
//			ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
//					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//					new String[] { id }, null);
//			while(pCur.moveToNext() && number.equals("0")){
//				//print out the column names
////				Log.e(LOG, "rows " + pCur.getCount() + " columns " + pCur.getColumnCount());
////				String str = new String();
////				for(String col : pCur.getColumnNames()){
////					str += col + ", ";
////				}
////				Log.e(LOG, str);
//				number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
//			}
//
//		} catch (Exception e) {
//			number = "-1";
//			Log.e(LOG, "EXCEPTION :) "+ e.toString());
//		}finally{
//			if(pCur != null)
//				pCur.close();
//		}
		
		// save the thread id to get the messages for the conversation detail
		view.setTag(id);
		// set view stuff
		contactImageView.setImageResource(R.drawable.ic_launcher);
		contactNameTextView.setText(name + " id => " + id);
		contactNumberTextView.setText(number);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mCursorInflater.inflate(mItemLayout, parent, false);
	}

}
