package com.skypine.iot.iothub.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.INaviInfoCallback;
import com.amap.api.navi.model.AMapNaviLocation;
import com.skypine.iot.iothub.R;
import com.skypine.iot.iothub.utils.AmapTTSController;
import com.skypine.iot.iothub.utils.CheckPermissionsActivity;
import com.skypine.iot.iothub.utils.SpeechUtils;

public class AmapActivity extends CheckPermissionsActivity implements INaviInfoCallback {
    AmapTTSController amapTTSController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap);
        finish();
//         SpeechUtils.getInstance(this).speakText();系统自带的语音播报
        amapTTSController = AmapTTSController.getInstance(getApplicationContext());
        amapTTSController.init();


        Poi end = new Poi("北京站", new LatLng(39.904556, 116.427231), "B000A83M61");
        AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), new AmapNaviParams(null), this);
    }

    @Override
    public void onGetNavigationText(String s) {
        amapTTSController.onGetNavigationText(s);
    }

    @Override
    public void onStopSpeaking() {
        amapTTSController.stopSpeaking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        amapTTSController.destroy();
    }

    @Override
    public void onInitNaviFailure() {

    }

    /**
     * //    当GPS位置有更新时的回调函数。
     * getAccuracy()
     * 返回定位精度。
     * java.lang.Double	getAltitude()
     * 返回海拔高度。
     * float	getBearing()
     * 返回定位方位（方向）。
     * NaviLatLng	getCoord()
     * 返回当前位置的经纬度坐标。
     * int	getMatchStatus()
     * 返回位置匹配状态。
     * float	getSpeed()
     * 返回当前定位点的速度， 如果此位置不具有速度，则返回0.0。
     * java.lang.Long	getTime()
     * 返回定位时间。
     * boolean	isMatchNaviPath()
     * 返回当前位置是否匹配到规划的道路上。
     *
     * @param aMapNaviLocation
     */


    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {
        //位置
        Log.e("location", aMapNaviLocation.getCoord().toString());
        //车速
        Log.e("speed", aMapNaviLocation.getSpeed() + "");
        //方向
        Log.e("bearing", aMapNaviLocation.getBearing() + "");

    }

    @Override
    public void onArriveDestination(boolean b) {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }


}
