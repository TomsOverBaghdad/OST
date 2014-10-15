package com.tom.ost;

import java.util.List;
import java.util.Vector;


import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {
	private MainPageAdapter mPagerAdapter;
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initializePaging();
		createTabs();
	}

	private void initializePaging() {
		mViewPager = (ViewPager) findViewById(R.id.pager);
		
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, ConversationFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, ContactsFragment.class.getName()));
		
		// Create the adapter that will return a fragment for each of the primary sections of the activity.
		mPagerAdapter = new MainPageAdapter(this.getSupportFragmentManager(), fragments);
		mViewPager.setAdapter(mPagerAdapter);
	}

	private void createTabs() {
		final ActionBar actionBar = getActionBar();
		// Specify that tabs should be displayed in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// Create a tab listener that is called when the user changes tabs.
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				// show the given tab
				mViewPager.setCurrentItem(tab.getPosition());
			}
			public void onTabUnselected(ActionBar.Tab tab,FragmentTransaction ft) {
				// hide the given tab
			}
			public void onTabReselected(ActionBar.Tab tab,FragmentTransaction ft) {
				// probably ignore this event
			}
		};
		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_menu_conversations)//.setText("Conversations")
				.setTabListener(tabListener));
		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_menu_people)//.setText("Contacts")
				.setTabListener(tabListener));
		//set up the viewPager so the tab changes with swipe as well as picking the tab
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between pages, select the corresponding tab.
						getActionBar().setSelectedNavigationItem(position);
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_compose_new_message:
			Intent newMessageIntent = new Intent(this, MessageActivity.class);
			newMessageIntent.putExtra(MessageActivity.IS_NEW, true);
			this.startActivity(newMessageIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	

}
