package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.IServo;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

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
    
    public static final double voltage = 5.0; 
    public static final double scaleFactor = voltage/512;
    
    private static final int HOST_PORT = 65;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private Datagram dg = null;
    private RadiogramConnection rCon = null;
    private IAnalogInput rightCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput leftCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A1];
    
    protected void startApp() throws MIDletStateChangeException {
        

        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        
        try {
            
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);  // only sending 12 bytes of data
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            notifyDestroyed();
        }
        
        
        //calibrate
        while (sw2.isOpen())
        {

            leds.getLED(2).setColor(new LEDColor(255,0,0));    
               leds.getLED(2).setOn();
               servo1.setValue(1540);
               servo2.setValue(1600);
        }
        
        leds.getLED(2).setOff();
        
        //drive forward
        servo2.setValue(1530);
         while(true){
       try {
            double leftAvg = 0;
            double rightAvg = 0;
            for (int i=0; i<3; i++) {
                leftAvg += (leftCarSensor.getVoltage())*(.3333);
                rightAvg += (rightCarSensor.getVoltage())*(.3333);
                Utils.sleep(100);
            }
            double inchesCarRight = leftAvg;
            double inchesCarLeft = rightAvg;
            System.out.println("left avg is: " + leftAvg);
             System.out.println("right avg is: " + rightAvg);
 
                if (inchesCarRight > .9) {
                    slideRight();
                    Utils.sleep(1200);
                }
                else if (inchesCarLeft > 1.1) {
                    slideLeft();
                    Utils.sleep(1200);
                }
                else {
         
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
       
        }
     }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    public void slideLeft() throws InterruptedException
    {
        //turn right
        servo1.setValue(1800);
        Thread.sleep(1200);
        
        //then turn back left
        servo1.setValue(1260);
       Thread.sleep(900);
       
       //straighten
       servo1.setValue(1540);
    }
    
    public void slideRight() throws InterruptedException
    {
        //turn right
        servo1.setValue(1260);
        Thread.sleep(1200);
        
        //turn back left
        servo1.setValue(1800);
       Thread.sleep(1000);
       
       //straighten
       servo1.setValue(1540);

    }
    public void switchReleased(SwitchEvent se) {
         
    }

    public void switchPressed(SwitchEvent se) {
}
}