package com.irongroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
	private static String TAG = MainActivity.class.getName();
	private WebView webView;
	private Handler handler;
	private WebSettings webSettings;
	private SharedPreferences sp;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		sp=this.getSharedPreferences("CALLTIME", 0);
		handler = new Handler();
		webView = (WebView) this.findViewById(R.id.webView);
		webSettings = webView.getSettings();
		//设置滚动条背景 
		webView.setScrollContainer(true);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY );
		//设置了之后 页面展示有问题
		//webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		webSettings.setJavaScriptEnabled(true);
		webView.loadUrl("file:///android_asset/total.html");
		
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		//获取播出电话记录列表
		webView.addJavascriptInterface(new Object() {
			public void getCallList(final String callnumber) {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback2(" + getCallTimeListJson("type=2 and duration !=0 and number='"+callnumber+"'")
								+ ");");
					}
				});
			}
		}, "calltime_outlist");
		//获取播出电话记录
		webView.addJavascriptInterface(new Object() {
			public void getOutgoing() {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback(" + getCallTimeJson("type=2 and duration !=0 ) group by (number")
								+ ");");
					}
				});
			}
		}, "calltime_out");
		//获取接听电话记录
		webView.addJavascriptInterface(new Object() {
			public void getIncoming() {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback(" + getCallTimeJson("type=1 and duration !=0 ) group by (number")
								+ ");");
					}
				});
			}
		}, "calltime_in");
		//获取接听电话记录列表
		webView.addJavascriptInterface(new Object() {
			public void getCallList(final String callnumber) {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback2(" + getCallTimeListJson("type=1 and duration !=0 and number='"+callnumber+"'")
								+ ");");
					}
				});
			}
		}, "calltime_inlist");
		//获取未接电话记录
		webView.addJavascriptInterface(new Object() {
			public void getMissing() {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback(" + getCallTimeJson("type=3 ) group by (number")
								+ ");");
					}
				});
			}
		}, "calltime_miss");
		//获取接听电话记录列表
		webView.addJavascriptInterface(new Object() {
			public void getCallList(final String callnumber) {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback2(" + getCallTimeListJson("type=3 and number='"+callnumber+"'")
								+ ");");
					}
				});
			}
		}, "calltime_misslist");
		//获取电话记录总计
		webView.addJavascriptInterface(new Object() {
			public void getTotal() {
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("javascript:callback(" + getCallTimeTotal()
								+ ");");
					}
				});
			}
		}, "calltime_total");
		//设置套餐分钟数
		webView.addJavascriptInterface(new Object() {
			public void setTotal(String month_total) {
				Editor editor=sp.edit();
				Integer m =0;
				try {
					m= Integer.parseInt(month_total);
				} catch (Exception e) {
					m=200;
				}
				//Log.i(TAG, "month_total-"+month_total);
				editor.putInt("month_total",m );
				editor.commit();
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("file:///android_asset/total.html");
					}
				});
			}
		}, "set_calltime_total");
		//设置过滤的免费电话
		webView.addJavascriptInterface(new Object() {
			public void setFreePhone(String phone_number) {
				Editor editor=sp.edit();
				//Log.i(TAG, "phone_number-"+phone_number.trim());
				editor.putString("free_phone_number",phone_number.trim());
				editor.commit();
				handler.post(new Runnable() {
					public void run() {
						webView.loadUrl("file:///android_asset/total.html");
					}
				});
			}
		}, "set_free_phone");
		
	}

	public String getCallTimeJson(String where) {
		String[] columns = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
				"max("+CallLog.Calls.DATE+") as latetime",
				"count("+CallLog.Calls.NUMBER+") as sum",
	            "sum(" + CallLog.Calls.DURATION  +") as times" };
		Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI,
				columns, where, null,
				"count(number) desc");
		
		try { 
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				long duration = 0;
				String durationStr = "";
				Map<String, Object> calllog = new HashMap<String, Object>();

				try {
					duration = cursor.getLong(cursor
							.getColumnIndex("times"));
					if (duration % 60 > 0) {
//						duration = duration / 60 + 1;
						durationStr = (duration / 60>0?(duration / 60+"分"):"") + duration % 60+ "秒";
					} else {
//						duration = duration / 60;
						durationStr = duration / 60 + "分";
					}
				} catch (Exception e) {
					duration = 1;
				}
				JSONObject json = new JSONObject();
				try {
					json.put("duration", durationStr);
					String number=cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
					json.put("number", number);
					long datetime = cursor.getLong(cursor.getColumnIndex("latetime"));
					Date date = new Date();
					date.setTime(datetime);
					json.put("calltime", getDateString(date));
					String alias=cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
					json.put("alias", alias==null?number:alias);
					String count=cursor.getString(cursor.getColumnIndex("sum"));
					json.put("count", count);
					jsonArray.put(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
			JSONObject outgoing=new JSONObject();
			outgoing.put("outgoings", jsonArray);
			Log.i(TAG,outgoing.toString());
			return outgoing.toString();
		} catch (Exception e) {
			return "";
		} finally {
			if (cursor != null)
				cursor.close();
		}

	}
	public String getCallTimeListJson(String where) {
		String[] columns = new String[] { CallLog.Calls.NUMBER, 
				CallLog.Calls.CACHED_NAME,
				CallLog.Calls.CACHED_NUMBER_TYPE,
				CallLog.Calls.TYPE,
				CallLog.Calls.DATE,
	            CallLog.Calls.DURATION };
		Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI,
				columns, where, null,
				CallLog.Calls.DEFAULT_SORT_ORDER);
		try { 
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				long duration = 0;
				String durationStr = "";
				Map<String, Object> calllog = new HashMap<String, Object>();

				try {
					duration = cursor.getLong(cursor
							.getColumnIndex(CallLog.Calls.DURATION ));
					if (duration % 60 > 0) {
//						duration = duration / 60 + 1;
						durationStr = (duration / 60>0?(duration / 60+"分"):"") + duration % 60+ "秒";
					} else {
//						duration = duration / 60;
						durationStr = duration / 60 + "分";
					}
				} catch (Exception e) {
					duration = 1;
				}
				JSONObject json = new JSONObject();
				try {
					json.put("duration", durationStr);
					String number=cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
					json.put("number", number);
					String type=cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
					json.put("type", type);
					String numbertype=cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE));
					json.put("numbertype", numbertype);
					long datetime = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
					Date date = new Date();
					date.setTime(datetime);
					json.put("calltime", getDateString(date));
					String alias=cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
					json.put("alias", alias==null?number:alias);
					jsonArray.put(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
			JSONObject outgoing=new JSONObject();
			outgoing.put("outgoings", jsonArray);
			Log.i(TAG+"--->",outgoing.toString());
			return outgoing.toString();
		} catch (Exception e) {
			return "";
		} finally {
			if (cursor != null)
				cursor.close();
		}

	}
	public String getCallTimeTotal(){
		// 获取所设置的月套餐包含通话时长
		int timesPerMonth = sp.getInt("month_total", 200);
		// 获取需要过滤的免费电话
		String free_phone_number=sp.getString("free_phone_number", "11%,10%,12%");
		String[] fpn=free_phone_number.split(",");
		
		// 获取本月已拨通话时长
		String where = "type=2 and datetime(substr(date,1,10),'unixepoch','localtime')>=datetime('now','start of month') ";
		for (int i = 0; i < fpn.length; i++) {
			where=where +(" and number not like '"+fpn[i]+"' ");
		}
		int outOfmonth = this.getCallTimeByWhere(where);
		// 获取本周已拨通话时长
		GregorianCalendar calendar = new GregorianCalendar();
		Date now = new Date();
		calendar.setTime(now);
		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
		SimpleDateFormat dateutil = new SimpleDateFormat("yyyy-MM-dd");
		String dayOfWeek = dateutil.format(calendar.getTime()) + " 00:00:00";
		where = "type=2 and datetime(substr(date,1,10),'unixepoch','localtime')>=datetime('"+ dayOfWeek + "')";
		int outOfWeek = this.getCallTimeByWhere(where);
		// 获取当日已拨通话时长
		where = "type=2 and datetime(substr(date,1,10),'unixepoch','localtime')>=date('now','localtime')||' 00:00:00'";
		int outOfToday = this.getCallTimeByWhere(where);
		// 获取当日已接通话时长
		where = "type=1 and datetime(substr(date,1,10),'unixepoch','localtime')>=date('now','localtime')||' 00:00:00'";
		int inOfToday = this.getCallTimeByWhere(where);
		// 获取本周已接通话时长
		where = "type=1 and datetime(substr(date,1,10),'unixepoch','localtime')>datetime('"+ dayOfWeek + "')";
		int inOfWeek = this.getCallTimeByWhere(where);
		// 获取本月已接通话时长
		where = "type=1 and datetime(substr(date,1,10),'unixepoch','localtime')>datetime('now','start of month')";
		int inOfMonth = this.getCallTimeByWhere(where);
		// 获取已拨所占月套餐的百分比
		double persent = (double) outOfmonth * 100 / (double) timesPerMonth;
		java.text.DecimalFormat df = new java.text.DecimalFormat("########.000");
		persent = (new Double(df.format(persent))).doubleValue();
		JSONObject json=new JSONObject();
		try {
			json.put("outOfWeek",outOfWeek);
			json.put("outOfToday",outOfToday);
			json.put("inOfToday",inOfToday);
			json.put("inOfWeek",inOfWeek);
			json.put("inOfMonth",inOfMonth);
			json.put("outOfmonth",outOfmonth);
			json.put("persent",persent);
			json.put("timesPerMonth",timesPerMonth);
			json.put("freephone",free_phone_number);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		//Log.i(TAG, json.toString());
		return json.toString();
	}
	private int getCallTimeByWhere(String where) {
		int total = 0;
		
		final Cursor cursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI,
				new String[] { CallLog.Calls.NUMBER, CallLog.Calls.DURATION },
				where, null, CallLog.Calls.DEFAULT_SORT_ORDER);
		// startManagingCursor(cursor)

		try {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				int duration = 0;
				try {
					duration = Integer.parseInt(cursor.getString(cursor
							.getColumnIndex(CallLog.Calls.DURATION)));
					if (duration % 60 > 0)
						duration = duration / 60 + 1;
					else
						duration = duration / 60;
				} catch (Exception e) {
					duration = 1;
				}
				total += duration;
			}
		} catch (Exception e) {
		}finally {
			if (cursor != null)
				cursor.close();
		}
		return total;
	}
	public static String getDateString(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 2, 2, R.string.exit_cn);
		menu.add(0, 1, 1, R.string.about_cn);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		Dialog dialog = null;
		switch (itemId) {
		
		case 1:
			dialog = new AlertDialog.Builder(this).setTitle(R.string.about_title).setMessage(
					R.string.about).setPositiveButton(R.string.about_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).create();
			dialog.show();
			break;
		case 2:
			this.onDestroy();
			break;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		finish();
	}

}