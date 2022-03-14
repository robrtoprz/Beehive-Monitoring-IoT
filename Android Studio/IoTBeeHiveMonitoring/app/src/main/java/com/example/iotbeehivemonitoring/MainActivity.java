package com.example.iotbeehivemonitoring;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.graphics.Color;
import android.os.Bundle;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

public class MainActivity extends AppCompatActivity {

    String value_Humidity;
    String value_Temperature;
    private TextView tvStatusMQTT;
    private TextView tvLastUpdate;
    private TextView tvhumidityDHT;
    private TextView tvtemperatureDS;
    private TextView tvmotionSensor;


    //MQTT CREDENTIALS
    static String MQTTHOST = "tcp://node02.myqtthub.com";
    static String USERNAME = "BeeHiveIoT";
    static String PASSWORD = "IoTbhMQTT2021";
    static String clientId = "bh2021id";
    MqttAndroidClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        tvhumidityDHT=(TextView) findViewById(R.id.tv_humidity);
        tvtemperatureDS=(TextView) findViewById(R.id.tv_temperature);
        tvmotionSensor=(TextView) findViewById(R.id.tv_motionSensor);
        tvStatusMQTT=(TextView) findViewById(R.id.tv_statusMQTT);
        tvLastUpdate=(TextView) findViewById(R.id.tv_lastUpdate);

        broker_Connection();
        broker_Data();
    }

    private void broker_Connection() {
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getBaseContext(), "Connected ", Toast.LENGTH_SHORT).show();
                    topicsMQTT();
                    tvStatusMQTT.setText("Connected");
                    tvStatusMQTT.setTextColor(Color.GREEN);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getBaseContext(), "Failed to Connect ", Toast.LENGTH_SHORT).show();
                    tvStatusMQTT.setText("Failed to Connect");
                    tvStatusMQTT.setTextColor(Color.RED);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void topicsMQTT() {
        try {
            client.subscribe("humData", 0);
            client.subscribe("tempData", 0);
            client.subscribe("motionData", 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void broker_Data() {
        client.setCallback(new MqttCallback() {
            @Override
            //IF WE LOOSE CONNECTION THEN NOTIFIES
            public void connectionLost(Throwable cause) {
                Toast.makeText(getBaseContext(),"Lost Connection",Toast.LENGTH_SHORT).show();
                tvStatusMQTT.setText("Lost Connection");
                tvStatusMQTT.setTextColor(Color.RED);
            }

            @Override
            //MESSAGES RECEIVED
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String humDataMessage;
                String tempDataMessage;
                String motionDataMessage;
                if(topic.matches("humData" ))
                {humDataMessage=new String(message.getPayload());

                    value_Humidity = humDataMessage;
                    tvhumidityDHT.setText(value_Humidity);
                    String currentTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                    tvLastUpdate.setText(currentTime);
                }
                if(topic.matches("tempData" ))
                {tempDataMessage=new String(message.getPayload());

                    value_Temperature = tempDataMessage;
                    tvtemperatureDS.setText(value_Temperature);
                }
                if(topic.matches("motionData" ))
                {motionDataMessage=new String(message.getPayload());
                    //MOTION
                    if(motionDataMessage.matches("1")) {
                        tvmotionSensor.setText("ACTIVE");
                        tvmotionSensor.setTextColor(Color.RED);
                    }
                    //NO MOTION
                    else if(motionDataMessage.matches("0")) {
                        tvmotionSensor.setText("INACTIVE");
                        tvmotionSensor.setTextColor(Color.parseColor("#E09E10"));
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Toast.makeText(getBaseContext(), "deliveryComplete ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}