package com.skypine.iot.iothub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.skypine.iot.iothub.R;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void onCar(View v) {
        //车
        startActivityWithoutExtras(CarActivity.class);
    }

    public void onNav(View v) {
        //高德地图导航
        startActivityWithoutExtras(AmapActivity.class);
    }


    public void onClick(View v) {
        //模拟导航
        startActivityWithoutExtras(NaviActivity.class);
    }

    public void onEmulator(View v) {
        //模拟导航fragment
        startActivityWithoutExtras(EmulatorActivity.class);

    }

    public void onEmulator2(View v) {
        //模拟导航fragment
        startActivityWithoutExtras(Emulator2Activity.class);

    }


    protected void startActivityWithoutExtras(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);

    }
}
