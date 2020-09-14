/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.example.jdy_spp;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jdy_spp.service.BluetoothChatService;
import com.example.jdy_spp.util.CommonUtil;

import static com.example.jdy_spp.SettingActivity.SEND_FILE_ACTION;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MainActivity extends Activity implements OnClickListener {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;
    private MyLocalReceiver mReceiver = new MyLocalReceiver();

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;

    int jdy_spp_rx_value = 0;
    int jdy_spp_tx_value = 0;
    int jdy_spp_stat = 0;


    //===================  自定义页面数据存储区域 ==============

    private Button start, stop;
    private Button heightAdd, heightSub, yuShengSong, yuShengJin;
    private Button danshuang;

    private TextView dis; // 接收数据显示句柄
    private ScrollView sv; // 翻页句柄
    private String smsg = ""; // 显示用数据缓存

    //+++++++++++++++++++++
    private TextView height_value;
    private TextView rope_value;
    private TextView speed_value;//速度
    private TextView shache_qiya;//刹车气压
    private TextView lihe_qiya;//离合气压
    private TextView tv_A;//大电流
    private TextView shache_lidu1;// 刹车力度
    private TextView lihe_sudu1;// 离合速度
    private TextView zheng, fan;//正反转数
    private TextView textView28, textView29, textView30, textView32;
    private TextView imageView21, imageView22, imageView24, imageView25;
    private ImageView imageView;//刹车加
    private ImageView imageView2;
    private ImageView imageView3;
    private ImageView imageView4;


    private boolean dianjikai, dongzuo, dan_shuangda;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    boolean connect_status_bit = false;
    private StringBuffer sbValues;

    //===================  自定义页面数据存储区域 end ==============

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化工具类，做toast提示或者其他操作
        CommonUtil.init(this.getApplicationContext());
        initView();

        // 获取蓝牙适配器,如果蓝牙适配器为空，则当前设备不支持蓝牙，关闭页面
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用！", Toast.LENGTH_LONG).show();
            finish();
        }
        // 初始化蓝牙服务
        mChatService = new BluetoothChatService(this, mHandler);

        // 初始化发送缓存区
        mOutStringBuffer = new StringBuffer("");

        //注册广播监听器
        registerLoginBroadcast();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            // 当蓝牙服务初始化后未连接成功时候，开启蓝牙连接服务
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mChatService != null) {
            mChatService.stop();
        }
        unRegisterLoginBroadcast();
        super.onDestroy();
    }

    private void ensureDiscoverable() {
        if (D)
            Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * 发送信息
     */
    private void sendString(String message) {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
            mOutStringBuffer.setLength(0);
        }
    }

    //发送文件
    private void sendFile() {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        mChatService.sendFile(getAssets());
    }


    private void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(resId);
        }
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                   //接收到来自单片机的数据
                   displayData(readBuf);

//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    CommonUtil.showToast(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    jdy_spp_stat = 1;
                    jdy_spp_rx_value = 0;
                    jdy_spp_tx_value = 0;
                    invalidateOptionsMenu();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    jdy_spp_stat = 0;
                    invalidateOptionsMenu();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // 蓝牙已就绪
                    initView();
                } else {
                    // 用户拒绝开启蓝牙，关闭页面
                    finish();
                }
        }
    }

    //连接蓝牙设备
    private void connectDevice(Intent data) {
        if (data == null || data.getExtras() == null) {
            return;
        }

        // 拿到连接设备的Mac地址
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        if (jdy_spp_stat == 1) {
            menu.findItem(R.id.secure_connect_scan).setVisible(true);
        } else {
            menu.findItem(R.id.secure_connect_scan).setVisible(false);
        }
        return true;
    }

    //菜单栏目点击处理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                mChatService.disconnect();
                return true;
            case R.id.insecure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                //ensureDiscoverable();
                Intent intent1 = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent1);
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //断开连接
    private void disConnect() {
        if (mChatService != null) {
            mChatService.disconnect();
        }
    }
    private void registerLoginBroadcast() {
        IntentFilter intentFilter = new IntentFilter(SEND_FILE_ACTION);
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mReceiver, intentFilter);
    }

    //取消注册
    private void unRegisterLoginBroadcast() {
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mReceiver);
    }


    private class MyLocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == SEND_FILE_ACTION) {
                sendFile();
            }
        }
    }

    // ==============  以下功能为移植过来的功能 =================

    public void initView() {
        dis = (TextView) findViewById(R.id.in); // 得到数据显示句柄
        sv = (ScrollView) findViewById(R.id.scroll_view);

        Button btnrec = (Button) findViewById(R.id.rec);
        btnrec.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                smsg = "";
                dis.setText(smsg); // 显示数据
              //  height_value.setText("," + (UN_I++) % 10);

            }
        });

        // kaiDianJi = (Button) findViewById(R.id.kai);
        //kaiDianJi.setOnClickListener(this);

        // kaiDianJi = (Button) findViewById(R.id.kai);
        // kaiDianJi.setOnClickListener(this);
        //++++++++++++++
        imageView21 = (TextView) findViewById(R.id.imageView21);
        imageView24 = (TextView) findViewById(R.id.imageView24);
        imageView22 = (TextView) findViewById(R.id.imageView22);
        imageView25 = (TextView) findViewById(R.id.imageView25);
        textView28 = (TextView) findViewById(R.id.textView28);
        textView29 = (TextView) findViewById(R.id.textView_29);
        // textView30 = (TextView) findViewById(R.id.textView30);
        // textView32 = (TextView) findViewById(R.id.textView32);
        height_value = (TextView) findViewById(R.id.height_value);// ,
        rope_value = (TextView) findViewById(R.id.rope_value);
        shache_lidu1 = (TextView) findViewById(R.id.shache_lidu1);
        zheng = (TextView) findViewById(R.id.zheng);
        fan = (TextView) findViewById(R.id.fan);
        lihe_sudu1 = (TextView) findViewById(R.id.lihe_sudu1);
        //speed_value = (TextView) findViewById(R.id.speed_value);
        shache_qiya = (TextView) findViewById(R.id.shache_qiya);
        lihe_qiya = (TextView) findViewById(R.id.lihe_qiya);
        tv_A = (TextView) findViewById(R.id.tv_A);
        //  tv_V = (TextView) findViewById(R.id.tv_V);
        //imageView = (ImageView) findViewById(R.id.imageView);
        // imageView2 = (ImageView) findViewById(R.id.imageView2);
        //imageView3 = (ImageView) findViewById(R.id.imageView3);
        // imageView4 = (ImageView) findViewById(R.id.imageView4);
        //+++++++++++++
        // guanDianJi = (Button) findViewById(R.id.guan);
        // guanDianJi.setOnClickListener(this);

        // safeOn =  (Button) findViewById(R.id.jingyan_qidong);
        // safeOn.setOnClickListener(this);
        //++++++++++按键触发句柄+++++++++++++++++++++++++++++++++
        start = (Button) findViewById(R.id.qidong);
        start.setOnClickListener(this);

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(this);

        heightAdd = (Button) findViewById(R.id.height_add);
        heightAdd.setOnClickListener(this);

        heightSub = (Button) findViewById(R.id.height_sub);
        heightSub.setOnClickListener(this);

        yuShengSong = (Button) findViewById(R.id.rope_add);
        yuShengSong.setOnClickListener(this);

        yuShengJin = (Button) findViewById(R.id.rope_sub);
        yuShengJin.setOnClickListener(this);

        danshuang = (Button) findViewById(R.id.danshuang);
        danshuang.setOnClickListener(this);
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

        //关于选项按钮触发事件
//        Button usetButton = (Button) findViewById(R.id.use);
//        usetButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainActivity.this,
//                        UseActivity.class);
//                //startActivity(intent);
//                startActivityForResult(intent, REQUEST_DATA);
//            }
//
//        });


//        Button exitButton = (Button) findViewById(R.id.exit);
//        exitButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                finish();
//            }
//        });

        //按钮长安操作
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.height_add:
                //高度加
                sendString("fafc01");
                break;
            case R.id.height_sub:
                //高度减
                sendString("fafc04");
                break;
            case R.id.rope_add:
                //余绳紧
                sendString("fafc02");
                break;
            case R.id.rope_sub:
                //余绳松
                sendString("fafc05");
                break;
            default:
                break;
            case R.id.qidong:
                sendString("fafc0a");
                break;
            case R.id.stop:
                //停止
                if (dongzuo) {
                    sendString("fafc09");
                    dongzuo = false;
                } else {
                    sendString("fafc0c");
                    dongzuo = true;
                }
                break;
        }
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dis.setText(status);
            }
        });
    }

    //接收数据方法
    /*private void displayData(byte[] data1) {
        if (data1 != null && data1.length > 0) {

            final StringBuilder stringBuilder = new StringBuilder(sbValues.length());
            for (byte byteChar : data1)
                stringBuilder.append(String.format(" %02X", byteChar));

            String da = stringBuilder.toString();
            sbValues.append(da);
            dis.setText(sbValues.toString());

            disp2Mobile(data1);
        }
    }
*/
    //接收数据方法
    private void displayData(byte[] data1) {
        if (data1 != null && data1.length > 0) {

            StringBuilder stringBuilder = new StringBuilder();
            for (byte byteChar : data1) {
             //   stringBuilder.append(String.format(" %02x", byteChar));
            }

            dis.setText(stringBuilder.toString());
            disp2Mobile(data1);
        }
    }
    int UN_I = 0;

    public void disp2Mobile(byte[] data1) {
        if (data1 == null) {
            return;
        }
        // gaodu
        //byte a1 = 33;//src[0];
        float sheche = 0;
        float dadianliu = 0;
        float lihe = 0;
        float zongqiya = 0;
        //int shache_kong = 0;//刹车电磁阀空锤
        // int lihe_kong = 0;//离合电磁阀空锤
        // int xieyan =0 ;//斜岩引起的锤倒
        // int lihe_ka = 0;//离合电磁阀卡顿空锤

        int a1 = data1[0] & 0xff;
        int a2 = data1[1] & 0xff;
       // int a3 = data1[2] & 0xff;
       // int a4 = data1[3] & 0xff;
        //int a5 = data1[4];
        int a30 = data1[29];
        //if (a1 == 0xAA  && a30 == 0xAA)
        {
            int a3 = data1[2] & 0xff;//高度
            int a4 = data1[3];//刹车提前
            int a5 = data1[4] & 0xff;//刹车时间
            int a6 = data1[5];//与绳长度
            int a7 = data1[6] & 0xff;//电流高8位
            int a8 = data1[7] & 0xff;//电流低8位
            int a9 = data1[8];//电机状态
            int a10 = data1[9];//启停状态
            int a11 = data1[10];//单双打
            int a12 =  data1[11];//正传数
            int a13 =  data1[12];//反传数
            int a14 =  data1[13];//进尺高
            int a15 =  data1[14];//进尺低
            int a16 =  data1[15];//报警值
            int a17 =  data1[18];//高1
            int a18 =  data1[19];//高2
                int a19 = data1[20] & 0xff;//离合延时 及速
            int a20 =  data1[23];//刹车电流
            int a21 =  data1[27];//刹车气压
            int a22 = data1[28] & 0xff;//离合气压
            dadianliu = a7 << 8 | a8;

            height_value.setText((int) 3 * a3 + "," + (UN_I++) % 10);
            rope_value.setText((int) 3 * a4 + "  ");
            shache_lidu1.setText((int) a5 + "  ");
            tv_A.setText((float) dadianliu + "  ");
            zheng.setText((int) a12 + "  ");
            fan.setText((int) a13 + "  ");
            lihe_sudu1.setText((int) 3 * a19 + "");
            shache_qiya.setText((int) a21 + "  ");
            lihe_qiya.setText((int) a22 + "");
            if (a11 == 1)
                danshuang.setBackgroundResource(R.drawable.shuangda);
            if (a11 == 2)
                danshuang.setBackgroundResource(R.drawable.danda);
            if (a9 == 0)
                start.setBackgroundResource(R.drawable.dianjiqidong);
            if (a9 == 1)
                start.setBackgroundResource(R.drawable.dianjiting);
            if (a10 == 1)
                stop.setBackgroundResource(R.drawable.qidonghongse);
            if (a10 == 2)
                stop.setBackgroundResource(R.drawable.qidong);
        }

    }
}
