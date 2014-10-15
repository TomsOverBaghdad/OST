package com.tom.ost.transaction;

import android.content.Context;
import android.drm.DrmConvertedStatus;
import android.drm.DrmManagerClient;
import android.util.Log;
//import android.provider.Downloads;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


public class DrmConvertSession {
    private DrmManagerClient mDrmClient;
    private int mConvertSessionId;
    private static final String TAG = "DrmConvertSession";

    private DrmConvertSession(DrmManagerClient drmClient, int convertSessionId) {
        mDrmClient = drmClient;
        mConvertSessionId = convertSessionId;
    }

    /**
     * Start of converting a file.
     *
     * @param context The context of the application running the convert session.
     * @param mimeType Mimetype of content that shall be converted.
     * @return A convert session or null in case an error occurs.
     */
    public static DrmConvertSession open(Context context, String mimeType) {
        DrmManagerClient drmClient = null;
        int convertSessionId = -1;
        if (context != null && mimeType != null && !mimeType.equals("")) {
            try {
                drmClient = new DrmManagerClient(context);
                try {
                    convertSessionId = drmClient.openConvertSession(mimeType);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Conversion of Mimetype: " + mimeType
                            + " is not supported.", e);
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Could not access Open DrmFramework.", e);
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG,
                        "DrmManagerClient instance could not be created, context is Illegal.");
            } catch (IllegalStateException e) {
                Log.w(TAG, "DrmManagerClient didn't initialize properly.");
            }
        }

        if (drmClient == null || convertSessionId < 0) {
            return null;
        } else {
            return new DrmConvertSession(drmClient, convertSessionId);
        }
    }
    /**
     * Convert a buffer of data to protected format.
     *
     * @param buffer Buffer filled with data to convert.
     * @param size The number of bytes that shall be converted.
     * @return A Buffer filled with converted data, if execution is ok, in all
     *         other case null.
     */
    public byte [] convert(byte[] inBuffer, int size) {
        byte[] result = null;
        if (inBuffer != null) {
            DrmConvertedStatus convertedStatus = null;
            try {
                if (size != inBuffer.length) {
                    byte[] buf = new byte[size];
                    System.arraycopy(inBuffer, 0, buf, 0, size);
                    convertedStatus = mDrmClient.convertData(mConvertSessionId, buf);
                } else {
                    convertedStatus = mDrmClient.convertData(mConvertSessionId, inBuffer);
                }

                if (convertedStatus != null &&
                        convertedStatus.statusCode == DrmConvertedStatus.STATUS_OK &&
                        convertedStatus.convertedData != null) {
                    result = convertedStatus.convertedData;
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Buffer with data to convert is illegal. Convertsession: "
                        + mConvertSessionId, e);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Could not convert data. Convertsession: " +
                        mConvertSessionId, e);
            }
        } else {
            throw new IllegalArgumentException("Parameter inBuffer is null");
        }
        return result;
    }

    /**
     * Ends a conversion session of a file.
     *
     * @param fileName The filename of the converted file.
     * @return Downloads.Impl.STATUS_SUCCESS if execution is ok.
     *         Downloads.Impl.STATUS_FILE_ERROR in case converted file can not
     *         be accessed. Downloads.Impl.STATUS_NOT_ACCEPTABLE if a problem
     *         occurs when accessing drm framework.
     *         Downloads.Impl.STATUS_UNKNOWN_ERROR if a general error occurred.
     */
    public int close(String filename) {
        DrmConvertedStatus convertedStatus = null;
        int result = Downloads.Impl.STATUS_UNKNOWN_ERROR;
        if (mDrmClient != null && mConvertSessionId >= 0) {
            try {
                convertedStatus = mDrmClient.closeConvertSession(mConvertSessionId);
                if (convertedStatus == null ||
                        convertedStatus.statusCode != DrmConvertedStatus.STATUS_OK ||
                        convertedStatus.convertedData == null) {
                    result = Downloads.Impl.STATUS_NOT_ACCEPTABLE;
                } else {
                    RandomAccessFile rndAccessFile = null;
                    try {
                        rndAccessFile = new RandomAccessFile(filename, "rw");
                        rndAccessFile.seek(convertedStatus.offset);
                        rndAccessFile.write(convertedStatus.convertedData);
                        result = Downloads.Impl.STATUS_SUCCESS;
                    } catch (FileNotFoundException e) {
                        result = Downloads.Impl.STATUS_FILE_ERROR;
                        Log.w(TAG, "File: " + filename + " could not be found.", e);
                    } catch (IOException e) {
                        result = Downloads.Impl.STATUS_FILE_ERROR;
                        Log.w(TAG, "Could not access File: " + filename + " .", e);
                    } catch (IllegalArgumentException e) {
                        result = Downloads.Impl.STATUS_FILE_ERROR;
                        Log.w(TAG, "Could not open file in mode: rw", e);
                    } catch (SecurityException e) {
                        Log.w(TAG, "Access to File: " + filename +
                                " was denied denied by SecurityManager.", e);
                    } finally {
                        if (rndAccessFile != null) {
                            try {
                                rndAccessFile.close();
                            } catch (IOException e) {
                                result = Downloads.Impl.STATUS_FILE_ERROR;
                                Log.w(TAG, "Failed to close File:" + filename
                                        + ".", e);
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "Could not close convertsession. Convertsession: " +
                        mConvertSessionId, e);
            }
        }
        return result;
    }
}