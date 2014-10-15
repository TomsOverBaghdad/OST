package com.tom.ost.transaction;


//import com.android.internal.telephony.TelephonyIntents;
//import com.android.internal.telephony.TelephonyProperties;
//import com.android.mms.R;
//import com.android.mms.data.Contact;
//import com.android.mms.ui.MessagingPreferenceActivity;
//import com.google.android.mms.MmsException;
//import com.google.android.mms.pdu.EncodedStringValue;
//import com.google.android.mms.pdu.NotificationInd;
//import com.google.android.mms.pdu.PduPersister;
//import com.google.android.mms.util.SqliteWrapper;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
//import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.telephony.ServiceState;
import android.util.Config;
import android.util.Log;
import android.widget.Toast;

import com.tom.ost.SqliteWrapper;
import com.tom.ost.pdUtils.EncodedStringValue;
import com.tom.ost.pdUtils.MmsException;
import com.tom.ost.pdUtils.NotificationInd;
import com.tom.ost.pdUtils.PduPersister;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static final int DEFERRED_MASK           = 0x04;

    public static final int STATE_UNSTARTED         = 0x80;
    public static final int STATE_DOWNLOADING       = 0x81;
    public static final int STATE_TRANSIENT_FAILURE = 0x82;
    public static final int STATE_PERMANENT_FAILURE = 0x87;

    private final Context mContext;
    private final Handler mHandler;
    private final SharedPreferences mPreferences;
    private boolean mAutoDownload;

//    private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
//            new OnSharedPreferenceChangeListener() {
//                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                    if (MessagingPreferenceActivity.AUTO_RETRIEVAL.equals(key)
//                            || MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING.equals(key)) {
//                        if (LOCAL_LOGV) {
//                            Log.v(TAG, "Preferences updated.");
//                        }
//
//                        synchronized (sInstance) {
//                            mAutoDownload = getAutoDownloadState(prefs);
//                            if (LOCAL_LOGV) {
//                                Log.v(TAG, "mAutoDownload ------> " + mAutoDownload);
//                            }
//                        }
//                    }
//                }
//            };

//    private final BroadcastReceiver mRoamingStateListener =
//            new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
//                        if (LOCAL_LOGV) {
//                            Log.v(TAG, "Service state changed: " + intent.getExtras());
//                        }
//
//                        ServiceState state = ServiceState.newFromBundle(intent.getExtras());
//                        boolean isRoaming = state.getRoaming();
//                        if (LOCAL_LOGV) {
//                            Log.v(TAG, "roaming ------> " + isRoaming);
//                        }
//                        synchronized (sInstance) {
//                            mAutoDownload = getAutoDownloadState(mPreferences, isRoaming);
//                            if (LOCAL_LOGV) {
//                                Log.v(TAG, "mAutoDownload ------> " + mAutoDownload);
//                            }
//                        }
//                    }
//                }
//            };

    private static DownloadManager sInstance;

    private DownloadManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
     //   mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);

//        context.registerReceiver(
//                mRoamingStateListener,
//                new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED));

//        mAutoDownload = getAutoDownloadState(mPreferences);
//        if (LOCAL_LOGV) {
//            Log.v(TAG, "mAutoDownload ------> " + mAutoDownload);
//        }
    }

    public boolean isAuto() {
        return mAutoDownload;
    }

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "DownloadManager.init()");
        }

        if (sInstance != null) {
            Log.w(TAG, "Already initialized.");
        }
        sInstance = new DownloadManager(context);
    }

    public static DownloadManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

//    static boolean getAutoDownloadState(SharedPreferences prefs) {
//        return getAutoDownloadState(prefs, isRoaming());
//    }

//    static boolean getAutoDownloadState(SharedPreferences prefs, boolean roaming) {
//        boolean autoDownload = prefs.getBoolean(
//                MessagingPreferenceActivity.AUTO_RETRIEVAL, true);
//
//        if (LOCAL_LOGV) {
//            Log.v(TAG, "auto download without roaming -> " + autoDownload);
//        }
//
//        if (autoDownload) {
//            boolean alwaysAuto = prefs.getBoolean(
//                    MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING, false);
//
//            if (LOCAL_LOGV) {
//                Log.v(TAG, "auto download during roaming -> " + alwaysAuto);
//            }
//
//            if (!roaming || alwaysAuto) {
//                return true;
//            }
//        }
//        return false;
//    }

//    static boolean isRoaming() {
//        String roaming = SystemProperties.get(
//                TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, null);
//        if (LOCAL_LOGV) {
//            Log.v(TAG, "roaming ------> " + roaming);
//        }
//        return "true".equals(roaming);
//    }

    public void markState(final Uri uri, int state) {
        // Notify user if the message has expired.
        try {
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext)
                    .load(uri);
            if ((nInd.getExpiry() < System.currentTimeMillis()/1000L)
                    && (state == STATE_DOWNLOADING)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, "download expired",//R.string.dl_expired_notification,
                                Toast.LENGTH_LONG).show();
                    }
                });
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri, null, null);
                return;
            }
        } catch(MmsException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        // Notify user if downloading permanently failed.
        if (state == STATE_PERMANENT_FAILURE) {
            mHandler.post(new Runnable() {
                public void run() {
                    try {
                        Toast.makeText(mContext, getMessage(uri),
                                Toast.LENGTH_LONG).show();
                    } catch (MmsException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
        } else if (!mAutoDownload) {
            state |= DEFERRED_MASK;
        }

        // Use the STATUS field to store the state of downloading process
        // because it's useless for M-Notification.ind.
        ContentValues values = new ContentValues(1);
        values.put(Mms.STATUS, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                uri, values, null, null);
    }

    public void showErrorCodeToast(int errorStr) {
        final int errStr = errorStr;
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(mContext, errStr, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG,"Caught an exception in showErrorCodeToast");
                }
            }
        });
    }

    private String getMessage(Uri uri) throws MmsException {
        NotificationInd ind = (NotificationInd) PduPersister
                .getPduPersister(mContext).load(uri);

        EncodedStringValue v = ind.getSubject();
        String subject = (v != null) ? v.getString()
                : "no subject--";// mContext.getString(R.string.no_subject);

        v = ind.getFrom();
        String from = (v != null)
                ? v.getString()//TODO Contact.get(v.getString(), true).getName()
                : "unkown sender";//mContext.getString(R.string.unknown_sender);

        return subject + " " + from;//mContext.getString(R.string.dl_failure_notification, subject, from);
    }

    public int getState(Uri uri) {
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                uri, new String[] {Mms.STATUS}, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0) &~ DEFERRED_MASK;
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNSTARTED;
    }
}