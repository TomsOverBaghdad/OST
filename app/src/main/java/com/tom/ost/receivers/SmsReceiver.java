package com.tom.ost.receivers;

import com.tom.ost.OSTNotificationManager;
import com.tom.ost.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver{//extends OSTMessagingReceiver{
	 @Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			if (extras == null)
				return;		
			Object[] smsextras = (Object[]) extras.get("pdus");
			for (int i = 0; i < smsextras.length; i++) {
				SmsMessage message = SmsMessage.createFromPdu((byte[]) smsextras[i]);
				//TODO add to db if default
				String thread_address = message.getOriginatingAddress();
				String body = message.getMessageBody();
				String thread_id = Utils.getThreadIdFromRecipientId(context,
									Utils.getContactRecipientIdFromAddress(context, thread_address));
				String thread_name = Utils.getContactNameFromAddress(context, thread_address);
				//notify user		
				OSTNotificationManager.notifyIncomingSms(context, thread_address, body, thread_id, thread_name, OSTNotificationManager.RECEIVED);
			}
	    }
}
