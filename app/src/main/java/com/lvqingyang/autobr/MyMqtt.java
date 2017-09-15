package com.lvqingyang.autobr;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;


/**
 * Created by MichaelJiang on 2017/3/16.
 */

public class MyMqtt {
    /**DeBug用信息**/
    private String Tag = "MyMqtt";
    private boolean isDebug = false;

    /**MQTT配置参数**/
    private static String host = "***.***.***.***";
    private static String port = "***";
    private static String userID = "";
    private static String passWord = "";
    private static String clientID =  UUID.randomUUID().toString();

    /**MQTT状态信息**/
    public boolean isConnect = false;

    /**系统变量**/
    private Handler fatherHandle;

    /**MQTT支持类**/
    private MqttAsyncClient mqttClient=null;

    /**
     * 构造函数，默认的Mqtt信息内容，直接在Log中输出相关信息
     */
    public MyMqtt(){
        fatherHandle = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what== ComponentSetting.MQTT_STATE_CONNECTED){
                    Log.d(Tag,"连接成功");
                }else if(msg.what==ComponentSetting.MQTT_STATE_LOST){
                    Log.d(Tag,"连接丢失，进行重连");
                }else if(msg.what==ComponentSetting.MQTT_STATE_FAIL){
                    Log.d(Tag,"连接成功");
                }else if(msg.what==ComponentSetting.MQTT_STATE_RECEIVE){
                    Log.d(Tag,(String)msg.obj);
                }
                super.handleMessage(msg);
            }
        };

    }

    /**
     * 构造函数，如果需要在其他类中调用Mqtt信息的话，需要传入一个Handle
     * @param uiHandle
     */
    public MyMqtt(Handler uiHandle){
        fatherHandle = uiHandle;
    }

    public static void setMqttSetting(String host, String port, String userID, String passWord, String clientID){
        MyMqtt.host = host;
        MyMqtt.port = port;
        MyMqtt.userID = userID;
        MyMqtt.passWord = passWord;
        MyMqtt.clientID = clientID;
    }

    /**
     * 进行Mqtt连接
     */
    public void connectMqtt(){
        try {
            mqttClient=new MqttAsyncClient("tcp://"+this.host+":"+this.port,"ClientID"+this.clientID,new MemoryPersistence());
            mqttClient.connect(getOptions(),null,mqttActionListener);
            mqttClient.setCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开Mqtt连接重新连接
     * @User    MichaelJiang
     */
    public void reStartMqtt(){
        disConnectMqtt();
        connectMqtt();
    }

    /**
     * 断开Mqtt连接
     * @User    MichaelJiang
     */
    public void disConnectMqtt(){
        try {
            mqttClient.disconnect();
            mqttClient = null;
            isConnect = false;
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器发送数据
     * @User    MichaelJiang
     */
    public void pubMsg(String Topic, String Msg, int Qos){
        if(!isConnect){
            Log.d(Tag,"Mqtt连接未打开");
            return;
        }
        try {
            /** Topic,Msg,Qos,Retained**/
            mqttClient.publish(Topic,Msg.getBytes(),Qos,false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器发送数据
     * @User    MichaelJiang
     */
    public void pubMsg(String Topic, byte[] Msg, int Qos){
        if(!isConnect){
            Log.d(Tag,"Mqtt连接未打开");
            return;
        }
        try {
            /** Topic,Msg,Qos,Retained**/
            mqttClient.publish(Topic,Msg,Qos,false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器订阅某一个Topic
     */
    public void subTopic(String Topic, int Qos){
        if(!isConnect){
            Log.d(Tag,"Mqtt连接未打开");
            return;
        }
        try {
            mqttClient.subscribe(Topic,Qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置Mqtt的连接信息
     */
    private MqttConnectOptions getOptions(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);//重连不保持状态
        if(this.userID!=null&&this.userID.length()>0&&this.passWord!=null&&this.passWord.length()>0){
            options.setUserName(this.userID);//设置服务器账号密码
            options.setPassword(this.passWord.toCharArray());
        }
        options.setConnectionTimeout(10);//设置连接超时时间
        options.setKeepAliveInterval(30);//设置保持活动时间，超过时间没有消息收发将会触发ping消息确认
        return options;
    }

    /**
     * 自带的监听类，判断Mqtt活动变化
     */
    private IMqttActionListener mqttActionListener=new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            //连接成功处理
            Message msg=new Message();
            msg.what=ComponentSetting.MQTT_STATE_CONNECTED;
            isConnect = true;
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            exception.printStackTrace();
            //连接失败处理
            Message msg=new Message();
            msg.what= ComponentSetting.MQTT_STATE_FAIL;
            isConnect = false;
            fatherHandle.sendMessage(msg);
            new Thread(){
                @Override
                public void run(){
                    try {
                        sleep(300);
                        connectMqtt();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    /**
     * 自带的监听回传类，向UiHandle发送Message
     */
    private MqttCallback callback=new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //连接断开
            Message msg=new Message();
            msg.what=ComponentSetting.MQTT_STATE_LOST;
            isConnect = false;
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //消息到达
            Message msg=new Message();
            msg.what=ComponentSetting.MQTT_STATE_RECEIVE;
            msg.obj=new String(message.getPayload())+"\n";
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //消息发送完成
        }
    };

}
