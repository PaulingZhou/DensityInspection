package com.zhou.densityinspection;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.zhou.densityinspection.BlueTooth.ServerOrClient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class DataActivity extends Activity {
	public enum Status {
		OUTFRAME, HEADFRAME1, HEADFRAME2, LENGTHFRAME,DATAFRAME,CHECKFRAME
	}

	private BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private TextView tvDataTemp, tvDataHumi,tvDataTime = null;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

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

	private readThread mreadThread = null;;
	private ClientThread clientConnectThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data);
		tvDataTemp = (TextView) findViewById(R.id.tv_data_temp);
		tvDataHumi = (TextView) findViewById(R.id.tv_data_humi);
		tvDataTime =(TextView) findViewById(R.id.tv_data_time);
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
		private int dataLength = 0,dataCount=0;
		private byte[] dataFrame = null;
		private byte checkData = 0x00;
		
		@Override
		public void HandleData(byte data) {
			switch (status) {
			case OUTFRAME:
				dataFrame = null;
				dataCount=0;
				dataLength=0;
				checkData = 0x00;
				if (data == (byte)0xAA)
					status = Status.HEADFRAME1;
				break;
			case HEADFRAME1:
				if(data == (byte)0xBB)
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
				if(dataCount == dataLength){
					status = Status.CHECKFRAME;
				}
				break;
				case CHECKFRAME:
//					Log.e(""+checkData, ""+data);
					status = Status.OUTFRAME;
					if(checkData == data){
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

	private Handler LinkDetectedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			byte[] buffer = (byte[]) msg.obj;
//			Log.e("bufferLength", ""+buffer.length);
			if(buffer[0] == 0x07){
				 int temp_i = (int)(buffer[2]&0xFF)*256+(int)(buffer[1]&0xFF);
				 int humi_i = (int)(buffer[4]&0xFF)*256+(int)(buffer[3]&0xFF);
				 int lengthTimerCount = (int)(buffer[6]&0xFF)*256+(int)(buffer[5]&0xFF);
				 float temp_f = (float) (((float)temp_i)*0.01-40);
				 float humi_f =
				 (float)(((float)humi_i)*0.0405-((float)humi_i*(float)humi_i)*2.8/1000000-4);
				 int lengthTimer = lengthTimerCount*209;
				 Log.e("temp_i", ""+temp_i);
				 Log.e("humi_i", ""+humi_i);
				 Log.e("temp_f", ""+temp_f);
				 Log.e("humi_f", ""+humi_f);
				 Log.e("lengthTimer", ""+lengthTimerCount);
				 tvDataTemp.setText("温度为："+temp_f+"℃");
				 tvDataHumi.setText("湿度为："+humi_f+"%");
				 tvDataTime.setText("声时为："+lengthTimer+"ns");
			}
//			if (buffer[0] == 0x01) {
//				int temp_i = (int) (buffer[2] & 0xFF) * 256
//						+ (int) (buffer[1] & 0xFF);
//				float temp_f = (float) ((float) temp_i * 0.0625);
//				tvDataTemp.setText("温度为：" + temp_f + "℃");
//				tvDataHumi.setText("无湿度数据");
//			}

		}
	};

}
