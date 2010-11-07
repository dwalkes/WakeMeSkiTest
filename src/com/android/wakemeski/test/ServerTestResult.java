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

import android.util.Log;

import com.android.wakemeski.core.Report;
import com.android.wakemeski.core.WakeMeSkiServer;

public class ServerTestResult {
	/**
	 * Keep a list of general errors per server
	 */
	private ArrayList<String> mErrorStrings = new ArrayList<String>();
	/**
	 * Keep track of reports per server that have errors 
	 */
	private ArrayList<Report> mErrorReports = new ArrayList<Report>();

	/**
	 * Keep track of Reports that don't have errors but are missing fresh snow totals
	 */
	private ArrayList<Report> mFreshSnowNotFoundReports = new ArrayList<Report>();
	
	/**
	 * Keep track of locations where all resort checks succeeded during test
	 */
	private ArrayList<Report> mSuccessLocations = new ArrayList<Report>();
	
	/**
	 * The server used to obtain these results
	 */
	private WakeMeSkiServer mServer;
	
	/**
	 * @param report
	 * @param messagePrefix
	 */
	public void logReportFailure(String tag, Report report, String messagePrefix ) {
		/*
		 * Start with the name of the resort
		 */
		String logString = report.getResort().toString() + " ";

		/*
		 * Add any URL's which might help us track down the problem.  We can
		 * grab these in a script running against the log output and include in the error report
		 * to make debugging easier.
		 */

		if( report.getRequestURL().length() !=0 ) {
			logString += "Request URL: " + report.getRequestURL() + " ";
		}
			
		if( report.getFreshSourceURL().length() != 0 ) {
			logString += "Fresh Source URL: " + report.getFreshSourceURL() + " ";
		}
		
		
		/*
		 * Add additional tag to message to make it easier to pull out of the logs
		 * for additional processing
		 */
		Log.e(tag + ":REPORT_FAILURE",messagePrefix + " : " + logString);
	}
	
	public ServerTestResult(WakeMeSkiServer server) {
		mServer = server;
	}

	public int getErrorCount() {
		return mErrorStrings.size() + mErrorReports.size() + mFreshSnowNotFoundReports.size();
	}
	
	
	public void logErrors( String tag ) {
		
		/**
		 * Look for general errors first
		 */
		if( mErrorStrings.size() != 0 ) {
			Log.e(tag,"Expected no errors " +
				  " but " + mErrorStrings.size() + " errors occurred:");
			for( String string : mErrorStrings) {
				Log.e(tag,string);
			}
		}
		/**
		 * Then look for reports with error 
		 */
		if( mErrorReports.size() != 0 ) {
			Log.e(tag,"Expected no reports with error but " + mErrorReports.size() + " have error conditions" );
			for( Report report : mErrorReports ) {
				logReportFailure(tag,report,report.getNonLocalizedError());
			}
		}
		/**
		 * Then reports that don't have error but also don't have a readable fresh snow total
		 */
		if( mFreshSnowNotFoundReports.size() != 0 ) {
			Log.e(tag,"Total of " +mFreshSnowNotFoundReports.size() + " reports missing fresh snow totals despite lack of error conditions");
			
			for( Report report : mFreshSnowNotFoundReports ) {
				logReportFailure(tag,report,"Fresh Snow Not Found");
			}
		}
	}

	public void logSuccessConditions(String tag) {
		/**
		 * log info message with successful locations
		 */
		if( mSuccessLocations.size() != 0 ) {
			Log.i(tag,"Total of " + mSuccessLocations.size()+ " resorts with no problems found on server " + mServer);
			for( Report report : mSuccessLocations) {
				Log.i(tag,report.getResort().getLocation() + " fresh snow: " + report.getFreshAsString() );
			}
		}
		
	}
	
	public void addError(String error) {
		mErrorStrings.add(error);
	}
	
	/**
	 * @param r report to process
	 * @return true of no errors were found, false if errors were found
	 */
	public boolean processReport(Report r) {
		boolean processSuccess = false;
		if( !r.hasErrors() ) {
			if( !r.hasFreshSnowTotal() ) {
				mFreshSnowNotFoundReports.add(r);
			} else {
				mSuccessLocations.add(r);
				processSuccess =  true;
			}
		} else {
			mErrorReports.add(r);
		}
		/**
		 * TODO: more testing of this report
		 */
		return processSuccess;
	}
	

	
}
