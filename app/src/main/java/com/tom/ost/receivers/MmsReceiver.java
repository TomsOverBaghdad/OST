package com.tom.ost.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.tom.ost.OSTNotificationManager;
import com.tom.ost.SqliteWrapper;
import com.tom.ost.pdUtils.ContentType;
import com.tom.ost.pdUtils.EncodedStringValue;
import com.tom.ost.pdUtils.GenericPdu;
import com.tom.ost.pdUtils.MmsException;
import com.tom.ost.pdUtils.MultimediaMessagePdu;
import com.tom.ost.pdUtils.NotificationInd;
import com.tom.ost.pdUtils.PduBody;
import com.tom.ost.pdUtils.PduHeaders;
import com.tom.ost.pdUtils.PduParser;
import com.tom.ost.pdUtils.PduPart;
import com.tom.ost.pdUtils.PduPersister;
import com.tom.ost.transaction.HttpUtils;
import com.tom.ost.transaction.MmsConfig;
import com.tom.ost.transaction.Transaction;
import com.tom.ost.transaction.TransactionBundle;
import com.tom.ost.transaction.TransactionService;
import com.tom.ost.transaction.TransactionSettings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

import static com.tom.ost.pdUtils.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.tom.ost.pdUtils.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.tom.ost.pdUtils.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;

public class MmsReceiver extends BroadcastReceiver{
    private final String TAG = "com.tom.ost.receivers.MmsReceiver";
    private ConnectivityManager mConnectivityManager;
    private TelephonyManager mTelephonyManager;
//    private Context mContext;
//    private Handler mHandler;

    private class MmsTask extends AsyncTask<Intent, Void, Void>{
        Context mContext;
        public MmsTask(Context context){
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            ContentResolver cr = mContext.getContentResolver();
            int type = pdu.getMessageType();
            long threadId = -1;

            try {
                switch (type) {
                    case MESSAGE_TYPE_DELIVERY_IND:
                    case MESSAGE_TYPE_READ_ORIG_IND: {
                        threadId = findThreadId(mContext, pdu, type);
                        if (threadId == -1) {
                            // The associated SendReq isn't found, therefore skip
                            // processing this PDU.
                            break;
                        }

                        Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI);
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        ContentValues values = new ContentValues(1);
                        values.put(Telephony.Mms.THREAD_ID, threadId);
                        SqliteWrapper.update(mContext, cr, uri, values, null, null);
                        break;
                    }
                    case MESSAGE_TYPE_NOTIFICATION_IND: {
                        NotificationInd nInd = (NotificationInd) pdu;

                        if (MmsConfig.getTransIdEnabled()) {
                            byte[] contentLocation = nInd.getContentLocation();
                            if ('=' == contentLocation[contentLocation.length - 1]) {
                                byte[] transactionId = nInd.getTransactionId();
                                byte[] contentLocationWithId = new byte[contentLocation.length
                                        + transactionId.length];
                                System.arraycopy(contentLocation, 0, contentLocationWithId,
                                        0, contentLocation.length);
                                System.arraycopy(transactionId, 0, contentLocationWithId,
                                        contentLocation.length, transactionId.length);
                                nInd.setContentLocation(contentLocationWithId);
                            }
                        }

//                        if (!isDuplicateNotification(mContext, nInd)) {
                            Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI);
                            // Start service to finish the notification transaction.
                            Intent svc = new Intent(mContext, TransactionService.class);
                            svc.putExtra(TransactionBundle.URI, uri.toString());
                            svc.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                    Transaction.NOTIFICATION_TRANSACTION);
                            mContext.startService(svc);
//                        } else if (LOCAL_LOGV) {
//                            Log.v(TAG, "Skip downloading duplicate message: "
//                                    + new String(nInd.getContentLocation()));
//                        }
                        break;
                    }
                    default:
                        Log.e(TAG, "Received unrecognized PDU.");
                }
            }
//            catch (MmsException e) {
//                Log.e(TAG, "Failed to save the data from PUSH: type=" + type, e);
//            }
            catch (RuntimeException e) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            }

            Log.v(TAG, "PUSH Intent processed.");

            return null;
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        // Hold a wake lock for 5 seconds, enough to give any
                    // services we start time to take their own wake locks.
                    PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                            "MMS PushReceiver");
                    wl.acquire(5000);
                    new MmsTask(context).execute(intent);




     //   mContext = context;

//        Bundle bundle = intent.getExtras();
//        byte[] intentData = bundle.getByteArray("data");
//        int transactionId = bundle.getInt("transactionId");
//        int pduType = bundle.getInt("pduType");
//        byte[] intentHeader = bundle.getByteArray("header");
//        String header = new String(intentHeader);
//        PduParser pduParser = new PduParser(intentData);
//        GenericPdu intentGenericPdu = pduParser.parse();


//        Toast.makeText(mContext, String.format("mt=>%d, raw=>%s, header=>%s", intentGenericPdu.getMessageType(), new String(intentData), header), Toast.LENGTH_LONG).show();

//        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//        mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                TransactionSettings transactionSettings = new TransactionSettings(mContext, mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).getExtraInfo());
//                String contentLocation = transactionSettings.getMmscUrl();
//                long noToken = -1L;
//                try {
//                    if(!beginMmsConnectivity()){
//                        OSTNotificationManager.notifyIncomingMms(mContext, "couldnt connect");
//                        return;
//                    }
//                    ensureRouteToHost(mConnectivityManager, contentLocation, transactionSettings);
//                    byte[] rawPdu = HttpUtils.httpConnection(mContext, noToken, contentLocation, null, HttpUtils.HTTP_GET_METHOD, transactionSettings.isProxySet(), transactionSettings.getProxyAddress(), transactionSettings.getProxyPort());
//                    processPduAttachments(new PduParser(rawPdu).parse());
//                    OSTNotificationManager.notifyIncomingMms(mContext,"YAY IT WORKS");
//                } catch (Exception e) {
//                    OSTNotificationManager.notifyIncomingMms(mContext, "err"+e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        };
//        Thread thread = new Thread(runnable);
//        thread.start();
//        Toast.makeText(mContext, "content location: "+contentLocation, Toast.LENGTH_LONG).show();


    }




    private boolean beginMmsConnectivity() {
        try {
            int result = mConnectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS"/*Phone.FEATURE_ENABLE_MMS*/);
//            Toast.makeText(mContext, "connectivity result: "+result, Toast.LENGTH_LONG).show();
            NetworkInfo info = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
//            Toast.makeText(mContext, "con: "+info.isConnected()+", cause: "+info.getReason(), Toast.LENGTH_LONG).show();

            //http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.0_r1/com/android/internal/telephony/Phone.java
//            boolean isAvailable = info != null && info.isConnected() && result == 0/*Phone.APN_ALREADY_ACTIVE*/ &&
//                        !/*Phone.REASON_VOICE_CALL_ENDED*/"2GVoiceCallEnded".equals(info.getReason());
            return result == 1 || result == 0;//APN_ALREADY_ACTIVE= 0;APN_REQUEST_STARTED = 1;
        } catch(Exception e) {
           // Toast.makeText(mContext, "con err: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void ensureRouteToHost(ConnectivityManager cm, String url, TransactionSettings settings) throws IOException {
        int inetAddr;
        if (settings.isProxySet()) {
//            Toast.makeText(mContext, "ER2H: proxy set", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ER2H: proxy set");
            String proxyAddr = settings.getProxyAddress();
            inetAddr = lookupHost(proxyAddr);
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!cm.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr))
                    throw new IOException("Cannot establish route to proxy " + inetAddr);
            }
        } else {
//            Toast.makeText(mContext, "ER2H: proxy NOT set", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ER2H: proxy NOT set");
            Uri uri = Uri.parse(url);
            Log.e(TAG, "URL is " + url + "getHost returns: "+ uri.getHost());
            inetAddr = lookupHost(uri.getHost());//
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!cm.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr))
                    throw new IOException("Cannot establish route to " + inetAddr + " for " + url);
            }
            Log.e(TAG, "GOT HOST");
        }
    }

    private int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            Log.e(TAG, "LOOKING UP HOST "+ hostname);
            inetAddress = InetAddress.getByName(hostname);//
        } catch (UnknownHostException e) {
            Log.e(TAG, "ERR: unknown host => " + e.getMessage());
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16) | ((addrBytes[1] & 0xff) << 8) | (addrBytes[0] & 0xff);
        return addr;
    }

    private HashSet<String> getRecipients(GenericPdu pdu) {
        int[] ADDRESS_FIELDS = {PduHeaders.TO, PduHeaders.FROM, PduHeaders.BCC, PduHeaders.CC};
        PduHeaders header = pdu.getPduHeaders();
        HashMap<Integer, EncodedStringValue[]> addressMap = new HashMap<Integer, EncodedStringValue[]>(ADDRESS_FIELDS.length);
        for (int addrType : ADDRESS_FIELDS) {
            EncodedStringValue[] array = null;
            if (addrType == PduHeaders.FROM) {
                EncodedStringValue v = header.getEncodedStringValue(addrType);
                if (v != null) {
                    array = new EncodedStringValue[1];
                    array[0] = v;
                }
            } else {
                array = header.getEncodedStringValues(addrType);
            }
            addressMap.put(addrType, array);
        }
        HashSet<String> recipients = new HashSet<String>();
        loadRecipients(PduHeaders.FROM, recipients, addressMap, false);
        loadRecipients(PduHeaders.TO, recipients, addressMap, true);
        return recipients;
    }

    private void loadRecipients(int addressType, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap, boolean excludeMyNumber) {
        EncodedStringValue[] array = addressMap.get(addressType);
        if (array == null) {
            return;
        }
        // If the TO recipients is only a single address, then we can skip loadRecipients when
        // we're excluding our own number because we know that address is our own.
        if (excludeMyNumber && array.length == 1) {
            return;
        }

        String myNumber = excludeMyNumber ? mTelephonyManager.getLine1Number() : null;
        for (EncodedStringValue v : array) {
            if (v != null) {
                String number = v.getString();
                if ((myNumber == null || !PhoneNumberUtils.compare(number, myNumber)) && !recipients.contains(number)) {
                    // Only add numbers which aren't my own number.
                    recipients.add(number);
                }
            }
        }
    }

    private void processPduAttachments(GenericPdu genericPdu) throws Exception {
        Log.e(TAG, "PROCESS PDU");
        if (genericPdu instanceof MultimediaMessagePdu) {
            Log.e(TAG, "\tis MM PDU");
            PduBody body = ((MultimediaMessagePdu) genericPdu).getBody();
            if (body != null) {
                int partsNum = body.getPartsNum();
//                Toast.makeText(mContext, String.format("receiving "+partsNum+" parts"), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "\t COUNT IS "+partsNum);
                for (int i = 0; i < partsNum; i++) {
                    try {
                        PduPart part = body.getPart(i);
                        if (part == null || part.getData() == null || part.getContentType() == null || part.getName() == null)
                            continue;
                        String partType = new String(part.getContentType());
                        String partName = new String(part.getName());
//                        Log.d("Part Name: " + partName);
//                        Log.d("Part Type: " + partType);
                        if (ContentType.isTextType(partType)) {
//                            Toast.makeText(mContext, String.format("receiving %s mms", "text"), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "\t IS TEXT");
                        } else if (ContentType.isImageType(partType)) {
//                            Toast.makeText(mContext, String.format("receiving %s mms", "image"), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "\t IS IMAGE");
                        } else if (ContentType.isVideoType(partType)) {
//                            Toast.makeText(mContext, String.format("receiving %s mms", "video"), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "\t IS VIDEO");
                        } else if (ContentType.isAudioType(partType)) {
//                            Toast.makeText(mContext, String.format("receiving %s mms", "audio"), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "\t IS AUDIO");
                        }
                    } catch (Exception e) {
                       // Toast.makeText(mContext, String.format("receiving ERROR"), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        // Bad part shouldn't ruin the party for the other parts
                    }
                }
            }
        } else {
            Log.d("poo","Not a MultimediaMessagePdu PDU");
        }
    }


}
