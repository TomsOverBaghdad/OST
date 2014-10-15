package com.tom.ost.services;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeMap;

import org.json.JSONObject;

import com.tom.ost.MessageActivity;
import com.tom.ost.OSTNotificationManager;
import com.tom.ost.Utils;


import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
/**
 * needs to be in the main package
 * **/
public class OSTMessagingService extends Service {

	private final static String TAG = "com.tom.ost.OSTMessagingService";
	public static final int SEND_SMS_MESSAGE = 0;
	public static final int SEND_MMS_MESSAGE = 1;
	public static final int CANCEL_MESSAGE = 2;
	public static final int UPDATE_TICK = 3;
	public static final int MESSAGE_SENT = 4;

	public static final String MESSAGE_SERVICE = "com.tom.ost.message_service";
	public static final String RESULT = "com.tom.ost.message_service.RESULT";
	public static final String TICK = "com.tom.ost.message_service.TICK";

	public HashSet<String> queue = new HashSet<String>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		queue = new HashSet<String>();
	}

	@Override
	public void onDestroy() {		
		// TODO when does a service get destroyed
		//what should i do?
		Toast.makeText(this, "service is being destroyed, "+queue.size() +" messages lost", Toast.LENGTH_LONG).show();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new OSTMessageHandler());

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {	
		Log.i(TAG, "BINDING");
//		Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
		return mMessenger.getBinder();
	}

	// message activity sends messages to this handler which get processed
	// in this case, the message would be the sms information
	// or it could be to cancel one of those messages
	private class OSTMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "INSIDE HANDLE MESSAGE");
			switch (msg.what) {
			case SEND_SMS_MESSAGE:
				final Bundle bundle = msg.getData();				
				//TODO startOST threaded
				this.post(new Runnable(){
					@Override
					public void run() {
						startOST(bundle);
					}					
				});
				break;
			case CANCEL_MESSAGE:
				if(queue.contains(msg.arg2)){
					queue.remove(msg.arg2);
					Toast.makeText(getApplicationContext(), "cancel inside service", Toast.LENGTH_SHORT).show();
					if(queue.isEmpty()){
						stopSelf();
					}
				}
				break;
			}
		}
	}
	
	
	//START WITH SMS IMPLEMENTAION
	public void startOST(final Bundle bundle) {
		final String address = bundle.getString(MessageActivity.THREAD_ADDRESSES);
		final String thread_id = bundle.getString(MessageActivity.THREAD_ID);
		final String thread_name = bundle.getString(MessageActivity.THREAD_NAME);
		final String body = bundle.getString("body");

        //TODO check that addresses are just one, or send multiple sms for each address
        //TODO might want to chck addresses before calling startOST

		queue.add(address);
		Toast.makeText(getApplicationContext(), "queue count "+queue.size(), Toast.LENGTH_SHORT).show();
		CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {// (10 sec countdown, 1 sec tics)
			@Override
			public void onTick(long millisUntilFinished) {
				if(!queue.contains(address)){
					Toast.makeText(getApplicationContext(), "ontick cancel", Toast.LENGTH_SHORT).show();
					this.cancel();
					return;
				}
				int tick = ((int) (millisUntilFinished / 1000));
				Intent messageIntent = new Intent(MESSAGE_SERVICE);
				messageIntent.putExtra(RESULT, UPDATE_TICK);
				messageIntent.putExtra(TICK, tick);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);				
				if(tick%2 == 0){
					Toast.makeText(getApplicationContext(), String.valueOf(tick), Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onFinish() {
				if(queue.contains(address)){
					Intent messageIntent = new Intent(MESSAGE_SERVICE);
					messageIntent.putExtra(RESULT, MESSAGE_SENT);	
					sendSms(thread_id, thread_name, address, body);		
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
					Toast.makeText(getApplicationContext(), "finish send", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(getApplicationContext(), "tick finsh, message not sent", Toast.LENGTH_SHORT).show();
				}
			}
		};
		countDownTimer.start();
	}
	
	private void sendSms(String thread_id, String thread_name, String address, String body){
		if(Utils.isDefaultSmsApp(getApplicationContext())){
			//store in db queued?
		}
		//the intent action should be unique in order to have multiple,
		//concurrent, pending intents
		String intentAction = TAG + "/" + address;
		Intent intent = new Intent(intentAction);//TODO change to more unique identifier for multple messages to each address
		intent.putExtra(MessageActivity.THREAD_ADDRESSES, address);
		intent.putExtra(MessageActivity.BODY, body);
		intent.putExtra(MessageActivity.THREAD_ID, thread_id);
		intent.putExtra(MessageActivity.THREAD_NAME, thread_name);
		
		PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String address = intent.getStringExtra(MessageActivity.THREAD_ADDRESSES);
				String body = intent.getStringExtra(MessageActivity.BODY);
				String thread_id = intent.getStringExtra(MessageActivity.THREAD_ID);
				String thread_name = intent.getStringExtra(MessageActivity.THREAD_NAME);

                int resultCode = getResultCode();
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    	Toast.makeText(getApplicationContext(), "received message sent PI", Toast.LENGTH_SHORT).show();
                    	//TODO if default sms, move from queued to sent;
                        break;
//                    case SmsManager.RESULT_ERROR_NO_SERVICE://details = "No service";
//                    case SmsManager.RESULT_ERROR_NULL_PDU://details = "Null PDU";
//                    case SmsManager.RESULT_ERROR_RADIO_OFF://details = "Radio off";
                    default:                    	
                    	OSTNotificationManager.notifyIncomingSms(getApplicationContext(), address, body, thread_id, thread_name, OSTNotificationManager.FAILED);
                        break;
                }
            	queue.remove(address);            	
                context.unregisterReceiver(this); 
                if(queue.isEmpty()){
            		Toast.makeText(context, "stop service", Toast.LENGTH_SHORT).show();
            		stopSelf();
            	}
			}		
		}, new IntentFilter(intentAction));		
		
		SmsManager.getDefault().sendTextMessage(address, null, body, sentIntent, null);
	}

}
