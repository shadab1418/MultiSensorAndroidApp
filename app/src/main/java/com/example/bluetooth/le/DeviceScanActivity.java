package com.example.bluetooth.le;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
/**
 * Activity for scanning and displaying available Bluetooth LE devices and Wifi .
 */
public class DeviceScanActivity extends ListActivity implements SensorEventListener{

	/*BLE device settings*/

	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	public static int counter = 0;
	Timer bletimer = new Timer();
	TimerTask bleScanInterval;

	private static final int REQUEST_ENABLE_BT = 1;
	// BLE Stops scanning after 5 seconds.
	private static final long SCAN_PERIOD = 1000;

	/*-----------END-----------*/

	/* Wifi scan settings*/
		// RF mix
		WifiManager mainWifiObj;
		WifiScanReceiver wifiReceiver;
        Timer wifitimer = new Timer();
        TimerTask UpdateWifi;
		String wifis[];
		// logging
		String str = "";
		String path = Environment.getExternalStorageDirectory().getPath();

		File newTextFile = new File(path, "/wifi.txt");
		File newTextFile2 = new File(path, "/ble.txt");
	/*-----------END--------------------------------------------------------*/

    /** Accelerometer Settings */
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double  deltaX = 0;
    private double deltaY = 0;
    private double  deltaZ = 0;
    /*-----------------*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_devices);
		mHandler = new Handler();

		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		if(newTextFile.exists()){

			try {
				FileWriter fw = new FileWriter(newTextFile,true);
				fw.write("*************************************************" + "\n");
				fw.close();
			} catch (IOException iox) {
				//do stuff with exception
				iox.printStackTrace();
			}


		}

        if(newTextFile2.exists()){

            try {
                FileWriter fw = new FileWriter(newTextFile,true);
                fw.write("*************************************************" + "\n");
                fw.close();
            } catch (IOException iox) {
                //do stuff with exception
                iox.printStackTrace();
            }


        }

		/*Wifi scan stuffs */

		// open record file
				/*str="test\n";
				try {
					FileWriter fw = new FileWriter(newTextFile,true);
					fw.write(str);
					fw.close();
				} catch (IOException iox) {
					//do stuff with exception
					iox.printStackTrace();
				}
				*/

				// wifi
				mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE );
				wifiReceiver = new WifiScanReceiver();



		/*-------------------------------------------------------------------------*/
        /* Accelerometer */
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            // fail! we dont have an accelerometer!
        }

       /*----------------------------------------------------------------*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(
					R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			mLeDeviceListAdapter.clear();
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		/*BLE start*/

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		setListAdapter(mLeDeviceListAdapter);
		//scanLeDevice(true);
		
		/*------------------------BLE END--------------------------*/
		
		/*Wifi Start */

		registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		/*---------------END----------------------*/

        /*Accelerometer*/

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		/*BLE*/
		scanLeDevice(false);
		mLeDeviceListAdapter.clear();
		
		/*Wifi*/
		unregisterReceiver(wifiReceiver);
        /*Accelerometer*/
        sensorManager.unregisterListener(this);

	}

    @Override
    public void onSensorChanged(SensorEvent event) {

        deltaX = event.values[0];
        deltaY = event.values[1];
        deltaZ = event.values[2];
        String accelerometer_data = "x = " + deltaX + " y = " + deltaY + " z = " + deltaZ;
        Log.d("Shadab", accelerometer_data);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* -------------------Wifi Scan ------------------------------------*/
		private class WifiScanReceiver extends BroadcastReceiver{
			public void onReceive(Context c, Intent intent) {
				List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
				wifis = new String[wifiScanList.size()];
				Calendar wifiTime = Calendar.getInstance();


				for(int i = 0; i < wifiScanList.size(); i++){
					String text = /*wifiScanList.get(i).SSID + "\t\t" + */wifiScanList.get(i).frequency + "\t\t" + wifiScanList.get(i).BSSID.toString() + "\t\t" + wifiScanList.get(i).level;
					Long wifimsLong = wifiTime.getTimeInMillis();//System.currentTimeMillis();
					String wifimillisecond = wifimsLong.toString();
					try {
						FileWriter fw = new FileWriter(newTextFile,true);
						fw.write(String.valueOf(wifiTime.get(Calendar.SECOND)) + "\t\t" + wifimillisecond + "\t\t" + text + "\n");
						fw.close();
					} catch (IOException iox) {
						//do stuff with exception
						iox.printStackTrace();
					}

					Log.d("view", text);
				}
			}
		}

		class UpdateWifiTask extends TimerTask {
			public void run() {
				mainWifiObj.startScan();
				//calculate the new position of myBall
			}
		}
		
	/*-------------------------END------------------------------------------*/

	// Timer setting for BLE Interval scan
	class scanInterval extends TimerTask{

		public void run(){

			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
			@Override
				public void run() {
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				//invalidateOptionsMenu();
				}
			}, 1000);

		mBluetoothAdapter.startLeScan(mLeScanCallback);

		}

	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			/* Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);*/
            TimerTask UpdateWifi1 = new UpdateWifiTask();
			TimerTask bleScanInterval1 = new scanInterval();
            UpdateWifi = UpdateWifi1;
			bleScanInterval = bleScanInterval1;
            wifitimer.scheduleAtFixedRate(UpdateWifi, 0, 1000); // wifi scan update every 1s
			mScanning = true;
			bletimer.scheduleAtFixedRate(bleScanInterval, 0, 2000); // BLE scan update every 1s
			//mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {

			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			bleScanInterval.cancel();
            //bletimer.cancel();

            if(newTextFile2.exists()){

                try {
                    FileWriter fw = new FileWriter(newTextFile2,true);
                    fw.write("*************************************************" + "\n");
                    fw.close();
                } catch (IOException iox) {
                    //do stuff with exception
                    iox.printStackTrace();
                }


            }

            UpdateWifi.cancel();
			if(newTextFile.exists()){

				try {
					FileWriter fw = new FileWriter(newTextFile,true);
					fw.write("*************************************************" + "\n");
					fw.close();
				} catch (IOException iox) {
					//do stuff with exception
					iox.printStackTrace();
				}


			}

		}
		invalidateOptionsMenu();
	}

	// Adapter for holding devices found through scanning.
	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> mLeDevices;
		private ArrayList<Map<BluetoothDevice, String>> mLeDevicesTest;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mLeDevicesTest = new ArrayList<Map<BluetoothDevice, String>>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(BluetoothDevice device, Map<BluetoothDevice, String> map) {

			 mLeDevicesTest.add(map);
			/*if (!mLeDevices.contains(device)) {
				mLeDevices.add(device);				
			}*/
		}

		public Map<BluetoothDevice, String> getDevice(int position) {
			return mLeDevicesTest.get(position);
		}

		public void clear() {
			mLeDevicesTest.clear();
		}

		@Override
		public int getCount() {
			return mLeDevicesTest.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevicesTest.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			//Log.d("view", String.valueOf(i));
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view
						.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view
						.findViewById(R.id.device_name);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			Map<BluetoothDevice, String > bleMap = mLeDevicesTest.get(i);
			BluetoothDevice device1 = null;
			String bleRSS = null;
			for (Entry<BluetoothDevice, String> entry : bleMap.entrySet())
			{
				bleRSS = entry.getValue();
				device1 = entry.getKey();
				//Log.d("view", entry.getValue());
			   // System.out.println(entry.getKey() + "/" + entry.getValue());
			}


			//BluetoothDevice device = mLeDevices.get(i);
			final String deviceName = device1.getName();
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.deviceName.setText(deviceName);
			else
				viewHolder.deviceName.setText(R.string.unknown_device);
			viewHolder.deviceAddress.setText(device1.getAddress() + " / " + bleRSS);

			return view;
		}
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {

			 final Map<BluetoothDevice, String > map = new HashMap<BluetoothDevice, String>();
            map.put(device, String.valueOf(rssi));// store the counter object with the key.

            //time testing
            long testtime = System.currentTimeMillis();
            Calendar bleTime = Calendar.getInstance();

			bleTime.setTimeInMillis(testtime);
            //time.get(Calendar.HOUR_OF_DAY);
			//Log.d("Shadab", path);
			//Log.d("Shadab", String.valueOf(bleTime.get(Calendar.HOUR_OF_DAY)) + String.valueOf(bleTime.get(Calendar.MINUTE)) + String.valueOf(bleTime.getTime()) + String.valueOf(testtime));

			String text = String.valueOf(device.getName() + "\t\t" /*+ device.getAddress() + "\t\t"*/ + String.valueOf(rssi) + "\n");
			Long blemsLong = bleTime.getTimeInMillis();
			String blemillisecond = blemsLong.toString();
			try {
				FileWriter fw = new FileWriter(newTextFile2,true);
                fw.write(String.valueOf(bleTime.get(Calendar.SECOND))  + "\t\t" + blemillisecond + "\t\t" + text);
				fw.close();
			} catch (IOException iox) {
				//do stuff with exception
				iox.printStackTrace();
			}


			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeDeviceListAdapter.addDevice(device,map);
					mLeDeviceListAdapter.notifyDataSetChanged();

				}
			});
		}
	};

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}