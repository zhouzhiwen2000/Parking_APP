package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class MainActivity extends Activity  implements AMapLocationListener {
    BluetoothAdapter mAdapter;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationClientOption mLocationOption = null;
    private final BluetoothLeScanner scanner;
    public MainActivity() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        scanner  = mAdapter.getBluetoothLeScanner();
    }
    private double now_lat = 0;
    private double now_lon = 0;

    private BluetoothGatt mGatt;
    private boolean opened =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!mAdapter.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
            mAdapter.enable();
//            return;
        }
        /*The following is to get Bluetooth scan permissions*/
        String[] permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        List<String> denyPermissions = new ArrayList<>();
        for (String value : permission) {
            if (ContextCompat.checkSelfPermission(this, value) != PackageManager.PERMISSION_GRANTED) {//判断权限是否已授权
                //没有权限 就添加
                denyPermissions.add(value);
            }
        }
        if (denyPermissions != null && !denyPermissions.isEmpty()) {
            //申请权限授权
            ActivityCompat.requestPermissions(this, denyPermissions.toArray(new String[denyPermissions.size()]), 100);
        } else {
            //已全部授权
            //do something
        }


        Handler mHandler = new Handler();
        scanner.startScan(mCallBack);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                scanner.stopScan(mCallBack);
//            }
//        }, 10000);


        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Transport);
        if(null != mLocationClient){
            mLocationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocation(true);
        mLocationOption.setOnceLocationLatest(true);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.startLocation();

        Button btn = findViewById(R.id.button1);
        btn.setOnClickListener(new View.OnClickListener() {
            //anonymous class
            @Override
            public void onClick(View v) {
                Navigation();
            }
        });
        HandlerThread thread = new HandlerThread("MyHandlerThread");
        thread.start();
        mHandler = new Handler(thread.getLooper());
        mHandler.post(new Runnable() {
            //anonymous class
                          @Override
                          public void run() {
                              while (!opened) {
                              }
                              Toast toast = Toast.makeText(MainActivity.this, "开锁成功", 3000);
                              toast.show();
                          }
                      }
        );


    }

    private void Navigation() {

        try {
            // 高德地图 先维度——后经度
            Intent intent = Intent
                    .getIntent("androidamap://route?sourceApplication=softname&slat="
                            + now_lat
                            + "&slon="
                            + now_lon
                            + "&sname="
                            + "当前位置"
                            + "&dlat="
                            + 32.078437
                            + "&dlon="
                            + 118.77066
                            + "&dname="
                            + "车库位置" + "&dev=0&m=0&t=0");
            startActivity(intent);
            Toast toast = Toast.makeText(this, "高德地图正在启动", 3000);
            toast.show();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                now_lat=amapLocation.getLatitude();//获取纬度
                now_lon=amapLocation.getLongitude();//获取经度
            }else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError","location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }
    ScanCallback mCallBack = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            BluetoothDevice dev_get=result.getDevice();
            TextView  txt = findViewById(R.id.txt1);
            String addr=dev_get.getAddress();
            txt.setText(addr);
            if(addr.equals("b4:e6:2d:ee:6c:df")||addr.equals("B4:E6:2D:EE:6C:DF"))//need to get it from the server
            {
                scanner.stopScan(this);
                dev_get.connectGatt(MainActivity.this,false,mGattCallback);
            }
        }
    };
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //anonymous class
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == GATT_SUCCESS &&newState == STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == GATT_SUCCESS) {
                mGatt = gatt;
                BluetoothGattService service = mGatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));//fixed value
                BluetoothGattCharacteristic characteristic=service.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));//fixed value
                characteristic.setValue("password");//need to get it from the server
                mGatt.writeCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == GATT_SUCCESS)
            {
                opened=true;
            }
        }
    };
}
