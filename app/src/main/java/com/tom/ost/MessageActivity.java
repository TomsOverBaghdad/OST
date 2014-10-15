package com.tom.ost;


import com.tom.ost.services.OSTMessagingService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.provider.Telephony;
import android.provider.Telephony.MmsSms;

public class MessageActivity extends FragmentActivity {
	/**constants**/
		//for intents
	public static final String THREAD_ID= "com.tom.ost.MessageActivity.THREAD_ID";
	public static final String THREAD_NAME = "com.tom.ost.MessageActivity.THREAD_NAME";
	public static final String THREAD_ADDRESSES = "com.tom.ost.MessageActivity.THREAD_ADDRESSES";
	public static final String BODY = "com.tom.ost.MessageActivity.BODY";
	public static final String IS_NEW = "com.tom.ost.MessageActivity.IS_NEW";
	private static final String LOG = "com.tom.ost.MessageActivity";	
		//
	private final String NEW = "New Message";
	
	/**fields**/
	private String mThreadId;
	private String mConversationName;
	private String mConversationAddresses;
	private MessageCursorAdapter mMessageCursorAdapter;
	private ContentResolver mContentResolver;
	private Cursor mSmsCursor;
	private Cursor mMmsCursor;
	private boolean mIsNew;
	/**view fields**/
	private ListView mMessageListView;
	private View mNewMessageView;
	private EditText mMessageBodyEditText;
	private Button mMessageSendButton;
	private EditText mMessageAddressEditText;
	private Button mMessageSearchContactsButton;
	/**service fields**/
	private Intent serviceIntent;
    Messenger mService = null;
    boolean mBound;
    /** Class for interacting with the main interface of the service. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };
	
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.message_view);
		
		this.mIsNew = getIntent().getBooleanExtra(IS_NEW, false);
		if(mIsNew){
			this.mNewMessageView = this.findViewById(R.id.NewMessageView);
			this.mMessageAddressEditText = (EditText)this.findViewById(R.id.MessageAddressEditText);//duplicate
			//this.mMessageSearchContactsButton = (Button) this.findViewById(R.id.MessageSearchContactsButton);
			this.mNewMessageView.setVisibility(View.VISIBLE);
			this.setTitle(this.NEW);
			return;
		}
		this.mThreadId = getIntent().getStringExtra(THREAD_ID);
		this.mConversationName = getIntent().getStringExtra(THREAD_NAME);
		this.mConversationAddresses = getIntent().getStringExtra(THREAD_ADDRESSES);
		
		this.mMessageListView = (ListView) this.findViewById(R.id.MessageListView);
		this.mMessageBodyEditText = (EditText) this.findViewById(R.id.MessageBodyEditText);//duplicate
		//this.mMessageSendButton = (Button) this.findViewById(R.id.MessageSendButton);
		
		this.setTitle(this.mConversationName);
		this.mContentResolver = this.getContentResolver();
		this.mMessageCursorAdapter = new MessageCursorAdapter(this, null, 0, this.mConversationAddresses);
		this.mMessageListView.setAdapter(mMessageCursorAdapter);
		new GetMessagesTask().execute();
		serviceIntent = new Intent(this, OSTMessagingService.class);
		startService(serviceIntent);
	}
	
	@Override  
	protected void onStart() {
	    super.onStart();
		bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(messageServiceReceiver,
						new IntentFilter(OSTMessagingService.MESSAGE_SERVICE));
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getApplicationContext())
				.unregisterReceiver(messageServiceReceiver);
	}

	@Override
	protected void onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	    super.onStop();
	}
	
	
	
	//TODO fix
	//somewhat hacky, just trying to get sms to work
	public void onSendButtonClick(View v){
		if(this.mMessageBodyEditText.getText().length() > 0 && this.mConversationAddresses != null){
//			Toast.makeText(this, "send button clicked\n bound = " + mBound, Toast.LENGTH_SHORT).show();
			SendSms();
		}
	}
	
	public void SendSms(){
		if (!mBound) return;
		
		Toast.makeText(this, "send sms", Toast.LENGTH_SHORT).show();
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, OSTMessagingService.CANCEL_MESSAGE, 0, 0);
        try {
        	Bundle bundaru = new Bundle();
        	bundaru.putString("body",  mMessageBodyEditText.getText().toString());
        	bundaru.putString(THREAD_ID, this.mThreadId);
    		bundaru.putString(THREAD_NAME, this.mConversationName);
        	bundaru.putString(THREAD_ADDRESSES, this.mConversationAddresses);
        	msg.what = OSTMessagingService.SEND_SMS_MESSAGE;
        	msg.setData(bundaru);
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}
	
	private class GetMessagesTask extends AsyncTask<Void, Void, Cursor> {

	    protected Cursor doInBackground(Void...params) {
	    	mSmsCursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null,
					Telephony.Sms.THREAD_ID+"="+mThreadId, null, Telephony.Sms.DATE + " ASC");
			mMmsCursor = getContentResolver().query(Telephony.Mms.CONTENT_URI, null,
					Telephony.Mms.THREAD_ID+"="+mThreadId, null, Telephony.Mms.DATE + " ASC");
			return new SortedCursor(new Cursor[]{mSmsCursor, mMmsCursor}, "date");
	    }
	    
	    protected void onPostExecute(Cursor result) {
	    	if(mMessageCursorAdapter != null)
	    		mMessageCursorAdapter.swapCursor(result);
	    }
	}	
	
	
	
	private BroadcastReceiver messageServiceReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {            
	        Bundle bundle = intent.getExtras();
	        String threadId = bundle.getString(MessageActivity.THREAD_ID);
	        if(threadId != mThreadId){
	        	return;
	        }
	        //update the ui
	        int result = bundle.getInt(OSTMessagingService.RESULT);
	        switch(result){
	        case OSTMessagingService.UPDATE_TICK:
	        	int tick = bundle.getInt(OSTMessagingService.TICK);	        	
//				updateTick(mid, tick);
	        	break;
	        case OSTMessagingService.MESSAGE_SENT:
//	        	removeOST( mid);
	        	break;
	        }
	    }
	};
	
	
	
}
