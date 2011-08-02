package com.irongroup;

import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


import android.test.ActivityInstrumentationTestCase;
import android.test.AndroidTestCase;
import android.util.Log;

public class Test extends ActivityInstrumentationTestCase<MainActivity> {
	public Test() {
		super("com.irongroup", MainActivity.class);
		// TODO Auto-generated constructor stub
	}

	private static final String TAG = "TestAndroidCallTime";

	public void testAndroidXml() throws Throwable {
		try {
			String test=getActivity().getCallTimeListJson("type=2 and duration !=0 and number='07955711153'");
			Log.i(TAG,test);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			
		}
	}
}
