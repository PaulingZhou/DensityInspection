package com.zhou.densityinspection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	private BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private TextView tvData = null;
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
		tvData = (TextView) findViewById(R.id.tv_data);
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
				Message msg = new Message();
				msg.obj = "请稍候，正在连接服务器" + BlueTooth.BlueToothAddress;
				LinkDetectedHandler.sendMessage(msg);
				socket.connect();
				msg.obj = "已经连接上服务器：" + BlueTooth.BlueToothAddress;
				LinkDetectedHandler.sendMessage(msg);
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
		private class readThread extends Thread{
			@Override
			public void run() {
				byte[] buffer = new byte[5];
				byte[] buf_data = new byte[10];
				int bytes=0;
				InputStream mmInStream = null;
				try {
					mmInStream = socket.getInputStream();
					int count = 0,i=0;
					while (true) {
							byte data = (byte) mmInStream.read();
							boolean ishead = false;
							if(data == (byte)0xAA && count ==0){
								count = 1;
							}if(data == (byte)0xBB && count ==1){
								count = 2;
								ishead = true;
							}
							if(count == 2&&(!ishead)){
								buffer[i] = data;
//								Log.e("buffer"+i, ""+data);
								i++;
							}if(i==5){
								i=0;
								count=0;
								Message msg = new Message();
								msg.obj = "";
								for(int a=0;a<5;a++){
									msg.obj = msg.obj + ""+buffer[a]; 
								}
								LinkDetectedHandler.sendMessage(msg);
							}
//						}
						// Read from the InputStream
//						    bytes = mmInStream.read(buffer);
//							Log.e("bytes", ""+bytes);
//							for (int i = 0; i < bytes; i++) {
//								buf_data[i] = buffer[i];
//								Log.e("buf_data"+i, ""+buffer[i]);
//							}
//							String s = new String(buf_data);
//							Message msg = new Message();
//							msg.obj = s;
//							msg.what = 1;
//							LinkDetectedHandler.sendMessage(msg);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	
	private Handler LinkDetectedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			Log.e("msg", (String) msg.obj);
			tvData.setText((String) msg.obj);
		}
	};
	
}
