package com.tom.ost.services;

import com.tom.ost.receivers.OSTMessagingReceiver;

import android.app.IntentService;
import android.content.Intent;

public class IntentMessagingService extends IntentService{

	private static final String TAG = "OSTMessagingService";
    // These actions are for this app only and are used by MessagingReceiver to start this service
    public static final String ACTION_MY_RECEIVE_SMS = "com.tom.ost.receivers.RECEIVE_SMS";
    public static final String ACTION_MY_RECEIVE_MMS = "com.tom.ost.receivers.RECEIVE_MMS";
    
    public IntentMessagingService() {
        super(TAG);
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String intentAction = intent.getAction();
            if (ACTION_MY_RECEIVE_SMS.equals(intentAction)) {
                // TODO: Handle incoming SMS
                // Ensure wakelock is released that was created by the WakefulBroadcastReceiver
            	 OSTMessagingReceiver.completeWakefulIntent(intent);
            } else if (ACTION_MY_RECEIVE_MMS.equals(intentAction)) {
                // TODO: Handle incoming MMS
                // Ensure wakelock is released that was created by the WakefulBroadcastReceiver
                OSTMessagingReceiver.completeWakefulIntent(intent);
            }
        }
    }
    
    
    
    
    
    
    
    
    /**
     * 
     * http://stackoverflow.com/questions/15114887/android-add-mms-to-database
     * 
     * **/
    
//    public static Uri insert(Context context, String[] to, String subject, byte[] imageBytes)
//    {
//        try
//        {           
//            Uri destUri = Uri.parse("content://mms");
//
//            // Get thread id
//            Set<String> recipients = new HashSet<String>();
//            recipients.addAll(Arrays.asList(to));
//            long thread_id = getOrCreateThreadId(context, recipients);
//            Log.e(">>>>>>>", "Thread ID is " + thread_id);
//
//            // Create a dummy sms
//            ContentValues dummyValues = new ContentValues();
//            dummyValues.put("thread_id", thread_id);
//            dummyValues.put("body", "Dummy SMS body.");
//            Uri dummySms = context.getContentResolver().insert(Uri.parse("content://sms/sent"), dummyValues);
//
//            // Create a new message entry
//            long now = System.currentTimeMillis();
//            ContentValues mmsValues = new ContentValues();
//            mmsValues.put("thread_id", thread_id);
//            mmsValues.put("date", now/1000L);
//            mmsValues.put("msg_box", MESSAGE_TYPE_OUTBOX);
//            //mmsValues.put("m_id", System.currentTimeMillis());
//            mmsValues.put("read", 1);
//            mmsValues.put("sub", subject);
//            mmsValues.put("sub_cs", 106);
//            mmsValues.put("ct_t", "application/vnd.wap.multipart.related");
//            mmsValues.put("exp", imageBytes.length);
//            mmsValues.put("m_cls", "personal");
//            mmsValues.put("m_type", 128); // 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
//            mmsValues.put("v", 19);
//            mmsValues.put("pri", 129);
//            mmsValues.put("tr_id", "T"+ Long.toHexString(now));
//            mmsValues.put("resp_st", 128);
//
//            // Insert message
//            Uri res = context.getContentResolver().insert(destUri, mmsValues);
//            String messageId = res.getLastPathSegment().trim();
//            Log.e(">>>>>>>", "Message saved as " + res);
//
//            // Create part
//            createPart(context, messageId, imageBytes);
//
//            // Create addresses
//            for (String addr : to)
//            {
//                createAddr(context, messageId, addr);
//            }
//
//            //res = Uri.parse(destUri + "/" + messageId);
//
//            // Delete dummy sms
//            context.getContentResolver().delete(dummySms, null, null);
//
//            return res;
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private static Uri createPart(Context context, String id, byte[] imageBytes) throws Exception
//    {
//        ContentValues mmsPartValue = new ContentValues();
//        mmsPartValue.put("mid", id);
//        mmsPartValue.put("ct", "image/png");
//        mmsPartValue.put("cid", "<" + System.currentTimeMillis() + ">");
//        Uri partUri = Uri.parse("content://mms/" + id + "/part");
//        Uri res = context.getContentResolver().insert(partUri, mmsPartValue);
//        Log.e(">>>>>>>", "Part uri is " + res.toString());
//
//        // Add data to part
//        OutputStream os = context.getContentResolver().openOutputStream(res);
//        ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
//        byte[] buffer = new byte[256];
//        for (int len=0; (len=is.read(buffer)) != -1;)
//        {
//            os.write(buffer, 0, len);
//        }
//        os.close();
//        is.close();
//
//        return res;
//    }
//
//    private static Uri createAddr(Context context, String id, String addr) throws Exception
//    {
//        ContentValues addrValues = new ContentValues();
//        addrValues.put("address", addr);
//        addrValues.put("charset", "106");
//        addrValues.put("type", 151); // TO
//        Uri addrUri = Uri.parse("content://mms/"+ id +"/addr");
//        Uri res = context.getContentResolver().insert(addrUri, addrValues);
//        Log.e(">>>>>>>", "Addr uri is " + res.toString());
//
//        return res;
//    }
    
    
    /*
     * from http://my.fit.edu/~vkepuska/ece5570/adt-bundle-windows-x86_64/sdk/sources/android-14/android/provider/Telephony.java
     * 
     * **/   
    
    
//    /**
//     * This is a single-recipient version of
//     * getOrCreateThreadId.  It's convenient for use with SMS
//     * messages.
//     */
//    public static long getOrCreateThreadId(Context context, String recipient) {
//        Set<String> recipients = new HashSet<String>();
//
//        recipients.add(recipient);
//        return getOrCreateThreadId(context, recipients);
//    }
//
//    /**
//     * Given the recipients list and subject of an unsaved message,
//     * return its thread ID.  If the message starts a new thread,
//     * allocate a new thread ID.  Otherwise, use the appropriate
//     * existing thread ID.
//     *
//     * Find the thread ID of the same set of recipients (in
//     * any order, without any additions). If one
//     * is found, return it.  Otherwise, return a unique thread ID.
//     */
//    public static long getOrCreateThreadId(
//            Context context, Set<String> recipients) {
//        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
//
//        for (String recipient : recipients) {
//            if (Mms.isEmailAddress(recipient)) {
//                recipient = Mms.extractAddrSpec(recipient);
//            }
//
//            uriBuilder.appendQueryParameter("recipient", recipient);
//        }
//
//        Uri uri = uriBuilder.build();
//        //if (DEBUG) Log.v(TAG, "getOrCreateThreadId uri: " + uri);
//
//        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
//                uri, ID_PROJECTION, null, null, null);
//        if (DEBUG) {
//            Log.v(TAG, "getOrCreateThreadId cursor cnt: " + cursor.getCount());
//        }
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    return cursor.getLong(0);
//                } else {
//                    Log.e(TAG, "getOrCreateThreadId returned no rows!");
//                }
//            } finally {
//                cursor.close();
//            }
//        }
//
//        Log.e(TAG, "getOrCreateThreadId failed with uri " + uri.toString());
//        throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
//    }
    
    
    
    
    
    
    
    
    
    
    

}
