package com.smartwebee.android.foot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

public class FootActivity extends BleSppActivity implements View.OnClickListener{
    private Button up,down;
    private Switch switch_bt;
    private EditText count;
    private TextView recount,debug;
    private RadioButton rb_sub,rb_obj,rb_qiangdu1,rb_qiangdu2,rb_qiangdu3;

    private static final int RECEIVE = 100;
    private String commond = "";
    private String receive = "";

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case RECEIVE:
                    if (receive.charAt(7) == '1'){
                        if (receive.charAt(5) == '1'){
                            Log.d("zzh", "handleMessage: "+receive);
                            rb_obj.setEnabled(false);
                            rb_sub.setEnabled(true);
                        } else if (receive.charAt(5) == '2') {
                            rb_obj.setEnabled(true);
                            rb_sub.setEnabled(false);
                        }
                        switch_bt.setChecked(true);
                    }else if (receive.charAt(7) == '2'){
                        rb_sub.setEnabled(true);
                        rb_obj.setEnabled(true);
                        switch_bt.setChecked(false);
                    }
                    int re_count = Integer.parseInt(receive.substring(8,10),16);
                    recount.setText(re_count+"");
                    break;
                default:
                    break;
            }
        }
    };

    protected final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
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
//                for(byte byteChar : data)
//                    stringBuilder.append(String.format("%02X ", byteChar));
//                Log.v("log",stringBuilder.toString());
                receive = getReceive(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
                debug.setText(bytesToString(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)));
                Log.d("zzh", "onReceive: " + receive);
                if (receive != null){
                    handler.sendEmptyMessage(RECEIVE);
                }
            }else if (BluetoothLeService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("zzh", "Connect request result=" + result);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foot);
        initView();
    }

    private void initView() {
        up = (Button) findViewById(R.id.up);
        up.setOnClickListener(this);
        down = (Button) findViewById(R.id.down);
        down.setOnClickListener(this);
        switch_bt = (Switch)findViewById(R.id.switch_bt);
        switch_bt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int count_nub;
                if (!TextUtils.isEmpty(count.getText())){
                    count_nub = Integer.parseInt(count.getText().toString());
                }else {
                    count_nub = 0;
                }
                if (isChecked){
                    if (count_nub < 16){
                        commond = "550" + Integer.toHexString(count_nub);
                    } else {
                        commond = "55" + Integer.toHexString(count_nub);
                    }
                    if (rb_sub.isChecked()){
                        commond = commond + "01";
                    }
                    if (rb_obj.isChecked()){
                        commond = commond + "02";
                    }
                    commond = commond + "0100AA";
                    getSendBuf(commond);
                    onSendBtnClicked();
                    commond = "";
                } else {
                    if (count_nub < 16){
                        commond = "550" + Integer.toHexString(count_nub);
                    } else {
                        commond = "55" + Integer.toHexString(count_nub);
                    }
                    if (rb_sub.isChecked()){
                        commond = commond + "01";
                    }
                    if (rb_obj.isChecked()){
                        commond = commond + "02";
                    }
                    commond = commond + "0200AA";
                    getSendBuf(commond);
                    onSendBtnClicked();
                    commond = "";
                }
            }
        });
        count = (EditText) findViewById(R.id.count);
        recount = (TextView) findViewById(R.id.recount);
        debug = (TextView)findViewById(R.id.debug);
        rb_obj = (RadioButton) findViewById(R.id.rb_obj);
        rb_sub = (RadioButton) findViewById(R.id.rb_sub);

        rb_qiangdu1 = (RadioButton) findViewById(R.id.qiangdu1);
        rb_qiangdu2 = (RadioButton) findViewById(R.id.qiangdu2);
        rb_qiangdu3 = (RadioButton) findViewById(R.id.qiangdu3);
    }

    private String getReceive(byte[] buf) {
        String receiver = bytesToString(buf);
        receiver = receiver.replaceAll(" ","");
        char a = receiver.charAt(0);
        char b = receiver.charAt(1);
        char c = receiver.charAt(receiver.length()-2);
        char d = receiver.charAt(receiver.length()-1);
        Log.d("zzh", "getReceive: " + receiver.length());
        if (receiver.length() == 12 && a == '5' && b == '5' && c == 'A' && d == 'A'){
            return receiver;
        } else {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        int count_nub;
        if (!TextUtils.isEmpty(count.getText())){
            count_nub = Integer.parseInt(count.getText().toString());
        } else {
            count_nub = 0;
        }
        switch (v.getId()){
            case R.id.down:
                if (count_nub > 0){
                    count_nub--;
                } else {
                    count_nub = 0;
                }
                count.setText(count_nub+"");
                break;
            case R.id.up:
                if (count_nub < 99){
                    count_nub++;
                }else {
                    count_nub = 99;
                }
                count.setText(count_nub+"");
                break;
        }
    }
}
