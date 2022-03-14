/* LIBRARIES */
#include <Arduino.h>
#include <WiFi.h>
#include "Adafruit_Sensor.h"
#include "DHT.h"
#include <OneWire.h>
#include <DallasTemperature.h>
#include <PubSubClient.h>

/* PINS */
#define DHTPIN 27
#define IRSENSORPIN 35
const int LED = 2;
const int DS18B20 = 33;

#define DHTTYPE DHT11

/* FUNCTIONS */
DHT dht(DHTPIN, DHTTYPE);
OneWire oneWire(DS18B20);
DallasTemperature sensors(&oneWire);

/* VARIABLES */
const unsigned long eventTime_Sensors = 1000;
const unsigned long eventTime_Write = 15000;
const unsigned long eventTime_IRSensor = 5000;
unsigned long previousTimeSensors = 0;
unsigned long previousTimeIRSensor = 0;
unsigned long previousTimeWrite = 0;
String temp_str;
String hum_str;
char motionStatus[20];
char temp[50];
char hum[50];
int sensorValue=HIGH;

/* WIFI CREDENTIALS */
const char* ssid="TP-Link_CAE0";
const char* password ="fpz042142";

/* MQTT VARIABLES */
const char* mqttServer = "node02.myqtthub.com";
const char* mqttUser = "BeeHiveIoT2";
const char* mqttPass = "IoTbhMQTT2021";
const char *mqttClientID = "bh2021id2";
const int mqttPort = 1883;

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

void setup() 
{
  pinMode(LED, OUTPUT);
  pinMode(IRSENSORPIN, INPUT);
  Serial.begin(115200);
  
  WiFi.begin(ssid,password);
  while (WiFi.status()!= WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi..");
  }
  Serial.println("Connected to the WiFi network");

  mqttClient.setServer(mqttServer,mqttPort);
  while (!mqttClient.connected()){
     if (mqttClient.connect(mqttClientID, mqttUser, mqttPass)) {
         Serial.println("Connected to Public MQTT Broker");
     } else {
         Serial.print("Failed to connect with MQTT Broker");
         Serial.println(mqttClient.state());
         delay(2000);
     }
  }

  sensors.begin();
  dht.begin();
}

void loop() 
{
  unsigned long currentTime = millis();

  //READ AND PRINT VALUES EVERY 1 SECOND
  if ( currentTime - previousTimeSensors >= eventTime_Sensors)
  {
    sensors.requestTemperatures();

    int temperatureC = sensors.getTempCByIndex(0);

    temp_str = String(temperatureC);
    temp_str.toCharArray(temp, temp_str.length() + 1);

    sensorValue = digitalRead(IRSENSORPIN);
    if (sensorValue == LOW) {
        Serial.println("IR Sensor: Motion");
        motionStatus[0] = '1';
        digitalWrite(LED, HIGH);
    } else {
        Serial.println("IR Sensor: Inactive");
        motionStatus[0] = '0';
        digitalWrite(LED, LOW);
    }

    int humidity = dht.readHumidity();

    if (isnan(humidity)) {
      Serial.println(F("Failed to read from DHT sensor!"));
      return;
    }

    hum_str = String(humidity);
    hum_str.toCharArray(hum, hum_str.length() + 1);

    Serial.print("Humidity: ");
    Serial.println(humidity);
    Serial.print("Temperature: ");
    Serial.print(temperatureC);
    Serial.println("ºC");
    Serial.println(" ");

    previousTimeSensors = currentTime;
  }

  //PUBLISH HUMIDITY AND TEMPERATURE DATA TO MYQTTHUB EVERY 15 SECONDS
  if ( currentTime - previousTimeWrite >= eventTime_Write)
  {
    mqttClient.publish("humData", hum, true);
    mqttClient.publish("tempData", temp, true);

    Serial.println("Data sent to MyQttHub");
    Serial.println(" ");

    previousTimeWrite = currentTime;
  }

  //PUBLISH MOTION DATA TO MYQTTHUB EVERY 5 SECONDS
  if( currentTime - previousTimeIRSensor >= eventTime_IRSensor)
  {
    mqttClient.publish("motionData", motionStatus, true);

    Serial.println("Data motion sent to MyQttHub");
    Serial.println(" ");

    previousTimeIRSensor = currentTime;
  }
}