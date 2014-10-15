package com.tom.ost;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ContactsFragment extends Fragment implements LoaderCallbacks<Cursor>{
	private final int LOADER_ID = 1986;
	private Context mContext;
//	private Cursor mCursor;
	private ContactCursorAdapter mContactCursorAdapter;
	private ListView mContactsListView;
	private final Uri CONTACT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	private final String SORT_ORDER = ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED + " DESC";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(container == null){
			return null;
		}
		View view = (LinearLayout) inflater.inflate(R.layout.contacts_layout, container, false);
		mContext = view.getContext();
		/////////ContactsContract.Contacts.CONTENT_URI
//		mCursor = mContext.getContentResolver().query(
		//////////		ContactsContract.Contacts.CONTENT_URI, null, ContactsContract.Contacts.HAS_PHONE_NUMBER + "> 0", null, ContactsContract.Contacts.LAST_TIME_CONTACTED+ " DESC");
//				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED + " DESC");
		mContactCursorAdapter = new ContactCursorAdapter(mContext, null, 0);
		mContactsListView = (ListView) view.findViewById(R.id.ContactsList);
		mContactsListView.setAdapter(mContactCursorAdapter);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), CONTACT_URI, new String[]{ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
			ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER},
					null, null, SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
		this.mContactCursorAdapter.swapCursor(newCursor);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		this.mContactCursorAdapter.swapCursor(null);
	}
}
