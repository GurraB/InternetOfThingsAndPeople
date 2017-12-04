// 18 Dec 2016 Revised for the IoTaP mah.se students

// This takes data off the serial port and graphs it.
// There is an option to log this data to a file.

// I wrote an arduino app that sends data in the format expected by this app.
// The arduino app sends accelerometer and gyroscope data.

import processing.serial.*;
import java.io.*;
import static javax.swing.JOptionPane.*;
import java.awt.Toolkit;
import wekaizing.*;

//Declare the weka variables
WekaData mydata;
WekaClassifier classifier;
String trainPath = "/Users/ag6031/Downloads/imu_Lab 3/imu_Lab_New/train_b.arff";
String lable = "left";
int counter = 1;
int counterg = 0;

int freq = 20; //frequency of the sampling in sensor
int sen = 5000; //sensitivity of the sensor
int insNum = 20; //length of sliding window
int ins = 1; // counter for gesture
boolean trainFile = true; // if trainFile is set to True, the application generates training dataset
boolean logFile = false; // logs sensordata
boolean gesture_rec = false; // recognizes gestures in real-time
long stamp = 0;

String portName = "";
PrintWriter file;
float minGyro = -40000;
float maxGyro = 30000;
float minAccel = -1000;
float maxAccel = 6000;
// Globals
int g_winW             = 820;   // Window Width
int g_winH             = 600;   // Window Height
boolean g_dumpToFile   = true;  // Dumps data to c:\\output.txt in a comma seperated format (easy to import into Excel)
boolean g_enableFilter = true;  // Enables simple filter to help smooth out data.
final boolean debug = false;
Object[] testData = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
//Serial myPort;        // The serial port

cDataArray g_xAccel    = new cDataArray(200);
cDataArray g_yAccel    = new cDataArray(200);
cDataArray g_zAccel    = new cDataArray(200);
cDataArray g_vRef      = new cDataArray(200);
cDataArray g_xRate     = new cDataArray(200);
cDataArray g_yRate     = new cDataArray(200);
cGraph g_graph         = new cGraph(10, 190, 800, 400);
Serial g_serial;
PFont  g_font;

void setup()
{
  if (trainFile){
    file = createWriter(lable+"_train.csv"); //bool tells to append
  for (int i = 1; i <= insNum; i = i+1) {
     file.print("AccX"+i+",AccY"+i+",AccZ"+i+",GyrX"+i+",GyrY"+i+",GyrZ"+i+","); //write the file header 
  }
    file.print("Label"+"\r\n"); //write the file header

  file.flush();
  }
  if (logFile){
    file = createWriter("log.csv"); //bool tells to append
    file.print("#,AccX,AccY,AccZ,GyrX,GyrY,GyrZ\r\n"); //write the file header

  file.flush();
  }
  
  String mypp = port();
  size(820, 600, P2D);
  println(Serial.list());
  g_serial = new Serial(this, mypp, 9600, 'N', 8, 1.0);
  g_font = loadFont("ArialMT-20.vlw");
  textFont(g_font, 20);

  // This draws the graph key info
  strokeWeight(1.5);
  stroke(255, 0, 0);     
  line(20, 420, 35, 420);
  stroke(0, 255, 0);     
  line(20, 440, 35, 440);
  stroke(0, 0, 255);     
  line(20, 460, 35, 460);
  stroke(255, 255, 0);   
  line(20, 480, 35, 480);
  stroke(255, 0, 255);   
  line(20, 500, 35, 500);
  stroke(0, 255, 255);   
  line(20, 520, 35, 520);
  fill(0, 0, 0);
  text("xAccel", 40, 430);
  text("yAccel", 40, 450);
  text("zAccel", 40, 470);
  text("xGyro", 40, 490);
  text("yGyro", 40, 510);
  text("zGyro", 40, 530);
if (gesture_rec){

  mydata = new WekaData(trainPath); //Initialize a WekaData with empty attributes and dataset
  // upload trainig data
  classifier = new WekaClassifier(WekaClassifier.LOGISTIC);//Initialize a new classifier with KStar algorithm
  classifier.Build(mydata);
  }
setupSensor();

}

void draw()
{
  // We need to read in all the avilable data so graphing doesn't lag behind
  while (g_serial.available () >= 18)
  {
    processSerialData();
  }

  strokeWeight(1);
  fill(255, 255, 255);
  g_graph.drawGraphBox();

  strokeWeight(1.5);
  stroke(255, 0, 0);
  g_graph.drawLine(g_xAccel, minAccel, maxAccel);
  stroke(0, 255, 0);
  g_graph.drawLine(g_yAccel, minAccel, maxAccel);
  stroke(0, 0, 255);
  g_graph.drawLine(g_zAccel, minAccel, maxAccel);
  stroke(255, 255, 0);
  g_graph.drawLine(g_vRef, minGyro, maxGyro);
  stroke(255, 0, 255);
  g_graph.drawLine(g_xRate, minGyro, maxGyro);
  stroke(0, 255, 255);
  g_graph.drawLine(g_yRate, minGyro, maxGyro);
}

// This reads in one set of the data from the serial port
void processSerialData() {
  if (g_serial.available() >= 18) {
    String inputString = g_serial.readStringUntil('\n');
    //print(inputString);
    if (inputString != null && inputString.length() > 0) {
      String [] inputStringArr = split(inputString, ",");
      if (inputStringArr.length >= 7 && inputStringArr[0].equals("h")) { //  we have 7 elements
        int xAccel = int(inputStringArr[1]);
        int yAccel = int(inputStringArr[2]);
        int zAccel = int(inputStringArr[3]);
        int vRef = int(inputStringArr[4]);
        int xRate = int(inputStringArr[5]);
        int yRate = int(inputStringArr[6]);

        g_xAccel.addVal(xAccel);
        g_yAccel.addVal(yAccel);
        g_zAccel.addVal(zAccel);
        g_vRef.addVal(vRef);
        g_xRate.addVal(xRate);
        g_yRate.addVal(yRate);
      if (logFile) {
      file.print(stamp + "," + xAccel + "," + yAccel + "," + zAccel + "," + vRef + "," + xRate + "," + yRate+"\r\n");
      file.flush();
      stamp++;
    }
       if (trainFile){
        String tempStr;
        tempStr = xAccel + "," + yAccel + "," + zAccel + "," + vRef + "," + xRate + "," + yRate + ",";
        file.print(tempStr); //(string, start char, end char)
        if (counter == insNum) {
          file.print (lable+"\r\n");
          counter = 0;
          fill(0, 0, 0);
           background(102);
          text("Gesture Type:"+lable+",  Gesture Number:"+ins, 400, 530); 
            Toolkit.getDefaultToolkit().beep();
          ins++;
      }
        file.flush();
        counter++;
        }
       
       // Gesture recognition with WEKA lib
       if (gesture_rec){
         //Add Acc and gyr attributes to the object
          testData[6*(counterg)] = xAccel;
          testData[6*(counterg)+1] = yAccel;
          testData[6*(counterg)+2] = zAccel;
          testData[6*(counterg)+3] = vRef;
          testData[6*(counterg)+4] = xRate;
          testData[6*(counterg)+5] = yRate;
          counterg++;
          println (counterg);
         //Predict the class (the gestures "up", "right","left","down" ... are assigned to numbers 1,2,3,4 ...)
         String []gesture = {"up", "down","left","right","tilt_left","tilt_right"};
         if (counterg == insNum) {
           testData[insNum] = 9;
           counterg = 0;
           fill(0, 0, 0);
           background(102);
           int pred = classifier.Classify(testData);
           text("Detected Gesture:"+gesture[pred], 400, 530); 
           Toolkit.getDefaultToolkit().beep();
           println ("detected Gesture:"+gesture[pred]);
         }
       }
       }
      }
    }
  }


// This class helps mangage the arrays of data I need to keep around for graphing.
class cDataArray
{
  float[] m_data;
  int m_maxSize;
  int m_startIndex = 0;
  int m_endIndex = 0;
  int m_curSize;

  cDataArray(int maxSize)
  {
    m_maxSize = maxSize;
    m_data = new float[maxSize];
  }

  void addVal(float val)
  {

    if (g_enableFilter && (m_curSize != 0))
    {
      int indx;

      if (m_endIndex == 0)
        indx = m_maxSize-1;
      else
        indx = m_endIndex - 1;

      m_data[m_endIndex] = getVal(indx)*.5 + val*.5;
    } else
    {
      m_data[m_endIndex] = val;
    }

    m_endIndex = (m_endIndex+1)%m_maxSize;
    if (m_curSize == m_maxSize)
    {
      m_startIndex = (m_startIndex+1)%m_maxSize;
    } else
    {
      m_curSize++;
    }
  }

  float getVal(int index)
  {
    return m_data[(m_startIndex+index)%m_maxSize];
  }

  int getCurSize()
  {
    return m_curSize;
  }

  int getMaxSize()
  {
    return m_maxSize;
  }
}

// This class takes the data and helps graph it
class cGraph
{
  float m_gWidth, m_gHeight;
  float m_gLeft, m_gBottom, m_gRight, m_gTop;

  cGraph(float x, float y, float w, float h)
  {
    m_gWidth     = w;
    m_gHeight    = h;
    m_gLeft      = x;
    m_gBottom    = g_winH - y;
    m_gRight     = x + w;
    m_gTop       = g_winH - y - h;
  }

  void drawGraphBox()
  {
    stroke(0, 0, 0);
    rectMode(CORNERS);
    rect(m_gLeft, m_gBottom, m_gRight, m_gTop);
  }

  void drawLine(cDataArray data, float minRange, float maxRange)
  {
    float graphMultX = m_gWidth/data.getMaxSize();
    float graphMultY = m_gHeight/(maxRange-minRange);

    for (int i=0; i<data.getCurSize ()-1; ++i)
    {
      float x0 = i*graphMultX+m_gLeft;
      float y0 = m_gBottom-((data.getVal(i)-minRange)*graphMultY);
      float x1 = (i+1)*graphMultX+m_gLeft;
      float y1 = m_gBottom-((data.getVal(i+1)-minRange)*graphMultY);
      line(x0, y0, x1, y1);
    }
  }
}

String port() {
  String COMx, COMlist = "";
  /*
  Other setup code goes here - I put this at
   the end because of the try/catch structure.
   */
  try {
    if (debug) printArray(Serial.list());
    int i = Serial.list().length;
    if (i != 0) {
      if (i >= 2) {
        // need to check which port the inst uses -
        // for now we'll just let the user decide
        for (int j = 0; j < i; ) {
          COMlist += char(j+'a') + " = " + Serial.list()[j];
          if (++j < i) COMlist += "\n";
        }
        COMx = showInputDialog("Which COM port is correct? (a,b,..):\n"+COMlist);
        if (COMx == null) exit();
        if (COMx.isEmpty()) exit();
        i = int(COMx.toLowerCase().charAt(0) - 'a') + 1;
      }
      portName = Serial.list()[i-1];
      if (debug) println(portName);
      return portName;
    } else {
      showMessageDialog(frame, "Device is not connected to the PC");
      exit();
    }
  }
  catch (Exception e)
  { //Print the type of error
    showMessageDialog(frame, "COM port is not available (may\nbe in use by another program)");
    println("Error:", e);
    exit();
  }
  return portName;
}
void setupSensor(){
  g_serial.write("w"+insNum+"\n"); // set the window size in the sensor
}
void mousePressed() {
  file.close();
  exit();
}