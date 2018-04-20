package com.skypine.iot.iothub.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviException;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.skypine.iot.iothub.R;
import com.skypine.iot.iothub.utils.AMapUtil;

/**
 * Created by syhuang on 2017/8/28.
 */

public class EmulatorFragment extends BaseFragment implements View.OnClickListener {
    private EditText startEdit;
    private EditText endEdit;
    private Button   query, accelerate, unaccelerate;
    ImageView imageView;
    private EditText startCityEdit;
    private EditText endCityEdit;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mContentView = inflater.inflate(R.layout.fragment_emulator, container, false);//setContentView(inflater, container);
        return mContentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        mAMapNaviView = (AMapNaviView) getView().findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        imageView = new ImageView(getContext());//绘制实景图
        setUi();

        startEdit = (EditText) getView().findViewById(R.id.startEdit);
        endEdit = (EditText) getView().findViewById(R.id.endEdit);
        query = (Button) getView().findViewById(R.id.query);
        query.setOnClickListener(this);
        accelerate = (Button) getView().findViewById(R.id.accelerate);
        unaccelerate = (Button) getView().findViewById(R.id.unaccelerate);
        query.setOnClickListener(this);
        accelerate.setOnClickListener(this);
        unaccelerate.setOnClickListener(this);

        startCityEdit = (EditText) getView().findViewById(R.id.startCityEdit);
        endCityEdit = (EditText) getView().findViewById(R.id.endCityEdit);
    }

    private void setUi() {
        AMapNaviViewOptions options = new AMapNaviViewOptions();
        //自动绘制路线
        options.setAutoDrawRoute(false);
        // 设置是否自动改变缩放等级
        options.setAutoChangeZoom(true);
        //设置导航界面UI是否显示。
        options.setLayoutVisible(true);
        //是否显示路口放大图(实景图)
        options.isRealCrossDisplayShow();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_car);
        //设置车图标
        options.setCarBitmap(bitmap);

        // 设置路口放大图的显示位置。
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mAMapNaviView.getWidth() / 4, getContext().getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mAMapNaviView.getHeight() / 2, getContext().getResources().getDisplayMetrics());

        Rect landscape = new Rect(0, 0, width, height);
        options.setCrossLocation(null, landscape);//横屏（竖屏放在左边）
        // 显示路口放大图(路口模型图)
        options.setCrossDisplayShow(true);
        //  设置是否显示路口放大图(实景图)
        options.setRealCrossDisplayShow(false);

        mAMapNaviView.setViewOptions(options);

    }

    @Override
    public void hideCross() {
        super.hideCross();
        mAMapNaviView.removeView(imageView);
    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {
        super.showCross(aMapNaviCross);
        //转弯回调,实景图
        Bitmap crossBitmap = aMapNaviCross.getBitmap();
        FrameLayout.LayoutParams params = new
                FrameLayout.LayoutParams(mAMapNaviView.getWidth() / 2, FrameLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);
        imageView.setImageBitmap(crossBitmap);
        mAMapNaviView.addView(imageView);
    }

    @Override
    public void onNaviCancel() {
        super.onNaviCancel();
        if (routeOverlay != null) {
            routeOverlay.removeFromMap();
        }
    }


    @Override
    public void onInitNaviSuccess() {
        super.onInitNaviSuccess();
        /**
         * 方法: int strategy=mAMapNavi.strategyConvert(congestion, avoidhightspeed, cost, hightspeed, multipleroute); 参数:
         *
         * @congestion 躲避拥堵
         * @avoidhightspeed 不走高速
         * @cost 避免收费
         * @hightspeed 高速优先
         * @multipleroute 多路径
         *
         *  说明: 以上参数都是boolean类型，其中multipleroute参数表示是否多条路线，如果为true则此策略会算出多条路线。
         *  注意: 不走高速与高速优先不能同时为true 高速优先与避免收费不能同时为true
         */

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.query:
                onSearched();
                break;
            case R.id.accelerate:
                onAccelerate();
                break;
            case R.id.unaccelerate:
                onUnaccelerate();
                break;
        }

    }

    int defaultSpeed = 75;

    /**
     * 加速
     */
    private void onAccelerate() {
        //设置模拟导航的行车速度
        if (defaultSpeed < 100) {
            defaultSpeed += 10;
            mAMapNavi.setEmulatorNaviSpeed(defaultSpeed);
        }
    }

    /**
     * 减速
     */
    private void onUnaccelerate() {
        //设置模拟导航的行车速度
        if (defaultSpeed > 50) {
            defaultSpeed -= 10;
            mAMapNavi.setEmulatorNaviSpeed(defaultSpeed);
        }
    }

    /**
     * 查询路线,地址编码
     */
    public void onSearched() {
        String startPoint = startEdit.getText().toString().trim();
        String endPoint = endEdit.getText().toString().trim();
        //起始城市
        String startCity = startCityEdit.getText().toString().trim();
        String endCity = endCityEdit.getText().toString().trim();
        queryAddress(startPoint, startCity, new QueryAddressListener() {

            @Override
            public void onSuccess(LatLng latlng) {
                mStartLatlng = new NaviLatLng(latlng.latitude, latlng.longitude);
            }
        });
        queryAddress(endPoint, endCity, new QueryAddressListener() {
            @Override
            public void onSuccess(LatLng latlng) {
                mEndLatlng = new NaviLatLng(latlng.latitude, latlng.longitude);
                if (mStartLatlng == null) {
                    return;
                }
                if (mEndLatlng == null) {
                    return;
                }
                setStartAndEnd();//设置初始化点
                int strategy = 0;
                try {
                    //再次强调，最后一个参数为true时代表多路径，否则代表单路径
                    strategy = mAMapNavi.strategyConvert(true, false, false, false, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mAMapNavi.calculateDriveRoute(sList, eList, mWayPointList, strategy);
            }
        });

    }

    /**
     * 查询地址成功
     */

    interface QueryAddressListener {
        public void onSuccess(LatLng latlng);
    }

    private void queryAddress(String name, String city, final QueryAddressListener listener) {
        GeocodeSearch geocodeSearch = new GeocodeSearch(getContext());
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

            }

            /**
             * 地理编码查询回调
             */
            @Override
            public void onGeocodeSearched(GeocodeResult result, int rCode) {
                if (rCode == AMapException.CODE_AMAP_SUCCESS) {

                    if (result != null && result.getGeocodeAddressList() != null
                            && result.getGeocodeAddressList().size() > 0) {
                        GeocodeAddress address = result.getGeocodeAddressList().get(0);

                        LatLng latlng = AMapUtil.convertToLatLng(address.getLatLonPoint());
                        listener.onSuccess(latlng);

                    } else {
                        Log.e("error", rCode + getString(R.string.nodata));
                    }
                } else {
                    Log.e("error", rCode + "失败");
                }
            }
        });
        GeocodeQuery query = new GeocodeQuery(name, city);
        // 第一个参数表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode，
        geocodeSearch.getFromLocationNameAsyn(query);
        // 设置同步地理编码请求
    }

    RouteOverLay routeOverlay;

    @Override
    public void onCalculateRouteSuccess(int[] ids) {
        super.onCalculateRouteSuccess(ids);
        if (routeOverlay != null) {
            routeOverlay.removeFromMap();
        }

        //        如果根据获取的导航路线来自定义绘制
        routeOverlay = new RouteOverLay(mAMapNaviView.getMap(), mAMapNavi.getNaviPath(), getActivity());
        routeOverlay.setStartPointBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.start));
        routeOverlay.setEndPointBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.end));
        routeOverlay.setWayPointBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.way));
        routeOverlay.setTrafficLine(true);

        //        // 设置路线的配置信息，如:路线的交通状态纹理图片。
        //        RouteOverlayOptions routeOverlayOptions = new RouteOverlayOptions();
        //        //设置导航线路的宽度
        //        routeOverlayOptions.setLineWidth(50);
        //        //设置交通状况情况良好下的纹理位图
        //        routeOverlayOptions.setSmoothTraffic(BitmapFactory.decodeResource(this.getResources(), R.drawable.custtexture));
        //        //设置交通状况拥堵下的纹理位图
        //        routeOverlayOptions.setJamTraffic(BitmapFactory.decodeResource(this.getResources(), R.drawable.custtexture));
        //        //设置路线的图标
        //        routeOverlayOptions.setNormalRoute(BitmapFactory.decodeResource(this.getResources(), R.drawable.custtexture));
        //        //        routeOverlay.setRouteOverlayOptions(routeOverlayOptions);

        try {
            routeOverlay.setWidth(30);
        } catch (AMapNaviException e) {
            //宽度须>0
            e.printStackTrace();
        }
        int color[] = new int[10];
        color[0] = Color.BLACK;
        color[1] = Color.RED;
        color[2] = Color.BLUE;
        color[3] = Color.YELLOW;
        color[4] = Color.GRAY;
        routeOverlay.addToMap(color, mAMapNavi.getNaviPath().getWayPointIndex());
        mAMapNavi.startNavi(AMapNavi.EmulatorNaviMode);

        mAMapNavi.startNavi(NaviType.EMULATOR);

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
     * @param location
     */
    @Override
    public void onLocationChange(AMapNaviLocation location) {
        super.onLocationChange(location);


        //位置
        Log.e("location", location.getCoord().toString());
        //车速
        Log.e("speed", location.getSpeed() + "");
        //方向
        Log.e("bearing", location.getBearing() + "");

    }


}
