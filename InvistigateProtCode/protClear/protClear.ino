#include <Wire.h> 
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>

#define rxPin 10
#define txPin 11
//Bluetooth
bool static writeSignal = true;
SoftwareSerial bluetoothPush = SoftwareSerial(rxPin, txPin);; // RX, TX For push message 
SoftwareSerial bluetoothPull = SoftwareSerial(rxPin, txPin);; // RX, TX For get message

//basic
const int EMGPin = A1;
float EMGVal = 0;

//analize
float currentTime = 0.0;
float delayViewPhase = 0.0;

//filter
const int NUM_READ = 100; 
float k = 0.4;  // filtration coefficient, 0.0-1.0
float Klf = 0.1;
float Khf = 0.9;
float EMGValc = 0;
float K0 = 0.0;
float K1 = 0.0;

LiquidCrystal_I2C lcd(0x27,16,2);  

void setup()
{
  //Initalization Bluetooth
  bluetoothPush.begin(9600); 
  bluetoothPull.begin(9600); 
  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);

  //Initalization Serial
  Serial.begin(9600);
  pinMode(A1, INPUT);

  //Initalization LCD display
  lcd.begin(16, 2);
  lcd.init();                     
  lcd.backlight();
  lcd.setCursor(0, 1);
  lcd.print("time:");
  lcd.setCursor(0, 0);
  lcd.print("Val:");
}

void loop() {
  //Part1: view signal through bluetooth
  printStatus();
  tryCheckMessage(); 

  if(writeSignal){
    currentTime = millis()/1000.0;

    //Part2: get a signal after filtering high and low frequencies
    EMGVal = analogRead(EMGPin);
    /*
    EMGValc = DCRemover(EMGVal);  // Highpass
    EMGVal = LPF(abs(EMGValc), EMGVal, Klf); // Lowpass 
 
    EMGVal = optimalRunningArithmeticFilter(EMGVal);
    */

    //Part5: view signal on display
    if(currentTime > delayViewPhase){
        lcd.setCursor(5, 1);
        lcd.print(currentTime);
        lcd.setCursor(4, 0);
        lcd.print(EMGVal);
        delayViewPhase = currentTime + 1.0;
    }
    //Serial.println(EMGVal);

    //Bluetooth view
    bluetoothPush.print("(VAL:"); 
    bluetoothPush.print(EMGVal);
    bluetoothPush.print("|TIME:");
    bluetoothPush.print(currentTime);
    bluetoothPush.print(")");
    bluetoothPush.println();
  }
  
  //delay(10); 
}


/**
  * sending status via bluetooth to another device
*/
void printStatus(){
  if(writeSignal){
    bluetoothPush.println("<ON>");
  } else {
    bluetoothPush.println("<OFF>");
  }
}

/**
  * the method attempts to recognize a command from another device
*/
void tryCheckMessage(){
  //WRITE_OR_NOT//---------------------------------------------------
  //*
  char message = bluetoothPull.read();
  if (bluetoothPull.available()) {
    if(message == '1'){
      writeSignal = true; 
    } 
    if(message == '0'){
      writeSignal = false; 
    }
  }
  //WRITE_OR_NOT//---------------------------------------------------
}


/**
  * filters out the signal by the optimal running arithmetic mean
  * @param newVal
  * @return optimal_running_arithmetic_value
*/
float optimalRunningArithmeticFilter(float newVal) {
  static int t = 0;
  static float vals[NUM_READ];
  static float average = 0;
  if (++t >= NUM_READ) t = 0; // перемотка t
  average -= vals[t];         // вычитаем старое
  average += newVal;          // прибавляем новое
  vals[t] = newVal;           // запоминаем в массив
  return ((float)average / NUM_READ);
}

/**
  * iteration of Lowpass Filter calculation
*/
float LPF(float X, float Y, float K){
      return (K*X) + (1-K)*Y;  
}

/**
  * iteration of Highpass Filter calculation
*/
float DCRemover(int X){
     float Y_out = 0;
     K0=X+Khf*K1;
     Y_out=K0-K1;
     K1=K0;
     return Y_out;
}