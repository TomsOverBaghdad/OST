package com.tom.ost;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Telephony;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MessageCursorAdapter extends CursorAdapter {
	private LayoutInflater mCursorInflater;
	private final String LOG = "com.tom.ost.MessageCursorAdapter";
	private final Context mContext;
	//HACKY TODO FIX
	//have extra views for mms so that new view gets called appropriately
	private final int[] mRowLayouts= new int[]{R.layout.message_view_item_me, R.layout.message_view_item_them,
			R.layout.message_view_item_me, R.layout.message_view_item_them};
	private final HashSet<String> mAddresses = new HashSet<String>();
	
	public MessageCursorAdapter(Context context, Cursor c, int flags, String addresses) {
		super(context, c, flags);
		extractAddresses(addresses);
		mContext = context;
		mCursorInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
	}

	private void extractAddresses(String addresses) {
		String[] a = addresses.split(", ");
		for(String address : a){
			mAddresses.add(address);
			Log.e(LOG, address);
		}		
	}

	/**Returns the index of the row layout to use
	 * TELEPHONY CONSTANTS
	 *  		sms:TYPE	mms:MESSAGE_BOX	
	 * All		0			0
	 * INBOX	1			1			
 *---- SENT		2			2	--------is me
	 * DRAFTS	3			3
	 * OUTBOX	4			4			//pending out
	 * FAILED	5			-
	 * QUEUED	6			-			
	 * **/
	private int getItemViewType(Cursor c){
		int type;
		Log.i(LOG, "getviewItem --START");
		if (c.getColumnIndex(Telephony.BaseMmsColumns.CONTENT_TYPE) > 0){
			Log.i(LOG, "getviewItem --IS MMS");
			type = c.getInt(c.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_BOX));
			return type == 2 ? 2 : 3;//(2 = my MMS view, 3 = their MMS view)
				
        } else {
        	Log.i(LOG, "getviewItem --IS SMS");
    		type = c.getInt(c.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE));
    		return type == 2 ? 0 : 1;//(0 = my SMS view, 1 = their SMS view)
        }
	}
	
	@Override
	public int getItemViewType(int position) {
		Cursor c = (Cursor) getItem(position);
		return this.getItemViewType(c);
	}

	@Override
	public int getViewTypeCount() {
		return this.mRowLayouts.length;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		long date = cursor.getLong(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
		if(holder.isSms){
			Log.i(LOG, "bind view --IS SMS");
			holder.body.setText(cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY)));
		}else{//IS MMS
			Log.i(LOG, "bind view --IS MMS");
			//MMS date comes back 3 digits short (ie without trailing 0's) need 13 digits to normalize
			date *= 1000;
//			holder.body.setText("I'm Mr. Meeseeks look at me!");			
			String selectionPart = "mid=" + cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns._ID));
			Uri uri = Uri.parse("content://mms/part/");
			Cursor mmscursor = mContext.getContentResolver().query(uri, null,
			    selectionPart, null, null);
//			String body = new String();
			holder.body.setText("");
			if (mmscursor.moveToFirst()) {
			    do {
			        String partId = mmscursor.getString(mmscursor.getColumnIndex("_id"));
//			        int i = 0;
//			        for(String col : mcursor.getColumnNames()){
//				        Log.d(LOG, ">>>>>>>>>>>>>> column["+i+"] = "+ col);	
//				        i++;
//			        }	
			        String type = mmscursor.getString(mmscursor.getColumnIndex("ct"));
			        if ("text/plain".equals(type)) {
			            String data = mmscursor.getString(mmscursor.getColumnIndex("_data"));
			            if (data != null) {
			               // body += getMmsText(partId);
			                holder.body.append(getMmsText(partId));
			            } else {
//			                body += mmscursor.getString(mmscursor.getColumnIndex("text"));
			            	holder.body.append(mmscursor.getString(mmscursor.getColumnIndex("text")));
			            }
			        }
			        else if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
			                "image/gif".equals(type) || "image/jpg".equals(type) ||
			                "image/png".equals(type)) {
			            Bitmap bitmap = getMmsImage(partId);
			            appendBitmapToTextView(holder.body, bitmap);
			        }
			    } while (mmscursor.moveToNext());
			}
			mmscursor.close();
//			holder.body.setText(body);			
		}
		Log.i(LOG, "bind view --GOT DATE --> "+ date);
		holder.date.setText(DateUtils.getRelativeTimeSpanString(date));
		holder.image.setImageResource(R.drawable.ic_launcher);
	}

	private String getMmsText(String id) {
		Log.i(LOG, "getMms >>>>>> TEXT!");
	    Uri partURI = Uri.parse("content://mms/part/" + id);
	    InputStream is = null;
	    StringBuilder sb = new StringBuilder();
	    try {
	        is = mContext.getContentResolver().openInputStream(partURI);
	        if (is != null) {
	            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
	            BufferedReader reader = new BufferedReader(isr);
	            String temp = reader.readLine();
	            while (temp != null) {
	                sb.append(temp);
	                temp = reader.readLine();
	            }
	        }
	    } catch (IOException e) {}
	    finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {}
	        }
	    }
	    return sb.toString();
	}
	
	private Bitmap getMmsImage(String _id) {
	    Uri partURI = Uri.parse("content://mms/part/" + _id);
	    InputStream is = null;
	    Bitmap bitmap = null;
	    try {
	        is = mContext.getContentResolver().openInputStream(partURI);
	        bitmap = BitmapFactory.decodeStream(is);
	    } catch (IOException e) {}
	    finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {}
	        }
	    }
	    return bitmap;
	}
	
	//TODO move to utils
	//TODO probably learn exactly how some of this works
	//hacked it together so i could keep moving
	private void appendBitmapToTextView(TextView tView, Bitmap bitmap) {
		bitmap = scaleDown(bitmap, 500, true);
		String str = "\n        \n";
		int length = str.length();
	    SpannableStringBuilder builder = new SpannableStringBuilder(str);
	    ImageSpan image = new ImageSpan(mContext, bitmap, ImageSpan.ALIGN_BASELINE);
	    builder.setSpan(image,  2, length-2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    tView.append(builder);
	    
	}
	
	//TODO move to utils
	public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
	        boolean filter) {
	    float ratio = Math.min(
	            (float) maxImageSize / realImage.getWidth(),
	            (float) maxImageSize / realImage.getHeight());
	    int width = Math.round((float) ratio * realImage.getWidth());
	    int height = Math.round((float) ratio * realImage.getHeight());

	    Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
	            height, filter);
	    return newBitmap;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		Log.i(LOG, "newView");
		ViewHolder holder = new ViewHolder();
		int rowLayoutIndex = this.getItemViewType(cursor);
		View rowView = mCursorInflater.inflate(this.mRowLayouts[rowLayoutIndex], parent, false);
		if(rowLayoutIndex < 2){
			holder.isSms = true;
			//holder.body = (TextView) rowView.findViewById(R.id.MessageItemBody);
		}else{//is mms
			holder.isSms = false;
			//holder.body = (TextView) rowView.findViewById(R.id.MessageItemBody);
		}
		holder.body = (TextView) rowView.findViewById(R.id.MessageItemBody);
		holder.date = (TextView) rowView.findViewById(R.id.MessageItemDate);
		holder.image = (ImageView) rowView.findViewById(R.id.MessageItemImageView);
		
		rowView.setTag(holder);
		return rowView;
	}
	
	
	private static class ViewHolder {
		public boolean isSms;
		public TextView body;
		public TextView date;
		public ImageView image;//contact image
	}
	
}
