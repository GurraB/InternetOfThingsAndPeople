#include <ESP8266WiFi.h>
#include <PubSubClient.h>
 
const char* ssid = "OnePlus3T";
const char* password =  "hjfav123";
const char* mqttServer = "m23.cloudmqtt.com";
const int mqttPort = 10691;
const char* mqttUser = "vhsqtfmb";
const char* mqttPassword = "dta6g6s1LUWA";
int led_pin = 2;
 
WiFiClient espClient;
PubSubClient client(espClient);
 
void setup() {
 
  Serial.begin(115200);
 
  WiFi.begin(ssid, password);
 
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi..");
  }
  Serial.println("Connected to the WiFi network");
 
  client.setServer(mqttServer, mqttPort);
  client.setCallback(callback);
 
  while (!client.connected()) {
    Serial.println("Connecting to MQTT...");
 
    if (client.connect("ESP8266Client", mqttUser, mqttPassword )) {
 
      Serial.println("connected");  
 
    } else {
      Serial.print("failed with state ");
      Serial.print(client.state());
      delay(2000);
    }
  }
  pinMode(led_pin, OUTPUT);
  client.subscribe("led");
 
}
 
void callback(char* topic, byte* payload, unsigned int length) {
  String message;
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  
  if (message == "led_off") {
    digitalWrite(led_pin, HIGH);
  } else if(message == "led_on") {
    digitalWrite(led_pin, LOW);
  } 
}
 
void loop() {
  client.loop();
}
