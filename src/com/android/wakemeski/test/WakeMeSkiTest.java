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
	//									+ "http://ddubtech.com/wakemeski/skireport"
	;

	private int mServerIndex = 0;
	private WakeMeSkiServer mServer;
	private LocationFinder	mFinder;
	private static final String TAG = "WakeMeSkiTest";
	private static final String SUMMARY_TAG = "WakeMeSkiTest:Summary";
	private static final int mMaxServers = HttpUtils.SERVER_LIST.length;
	/**
	 * Keep a list of general errors per server
	 */
	private ArrayList<String>[] mErrorStrings = ( ArrayList<String>[]) new ArrayList[mMaxServers];
	/**
	 * Keep track of reports per server that have errors 
	 */
	private ArrayList<Report>[] mErrorReports = ( ArrayList<Report>[]) new ArrayList[mMaxServers];

	/**
	 * Keep track of Reports that don't have errors but are missing fresh snow totals
	 */
	private ArrayList<Report>[] mFreshSnowNotFoundReports = ( ArrayList<Report>[]) new ArrayList[mMaxServers];
	
	/**
	 * Keep track of locations where all resort checks succeeded during test
	 */
	private ArrayList<Location>[] mSuccessLocations = ( ArrayList<Location>[])new ArrayList[mMaxServers];
	
	/**
	 * Initialize the test fixture to start testing with the first available server
	 */
	protected void setUp() {
		initServer(0);
	}
	
	/**
	 * 
	 * @param report
	 * @param messagePrefix
	 */
	void logReportFailure(Report report, String messagePrefix ) {
		/*
		 * Start with the name of the resort
		 */
		String logString = report.getResort().toString() + " ";
		
		/*
		 * Add any URL's which might help us track down the problem.  We can
		 * grab these in a script running against the log output and include in the error report
		 * to make debugging easier.
		 */
		if( report.getFreshSourceURL().length() != 0 ) {
			logString += "Fresh Source URL: " + report.getFreshSourceURL();
		}
		
		/*
		 * Add additional tag to message to make it easier to pull out of the logs
		 * for additional processing
		 */
		Log.e(SUMMARY_TAG + ":REPORT_FAILURE",messagePrefix + " : " + logString);
	}
	
	/**
	 * On completion, look for errors logged by the server.  Log a summary
	 * and throw an assertion exception if errors were found.
	 */
	protected void tearDown() {
		String SUMMARY_TAG = new String("WakeMeSkiTest:Summary");
		int errors = 0;
		for( int i=0; i<mMaxServers ;i++ ) {
			Log.i(SUMMARY_TAG,"Results for server " + HttpUtils.SERVER_LIST[i]);
			/**
			 * Look for general errors first
			 */
			if(mErrorStrings[i] != null && mErrorStrings[i].size() != 0 ) {
				Log.e(SUMMARY_TAG,"Expected no errors " +
					  " but " + mErrorStrings[i].size() + " errors occurred:");
				for( String string : mErrorStrings[i]) {
					Log.e(SUMMARY_TAG,string);
				}
				errors += mErrorStrings[i].size();
			}
			/**
			 * Then look for reports with error 
			 */
			if(mErrorReports[i] != null && mErrorReports[i].size() != 0 ) {
				Log.e(SUMMARY_TAG,"Expected no reports with error but " + mErrorReports[i].size() + " have error conditions" );
				for( Report report : mErrorReports[i]) {
					logReportFailure(report,report.getError());
				}
				errors+= mErrorReports[i].size();
			}
			/**
			 * Then reports that don't have error but also don't have a readable fresh snow total
			 */
			if( mFreshSnowNotFoundReports[i] != null &&
					mFreshSnowNotFoundReports[i].size() != 0 ) {
				Log.e(SUMMARY_TAG,"Total of " +mFreshSnowNotFoundReports[i].size() + " reports missing fresh snow totals despite lack of error conditions");
				
				for( Report report : mFreshSnowNotFoundReports[i]) {
					logReportFailure(report,"Fresh Snow Not Found");
				}
				errors += mFreshSnowNotFoundReports[i].size();
			}
			/**
			 * Finally, log info message with successful locations
			 */
			if( mSuccessLocations[i] != null && 
					mSuccessLocations[i].size() != 0 ) {
				Log.i(SUMMARY_TAG,"Total of " + mSuccessLocations[i].size()+ " resorts with no problems found on server " + HttpUtils.SERVER_LIST[i]);
				for( Location location : mSuccessLocations[i]) {
					Log.i(SUMMARY_TAG,location.toString());
				}
			}
		}

		Assert.assertEquals(0,errors);

	}

	void initServer(String url)
	{
		Log.i(TAG, "Testing server" + url );
		mServer = new WakeMeSkiServer(url); 
		mFinder = new LocationFinder(
				mServer);
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
			if(index < HttpUtils.SERVER_LIST.length) {
				mServerIndex = index;
				initServer(HttpUtils.SERVER_LIST[mServerIndex]);
				initSuccess=true;
			}
		}
		if(initSuccess) {
			mErrorStrings[index] = new ArrayList<String>();
			mErrorReports[index] = new ArrayList<Report>();
			mFreshSnowNotFoundReports[index] = new ArrayList<Report>();
			mSuccessLocations[index] = new ArrayList<Location>();
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
			mErrorStrings[mServerIndex].add(errorIfTrue);
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

		if( !r.hasErrors() ) {
			if( !r.hasFreshSnowTotal() ) {
				mFreshSnowNotFoundReports[mServerIndex].add(r);
			} else {
				mSuccessLocations[mServerIndex].add(r.getResort().getLocation());
			}
		} else {
			mErrorReports[mServerIndex].add(r);
		}
		
		/**
		 * TODO: more testing of this report
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
