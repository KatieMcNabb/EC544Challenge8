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
    private double leftDistance;
    private double rightDistance;
    private double p1 = -10.76;
    private double p2 = 67.71;
    private double p3 = -163.9;
    private double p4 = 169.3;
    
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
               servo1.setValue(1550);
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
                double leftVoltage = leftCarSensor.getVoltage();   
                leftDistance = p1 * (pow(leftVoltage, 3)) + p2 * (pow(leftVoltage, 2)) + p3 * leftVoltage + p4;
                double rightVoltage = rightCarSensor.getVoltage();   
                rightDistance = p1 * (pow(rightVoltage, 3)) + p2 * (pow(rightVoltage, 2)) + p3 * rightVoltage + p4;
                leftAvg += leftDistance*.3333;
                rightAvg += rightDistance*(.3333);
                Utils.sleep(50);
            }
            System.out.println("left avg distance: " + leftAvg + " cm");
             System.out.println("right avg is: " + rightAvg + " cm");
 
                if (leftAvg < 56) {
                    slideRight();
                    Utils.sleep(1200);
                }
                else if (rightAvg < 60) {
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
       servo1.setValue(1550);
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
       servo1.setValue(1550);

    }
    public void switchReleased(SwitchEvent se) {
         
    }

    public void switchPressed(SwitchEvent se) {
}
    public double pow(double x, double y) {
        int den = 1024; //declare the denominator to be 1024  
        /*Conveniently 2^10=1024, so taking the square root 10  
        times will yield our estimate for n.??In our example  
        n^3=8^2n^1024 = 8^683.*/
        int num = (int) (y * den); // declare numerator
        int iterations;
        iterations = 10;
        double n = Double.MAX_VALUE; /* we initialize our
         * estimate, setting it to max*/
        while (n >= Double.MAX_VALUE && iterations > 1) {
            /*??We try to set our estimate equal to the right
             * hand side of the equation (e.g., 8^2048).??If this
             * number is too large, we will have to rescale. */
            n = x;
            for (int i = 1; i < num; i++) {
                n *= x;
            }
            /*here, we handle the condition where our starting
             * point is too large*/
            if (n >= Double.MAX_VALUE) {
                iterations--;
                den = (int) (den / 2);
                num = (int) (y * den); //redefine the numerator
            }
        }
        /*************************************************  
         ** We now have an appropriately sized right-hand-side.  
         ** Starting with this estimate for n, we proceed.  
         **************************************************/
        for (int i = 0; i < iterations; i++) {
            n = Math.sqrt(n);
        }
        // Return our estimate
        return n;
    }
}