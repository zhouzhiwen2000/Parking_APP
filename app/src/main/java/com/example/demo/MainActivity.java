package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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

public class MainActivity extends Activity  implements AMapLocationListener {
    BluetoothAdapter mAdapter;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationClientOption mLocationOption = null;
    public MainActivity() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    private double now_lat = 0;
    private double now_lon = 0;
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

        if (mAdapter.isDiscovering())
        {
            mAdapter.cancelDiscovery();
        }
        // 开启发现蓝牙设备
        mAdapter.startDiscovery();
        // 注册用以接收到已搜索到的蓝牙设备的receiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);
        // 注册搜索完时的receiver
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);

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
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {//anonymous class
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            TextView  txt = findViewById(R.id.txt1);
            if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    //list.add(device);
                    txt.setText("fd1");
                }
                //mMyBluetoothAdapter.notifyDataSetChanged();
            }
            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            {
                txt.setText("搜索蓝牙设备");
            }
        }
    };

    private void Navigation() {
        float j, w;
        j=w=0;
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
}
