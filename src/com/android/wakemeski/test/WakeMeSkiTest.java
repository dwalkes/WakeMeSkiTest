package com.android.wakemeski.test;

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

	private int mIndex = 0;
	private WakeMeSkiServer mServer;
	private LocationFinder	mFinder;
	private static final String TAG = "LocationFinderTest";
	
	/**
	 * @return the location finder for the next server in the list of supported servers
	 */
	LocationFinder nextServer() {
		if( mIndex < HttpUtils.SERVER_LIST.length ) {
			String server = HttpUtils.SERVER_LIST[mIndex];
			Log.i(TAG, "Testing server" + server );

			mServer = new WakeMeSkiServer(server); 
			mFinder = new LocationFinder(
					mServer);
			
			mIndex++;
		} else {
			mFinder = null;
		}
		return mFinder;
	}
	

	/**
	 * 
	 * @param location
	 */
	private void testLocation(Location location) {
		Log.i(TAG,"Testing location " + location);
		Resort resort = new Resort(location);
		Report r = Report.loadReport(getContext(), 
				(ConnectivityManager)getContext().getSystemService((Context.CONNECTIVITY_SERVICE)), resort, mServer);
		Assert.assertFalse(r.hasErrors());
		/**
		 * TODO: more testing of this location
		 */
	}
	
	private void testRegion(String region) throws Throwable {
		Location locations[] = mFinder.getLocations(region);
		Assert.assertTrue(locations.length > 0);
		Log.i(TAG, "Found " + locations.length + " locations in region " + region);
		for (Location location:locations) {
			testLocation(location);
		}
	}

	public void testLocations() throws Throwable {
		LocationFinder finder;
		while ((finder = nextServer()) != null) {
			String[] regions = finder.getRegions(); 
			Assert.assertEquals(9,regions.length);
			for (String region:regions) {
				testRegion(region);
			}
			
		}
	}
}
