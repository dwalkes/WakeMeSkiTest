/*
 * Copyright (C) 2010 Dan Walkes, Andy Doan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.wakemeski.test;

import java.util.ArrayList;

import junit.framework.Assert;
import android.content.Context;
import android.net.ConnectivityManager;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.wakemeski.core.HttpUtils;
import com.android.wakemeski.core.Location;
import com.android.wakemeski.core.LocationFinder;
import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.Resort;
import com.android.wakemeski.core.WakeMeSkiServer;

/**
 * Tests the location finder class and all locations
 * Creates a report with detail about the status of all resorts including
 * any error conditions.
 * @author dan
 */
public class WakeMeSkiTest extends AndroidTestCase {

	private int mServerIndex = 0;
	private WakeMeSkiServer mServer;
	private LocationFinder	mFinder;
	private static final String TAG = "WakeMeSkiTest";
	private static final int mMaxServers = HttpUtils.SERVER_LIST.length;
	private ArrayList<String>[] mErrors = ( ArrayList<String>[]) new ArrayList[mMaxServers];
	private ArrayList<String>[] mFreshSnowNotFound = ( ArrayList<String>[]) new ArrayList[mMaxServers];
	private ArrayList<String>[] mSuccessLocations = ( ArrayList<String>[])new ArrayList[mMaxServers];
	
	/**
	 * Initialize the test fixture to start testing with the first available server
	 */
	protected void setUp() {
		initServer(0);
	}
	
	/**
	 * On completion, look for errors logged by the server.  Log a summary
	 * and throw an assertion exception if errors were found.
	 */
	protected void tearDown() {
		String SUMMARY_TAG = new String("WakeMeSkiTest:Summary");
		int errors = 0;
		for( int i=0; i<mMaxServers ;i++ ) {
			if(mErrors[i] != null && mErrors[i].size() != 0 ) {
				Log.e(SUMMARY_TAG,"Expected no errors running test on server " +
					HttpUtils.SERVER_LIST[i] + " but " + mErrors[i].size() + " errors occurred:");
				for( String error : mErrors[i]) {
					Log.e(SUMMARY_TAG,error);
				}
				errors += mErrors[i].size();
			}
			if( mFreshSnowNotFound[i] != null &&
					mFreshSnowNotFound[i].size() != 0 ) {
				Log.e(SUMMARY_TAG,"Total of " +mFreshSnowNotFound[i].size() + " resorts missing fresh snow totals on server " + HttpUtils.SERVER_LIST[i] + " despite lack of error conditions");
				
				for( String location : mFreshSnowNotFound[i]) {
					Log.e(SUMMARY_TAG,location);
				}
			}
			if( mSuccessLocations[i] != null && 
					mSuccessLocations[i].size() != 0 ) {
				Log.i(SUMMARY_TAG,"Total of " + mSuccessLocations[i].size()+ " resorts with no problems found on server " + HttpUtils.SERVER_LIST[i]);
				for( String location : mSuccessLocations[i]) {
					Log.i(SUMMARY_TAG,location);
				}
			}
		}

		Assert.assertEquals(0,errors);
	}
	
	/**
	 * Initialize the specified zero reference server number
	 * as found in HttpUtils.SERVER_LIST
	 * @param index Into the server list
	 * @return true if this is a valid index
	 */
	boolean initServer(int index) {
		boolean initSuccess = false;
		if( index < HttpUtils.SERVER_LIST.length ) {
			mServerIndex = index;
			String server = HttpUtils.SERVER_LIST[mServerIndex];
			Log.i(TAG, "Testing server" + server );
			mErrors[index] = new ArrayList<String>();
			mFreshSnowNotFound[index] = new ArrayList<String>();
			mSuccessLocations[index] = new ArrayList<String>();
	
			mServer = new WakeMeSkiServer(server); 
			mFinder = new LocationFinder(
					mServer);
			initSuccess = true;
		}
		return initSuccess;
	}

	/**
	 * @return true if a new server is setup for test, false if no more servers
	 * are available
	 */
	boolean nextServer() {
		return initServer(mServerIndex+1);
	}
	
	/**
	 * @param condition to check
	 * @param errorIfTrue error message if condition fails
	 */
	private void expectFalse( boolean condition, String errorIfTrue )
	{
		if( condition ) {
			Log.e(TAG, errorIfTrue);
			mErrors[mServerIndex].add(errorIfTrue);
		}
	}
	
	
	/**
	 * @param condition to check
	 * @param errorIfFalse error message if condition check fails
	 */
	private void expectTrue( boolean condition, String errorIfFalse )
	{
		expectFalse(!condition,errorIfFalse);
	}
	
	
	private void expectEquals( int expected, int actual, String errorIfNoMatch )
	{
		expectTrue(expected==actual,errorIfNoMatch + " Expected " + expected + " but found " + actual );
	}

	/**
	 * @param location The ski resort location to test
	 */
	private void testLocation(Location location) {
		boolean hasProblems;
		Log.i(TAG,"Testing location " + location);
		Resort resort = new Resort(location);
		Report r = Report.loadReport(getContext(), 
				(ConnectivityManager)getContext().getSystemService((Context.CONNECTIVITY_SERVICE)), resort, mServer);
		hasProblems = r.hasErrors();
		expectFalse(r.hasErrors(),location + " has errors " + r.getError());
		if( !hasProblems ) {
			if( !r.hasFreshSnowTotal() ) {
				mFreshSnowNotFound[mServerIndex].add(location.toString());
				hasProblems = true;
			}
		}
		if( !hasProblems ) {
			mSuccessLocations[mServerIndex].add(location.toString());
		}
		/**
		 * TODO: more testing of this location
		 */
	}
	
	/**
	 * @param region The region to test.  Test all locations in this region
	 * @throws Throwable
	 */
	private void testRegion(String region) throws Throwable {
		Location locations[] = mFinder.getLocations(region);
		expectTrue(locations.length > 0,"Expect at least 1 location per region but found 0 in region " + region);
		Log.i(TAG, "Found " + locations.length + " locations in region " + region);
		for (Location location:locations) {
			testLocation(location);
		}
	}

	/**
	 * Test all locations and all servers
	 * @throws Throwable
	 */
	public void testLocations() throws Throwable {
		do
		{
			String[] regions = mFinder.getRegions(); 
			expectEquals(9,regions.length,"regions.length did not match expected value");
			for (String region:regions) {
				testRegion(region);
			}
		} while (nextServer());
	}
	
}
