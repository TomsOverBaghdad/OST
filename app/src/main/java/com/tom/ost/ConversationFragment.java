package com.tom.ost;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ConversationFragment extends Fragment implements LoaderCallbacks<Cursor>{
	private static final int LOADER_ID = 1990;
	private final String LOG = "com.tom.OST.ConversationFragment";
	private final String SORT_ORDER = Telephony.ThreadsColumns.DATE + " DESC";
	private ListView mConversationListView;
	private ConversationCursorAdapter mConversationCursorAdapter;
//	private Cursor mCursor;
	private Context mContext;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(container == null){
			return null;
		}
		View view = (LinearLayout) inflater.inflate(R.layout.conversation_layout, container, false);
		mContext = view.getContext();
		mConversationCursorAdapter = new ConversationCursorAdapter(mContext, null, 0);

		mConversationListView = (ListView)view.findViewById(R.id.ConversationList);		
	    mConversationListView.setAdapter(mConversationCursorAdapter);
	    mConversationListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ConversationTag ct = (ConversationTag) view.getTag();
				Context context = parent.getContext();
				Intent messageActivityIntent = new Intent(context, MessageActivity.class);
				messageActivityIntent.putExtra(MessageActivity.THREAD_ID, ct.thread_id);
				messageActivityIntent.putExtra(MessageActivity.THREAD_NAME, ct.name);
				messageActivityIntent.putExtra(MessageActivity.THREAD_ADDRESSES, ct.addresses);
				context.startActivity(messageActivityIntent);				
			}	    	
	    });    
	    
		// footer stuff
		// View footerView =
		// this.getLayoutInflater().inflate(R.layout.conversation_footer, null);
		// footerTextView = (TextView)
		// footerView.findViewById(R.id.ConversationFooterTextView);
		//
		// this.getListView().addFooterView(footerView);
	    
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		//TODO footerTextView.setText(String.format("You have %d conversations", objects.length));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle bundle) {
		//TODO needed to add simple=true for some unknown reason, find out if this will work on non samsung devices
		Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
		return new CursorLoader(getActivity(), uri, 
				new String[]{Telephony.ThreadsColumns._ID, Telephony.ThreadsColumns.RECIPIENT_IDS,
					Telephony.ThreadsColumns.SNIPPET}, null, null, SORT_ORDER);
//		Uri uri = Uri.parse("content://mms-sms/conversations/");
//		return new CursorLoader(getActivity(), uri, new String[]{"*"}, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
		this.mConversationCursorAdapter.swapCursor(newCursor);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.mConversationCursorAdapter.swapCursor(null);
	}

}
