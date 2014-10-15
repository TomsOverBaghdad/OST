package com.tom.ost.receivers;

import com.tom.ost.Utils;
import com.tom.ost.services.IntentMessagingService;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;


public class OSTMessagingReceiver extends WakefulBroadcastReceiver {

	@Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        // If on KitKat+ and default messaging app then look for new deliver actions actions.
        if (Utils.hasKitKat() && Utils.isDefaultSmsApp(context)) {
            if (Intents.SMS_DELIVER_ACTION.equals(action)) {
                handleIncomingSms(context, intent);
            } else if (Intents.WAP_PUSH_DELIVER_ACTION.equals(action)) {
                handleIncomingMms(context, intent);
            }
        } else { // Otherwise look for old pre-KitKat actions
            if (Intents.SMS_RECEIVED_ACTION.equals(action)) {
//                handleIncomingSms(context, intent);

//            	Bundle extras = intent.getExtras();
//        		if (extras == null)
//        			return;		
//        		Object[] smsextras = (Object[]) extras.get("pdus");
//        		for (int i = 0; i < smsextras.length; i++) {
//        			SmsMessage message = SmsMessage.createFromPdu((byte[]) smsextras[i]);
//        			
//        		}	
            	
            	
            	
            	
            	
            	
            	
            } else if (Intents.WAP_PUSH_RECEIVED_ACTION.equals(action)) {
                //handleIncomingMms(context, intent);
            }
        }
    }
    private void handleIncomingSms(Context context, Intent intent) {
        // TODO: Handle SMS here
        // As an example, we'll start a wakeful service to handle the SMS
        intent.setAction(IntentMessagingService.ACTION_MY_RECEIVE_SMS);
        intent.setClass(context, IntentMessagingService.class);
        startWakefulService(context, intent);
    }
    private void handleIncomingMms(Context context, Intent intent) {
        // TODO: Handle MMS here
        // As an example, we'll start a wakeful service to handle the MMS
        intent.setAction(IntentMessagingService.ACTION_MY_RECEIVE_MMS);
        intent.setClass(context, IntentMessagingService.class);
        startWakefulService(context, intent);
    }

}
