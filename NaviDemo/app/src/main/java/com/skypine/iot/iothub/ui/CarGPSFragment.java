package com.skypine.iot.iothub.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.skypine.iot.iothub.R;
import com.skypine.iot.iothub.utils.AMapUtil;
import com.skypine.iot.iothub.utils.ToastUtil;
import com.skypine.iot.iothub.view.DrivingRouteOverlay;
import com.skypine.iot.iothub.view.HintDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by syhuang on 2017/8/28.
 */

public class CarGPSFragment extends Fragment implements AMap.OnMyLocationChangeListener, RouteSearch.OnRouteSearchListener, View.OnClickListener {
    private MapView mMapView;
    private AMap mAMap;
    private Polyline mPolyline;

    private EditText startEdit;
    private EditText endEdit;
    /**************************定位***************************/

    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private MyLocationStyle myLocationStyle;//设置定位显示方式

    /***************************************************/
    private final int ROUTE_TYPE_DRIVE = 2;
    LatLng startLatLng, entLatlng;//起始点
    private ProgressDialog progDialog = null;// 搜索时进度条
    private RouteSearch mRouteSearch;//查询路线
    private DriveRouteResult mDriveRouteResult;//结果

    private Button query, start, stop, accelerate, unaccelerate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mContentView = inflater.inflate(R.layout.fragment_cargps, container, false);//setContentView(inflater, container);
        return mContentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMapView = (MapView) getView().findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        startEdit = (EditText) getView().findViewById(R.id.startEdit);
        endEdit = (EditText) getView().findViewById(R.id.endEdit);
        query = (Button) getView().findViewById(R.id.query);
        start = (Button) getView().findViewById(R.id.start);
        stop = (Button) getView().findViewById(R.id.stop);
        accelerate = (Button) getView().findViewById(R.id.accelerate);
        unaccelerate = (Button) getView().findViewById(R.id.unaccelerate);
        query.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        accelerate.setOnClickListener(this);
        unaccelerate.setOnClickListener(this);
//        checkStoragePermission();

        init();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.query:
                queryAddress();
                break;
            case R.id.start:
                startMove();
                break;
            case R.id.stop:
                stopMove();
                break;
            case R.id.accelerate:
                accelerate();
                break;
            case R.id.unaccelerate:
                unaccelerate();
                break;
        }

    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();

        }

        //路线监听
        mRouteSearch = new RouteSearch(getActivity());
        mRouteSearch.setRouteSearchListener(this);
        //定位
        initLocation();
        //6.0权限获取
        if (Build.VERSION.SDK_INT >= 23) {
            checkLocationPermission();
        } else {
            startLocation();
        }
    }


    /**
     * 查询路线
     */
    public void queryAddress() {
        Log.e("查询", "query");
        String startPoint = startEdit.getText().toString().trim();
        String endPoint = endEdit.getText().toString().trim();
        queryAddress(startPoint, new QueryAddressListener() {
            @Override
            public void onSuccess(LatLng latlng) {
                startLatLng = latlng;
            }
        });
        queryAddress(endPoint, new QueryAddressListener() {
            @Override
            public void onSuccess(LatLng latlng) {
                entLatlng = latlng;

                if (startLatLng != null) {
                    mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            new LatLngBounds(startLatLng, entLatlng), 200));

                    setfromandtoMarker();
                    searchRouteResult(ROUTE_TYPE_DRIVE, RouteSearch.DrivingDefault);
                }
            }
        });

    }

    /**
     * 添加起始坐标点
     */
    private void setfromandtoMarker() {
        mAMap.addMarker(new MarkerOptions()
                .position(startLatLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
        mAMap.addMarker(new MarkerOptions()
                .position(entLatlng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));
    }

    /***********************************路线*********************************/
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    List<LatLng> points;//轨迹
    long time;//时间
    float distance;//距离
    float speed;//速度

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        dissmissProgressDialog();
        mAMap.clear();// 清理地图上的所有覆盖物
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mDriveRouteResult = result;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    drivePath.getSteps();
                    for (DriveStep path : drivePath.getSteps()) {
                        Log.e("路线", path.getPolyline().toString());
                    }
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            getContext(), mAMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(false);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                    //时间
                    time = drivePath.getDuration();//s
                    //距离
                    distance = drivePath.getDistance();//m
                    speed = distance / time;//m/s
                    //路径
                    points = readLine(drivePath.getSteps());

                } else if (result != null && result.getPaths() == null) {
                    ToastUtil.show(getContext(), getString(R.string.nodata));
                }

            } else {
                ToastUtil.show(getContext(), getString(R.string.nodata));
            }
        } else {
            ToastUtil.showerror(getContext(), errorCode);
        }


    }

    /**
     * 小车行车轨迹
     *
     * @param steps
     * @return
     */
    private List<LatLng> readLine(List<DriveStep> steps) {
        List<LatLng> points = new ArrayList<>();
        points.add(startLatLng);
        for (DriveStep step : steps) {
            List<LatLonPoint> latlonPoints = step.getPolyline();
            for (LatLonPoint point : latlonPoints) {
                points.add(AMapUtil.convertToLatLng(point));
            }
        }
        points.add(entLatlng);
        return points;
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }


/***********************************路线*********************************/
    /**
     * 查询地址成功
     */

    interface QueryAddressListener {
        public void onSuccess(LatLng latlng);
    }

    private void queryAddress(String name, final QueryAddressListener listener) {
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

                    Log.e("susss", result.getGeocodeAddressList().toString());
                    if (result != null && result.getGeocodeAddressList() != null
                            && result.getGeocodeAddressList().size() > 0) {
                        GeocodeAddress address = result.getGeocodeAddressList().get(0);
                        address.getAdcode();

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
        GeocodeQuery query = new GeocodeQuery(name, null);
        // 第一个参数表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode，
        geocodeSearch.getFromLocationNameAsyn(query);
        // 设置同步地理编码请求
    }

    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int routeType, int mode) {
        if (startLatLng == null) {
            ToastUtil.show(getContext(), getString(R.string.start_gps));
            return;
        }
        if (entLatlng == null) {
            ToastUtil.show(getContext(), getString(R.string.end_gps));
        }
        showProgressDialog();
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                AMapUtil.convertToLatLonPoint(startLatLng), AMapUtil.convertToLatLonPoint(entLatlng));
        if (routeType == ROUTE_TYPE_DRIVE) {// 驾车路径规划
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, mode, null,
                    null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
            mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
        }
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(getContext());
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }


    /**
     * 方法必须重写
     */
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();

    }

    /**
     * 方法必须重写
     */
    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();

    }

    /**
     * 方法必须重写
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();

    }


    private SmoothMoveMarker smoothMarker;
    private Marker marker;
    private boolean isMove = true;//是否设置了小车
    private double currentDis;//当前距离

    /**
     * 停止小车移动
     */
    public void stopMove() {
        if (smoothMarker != null) {
            smoothMarker.stopMove();
            isMove = false;

        }
    }

    float currentSpeed = speed;//当前速度

    /**
     * 加速
     */
    public void accelerate() {
        if (smoothMarker != null) {

            currentSpeed += (18 * 1000 / 3600);//18km/h
            int currentTime = (int) (distance / currentSpeed);
            smoothMarker.setTotalDuration(currentTime);

        }
    }

    /**
     * 减速
     */
    public void unaccelerate() {
        if (smoothMarker != null) {

            currentSpeed -= (18 * 1000 / 3600);//18km/h
            int currentTime = (int) (distance / currentSpeed);
            smoothMarker.setTotalDuration(currentTime);//总时间
        }
    }

    /**
     * 开始移动
     */
    public void startMove() {

        if (isMove) {
            if (this.points == null) {
                ToastUtil.show(getContext(), "请先设置路线");
                return;
            }

            // 读取轨迹点
            List<LatLng> points = this.points;
            // 构建 轨迹的显示区域
            LatLngBounds bounds = new LatLngBounds(points.get(0), points.get(points.size() - 2));
            mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

            // 实例 SmoothMoveMarker 对象
            smoothMarker = new SmoothMoveMarker(mAMap);
            if (marker != null) {
                marker.remove();
            }
            marker = smoothMarker.getMarker();

            // 设置 平滑移动的 图标
            smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.mipmap.icon_car));

            // 取轨迹点的第一个点 作为 平滑移动的启动
            LatLng drivePoint = points.get(0);
            Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
            points.set(pair.first, drivePoint);
            List<LatLng> subList = points.subList(pair.first, points.size());

            // 设置轨迹点
            smoothMarker.setPoints(subList);
            // 设置平滑移动的总时间  单位  秒
            smoothMarker.setTotalDuration((int) time);

            // 设置  自定义的InfoWindow 适配器
//            mAMap.setInfoWindowAdapter(infoWindowAdapter);
            // 显示 infowindow
            smoothMarker.getMarker().showInfoWindow();

            // 设置移动的监听事件  返回 距终点的距离  单位 米
            smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
                @Override
                public void move(final double distance) {

                    changeCenterMap(smoothMarker.getPosition());
                    currentDis = distance;


                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            smoothMarker.getMarker().setSnippet("距离终点还有： " + (int) distance + "米\nGPS：" + smoothMarker.getPosition()
                                    + "\n当前车速：" + currentSpeed / 3.6 + "km/h");
//                            if (infoWindowLayout != null && title != null) {
//
//
////                                1m/s=3.6×km/h
//                                title.setText("距离终点还有： " + (int) distance + "米\nGPS：" + smoothMarker.getPosition()
//                                        + "\n当前车速：" + currentSpeed / 3.6 + "km/h");
//                            }
                        }
                    });

                }
            });

            // 开始移动
            smoothMarker.startSmoothMove();
            isMove = false;
        } else {
            smoothMarker.startSmoothMove();
        }

    }

    /**
     * 个性化定制的信息窗口视图的类
     * 如果要定制化渲染这个信息窗口，需要重载getInfoWindow(Marker)方法。
     * 如果只是需要替换信息窗口的内容，则需要重载getInfoContents(Marker)方法。
     */
    AMap.InfoWindowAdapter infoWindowAdapter = new AMap.InfoWindowAdapter() {

        // 个性化Marker的InfoWindow 视图
        // 如果这个方法返回null，则将会使用默认的信息窗口风格，内容将会调用getInfoContents(Marker)方法获取
        @Override
        public View getInfoWindow(Marker marker) {

            return getInfoWindowView(marker);
        }

        // 这个方法只有在getInfoWindow(Marker)返回null 时才会被调用
        // 定制化的view 做这个信息窗口的内容，如果返回null 将以默认内容渲染
        @Override
        public View getInfoContents(Marker marker) {

            return getInfoWindowView(marker);
        }
    };

    LinearLayout infoWindowLayout;
    TextView title;
    TextView snippet;

    /**
     * 自定义View并且绑定数据方法
     *
     * @param marker 点击的Marker对象
     * @return 返回自定义窗口的视图
     */
    private View getInfoWindowView(Marker marker) {
        if (infoWindowLayout == null) {
            infoWindowLayout = new LinearLayout(getContext());
            infoWindowLayout.setOrientation(LinearLayout.VERTICAL);
            title = new TextView(getContext());
            snippet = new TextView(getContext());
            title.setTextColor(Color.BLACK);
            snippet.setTextColor(Color.BLACK);
            infoWindowLayout.setBackgroundResource(R.drawable.infowindow_bg);

            infoWindowLayout.addView(title);
            infoWindowLayout.addView(snippet);
        }

        return infoWindowLayout;
    }


    private void checkLocationPermission() {
        // 检查是否有定位权限
        // 检查权限的方法: ContextCompat.checkSelfPermission()两个参数分别是Context和权限名.
        // 返回PERMISSION_GRANTED是有权限，PERMISSION_DENIED没有权限
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //没有权限，向系统申请该权限。
            Log.i("MY", "没有权限");
            requestPermission(LOCATION_PERMISSION_CODE);
        } else {
            //已经获得权限，则执行定位请求。
//            showMessage(getString(R.string.location_yes));
            startLocation();

        }
    }

//    private void checkStoragePermission() {
//        // 检查是否有存储的读写权限
//        // 检查权限的方法: ContextCompat.checkSelfPermission()两个参数分别是Context和权限名.
//        // 返回PERMISSION_GRANTED是有权限，PERMISSION_DENIED没有权限
//        if (ContextCompat.checkSelfPermission(getContext(),
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            //没有权限，向系统申请该权限。
////            Log.i("MY", "没有权限");
//            requestPermission(STORAGE_PERMISSION_CODE);
//        } else {
//            //同组的权限，只要有一个已经授权，则系统会自动授权同一组的所有权限，比如WRITE_EXTERNAL_STORAGE和READ_EXTERNAL_STORAGE
////            Toast.makeText(getMContext(), "已获取存储的读写权限", Toast.LENGTH_SHORT).show();
//        }
//    }

    public static boolean IsEmptyOrNullString(String s) {
        return (s == null) || (s.trim().length() == 0);
    }

    private String getPermissionString(int requestCode) {
        String permission = "";
        switch (requestCode) {
            //ACCESS_COARSE_LOCATION
            case LOCATION_PERMISSION_CODE:
                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                break;
            case STORAGE_PERMISSION_CODE:
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                break;
        }
        return permission;
    }

    /**
     * 授权
     *
     * @param permissioncode
     */

    private void requestPermission(int permissioncode) {
        String permission = getPermissionString(permissioncode);
        if (!IsEmptyOrNullString(permission)) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    permission)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                if (permissioncode == LOCATION_PERMISSION_CODE) {
                    Log.e("MY", "------------1");
                    HintDialogFragment newFragment = HintDialogFragment.newInstance(R.string.location_description_title,
                            R.string.location_description_why_we_need_the_permission,
                            permissioncode);
                    newFragment.show(getActivity().getFragmentManager(), HintDialogFragment.class.getSimpleName());
                } else if (permissioncode == STORAGE_PERMISSION_CODE) {
                    Log.e("MY", "------------2");
                    HintDialogFragment newFragment = HintDialogFragment.newInstance(R.string.storage_description_title,
                            R.string.storage_description_why_we_need_the_permission,
                            permissioncode);
                    newFragment.show(getActivity().getFragmentManager(), HintDialogFragment.class.getSimpleName());
                }


            } else {
                Log.e("MY", "返回false 不需要解释为啥要权限，可能是第一次请求，也可能是勾选了不再询问");
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{permission}, permissioncode);
            }
        }
    }

    /**
     * 定位
     */
    private void initLocation() {
        // 如果要设置定位的默认状态，可以在此处进行设置
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类


        /****************************************************************/
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked));
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0)); // 设置圆形的填充颜色
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0)); // 自定义精度范围的圆形边框颜色


        /*********************基本地图********************/
        mAMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        mAMap.getUiSettings().setCompassEnabled(true);//显示指南针
        mAMap.getUiSettings().setScaleControlsEnabled(true);//显示比例尺
        mAMap.getUiSettings().setZoomControlsEnabled(false);//设置缩放按钮是否可见。
        mAMap.setTrafficEnabled(true);//设置是否打开交通图层。
        mAMap.showIndoorMap(true);//  设置是否显示室内地图，默认不显示。


    }

    /**
     * 开始定位
     */
    private void startLocation() {
        /****************************************************************/
        mAMap.setMyLocationStyle(myLocationStyle);

        // 只定位，不进行其他操作，默认打开到定位的地方
        mAMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
        //设置SDK 自带定位消息监听
        mAMap.setOnMyLocationChangeListener(this);
        // 启动定位
//        mlocationClient.startLocation();
        mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

//        Log.i("MY", "startLocation");
    }

    @Override
    public void onMyLocationChange(Location location) {
// 定位回调监听
        if (location != null) {
            changeCenterMap(new LatLng(location.getLatitude(), location.getLongitude()));//改变中心点
//            Log.e("amap", "onMyLocationChange 定位成功， lat: " + location.getLatitude() + " lon: " + location.getLongitude());
            //存储定位信息
//            AppConfig.getInstance().putString(Contents.KEY_LAT, location.getLatitude() + "");
//            AppConfig.getInstance().putString(Contents.KEY_LON, location.getLongitude() + "");
            Bundle bundle = location.getExtras();
            if (bundle != null) {
                int errorCode = bundle.getInt(MyLocationStyle.ERROR_CODE);
                String errorInfo = bundle.getString(MyLocationStyle.ERROR_INFO);
                // 定位类型，可能为GPS WIFI等，具体可以参考官网的定位SDK介绍
                int locationType = bundle.getInt(MyLocationStyle.LOCATION_TYPE);

                /*
                errorCode
                errorInfo
                locationType
                */
                Log.e("amap", "定位信息， code: " + errorCode + " errorInfo: " + errorInfo + " locationType: " + locationType);
            } else {
                Log.e("amap", "定位信息， bundle is null ");

            }

        } else {
            Log.e("amap", "定位失败");
        }
    }

    /**
     * 改变中心点
     */


    private void changeCenterMap(LatLng drivePoint) {
        //获取当前地图状态
        CameraPosition cameraPosition = mAMap.getCameraPosition();
        // 目标位置的屏幕中心点经纬度坐标。
        // 目标可视区域的缩放级别
        // 目标可视区域的倾斜度，以角度为单位
        // 可视区域指向的方向，以角度为单位，从正北向逆时针方向计算，从0 度到360 度。
        mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                drivePoint, cameraPosition.zoom, cameraPosition.tilt, cameraPosition.bearing)));

    }
}
