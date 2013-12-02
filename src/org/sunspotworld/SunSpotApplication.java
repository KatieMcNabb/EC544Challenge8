package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.IServo;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 */
public class SunSpotApplication extends MIDlet implements ISwitchListener {
     // devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ISwitch sw2 = (ISwitch)Resources.lookup(ISwitch.class, "SW2"); 
    private ISwitch sw1 = (ISwitch)Resources.lookup(ISwitch.class, "SW1"); 
    // left servo from driver's view
    private IServo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // right servo from driver's view
    private IServo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private static final int SERVO_CENTER_VALUE = 1500;
        private static final int SERVO2_HIGH = 10; //speeding step high
    private static final int SERVO2_LOW = 5; //speeding step low
        private static final int SERVO2_MAX_VALUE = 2000;
        private static final int SERVO2_MIN_VALUE = 1450;
         private int current1 = SERVO_CENTER_VALUE;
         private static final int SERVO1_MAX_VALUE = 1530;
    private static final int SERVO1_MIN_VALUE = 1400;
    private static final int SERVO1_HIGH = 20; //steering step high
    private static final int SERVO1_LOW = 10; //steering step low
    private int step1 = SERVO1_LOW;
    private boolean go = true;
    
    public static final double voltage = 5.0; 
    public static final double scaleFactor = voltage/512;
    
    private static final int HOST_PORT = 65;
    private int current2 = SERVO_CENTER_VALUE;
    private int step2 = SERVO2_LOW;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private Datagram dg = null;
    private RadiogramConnection rCon = null;
    private IAnalogInput rightCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput leftCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A1];
    
    protected void startApp() throws MIDletStateChangeException {
    //    
      //  servo2.setValue(1450);

        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        //start_wheels
        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);  // only sending 12 bytes of data
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            notifyDestroyed();
        }
        System.out.println("setting calibration values");
        servo1.setValue(1500);
        servo2.setValue(1500);
        sw2.addISwitchListener(this);
        sw1.addISwitchListener(this);
        


   
     }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
    
     private void left() {
        System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE){
            servo1.setValue(current1+step1);
        } else {
            servo1.setValue(SERVO1_MAX_VALUE);
        }
    }
     private void right() {
        System.out.println("right");
        current1 = servo1.getValue();
        if (current1-step1 > SERVO1_MIN_VALUE){
            servo1.setValue(current1-step1);
        } else {
            servo1.setValue(SERVO1_MIN_VALUE);
        }
    }


    public void switchPressed(SwitchEvent se) {
        if (se.getSwitch()!=sw2) {
            System.out.println("switch one called");
            go = false;
            return;
        }
         System.out.println("switch pressed");
         while(true && go){
            try {
           dg.reset();
                double inchesCarRight = -1*(rightCarSensor.getVoltage()/scaleFactor);
                double inchesCarLeft = (leftCarSensor.getVoltage())/scaleFactor;
                dg.writeDouble(inchesCarLeft);
                dg.writeDouble(inchesCarRight);
                rCon.send(dg);
                
        servo2.setValue(1450);
      
                
    System.out.println("forward");
                
               // Utils.sleep(50);
            } catch (IOException ex){
                ex.printStackTrace();
            }
        }
         System.out.println("re-centering servos");
         servo2.setValue(1500);
            servo1.setValue(1500);
         
    }

    public void switchReleased(SwitchEvent se) {
         
    }
}