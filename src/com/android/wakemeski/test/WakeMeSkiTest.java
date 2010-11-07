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
import java.util.Arrays;

import junit.framework.Assert;
import android.content.Context;
import android.net.ConnectivityManager;
import android.test.AndroidTestCase;
import android.util.Log;

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
	/*
	 * If you just deployed an update to the server and want to test it you need to run without cache.
	 * Otherwise if running in an overnight script or doing test development you could potentially 
	 * disable this setting and use the cache if available.  Tests will run faster when using cache 
	 */
	private boolean mNoCache = true;
	
	private static final String OVERRIDE_SERVER = "" 
	/**
	 * Un-comment line below to test only on a specific server
	 */
	//								+ "http://ddubtech.com/wakemeski/skireport"
	;

	/**
	 * List regions with issues.  On a region with issues, don't attempt other locations when
	 * the first location fails (otherwise the test takes a long time to run)
	 */
	private static final String[] mProblemRegions = 
	{
									"Montana"
	};

	private int mServerIndex = 0;
	private WakeMeSkiServer mServer;
	private LocationFinder	mFinder;
	private static final String TAG = "WakeMeSkiTest";
	private ArrayList<ServerTestResult> mTestResultList = new ArrayList<ServerTestResult>();
	private ServerTestResult mServerResult=null;

	
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

		for( int i=0; i<=mServerIndex ;i++ ) {
			String server;
			if( OVERRIDE_SERVER.length() != 0 ) {
				server = OVERRIDE_SERVER;
			} else {
				server = WakeMeSkiServer.SERVER_LIST[i];
			}
			Log.i(SUMMARY_TAG,"Results for server " + server );
			
			mServerResult.logErrors(SUMMARY_TAG);
			
			mServerResult.logSuccessConditions(SUMMARY_TAG);
		}

		Assert.assertEquals(0,mServerResult.getErrorCount());

	}

	void initServer(String url)
	{
		Log.i(TAG, "Testing server" + url );
		mServer = new WakeMeSkiTestServer(this.getContext(),url); 
		mFinder = new LocationFinder(
				mServer);
		mServerResult = new ServerTestResult(mServer);
	}
	
	/**
	 * Initialize the specified zero reference server number
	 * as found in HttpUtils.SERVER_LIST
	 * @param index Into the server list
	 * @return true if this is a valid index
	 */
	boolean initServer(int index) {
		boolean initSuccess = false;

		if(OVERRIDE_SERVER.length() != 0) {
			if(index == 0) {
				initServer(OVERRIDE_SERVER);
			} else {
				initSuccess = false;
			}
		} else {
			if(index < WakeMeSkiServer.SERVER_LIST.length) {
				mServerIndex = index;
				initServer(WakeMeSkiServer.SERVER_LIST[mServerIndex]);
				initSuccess=true;
			}
		}
		return initSuccess;
	}

	/**
	 * @return true if a new server is setup for test, false if no more servers
	 * are available or configured
	 */
	boolean nextServer() {
		if( mServerResult != null ) {
			/**
			 * Add any previous results to the result list 
			 */
			mTestResultList.add(mServerResult);
		}
		
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
			mServerResult.addError(errorIfTrue);
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
	 * @return the report for this location
	 */
	private Report testLocation(Location location) {
		Log.i(TAG,"Testing location " + location);
		Resort resort = new Resort(location);
		Report r;
		if( mNoCache ) {
			r = Report.loadReportNoCache(getContext(), 
					(ConnectivityManager)getContext().getSystemService((Context.CONNECTIVITY_SERVICE)), resort, mServer);
		} else {
			r = Report.loadReport(getContext(), 
					(ConnectivityManager)getContext().getSystemService((Context.CONNECTIVITY_SERVICE)), resort, mServer);
		}

		/**
		 * Process the report and cache any error conditions
		 */
		mServerResult.processReport(r);
		return r;
		
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
			Report report = testLocation(location);
			if( Arrays.asList(mProblemRegions).contains(region) ) {
				if( report.hasErrors() ) {
					Log.i(TAG,"Skipping all additional locations in Region " + region + " as it is identified as a problem region");
					break;
				} else {
					Log.i(TAG,"Region " + region + " appears to be working again!! at least for location " + location);
				}
			}
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
			expectEquals(10,regions.length,"regions.length did not match expected value");
			for (String region:regions) {
				
				testRegion(region);
			}
		} while (nextServer());
	}
	
}
