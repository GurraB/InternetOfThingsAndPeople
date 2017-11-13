const int TRIG_PIN = D0;
const int ECHO_PIN = D1;

const unsigned int MAX_DIST = 23200;
void setup() {
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(D2, OUTPUT);
  digitalWrite(TRIG_PIN, LOW);
  Serial.begin(9600);
}

void loop() {
  unsigned long t1;
  unsigned long t2;
  unsigned long pulse_width;
  float cm;
  float inches;

  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  while(digitalRead(ECHO_PIN) == 0);

  t1 = micros();
  while(digitalRead(ECHO_PIN) == 1);
  t2 = micros();

  pulse_width = t2 - t1;

  cm = pulse_width / 58.0;
  inches = pulse_width / 148.0;

  if(pulse_width > MAX_DIST) {
    Serial.println("Out of range");
  }
  else {
    Serial.print(cm);
    Serial.print(" cm \t");
    Serial.print(inches);
    Serial.println(" in");
  }
  if (cm < 10) {
    digitalWrite(D2, HIGH);
  } else {
    digitalWrite(D2, LOW);
  }
  delay(60);
}
