package com.tom.ost;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

public class OSTNotificationManager {
	public final static int FAILED = -1;
	public final static int RECEIVED = 0;
	public static NotificationManager mNotificationManager;
	public static NotificationCompat.Builder mNotifyBuilder;
	private static int notifyID = 1776;//because America
	
	private OSTNotificationManager(){		
	}

    public static void notifyIncomingMms(Context context, String str){
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("MMS")
                .setContentText(str)
                .setSmallIcon(R.drawable.ic_launcher_ost)
//                .setContentIntent(pIntent)
                .setAutoCancel(true);
        // Because the ID remains unchanged, the existing notification is updated.
        mNotificationManager.notify(notifyID, mNotifyBuilder.build());
    }


	public static void notifyIncomingSms(Context context, String thread_address, String body, String thread_id, 
			String thread_name, int type) {
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(thread_id == null || thread_id == "not found"){//TODO should probably change the not found to something less hacky
			if(thread_address != null && thread_address != "not found"){//dont think this is ever a problem???			
				String recipient_id = Utils.getContactRecipientIdFromAddress(context, thread_address);
				thread_id = Utils.getThreadIdFromRecipientId(context, recipient_id);//TODO need a backup if thread not found?
				thread_name = Utils.getContactNameFromAddress(context, thread_address);
			}
			else{//address cant be null
				Toast.makeText(context, "NOTIFICATION: null address", Toast.LENGTH_SHORT).show();
				return;
			}
		}		
		
		Intent notificationIntent = new Intent(context, MessageActivity.class);
				notificationIntent.putExtra(MessageActivity.THREAD_ID, thread_id);
				notificationIntent.putExtra(MessageActivity.THREAD_NAME, thread_name);
				notificationIntent.putExtra(MessageActivity.THREAD_ADDRESSES, thread_address);
				
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
		stackBuilder.addParentStack(MessageActivity.class);
		stackBuilder.addNextIntent(notificationIntent);
		
		PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		String title;
		int icon;
		switch(type){
			case FAILED:
				icon = R.drawable.ic_launcher;
				title = "Message Failed";
				body = new String("Message was not sent");
				break;
			case RECEIVED:
			default:
				icon = R.drawable.ic_launcher_ost;
				title = "New Message";
				break;
		}
		
		mNotifyBuilder = new NotificationCompat.Builder(context)
		    .setContentTitle(title)
		    .setContentText(body)
		    .setSmallIcon(icon)
		    .setContentIntent(pIntent)
		    .setAutoCancel(true);
		//TODO update if there are multiple messages//currently shows the most recent

		// Because the ID remains unchanged, the existing notification is updated.
		mNotificationManager.notify(notifyID, mNotifyBuilder.build());
		
	}

}
