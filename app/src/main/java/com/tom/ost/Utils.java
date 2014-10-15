package com.tom.ost;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Telephony;
import android.provider.Telephony.Sms.Intents;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class Utils {
//	 /**
//     * Check if the device runs Android 4.3 (JB MR2) or higher.
//     */
//    public static boolean hasJellyBeanMR2() {
//        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;
//    }
    /**
     * Check if the device runs Android 4.4 (KitKat) or higher.
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }
    /**
     * Check if your app is the default system SMS app.
     * @param context The Context
     * @return True if it is default, False otherwise. Pre-KitKat will always return True.
     */
    public static boolean isDefaultSmsApp(Context context) {
        if (hasKitKat()) {
            return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
        }
        return false;
    }
    /**
     * Trigger the intent to open the system dialog that asks the user to change the default
     * SMS app.
     * @param context The Context
     */
    public static void setDefaultSmsApp(Context context) {
        // This is a new intent which only exists on KitKat
        if (hasKitKat()) {
            Intent intent = new Intent(Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.startActivity(intent);
        }
    }
	
	
	
	public static String getContactNameFromAddress(Context context, String number) {
		// / number is the phone number
		Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		String[] mPhoneNumberProjection = { PhoneLookup._ID,
				PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME };
		Cursor cur = context.getContentResolver().query(lookupUri,
				mPhoneNumberProjection, null, null, null);
		try {
			if (cur.moveToFirst()) {
				int column = cur.getColumnIndex(Phone.DISPLAY_NAME);
				return cur.getString(column);
			}
		} finally {
			if (cur != null)
				cur.close();
		}
		// default name is number
		return number;
	}

	

	public static String getContactAddressFromRecipientId(Context context, String recipientId) {
		Cursor c = context.getContentResolver().query(Uri.parse("content://mms-sms/canonical-address/" + recipientId), null, null, null, null);
		return getFirstFromCursor(c);
	}
	public static String getContactRecipientIdFromAddress(Context context, String address) {
		Cursor c = context.getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null,  //  null,null,null);
							"PHONE_NUMBERS_EQUAL(address, "+ address+")", null, null);
		return getFirstFromCursor(c);
	}
	public static String getThreadIdFromRecipientId(Context context, String recipient_id) {
		Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
		Cursor c = context.getContentResolver().query(uri, new String[]{"_id"}, "recipient_ids ="+ recipient_id, null, null);
		return getFirstFromCursor(c);
	}
	
	private static String getFirstFromCursor(Cursor c){
		String to_return = "not found";
		if(c.moveToFirst()){
			to_return = c.getString(0);
		}
		c.close();
		return to_return;
	}
	
}
