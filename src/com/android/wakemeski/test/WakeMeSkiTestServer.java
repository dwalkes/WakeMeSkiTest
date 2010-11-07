package com.android.wakemeski.test;

import android.content.Context;

import com.android.wakemeski.core.WakeMeSkiServer;

/**
 * Override getId() on the base class to return a test_ prefix to help
 * us identify requests coming from our test scripts rather than a real
 * android device.
 */
public class WakeMeSkiTestServer extends WakeMeSkiServer {

	public WakeMeSkiTestServer(Context c, String serverUrl) {
		super(c,serverUrl);
	}
	
	/**
	 * Overrides base class getId() method to prefix with "test_"
	 */
	public String getId() {
		return "test_" + super.getId();
	}
}
