package com.tom.ost.transaction;


//import static com.android.mms.transaction.TransactionState.FAILED;
//import static com.android.mms.transaction.TransactionState.INITIALIZED;
//import static com.android.mms.transaction.TransactionState.SUCCESS;
//import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
//import static com.google.android.mms.pdu.PduHeaders.STATUS_DEFERRED;
//import static com.google.android.mms.pdu.PduHeaders.STATUS_RETRIEVED;
//import static com.google.android.mms.pdu.PduHeaders.STATUS_UNRECOGNIZED;
//
//import com.android.mms.MmsConfig;
//import com.android.mms.util.DownloadManager;
//import com.android.mms.util.Recycler;
//import com.google.android.mms.MmsException;
//import com.google.android.mms.pdu.GenericPdu;
//import com.google.android.mms.pdu.NotificationInd;
//import com.google.android.mms.pdu.NotifyRespInd;
//import com.google.android.mms.pdu.PduComposer;
//import com.google.android.mms.pdu.PduHeaders;
//import com.google.android.mms.pdu.PduParser;
//import com.google.android.mms.pdu.PduPersister;
//import com.google.android.mms.util.SqliteWrapper;

//import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.telephony.TelephonyManager;
import android.util.Config;
import android.util.Log;

import com.tom.ost.SqliteWrapper;
import com.tom.ost.pdUtils.GenericPdu;
import com.tom.ost.pdUtils.MmsException;
import com.tom.ost.pdUtils.NotificationInd;
import com.tom.ost.pdUtils.NotifyRespInd;
import com.tom.ost.pdUtils.PduHeaders;
import com.tom.ost.pdUtils.PduParser;
import com.tom.ost.pdUtils.PduPersister;

import java.io.IOException;

/**
 * The NotificationTransaction is responsible for handling multimedia
 * message notifications (M-Notification.ind).  It:
 *
 * <ul>
 * <li>Composes the notification response (M-NotifyResp.ind).
 * <li>Sends the notification response to the MMSC server.
 * <li>Stores the notification indication.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 *
 * NOTE: This MMS client handles all notifications with a <b>deferred
 * retrieval</b> response.  The transaction service, upon succesful
 * completion of this transaction, will trigger a retrieve transaction
 * in case the client is in immediate retrieve mode.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = "NotificationTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private Uri mUri;
    private NotificationInd mNotificationInd;
    private String mContentLocation;

    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        mId = new String(mNotificationInd.getTransactionId());
        mContentLocation = new String(mNotificationInd.getContentLocation());

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /**
     * This constructor is only used for test purposes.
     */
    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            mUri = PduPersister.getPduPersister(context).persist(
                    ind, Inbox.CONTENT_URI);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(ind.getTransactionId());
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.mms.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this).start();
    }

    public void run() {
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = downloadManager.isAuto();
        boolean dataSuspended = (TelephonyManager.getDefault().getDataState() ==
                TelephonyManager.DATA_SUSPENDED);
        try {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Notification transaction launched: " + this);
            }

            // By default, we set status to STATUS_DEFERRED because we
            // should response MMSC with STATUS_DEFERRED when we cannot
            // download a MM immediately.
            int status = PduHeaders.STATUS_DEFERRED;
            // Don't try to download when data is suspended, as it will fail, so defer download
            if (!autoDownload || dataSuspended) {
                downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED);
                sendNotifyRespInd(status);
                return;
            }

            downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING);

            if (LOCAL_LOGV) {
                Log.v(TAG, "Content-Location: " + mContentLocation);
            }

            byte[] retrieveConfData = null;
            // We should catch exceptions here to response MMSC
            // with STATUS_DEFERRED.
            try {
                retrieveConfData = getPdu(mContentLocation);
            } catch (IOException e) {
                mTransactionState.setState(TransactionState.FAILED);
            }

            if (retrieveConfData != null) {
                GenericPdu pdu = new PduParser(retrieveConfData).parse();
                if ((pdu == null) || (pdu.getMessageType() != PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)) {
                    Log.e(TAG, "Invalid M-RETRIEVE.CONF PDU.");
                    mTransactionState.setState(TransactionState.FAILED);
                    status = PduHeaders.STATUS_UNRECOGNIZED;
                } else {
                    // Save the received PDU (must be a M-RETRIEVE.CONF).
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    Uri uri = p.persist(pdu, Inbox.CONTENT_URI);
                    // We have successfully downloaded the new MM. Delete the
                    // M-NotifyResp.ind from Inbox.
                    SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                            mUri, null, null);
                    // Notify observers with newly received MM.
                    mUri = uri;
                    status = PduHeaders.STATUS_RETRIEVED;
                }
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "status=0x" + Integer.toHexString(status));
            }

            // Check the status and update the result state of this Transaction.
            switch (status) {
                case PduHeaders.STATUS_RETRIEVED:
                    mTransactionState.setState(TransactionState.SUCCESS);
                    break;
                case PduHeaders.STATUS_DEFERRED:
                    // STATUS_DEFERRED, may be a failed immediate retrieval.
                    if (mTransactionState.getState() == TransactionState.INITIALIZED) {
                        mTransactionState.setState(TransactionState.SUCCESS);
                    }
                    break;
            }

            sendNotifyRespInd(status);

            // Make sure this thread isn't over the limits in message count.
            Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
        } finally {
            mTransactionState.setContentUri(mUri);
            if (!autoDownload || dataSuspended) {
                // Always mark the transaction successful for deferred
                // download since any error here doesn't make sense.
                mTransactionState.setState(TransactionState.SUCCESS);
            }
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                Log.e(TAG, "NotificationTransaction failed.");
            }
            notifyObservers();
        }
    }

    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                mNotificationInd.getTransactionId(),
                status);

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }
}