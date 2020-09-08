package com.example.jdy_spp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.jdy_spp.util.CommonUtil;

import java.lang.reflect.Method;
import java.util.Set;

public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    Set<BluetoothDevice> pairedDevices;

    int jdy_index = 0;
    int jdy_index_en = 0;

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        if (aItem.getItemId() == 0) {
            if (jdy_index_en == 1) {
                jdy_index_en = 0;
                int icc = 0;
                for (BluetoothDevice device : pairedDevices) {
                    if (icc == jdy_index) {
                        //mPairedDevicesArrayAdapter.add( ""+icc );
                        unpairDevice(device);
                    }
                    icc++;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // doDiscovery();
                // v.setVisibility(View.GONE);
                finish();
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        newDevicesListView.setBackgroundColor(Color.rgb(50, 100, 50));


        //mNewDevicesArrayAdapter.set

        //this.mFavList = (ListView) findViewById(R.id.new_devices);

        pairedListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

            public void onCreateContextMenu(ContextMenu conMenu, View view, ContextMenuInfo info) {
                jdy_index = ((AdapterContextMenuInfo) info).position;
                jdy_index_en = 1;
                conMenu.setHeaderTitle("");
                conMenu.add(0, 0, 0, "clear band=" + jdy_index);


            }
        });

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        /* Set<BluetoothDevice>*/
        pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        mBtAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //发现蓝牙设备
                if (bd.getBondState() == BluetoothDevice.BOND_NONE) {//如果没有配对
                    //进行配对
                    try {
                        mNewDevicesArrayAdapter.add(bd.getName() + "\n" + bd.getAddress());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //new ConnectThread().start();//已经配对进行连接
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {//配对状态的改变
                BluetoothDevice bd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (bd.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        CommonUtil.showToast("正在配对...");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        CommonUtil.showToast("配对成功");
                        break;
                    case BluetoothDevice.BOND_NONE:
                        CommonUtil.showToast("取消配对");
                        break;
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                CommonUtil.showToast("已经连接上蓝牙设备");

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                CommonUtil.showToast("已经断开蓝牙设备");
            }
        }
    };
}
