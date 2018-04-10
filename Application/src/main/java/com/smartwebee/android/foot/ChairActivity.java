package com.smartwebee.android.foot;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class ChairActivity extends Activity implements View.OnClickListener{
    private final static String TAG = BleSppActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    static long recv_cnt = 0;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;

    public static final int INTI_STA = 99;
    public static final int INIT_COMP = 100;
    public static final int SWITCH_ON = 101;
    public static final int SWITCH_OFF = 102;
    public static final int DANGWEI = 103;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Button zongguan_bt,init;
    private Switch dianji,duanlian;
    private RadioButton rbt_1,rbt_2,rbt_3,rbt_4,rbt_5;
    private LinearLayout kongzhi;
    private ProgressDialog dialog;

    private long recvBytes=0;
    private long lastSecondBytes=0;
    private long sendBytes;
    private StringBuilder mData = new StringBuilder();

    int sendIndex = 0;
    int sendDataLen=0;
    byte[] sendBuf;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case DANGWEI:
                    getSendBuf("550501AA");
                    onSendBtnClicked();
                    break;
                case SWITCH_ON:
                    kongzhi.setVisibility(View.VISIBLE);
                    break;
                case SWITCH_OFF:
                    kongzhi.setVisibility(View.GONE);
                    break;
                case INTI_STA:
                    dialog = new ProgressDialog(ChairActivity.this);
                    dialog.setTitle("初始化数据，请稍等");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    break;
                case INIT_COMP:
                    dialog.dismiss();
                    break;
                default:
                    break;
            }
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //特征值找到才代表连接成功
                mConnected = true;
                invalidateOptionsMenu();
                updateConnectionState(R.string.connected);
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED.equals(action)){
                mBluetoothLeService.connect(mDeviceAddress);
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                final StringBuilder stringBuilder = new StringBuilder();
//                 for(byte byteChar : data)
//                      stringBuilder.append(String.format("%02X ", byteChar));
//                Log.v("log",stringBuilder.toString());
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }else if (BluetoothLeService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
//                mSendBytes.setText(sendBytes + " ");
                if (sendDataLen>0)
                {
                    Log.v("log","Write OK,Send again");
                    onSendBtnClicked();
                }
                else {
                    Log.v("log","Write Finish");
                }
            }

        }
    };

    private void displayData(byte[] buf) {
        String value = bytesToString(buf);
        Log.d("zzh","value = "+value);
        if(value.equals("55 02 00 AA ")) {
            Message msg = new Message();
            msg.what = SWITCH_OFF;
            msg.obj = value;
            handler.sendMessage(msg);
        }else if (value.equals("55 02 01 AA ")){
            Message msg = new Message();
            msg.what = SWITCH_ON;
            msg.obj = value;
            handler.sendMessage(msg);
        }else if (value.equals("55 04 AA ")){
            Message msg = new Message();
            msg.what = DANGWEI;
            msg.obj = value;
            handler.sendMessage(msg);
        }else if (value.equals("55 03 AA ")){
            Message msg = new Message();
            msg.what = INIT_COMP;
            msg.obj = value;
            handler.sendMessage(msg);
        }
        mData = mData.delete(0, mData.length());
    }

    private String getValue(String s) {
        return null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chair);

        initView();
        //获取蓝牙的名字和地址
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void initView() {
        zongguan_bt = (Button) findViewById(R.id.zongguan_bt);
        init = (Button) findViewById(R.id.init);
        dianji = (Switch) findViewById(R.id.dianji);
        duanlian = (Switch) findViewById(R.id.duanlian);
        rbt_1 = (RadioButton) findViewById(R.id.rbt_1);
        rbt_2 = (RadioButton) findViewById(R.id.rbt_2);
        rbt_3 = (RadioButton) findViewById(R.id.rbt_3);
        rbt_4 = (RadioButton) findViewById(R.id.rbt_4);
        rbt_5 = (RadioButton) findViewById(R.id.rbt_5);
        kongzhi = (LinearLayout) findViewById(R.id.kongzhi);
        kongzhi.setVisibility(View.GONE);

        zongguan_bt.setOnClickListener(this);
        init.setOnClickListener(this);
        dianji.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getSendBuf("550201AA");
                    onSendBtnClicked();
                } else {
                    getSendBuf("550200AA");
                    onSendBtnClicked();
                }
            }
        });
        duanlian.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (rbt_1.isChecked()){
                        getSendBuf("550401AA");
                        onSendBtnClicked();
                    }else if (rbt_2.isChecked()){
                        getSendBuf("550402AA");
                        onSendBtnClicked();
                    }else if (rbt_3.isChecked()){
                        getSendBuf("550403AA");
                        onSendBtnClicked();
                    }else if (rbt_4.isChecked()){
                        getSendBuf("550404AA");
                        onSendBtnClicked();
                    }else if (rbt_5.isChecked()){
                        getSendBuf("550405AA");
                        onSendBtnClicked();
                    }

                } else {
                    getSendBuf("550500AA");
                    onSendBtnClicked();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESSFUL);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_NO_DISCOVERED);
        return intentFilter;
    }

    //动态效果
    public void convertText(final TextView textView, final int convertTextId) {
        final Animation scaleIn = AnimationUtils.loadAnimation(this,
                R.anim.text_scale_in);
        Animation scaleOut = AnimationUtils.loadAnimation(this,
                R.anim.text_scale_out);
        scaleOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textView.setText(convertTextId);
                textView.startAnimation(scaleIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        textView.startAnimation(scaleOut);
    }

    private String getHexString(String command) {
        String s = command;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') ||
                    ('A' <= c && c <= 'F')) {
                sb.append(c);
            }
        }
        if ((sb.length() % 2) != 0) {
            sb.deleteCharAt(sb.length());
        }
        return sb.toString();
    }

    private void getSendBuf(String command) {
        // TODO Auto-generated method stub
        Log.d("yuhao", "getSendBuf---------------------");
        sendIndex = 0;
        sendBuf = stringToBytes(getHexString(command));
        Log.d("yuhao", "sendBuf=--------------"+sendBuf);
        sendDataLen = sendBuf.length;
    }


    private byte[] stringToBytes(String s) {
        byte[] buf = new byte[s.length() / 2];
        for (int i = 0; i < buf.length; i++) {
            try {
                buf[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return buf;
    }

    public String asciiToString(byte[] bytes) {
        char[] buf = new char[bytes.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (char) bytes[i];
            sb.append(buf[i]);
        }
        return sb.toString();
    }

    public String bytesToString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];

            sb.append(hexChars[i * 2]);
            sb.append(hexChars[i * 2 + 1]);
            sb.append(' ');
        }
        return sb.toString();
    }

    private void onSendBtnClicked() {
        if (sendDataLen>20) {
            sendBytes += 20;
            final byte[] buf = new byte[20];
            // System.arraycopy(buffer, 0, tmpBuf, 0, writeLength);
            for (int i=0;i<20;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            sendIndex+=20;
            mBluetoothLeService.writeData(buf);
            sendDataLen -= 20;
        }
        else {
            sendBytes += sendDataLen;
            final byte[] buf = new byte[sendDataLen];
            for (int i=0;i<sendDataLen;i++)
            {
                buf[i] = sendBuf[sendIndex+i];
            }
            mBluetoothLeService.writeData(buf);
            sendDataLen = 0;
            sendIndex = 0;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.zongguan_bt:
                getSendBuf("550100AA");
                onSendBtnClicked();
                break;
            case R.id.init:
                getSendBuf("550300AA");
                onSendBtnClicked();
                handler.sendEmptyMessage(INTI_STA);
            default:
                break;
        }
    }
}
