package com.zhou.densityinspection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import com.zhou.densityinspection.BlueTooth.ServerOrClient;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class DataActivity extends Activity {
	public enum Status {
		OUTFRAME, HEADFRAME1, HEADFRAME2, LENGTHFRAME, DATAFRAME, CHECKFRAME
	}

	private readThread mreadThread = null;;
	private ClientThread clientConnectThread = null;
	private ViewPager viewPagerForData = null;
	private ActionBar mActionBar = null;
	private List<Tab> tabList = new ArrayList<ActionBar.Tab>();
	private BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private TextView tvDataTemp, tvDataHumi, tvDataTime = null;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private XYMultipleSeriesDataset mDataSet = new XYMultipleSeriesDataset();
	private XYMultipleSeriesDataset mDataSetTemp = new XYMultipleSeriesDataset();
	private XYMultipleSeriesDataset mDataSetHumi = new XYMultipleSeriesDataset();
	private XYMultipleSeriesDataset mDataSetSoundTime = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRender = new XYMultipleSeriesRenderer();
	private XYSeries mSeriesTemp,mSeriesHumi,mSeriesSoundTime;
	private GraphicalView mChartView, mChartViewTemp, mChartViewHumi,
			mChartViewSoundTime;
	private double coOrdinateX = 0, coOrdinateY = 0,coOrdinateYTemp=0,coOrdinateYHumi=0,coOrdinateYSoundTime = 0;
	SoundTimeEliminateMeasurement soundTimeEliminateMeasurement = new SoundTimeEliminateMeasurement();
	private View viewPrimitive,viewTemp,viewHumi,viewSoundTime;


	@Override
	protected void onResume() {
		super.onResume();
		if (BlueTooth.isOpen == true) {
			Toast.makeText(DataActivity.this, "连接已打开", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (BlueTooth.serverOrClient == ServerOrClient.CLIENT) {
			Log.e("Client", "true");
			String address = BlueTooth.BlueToothAddress;
			Log.e("address", address);
			if (!address.equals("null")) {
				device = mBluetoothAdapter.getRemoteDevice(address);
				clientConnectThread = new ClientThread();
				clientConnectThread.start();
				BlueTooth.isOpen = true;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data);
		Init();
	}

	// ViewPager&ActionBar初始化
	private void Init() {

		viewPagerForData = (ViewPager) findViewById(R.id.viewPagerForData);
		LayoutInflater Flater = LayoutInflater.from(this);
		viewPrimitive = Flater.inflate(R.layout.dataprimitive, null);
		viewTemp = Flater.inflate(R.layout.datatemp, null);
		viewHumi = Flater.inflate(R.layout.datahumi, null);
		viewSoundTime = Flater.inflate(R.layout.datasoundtime, null);
		ArrayList<View> dataViews = new ArrayList<View>();
		ArrayList<String> dataStrs = new ArrayList<String>();

		dataViews.add(viewPrimitive);
		dataStrs.add("数据首页");
		dataViews.add(viewTemp);
		dataStrs.add("温度数据");
		dataViews.add(viewHumi);
		dataStrs.add("湿度数据");
		dataViews.add(viewSoundTime);
		dataStrs.add("声时数据");
		viewPagerForData.setAdapter(new PagerViewAdapter(dataViews));
		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowHomeEnabled(false);
		for (int i = 0; i < dataStrs.size(); i++) {
			tabList.add(mActionBar.newTab().setText(dataStrs.get(i))
					.setTabListener(new MyTabListener(i)));
			mActionBar.addTab(tabList.get(i));
		}
		viewPagerForData.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				mActionBar.setSelectedNavigationItem(arg0);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});
		tvDataTemp = (TextView) viewPrimitive.findViewById(R.id.tv_data_temp);
		tvDataHumi = (TextView) viewPrimitive.findViewById(R.id.tv_data_humi);
		tvDataTime = (TextView) viewPrimitive.findViewById(R.id.tv_data_time);

		mRender.setApplyBackgroundColor(true);
		mRender.setShowGrid(true);
		mRender.setGridColor(Color.RED);
		mRender.setBackgroundColor(Color.argb(100, 50, 50, 50));
		mRender.setAxisTitleTextSize(16);
		mRender.setChartTitleTextSize(20);
		mRender.setLabelsTextSize(15);
		mRender.setLegendTextSize(15);
		mRender.setMargins(new int[] { 20, 30, 15, 0 });
//		mRender.set
		mRender.setZoomButtonsVisible(true);
		mRender.setPointSize(1);
		String seriesTitleTemp = "温度", seriesTitleHumi = "湿度", seriesTitleSoundTime = "声时";
		// create a new series of data
		XYSeries seriesTemp = new XYSeries(seriesTitleTemp), seriesHumi = new XYSeries(
				seriesTitleHumi), seriesSoundTime = new XYSeries(
				seriesTitleSoundTime);
		mDataSet.addSeries(seriesTemp);
		mDataSetTemp.addSeries(seriesTemp);
		mDataSetHumi.addSeries(seriesHumi);
		mDataSetSoundTime.addSeries(seriesSoundTime);
		mSeriesTemp = seriesTemp;
		mSeriesHumi = seriesHumi;
		mSeriesSoundTime = seriesSoundTime;
		// create a new renderer for the new series
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		mRender.addSeriesRenderer(renderer);
		// set some renderer properties
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		renderer.setDisplayChartValues(false);
		renderer.setDisplayChartValuesDistance(10);
//		 mCurrentRenderer = renderer;
		// mChartView.repaint();
		ChartViewInit();
	}

	private void ChartViewInit() {
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) viewPrimitive
					.findViewById(R.id.chart);
			mChartView = ChartFactory.getLineChartView(this, mDataSet, mRender);
			layout.addView(mChartView, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			mChartView.setVisibility(View.INVISIBLE);
		} else {
			mChartView.repaint();
		}
		if (mChartViewTemp == null) {
			LinearLayout layoutTemp = (LinearLayout) viewTemp
					.findViewById(R.id.chartTemp);
			mChartViewTemp = ChartFactory.getLineChartView(this, mDataSetTemp,
					mRender);
			layoutTemp.addView(mChartViewTemp, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}else {
			mChartViewTemp.repaint();
		}
		
		if (mChartViewHumi == null) {
			LinearLayout layoutTemp = (LinearLayout) viewHumi
					.findViewById(R.id.chartHumi);
			mChartViewHumi = ChartFactory.getLineChartView(this, mDataSetHumi,
					mRender);
			layoutTemp.addView(mChartViewHumi, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}else {
			mChartViewHumi.repaint();
		}
		
		if (mChartViewSoundTime == null) {
			LinearLayout layoutTemp = (LinearLayout) viewSoundTime
					.findViewById(R.id.chartSoundTime);
			mChartViewSoundTime = ChartFactory.getLineChartView(this, mDataSetSoundTime,
					mRender);
			layoutTemp.addView(mChartViewSoundTime, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}else {
			mChartViewSoundTime.repaint();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// 开启客户端
	private class ClientThread extends Thread {

		@Override
		public void run() {
			try {
				socket = device.createRfcommSocketToServiceRecord(UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				// Toast.makeText(DataActivity.this, "请稍候，正在连接服务器" +
				// BlueTooth.BlueToothAddress, Toast.LENGTH_SHORT).show();
				// Message msg = new Message();
				// msg.obj = "请稍候，正在连接服务器" + BlueTooth.BlueToothAddress;
				// LinkDetectedHandler.sendMessage(msg);
				socket.connect();
				// Toast.makeText(DataActivity.this, "已经连接上服务器：" +
				// BlueTooth.BlueToothAddress, Toast.LENGTH_SHORT).show();
				// msg.obj = "已经连接上服务器：" + BlueTooth.BlueToothAddress;
				// LinkDetectedHandler.sendMessage(msg);
				mreadThread = new readThread();
				mreadThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// super.run();
		}

	};

	// 读取数据
	private class readThread extends Thread {
		@Override
		public void run() {
			// 新建一个handleDataFromBlueTooth的类去实现DataHandler接口
			HandleDataFromBlueTooth handleDataFromBlueTooth = new HandleDataFromBlueTooth();
			InputStream mmInStream = null;
			try {
				mmInStream = socket.getInputStream();
				while (true) {
					byte data = (byte) mmInStream.read();
					handleDataFromBlueTooth.HandleData(data);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	public class HandleDataFromBlueTooth implements DataHandler {
		// 创建私有的静态枚举型变量
		private Status status = Status.OUTFRAME;
		private int dataLength = 0, dataCount = 0;
		private byte[] dataFrame = null;
		private byte checkData = 0x00;

		@Override
		public void HandleData(byte data) {
			switch (status) {
			case OUTFRAME:
				dataFrame = null;
				dataCount = 0;
				dataLength = 0;
				checkData = 0x00;
				if (data == (byte) 0xAA)
					status = Status.HEADFRAME1;
				break;
			case HEADFRAME1:
				if (data == (byte) 0xBB)
					status = Status.HEADFRAME2;
				else
					status = Status.OUTFRAME;
				break;
			case HEADFRAME2:
				dataLength = data;
				dataFrame = new byte[dataLength];
				status = Status.DATAFRAME;
				checkData ^= data;
				break;
			case DATAFRAME:
				dataFrame[dataCount] = data;
				dataCount++;
				checkData ^= data;
				if (dataCount == dataLength) {
					status = Status.CHECKFRAME;
				}
				break;
			case CHECKFRAME:
				// Log.e(""+checkData, ""+data);
				status = Status.OUTFRAME;
				if (checkData == data) {
					Message msg = new Message();
					msg.obj = dataFrame;
					LinkDetectedHandler.sendMessage(msg);
				}
				break;
			default:
				break;
			}
		}

	}

	public class SoundTimeEliminateMeasurement implements SoundTimeRevise {
		private int soundTimeEarly = 0, deltaSound = 0, dataCount = 0;
		private double deltaSoundCount = 0;
		private boolean firstSoundData = true;

		@Override
		public int SoundTimeRevising(int soundTime) {
			dataCount++;
			Log.e("dataCount", "" + dataCount);
			if (firstSoundData) {
				soundTimeEarly = soundTime;
				firstSoundData = false;
			}
			deltaSound = soundTimeEarly - soundTime;
			deltaSoundCount = (double) deltaSound / 132;
//			Log.e("before" + deltaSoundCount, "" + deltaSound);
			if (deltaSoundCount < -0.5)
				deltaSoundCount -= 0.5;
			else if (deltaSoundCount > 0.5)
				deltaSoundCount += 0.5;
			deltaSoundCount = (int) deltaSoundCount;
			soundTime += deltaSoundCount * 132;
			deltaSound = soundTimeEarly - soundTime;
//			Log.e("after" + deltaSoundCount, "" + deltaSound);
			soundTimeEarly = soundTime;
			return soundTime;
		}
	}

	class PagerViewAdapter extends PagerAdapter {

		private ArrayList<View> Views;

		public PagerViewAdapter(ArrayList<View> views) {
			Views = views;
		}

		@Override
		public int getCount() {
			return Views.size();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView(Views.get(position));
		}

		@Override
		public int getItemPosition(Object object) {
			return super.getItemPosition(object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			((ViewPager) container).addView(Views.get(position));
			return Views.get(position);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

	}

	public class MyTabListener implements ActionBar.TabListener {

		private int tabOrder;

		public MyTabListener(int i) {
			tabOrder = i;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			viewPagerForData.setCurrentItem(tabOrder);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

	}

	private Handler LinkDetectedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			byte[] buffer = (byte[]) msg.obj;
			if (buffer[0] == 0x07) {
				int temp_i = (buffer[2] & 0xFF) * 256 + (buffer[1] & 0xFF);
				int humi_i = (buffer[4] & 0xFF) * 256 + (buffer[3] & 0xFF);
				int lengthTimerCount = (buffer[6] & 0xFF) * 256
						+ (buffer[5] & 0xFF);
				lengthTimerCount = soundTimeEliminateMeasurement
						.SoundTimeRevising(lengthTimerCount);
				float temp_f = (float) ((temp_i) * 0.01 - 40);
				float humi_f = (float) ((humi_i) * 0.0405
						- ((float) humi_i * (float) humi_i) * 2.8 / 1000000 - 4);
				double lengthTimer = lengthTimerCount * 20.4 / 1000;
				coOrdinateX++;
				Log.e("x="+coOrdinateX, "y="+coOrdinateY);
				// Log.e("temp_i", ""+temp_i);
				// Log.e("humi_i", ""+humi_i);
				// Log.e("temp_f", ""+temp_f);
				// Log.e("humi_f", ""+humi_f);
				// Log.e("lengthTimer", ""+lengthTimerCount);
				tvDataTemp.setText("温度为：" + temp_f + "℃");
				tvDataHumi.setText("湿度为：" + humi_f + "%");
				tvDataTime.setText("声时为：" + lengthTimer + "us");
				coOrdinateY = temp_f;
				coOrdinateYTemp = temp_f;
				coOrdinateYHumi = humi_f/2;
				coOrdinateYSoundTime = lengthTimer; 
				mSeriesTemp.add(coOrdinateX, coOrdinateYTemp);
				mSeriesHumi.add(coOrdinateX, coOrdinateYHumi);
				mSeriesSoundTime.add(coOrdinateX, coOrdinateYSoundTime);
				mChartView.repaint();
				mChartViewTemp.repaint();
				mChartViewHumi.repaint();
				mChartViewSoundTime.repaint();
				FileOutputStream oTemp, oTime, oHumi = null;
				try {
					File dataOutputTemp = new File(Environment
							.getExternalStorageDirectory().toString()
							+ "/DensityInspection/dataOutputTemp.txt");
					File dataOutputHumi = new File(Environment
							.getExternalStorageDirectory().toString()
							+ "/DensityInspection/dataOutputHumi.txt");
					File dataOutputSoundTime = new File(Environment
							.getExternalStorageDirectory().toString()
							+ "/DensityInspection/dataOutputSoundTime.txt");
					dataOutputTemp.createNewFile();
					dataOutputHumi.createNewFile();
					dataOutputSoundTime.createNewFile();
					oTemp = new FileOutputStream(dataOutputTemp, true);
					oTime = new FileOutputStream(dataOutputSoundTime, true);
					oHumi = new FileOutputStream(dataOutputHumi, true);
					oTemp.write(("" + temp_f + " ").getBytes("utf-8"));
					oTime.write(("" + lengthTimer + " ").getBytes("utf-8"));
					oHumi.write(("" + humi_f + " ").getBytes("utf-8"));
					oTemp.close();
					oTime.close();
					oHumi.close();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// if (buffer[0] == 0x01) {
			// int temp_i = (int) (buffer[2] & 0xFF) * 256
			// + (int) (buffer[1] & 0xFF);
			// float temp_f = (float) ((float) temp_i * 0.0625);
			// tvDataTemp.setText("温度为：" + temp_f + "℃");
			// tvDataHumi.setText("无湿度数据");
			// }

		}
	};

}
