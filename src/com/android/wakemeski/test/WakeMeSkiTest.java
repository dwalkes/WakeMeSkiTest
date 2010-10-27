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
 * @author dan
 *
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
	
	protected void setUp() {
		initServer(0);
	}
	
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
	 * @return true if a new server is setup for test
	 */
	boolean nextServer() {
		return initServer(mServerIndex+1);
	}
	
	private void expectFalse( boolean condition, String errorIfTrue )
	{
		if( condition ) {
			Log.e(TAG, errorIfTrue);
			mErrors[mServerIndex].add(errorIfTrue);
		}
	}
	
	private void expectTrue( boolean condition, String errorIfFalse )
	{
		expectFalse(!condition,errorIfFalse);
	}
	
	private void expectEquals( int expected, int actual, String errorIfNoMatch )
	{
		expectTrue(expected==actual,errorIfNoMatch + " Expected " + expected + " but found " + actual );
	}

	/**
	 * 
	 * @param location
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
	
	private void testRegion(String region) throws Throwable {
		Location locations[] = mFinder.getLocations(region);
		expectTrue(locations.length > 0,"Expect at least 1 location per region but found 0 in region " + region);
		Log.i(TAG, "Found " + locations.length + " locations in region " + region);
		for (Location location:locations) {
			testLocation(location);
		}
	}

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
