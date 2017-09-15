package com.lvqingyang.autobr;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.skydoves.colorpickerview.ColorPickerView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private int mColor= Color.WHITE;
    private static final String TOPIC = "RGB";
    private Gson mGson=new Gson();

    private MyMqtt mMyMqtt=new MyMqtt(new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what== ComponentSetting.MQTT_STATE_CONNECTED){
                Log.d(TAG,"连接成功");
                MyToast.show(MainActivity.this, "连接成功" );
            }else if(msg.what==ComponentSetting.MQTT_STATE_LOST){
                Log.d(TAG,"连接丢失，进行重连");
                MyToast.show(MainActivity.this, "连接丢失，进行重连" );
            }else if(msg.what==ComponentSetting.MQTT_STATE_FAIL){
                Log.d(TAG,"连接失败");
                MyToast.show(MainActivity.this, "连接失败" );
            }else if(msg.what==ComponentSetting.MQTT_STATE_RECEIVE){
                Log.d(TAG,(String)msg.obj);
            }
            super.handleMessage(msg);
        }
    });
    private com.skydoves.colorpickerview.ColorPickerView colorPickerView;
    private android.widget.ImageView ivcolor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.ivcolor = (ImageView) findViewById(R.id.iv_color);
        this.colorPickerView = (ColorPickerView) findViewById(R.id.colorPickerView);
        colorPickerView.setColorListener(new ColorPickerView.ColorListener() {
            @Override
            public void onColorSelected(int color) {
                mColor=color;
                ivcolor.setBackgroundColor(color);
            }
        });
        ivcolor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyColor c=new MyColor(Color.red(mColor),
                        Color.green(mColor),
                        Color.blue(mColor));
                if (BuildConfig.DEBUG) Log.d(TAG, "onClick: "+mGson.toJson(c));
                mMyMqtt.pubMsg(TOPIC,mGson.toJson(c) ,1);
            }
        });

        mMyMqtt.connectMqtt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMyMqtt.disConnectMqtt();
    }
}
