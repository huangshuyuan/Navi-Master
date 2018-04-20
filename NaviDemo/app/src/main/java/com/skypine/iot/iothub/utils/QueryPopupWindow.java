package com.skypine.iot.iothub.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;

import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.skypine.iot.iothub.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class QueryPopupWindow extends PopupWindow {


    private Button btn_query, btn_cancel;
    private EditText address;
    private ListView mhintList;
    private View     mMenuView;
    Context mContext;

    public Button getBtn_cancel() {
        return btn_cancel;
    }

    public void setBtn_cancel(Button btn_cancel) {
        this.btn_cancel = btn_cancel;
    }

    public QueryPopupWindow(Activity context, OnClickListener itemsOnClick) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mMenuView = inflater.inflate(R.layout.query_dialog, null);
        btn_query = (Button) mMenuView.findViewById(R.id.btn_query);
        mhintList = (ListView) mMenuView.findViewById(R.id.hintList);
        btn_cancel = (Button) mMenuView.findViewById(R.id.btn_cancel);
        address = (EditText) mMenuView.findViewById(R.id.queryEdit);
        address.addTextChangedListener(mTextWatcher);
        //取消按钮
        btn_cancel.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                //销毁弹出框
                dismiss();
            }
        });
        //设置按钮监听
        btn_query.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                queryPoi(address.getText().toString().trim());
            }
        });
        //设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LayoutParams.FILL_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.mypopwindow_anim_style);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    }
                }
                return true;
            }
        });

    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 1) {
                queryPoi(address.getText().toString().trim());
            }

        }
    };

    private PoiResult poiResult; // poi返回的结果
    private int currentPage = 0;// 当前页面，从0开始计数
    private PoiSearch.Query queryS;// Poi查询条件类
    private PoiSearch       poiSearch;// POI搜索

    public interface QueryListener {

        public void onSuccess(NaviLatLng latlng, String title);
    }

    QueryListener queryAddressListener = null;

    public QueryListener getQueryAddressListener() {
        return queryAddressListener;
    }

    public void setQueryAddressListener(QueryListener queryAddressListener) {
        this.queryAddressListener = queryAddressListener;
    }

    /**
     * 搜索地址
     *
     * @param keyWord
     */
    private void queryPoi(String keyWord) {
        currentPage = 0;
        queryS = new PoiSearch.Query(keyWord, "", "");// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        queryS.setPageSize(20);// 设置每页最多返回多少条poiitem
        queryS.setPageNum(currentPage);// 设置查第一页

        poiSearch = new PoiSearch(mContext, queryS);
        poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult result, int rCode) {
                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                    if (result != null && result.getQuery() != null) {// 搜索poi的结果
                        if (result.getQuery().equals(queryS)) {// 是否是同一条
                            poiResult = result;
                            // 取得搜索到的poiitems有多少页
                            List<PoiItem> poiItems = poiResult.getPois();
                            //存储信息
                            final List<HashMap<String, String>> listString = new ArrayList<HashMap<String, String>>();
                            //存储点
                            final List<NaviLatLng> pointList = new ArrayList<NaviLatLng>();
                            for (int i = 0; i < poiItems.size(); i++) {
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("name", poiItems.get(i).getTitle());
                                map.put("address", poiItems.get(i).getCityName() + poiItems.get(i).getAdName() + poiItems.get(i).getSnippet());
                                listString.add(map);

                                pointList.add(new NaviLatLng(poiItems.get(i).getLatLonPoint()
                                        .getLatitude(), poiItems.get(i)
                                        .getLatLonPoint().getLongitude()));
                            }
                            SimpleAdapter aAdapter = new SimpleAdapter(mContext, listString, R.layout.item_layout,
                                    new String[]{"name", "address"}, new int[]{R.id.poi_field_id, R.id.poi_value_id});
                            mhintList.setAdapter(aAdapter);
                            mhintList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                    //设置选中
                                    //销毁弹出框
                                    dismiss();
                                    queryAddressListener.onSuccess(pointList.get(position), listString.get(position).get("name"));
                                }
                            });
                            aAdapter.notifyDataSetChanged();

                        }
                    } else {
                        ToastUtil.show(mContext,
                                R.string.nodata);
                    }
                } else {
                    ToastUtil.showerror(mContext, rCode);
                }

            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        });
        poiSearch.searchPOIAsyn();
    }


}
