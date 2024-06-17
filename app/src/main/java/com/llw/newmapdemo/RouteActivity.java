package com.llw.newmapdemo;

import static com.llw.newmapdemo.utils.MapUtil.convertToLatLonPoint;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.llw.newmapdemo.databinding.ActivityRouteBinding;
import com.llw.newmapdemo.overlay.BusRouteOverlay;
import com.llw.newmapdemo.overlay.DrivingRouteOverlay;
import com.llw.newmapdemo.overlay.RideRouteOverlay;
import com.llw.newmapdemo.overlay.WalkRouteOverlay;
import com.llw.newmapdemo.utils.MapUtil;

import java.util.List;

public class RouteActivity extends AppCompatActivity implements
        AMapLocationListener, LocationSource, AMap.OnMapClickListener, RouteSearch.OnRouteSearchListener, GeocodeSearch.OnGeocodeSearchListener, View.OnKeyListener {


    private static final String TAG = "RouteActivity";
    private ActivityRouteBinding binding;
    //地图控制器
    private AMap aMap = null;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //位置更改监听
    private LocationSource.OnLocationChangedListener mListener;
    //定义一个UiSettings对象
    private UiSettings mUiSettings;
    //定位样式
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    //起点
    private LatLonPoint mStartPoint;
    //终点
    private LatLonPoint mEndPoint;
    //路线搜索对象
    private RouteSearch routeSearch;
    //出行方式数组
    private static final String[] travelModeArray = {"步行出行", "骑行出行", "驾车出行", "公交出行"};

    //出行方式值
    private static int TRAVEL_MODE = 0;

    //数组适配器
    private ArrayAdapter<String> arrayAdapter;
    //城市
    private String city;
    //地理编码搜索
    private GeocodeSearch geocodeSearch;
    //解析成功标识码
    private static final int PARSE_SUCCESS_CODE = 1000;
    //定位地址
    private String locationAddress;
    //起点地址转坐标标识   1
    private int tag = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //初始化定位
        initLocation();
        //初始化地图
        initMap(savedInstanceState);
        //启动定位
        mLocationClient.startLocation();
        //初始化路线
        initRoute();
        //初始化出行方式
        initTravelMode();
    }

    /**
     * 初始化路线
     */
    private void initRoute() {
        try {
            routeSearch = new RouteSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        routeSearch.setRouteSearchListener(this);
    }

    /**
     * 初始化出行方式
     */
    private void initTravelMode() {
        //将可选内容与ArrayAdapter连接起来
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, travelModeArray);
        //设置下拉列表的风格
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter 添加到spinner中
        binding.spinner.setAdapter(arrayAdapter);
        //添加事件Spinner事件监听
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TRAVEL_MODE = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //起点 键盘按键监听
        binding.etStartAddress.setOnKeyListener(this);
        //终点 键盘按键监听
        binding.etEndAddress.setOnKeyListener(this);
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mLocationClient != null) {
            mLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocationLatest(true);
            mLocationOption.setNeedAddress(true);
            mLocationOption.setHttpTimeOut(20000);
            mLocationOption.setLocationCacheEnable(false);
            mLocationClient.setLocationOption(mLocationOption);
        }
    }

    /**
     * 初始化地图
     *
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        binding.mapView.onCreate(savedInstanceState);
        //初始化地图控制器对象
        aMap = binding.mapView.getMap();
        //设置最小缩放等级为12 ，缩放级别范围为[3, 20]
        aMap.setMinZoomLevel(12);
        //开启室内地图
        aMap.showIndoorMap(true);
        //实例化UiSettings类对象
        mUiSettings = aMap.getUiSettings();
        //隐藏缩放按钮 默认显示
        mUiSettings.setZoomControlsEnabled(false);
        //显示比例尺 默认不显示
        mUiSettings.setScaleControlsEnabled(true);
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        //地图点击监听
        aMap.setOnMapClickListener(this);
        //构造 GeocodeSearch 对象
        try {
            geocodeSearch = new GeocodeSearch(this);
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }
        //设置监听
        geocodeSearch.setOnGeocodeSearchListener(this);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //获取纬度
                double latitude = aMapLocation.getLatitude();
                //获取经度
                double longitude = aMapLocation.getLongitude();
                //地址
                locationAddress = aMapLocation.getAddress();
                //设置当前所在地
                binding.etStartAddress.setText(locationAddress);
                //binding.etStartAddress.setEnabled(false);//禁用输入
                // 城市赋值
                city = aMapLocation.getCity();
                //设置起点
                mStartPoint = MapUtil.convertToLatLonPoint(new LatLng(latitude, longitude));
                //停止定位后，本地定位服务并不会被销毁
                mLocationClient.stopLocation();
                //显示地图定位结果
                if (mListener != null) {
                    // 显示系统图标
                    mListener.onLocationChanged(aMapLocation);
                }

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁定位客户端，同时销毁本地定位服务。
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        binding.mapView.onDestroy();
    }

    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始路线搜索
     */
    private void startRouteSearch() {
        //在地图上添加起点Marker
        aMap.addMarker(new MarkerOptions()
                .position(MapUtil.convertToLatLng(mStartPoint))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
        //在地图上添加终点Marker
        aMap.addMarker(new MarkerOptions()
                .position(MapUtil.convertToLatLng(mEndPoint))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));

        //搜索路线 构建路径的起终点
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);

        //出行方式判断
        switch (TRAVEL_MODE) {
            case 0://步行
                //构建步行路线搜索对象
                RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WalkDefault);
                // 异步路径规划步行模式查询
                routeSearch.calculateWalkRouteAsyn(query);
                break;
            case 1://骑行
                //构建骑行路线搜索对象
                RouteSearch.RideRouteQuery rideQuery = new RouteSearch.RideRouteQuery(fromAndTo, RouteSearch.WalkDefault);
                //骑行规划路径计算
                routeSearch.calculateRideRouteAsyn(rideQuery);
                break;
            case 2://驾车
                //构建驾车路线搜索对象  剩余三个参数分别是：途经点、避让区域、避让道路
                RouteSearch.DriveRouteQuery driveQuery = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.WalkDefault, null, null, "");
                //驾车规划路径计算
                routeSearch.calculateDriveRouteAsyn(driveQuery);
                break;
            case 3://公交
                //构建驾车路线搜索对象 第三个参数表示公交查询城市区号，第四个参数表示是否计算夜班车，0表示不计算,1表示计算
                RouteSearch.BusRouteQuery busQuery = new RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BusLeaseWalk, city,0);
                //公交规划路径计算
                routeSearch.calculateBusRouteAsyn(busQuery);
                break;
            default:
                break;
        }
    }


    /**
     * 点击地图
     */
    @Override
    public void onMapClick(LatLng latLng) {
        //终点
        mEndPoint = convertToLatLonPoint(latLng);
        //开始路线搜索
        startRouteSearch();
    }

    /**
     * 公交规划路径结果
     *
     * @param busRouteResult 结果
     * @param code           结果码
     */
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int code) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (code != AMapException.CODE_AMAP_SUCCESS) {
            showMsg("错误码；" + code);
            return;
        }
        if (busRouteResult == null || busRouteResult.getPaths() == null) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        if (busRouteResult.getPaths().isEmpty()) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        final BusPath busPath = busRouteResult.getPaths().get(0);
        if (busPath == null) {
            return;
        }
        // 绘制路线
        BusRouteOverlay busRouteOverlay = new BusRouteOverlay(
                this, aMap, busPath,
                busRouteResult.getStartPos(),
                busRouteResult.getTargetPos());
        busRouteOverlay.removeFromMap();
        busRouteOverlay.addToMap();
        busRouteOverlay.zoomToSpan();

        int dis = (int) busPath.getDistance();
        int dur = (int) busPath.getDuration();
        String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
        Log.d(TAG, des);
        //显示公交花费时间
        binding.tvTime.setText(des);
        binding.layBottom.setVisibility(View.VISIBLE);
        //跳转到路线详情页面
        binding.tvDetail.setOnClickListener(v -> {
            Intent intent = new Intent(RouteActivity.this,
                    RouteDetailActivity.class);
            intent.putExtra("type",3);
            intent.putExtra("path", busPath);
            startActivity(intent);
        });
    }

    /**
     * 驾车规划路径结果
     *
     * @param driveRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int code) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (code != AMapException.CODE_AMAP_SUCCESS) {
            showMsg("错误码；" + code);
            return;
        }
        if (driveRouteResult == null || driveRouteResult.getPaths() == null) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        if (driveRouteResult.getPaths().isEmpty()) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        final DrivePath drivePath = driveRouteResult.getPaths().get(0);
        if (drivePath == null) {
            return;
        }
        // 绘制路线
        DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                this, aMap, drivePath,
                driveRouteResult.getStartPos(),
                driveRouteResult.getTargetPos(), null);
        drivingRouteOverlay.removeFromMap();
        drivingRouteOverlay.addToMap();
        drivingRouteOverlay.zoomToSpan();

        int dis = (int) drivePath.getDistance();
        int dur = (int) drivePath.getDuration();
        String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
        Log.d(TAG, des);
        //显示驾车花费时间
        binding.tvTime.setText(des);
        binding.layBottom.setVisibility(View.VISIBLE);
        //跳转到路线详情页面
        binding.tvDetail.setOnClickListener(v -> {
            Intent intent = new Intent(RouteActivity.this,
                    RouteDetailActivity.class);
            intent.putExtra("type",2);
            intent.putExtra("path", drivePath);
            startActivity(intent);
        });
    }

    /**
     * 步行规划路径结果
     *
     * @param walkRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int code) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (code != AMapException.CODE_AMAP_SUCCESS) {
            showMsg("错误码；" + code);
            return;
        }
        if (walkRouteResult == null || walkRouteResult.getPaths() == null) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        if (walkRouteResult.getPaths().isEmpty()) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        final WalkPath walkPath = walkRouteResult.getPaths().get(0);
        if (walkPath == null) {
            return;
        }
        //绘制路线
        WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
                this, aMap, walkPath,
                walkRouteResult.getStartPos(),
                walkRouteResult.getTargetPos());
        walkRouteOverlay.removeFromMap();
        walkRouteOverlay.addToMap();
        walkRouteOverlay.zoomToSpan();

        int dis = (int) walkPath.getDistance();
        int dur = (int) walkPath.getDuration();
        String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
        Log.d(TAG, des);
        //显示步行花费时间
        binding.tvTime.setText(des);
        binding.layBottom.setVisibility(View.VISIBLE);
        //跳转到路线详情页面
        binding.tvDetail.setOnClickListener(v -> {
            Intent intent = new Intent(RouteActivity.this,
                    RouteDetailActivity.class);
            intent.putExtra("type",0);
            intent.putExtra("path", walkPath);
            startActivity(intent);
        });
    }


    /**
     * 骑行规划路径结果
     *
     * @param rideRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onRideRouteSearched(final RideRouteResult rideRouteResult, int code) {
        aMap.clear();// 清理地图上的所有覆盖物
        if (code != AMapException.CODE_AMAP_SUCCESS) {
            showMsg("错误码；" + code);
            return;
        }
        if (rideRouteResult == null || rideRouteResult.getPaths() == null) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        if (rideRouteResult.getPaths().isEmpty()) {
            showMsg("对不起，没有搜索到相关数据！");
            return;
        }
        final RidePath ridePath = rideRouteResult.getPaths()
                .get(0);
        if (ridePath == null) {
            return;
        }
        RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(
                this, aMap, ridePath,
                rideRouteResult.getStartPos(),
                rideRouteResult.getTargetPos());
        rideRouteOverlay.removeFromMap();
        rideRouteOverlay.addToMap();
        rideRouteOverlay.zoomToSpan();

        int dis = (int) ridePath.getDistance();
        int dur = (int) ridePath.getDuration();
        String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
        Log.d(TAG, des);
        binding.tvTime.setText(des);
        binding.layBottom.setVisibility(View.VISIBLE);
        binding.tvDetail.setOnClickListener(v -> {
            Intent intent = new Intent(RouteActivity.this,
                    RouteDetailActivity.class);
            intent.putExtra("type",1);
            intent.putExtra("path", ridePath);
            startActivity(intent);
        });
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }


    /**
     * 地址转坐标
     *
     * @param geocodeResult
     * @param rCode
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        if (rCode != PARSE_SUCCESS_CODE) {
            showMsg("获取坐标失败，错误码：" + rCode);
            return;
        }
        List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
        if (geocodeAddressList != null && !geocodeAddressList.isEmpty()) {
            //判断是不是起点的搜索
            if (tag == 1) {
                //起点
                mStartPoint = geocodeAddressList.get(0).getLatLonPoint();
            } else {
                //终点
                mEndPoint = geocodeAddressList.get(0).getLatLonPoint();
            }
            if (mStartPoint != null && mEndPoint != null) {
                //开始路线搜索
                startRouteSearch();
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
            //获取输入框的值 出发地（起点）
            String startAddress = binding.etStartAddress.getText().toString().trim();
            //获取输入框的值 目的地（终点）
            String endAddress = binding.etEndAddress.getText().toString().trim();

            //判断出发地是否有值  不管这个值是定位还是手动输入
            if (startAddress.isEmpty()) {
                showMsg("请输入当前的出发地");
                return false;
            }
            //判断目的地是否有值
            if (endAddress.isEmpty()) {
                showMsg("请输入要前往的目的地");
                return false;
            }

            //当出发地输入框有值的时候，判断这个值是否是定位的地址，是则说明你没有更改过，则不需要进行地址转坐标，不是则需要转换。
            if (!locationAddress.equals(startAddress)) {
                tag = 1;
                GeocodeQuery startQuery = new GeocodeQuery(startAddress, city);
                geocodeSearch.getFromLocationNameAsyn(startQuery);
            } else {
                tag = -1;
            }

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            //隐藏软键盘
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

            //通过输入的目的地转为经纬度，然后进行地图上添加标点，最后计算出行路线规划

            // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
            GeocodeQuery endQuery = new GeocodeQuery(endAddress, city);
            geocodeSearch.getFromLocationNameAsyn(endQuery);
            return true;
        }
        return false;
    }
}