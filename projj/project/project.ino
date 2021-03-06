#include <ESP8266WiFi.h>
#include <PubSubClient.h>
const int TRIG_PIN = D0;
const int ECHO_PIN = D1;

const unsigned int MAX_DIST = 23200;

const char* ssid = "*****";
const char* password =  "*****";
const char* mqttServer = "m14.cloudmqtt.com";
const int mqttPort = 14052;
const char* mqttUser = "*****";
const char* mqttPassword = "*******";
int led_up = 3, led_down = 4, led_right = 5, led_left = 6, led_tilt_right = 7, led_tilt_left = 8;
const char* dancemoves[] = {"up", "down", "right", "left", "tilt right", "tilt left", "clockwise","notClockwise"};
unsigned long t1 = 0;
unsigned long t2 = 0;
unsigned long t3 = 0;
unsigned long lock_timer = 0;
unsigned long pulse_width = 290000;
boolean active = false;
boolean locked = true;

WiFiClient espClient;
PubSubClient client(espClient);
int d1 = 5;
int d2 = 4;
int d3 = 2;
String activeUser;

void setup() {
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(13, INPUT);
  pinMode(15, INPUT);
  pinMode(d1, OUTPUT);
  pinMode(d2, OUTPUT);
  pinMode(d3, OUTPUT);
  digitalWrite(TRIG_PIN, LOW);
  Serial.begin(115200);
  Serial.println("Hello, anton is dork");

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
 
    if (client.connect("ESP8266ClientPRO", mqttUser, mqttPassword )) {
 
      Serial.println("connected");  
 
    } else {
      Serial.print("failed with state ");
      Serial.print(client.state());
      delay(2000);
    }
  }
  attachInterrupt(15, echo_high, RISING);
  attachInterrupt(13, echo_low, FALLING);
  reset();
  client.subscribe("esp/test");
  client.publish("esp/test", "anton is dork");
}

void loop() {
  client.loop();
  if (micros() - t3 > 100000) {
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);
    t3 = micros();
  }
}

void echo_high() {
  t1 = micros();
}

void echo_low() {
  t2 = micros();
  pulse_width = t2 - t1;
  //Serial.print("cm: ");
  //Serial.println(pulse_width / 58.0);
  if ((pulse_width / 58.0) < 50) {
    active = true;
  } else {
    active = false;
  }
}

void reset() {
  digitalWrite(d1, LOW);
  digitalWrite(d2, LOW);
  digitalWrite(d3, LOW);
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.println("callback");
  float cm;
  float inches;
  String currentUser;
  String message;
  for (int i = 0; i < length-1; i++) {
    message += (char)payload[i];
  }
  currentUser = (char)payload[length-1];
  Serial.print("message: ");
  Serial.println(message);
  Serial.print("User: ");
  Serial.println(currentUser);
  reset();
  delayMicroseconds(100);
  if (active) {
    Serial.println("is active yes");
    if (message == dancemoves[0] && !locked) {
      digitalWrite(d1, HIGH);
    } else if (message == dancemoves[1] && !locked && activeUser == currentUser) {
      digitalWrite(d2, HIGH);
    } else if (message == dancemoves[2] && !locked && activeUser == currentUser) {
      digitalWrite(d1, HIGH);
      digitalWrite(d2, HIGH);
    } else if (message == dancemoves[3] && !locked && activeUser == currentUser) {
      digitalWrite(d3, HIGH);
    } else if (message == dancemoves[4] && !locked && activeUser == currentUser) {
      digitalWrite(d3, HIGH);
      digitalWrite(d1, HIGH);
    } else if (message == dancemoves[5] && !locked && activeUser == currentUser) {
      digitalWrite(d3, HIGH);
      digitalWrite(d2, HIGH);
    } else if(message == dancemoves[6]) {
      digitalWrite(d1, HIGH);
      digitalWrite(d2, HIGH);
      digitalWrite(d3, HIGH);
      delay(100);
      digitalWrite(d1, LOW);
      digitalWrite(d2, LOW);
      digitalWrite(d3, LOW);
      delay(100);
      digitalWrite(d1, HIGH);
      digitalWrite(d2, HIGH);
      digitalWrite(d3, HIGH);
      delay(100);
      digitalWrite(d1, LOW);
      digitalWrite(d2, LOW);
      digitalWrite(d3, LOW);
      delay(100);
      digitalWrite(d1, HIGH);
      digitalWrite(d2, HIGH);
      digitalWrite(d3, HIGH);
      delay(100);
      digitalWrite(d1, LOW);
      digitalWrite(d2, LOW);
      digitalWrite(d3, LOW);
      locked = false;
      activeUser = currentUser;
    } 
  } else {
    locked = true;
  }
}
