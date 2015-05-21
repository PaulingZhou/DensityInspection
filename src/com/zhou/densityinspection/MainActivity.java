package com.zhou.densityinspection;

import java.util.ArrayList;
import java.util.Set;

import com.zhou.densityinspection.BlueTooth.ServerOrClient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;


public class MainActivity extends Activity {
	private ListView mListView;
	private ArrayList<SiriListItem> list;
	Context mContext;
	ChatListAdapter mAdapter;
	
	private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
	protected void onStart() {
		super.onStart();
		Log.e("activity", "start");
		if(!mBtAdapter.isEnabled()){
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, 3);
		}
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("activity", "create");
        setContentView(R.layout.activity_main);
        mContext = this;
        init();
    }
	
	private void init(){
		list = new ArrayList<MainActivity.SiriListItem>();
		mAdapter = new ChatListAdapter(mContext, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mDeviceClickListener);
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		
		if(pairedDevices.size()>0){
			for(BluetoothDevice device : pairedDevices){
				list.add(new SiriListItem(device.getName()+"\n"+device.getAddress(), true));
				mAdapter.notifyDataSetChanged();
				mListView.setSelection(list.size()-1);
			}
		}else{
			list.add(new SiriListItem("没有找到设备", true));
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size()-1);
		}
	}
	
	public class SiriListItem {
		String message;
		boolean isSiri;
		public SiriListItem(String msg, boolean siri) {
			message = msg;
			Log.e("SiriListItem", msg);
			isSiri = siri;
		}
	}
	
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
			SiriListItem item = list.get(position);
			String info = item.message;
			String address = info.substring(info.length()-17);
			Log.e("address",  address);
			BlueTooth.BlueToothAddress = address;
			BlueTooth.serverOrClient = ServerOrClient.CLIENT;
			Intent intent = new Intent(MainActivity.this,DataActivity.class);
			startActivity(intent);
		}
	};
}
