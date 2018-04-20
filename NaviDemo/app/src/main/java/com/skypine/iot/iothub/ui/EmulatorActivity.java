package com.skypine.iot.iothub.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;
import com.skypine.iot.iothub.R;
import com.skypine.iot.iothub.utils.AmapTTSController;
import com.skypine.iot.iothub.utils.CheckPermissionsActivity;

/**
 *
 */
public class EmulatorActivity extends CheckPermissionsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);           //设置标题栏样式
        setContentView(R.layout.activity_emulator);

    }


}
