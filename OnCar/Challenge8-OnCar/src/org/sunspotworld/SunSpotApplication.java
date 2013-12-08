package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
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
import com.sun.spot.util.Utils;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 * 
 * L(0) green = driving self
 * L(2) green = calibrating
 * L(7) green = overriden by remote
 */
public class SunSpotApplication extends MIDlet implements ISwitchListener {
    
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID; 
    private static final String BROADCAST_PORT      = "161"; 
    private static final String BROADCAST_PORT_BEACON = "162";
    private static final String BROADCAST_PORT_BASESTATION = "163";
    private static final int PACKETS_PER_SECOND     = 1; 
    private static final int PACKET_INTERVAL        = 3000 / PACKETS_PER_SECOND; 
    private int channel = 21; 
    private int power = 32;  
    // devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ISwitch sw2 = (ISwitch)Resources.lookup(ISwitch.class, "SW2"); 
    private IServo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]); //moves car right/left
    private IServo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]); //moves car forward/backward
    
    public static final double voltage = 5.0; 
    public static final double scaleFactor = voltage/512;
    private double leftDistance;
    private double rightDistance;
    private double p1 = -10.76;
    private double p2 = 67.71;
    private double p3 = -163.9;
    private double p4 = 169.3;
    
    private LEDColor red   = new LEDColor(50,0,0); 
    private LEDColor green = new LEDColor(0,50,0); 
    private LEDColor blue  = new LEDColor(0,0,50);
    
    private static final int HOST_PORT = 65;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private Datagram dg = null;
    private RadiogramConnection rCon = null;
    private IAnalogInput rightCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput leftCarSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A1];
    
    private boolean driveSelf = false;
    private boolean override = false;
    private boolean turnAtBeacon = false;
    private boolean didStartDriving = false;
    private long beaconSrcAddress; 
    private int caseNum;
    
    private double srcXtilt;
    private double srcYtilt;

    protected void startApp() throws MIDletStateChangeException {
     
        initialize();
        run();
     }
    
    
    /*
     * transmit to basestation for our visual display.
     */
     private void xmitLoop () {
 
        System.out.println("xmit loop entered ");
        RadiogramConnection txConn = null;
        
        while (true) {
            try {
                
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT_BASESTATION);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());

                    xdg.reset();

                    xdg.writeBoolean(turnAtBeacon);
                    xdg.writeBoolean(didStartDriving);
                    xdg.writeLong(beaconSrcAddress);
                    txConn.send(xdg);
                    didStartDriving = false;
                    pause(300);
                    //System.out.println("trasnsmitting" + beaconSrcAddress + turnAtBeacon + didStartDriving);
                   
            } catch (IOException ex) {
                // ignore
            } finally {
                if (txConn != null) {
                    try {
                        txConn.close();
                    } catch (IOException ex) { }
                }
            }
        }
    }
     /*
      * loop to receive from beacon
      */
    private void receiveBeaconLoop () {
            System.out.println("receiving beacon loop entered ");
            RadiogramConnection rcvConnBeacon = null;
            
            
            while (true) {
                try {
                    rcvConnBeacon = (RadiogramConnection)Connector.open("radiogram://:" + BROADCAST_PORT_BEACON); 
                    rcvConnBeacon.setTimeout(PACKET_INTERVAL - 5); 
                    Radiogram rdgBeacon = (Radiogram)rcvConnBeacon.newDatagram(rcvConnBeacon.getMaximumLength()); 
                    rdgBeacon.reset();
                    rcvConnBeacon.receive(rdgBeacon);
                    turnAtBeacon = rdgBeacon.readBoolean();
                    beaconSrcAddress = rdgBeacon.readLong();
                    System.out.println("turn at beacon is " + turnAtBeacon);
       
                       
                } catch (IOException ex) {
                    // ignore
                } finally {
                    if (rcvConnBeacon != null) {
                        try {
                            rcvConnBeacon.close();
                        } catch (IOException ex) { }
                    }
                }
            }
       
    }
    /*
     * loop to receive data from the remote
     */
    private void recvRemoteLoop () {
        RadiogramConnection rcvConn = null; 
        
        
        
        boolean recvDo = true; 
        int nothing = 0; 
        
        while (recvDo) { 
            try { 
                rcvConn = (RadiogramConnection)Connector.open("radiogram://:" + BROADCAST_PORT); 
                rcvConn.setTimeout(PACKET_INTERVAL - 5); 
                Radiogram rdg = (Radiogram)rcvConn.newDatagram(rcvConn.getMaximumLength()); 

                while (recvDo) {
                    try {   
                            rdg.reset(); 
                            rcvConn.receive(rdg);
                            long srcAddr = rdg.readLong(); // src MAC address 
                            srcXtilt = rdg.readDouble(); // src's STEER 
                            srcYtilt = rdg.readDouble();
                            override = rdg.readBoolean(); // are we getting an override message from remote
                            driveSelf = rdg.readBoolean(); // drive self command from remote
                            
                            System.out.println("drive self is " +driveSelf);
                            System.out.println("override is " +override);
                            
                            
                    } 
                    catch (TimeoutException tex) {        // timeout - display no packet received 
                        leds.getLED(0).setColor(red); 
                        leds.getLED(0).setOn(); 
                        nothing++; 
                        if (nothing > 2 * PACKETS_PER_SECOND) { 
                            for (int ledint = 0; ledint<=7; ledint++){ // if nothing received eventually turn off LEDs 
                                leds.getLED(ledint).setOff(); 
                            } 
                        } 
                    } 
                } 
            } catch (IOException ex) { 
                // ignore 
            } finally { 
                if (rcvConn != null) { 
                    try { 
                        rcvConn.close(); 
                    } catch (IOException ex) { } 
                } 
            } 
        } 
    } 

    private void stateMachineLoop()
    {
        while (true)
        {
            if (override == false && driveSelf == false)
            {
                caseNum = 1;
            }
            else if (driveSelf == true && override == false && turnAtBeacon == true)
                //turn  if the beacon is tripeed
            {
                caseNum = 4;
                
            }
            
            else if (driveSelf == true && override == false)
            {
                caseNum = 2;
            }
            
            /*an override command always take precendence*/
            else if (override == true)
            {
                caseNum = 3;
            }
            
            switch(caseNum)
            {
                //calibrates
                case 1:
                {
                   //middle light green indicates calibration
                   leds.getLED(2).setColor(green);    
                   leds.getLED(2).setOn();
                   leds.getLED(0).setOff();
                   leds.getLED(7).setOff();
                                       
                   servo1.setValue(1550);
                   servo2.setValue(1600);
                   //System.out.println("case 1");
                   break;
                }//end case 1 code

                    
                //case 2 car drives itself
                case 2:
                {
                    didStartDriving = true;
                    //System.out.println("case 2");
                    leds.getLED(2).setOff();
                    leds.getLED(7).setOff();
                    leds.getLED(0).setColor(green);
                    leds.getLED(0).setOn();
                    
                    //drive forward
                    servo2.setValue(1500);

                    try {
                        double leftAvg = 0;
                        double rightAvg = 0;
                        for (int i = 0; i < 3; i++) {
                            double leftVoltage = leftCarSensor.getVoltage();
                            leftDistance = p1 * (pow(leftVoltage, 3)) + p2 * (pow(leftVoltage, 2)) + p3 * leftVoltage + p4;
                            double rightVoltage = rightCarSensor.getVoltage();
                            rightDistance = p1 * (pow(rightVoltage, 3)) + p2 * (pow(rightVoltage, 2)) + p3 * rightVoltage + p4;
                            leftAvg += leftDistance * .3333;
                            rightAvg += rightDistance * (.3333);
                            Utils.sleep(50);
                        }
                        

                        if (leftAvg < 56) {
                            slideRight();
                            if (!turnAtBeacon)
                            {
                                Utils.sleep(1200);
                            }
                        } else if (rightAvg < 60) {
                            slideLeft();
                            if (!turnAtBeacon)
                            {
                                Utils.sleep(1200);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();

                    }

                break;
              }//end case 2 code
                    
                    
                //case three is drive based on remote command     
                case 3:
                {
                    //System.out.println("Case 3");
                    leds.getLED(2).setOff();
                    leds.getLED(0).setOff();
                    leds.getLED(7).setColor(green);
                    leds.getLED(7).setOn();
                    
                    
                    /* old code */
                    //int servo1Remote = (int) (1540 - (int)240*srcXtilt);
                    //int servo2Remote = (int) (1600 - (int)180*srcYtilt);
                    //servo1.setValue(servo1Remote);
                    //servo2.setValue(1500); //for now we will have constant speed
                    
                    /*determine xTilt*/
                    if (srcXtilt < -.2)
                    {
                        servo1.setValue(1800);
                    }
                    else if (srcXtilt > .2)
                    {
                        servo1.setValue(1260);
                    }
                    else
                    {
                        servo1.setValue(1550);
                    }
                    
                    /*determine yTilt*/
                    if (srcYtilt < -.3)
                    {
                        servo2.setValue(1850);
                    }
                    else if (srcYtilt > .3)
                    {
                        servo2.setValue(1500);
                    }
                    else
                    {
                        servo2.setValue(1600);
                    }
                    
                    break;
                }//end case 3 code
                case 4:
                {
                try {
                    //turn corner
                    //flash blue while turning
                    leds.getLED(3).setColor(blue);
                    leds.getLED(3).setOn();
                    TurnLeft();
                    leds.getLED(3).setOff();
                    
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                    break;
                }
                
            }
        }
    }
    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }
     
    /** 
     * Initialize any needed variables. 
     */
    private void initialize() {  
        
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager(); 
        rpm.setChannelNumber(channel); 
        rpm.setPanId(PAN_ID); 
        rpm.setOutputPower(power - 32); 
    } 
    
    /*
     * run various threads
     */
    private void run() { 
  
        new Thread() { 
            public void run () { 
                recvRemoteLoop(); 
            } 
        }.start();                      // spawn a thread to receive packets 

        new Thread() { 
            public void run () { 
                xmitLoop(); 
            } 
        }.start();                      // spawn a thread to receive packets 
        
        new Thread() { 
            public void run () { 
                stateMachineLoop(); 
            } 
        }.start();                      // spawn a thread to operate car control state machine
        new Thread() { 
            public void run () { 
                receiveBeaconLoop(); 
            } 
        }.start();                      // spawn a thread to operate car control state machine
 
 
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    public void slideLeft() throws InterruptedException
    {
        if (!turnAtBeacon)
        {
        //turn right
        servo1.setValue(1800);
        Thread.sleep(1200);
        }
        else
        {
            return;
        }
        if (!turnAtBeacon)
        {
        //then turn back left
        servo1.setValue(1260);
        Thread.sleep(900);
        }
        else {
            return;
        }
                    
       //straighten
       servo1.setValue(1550);
        
    }
    
    public void slideRight() throws InterruptedException
    {
        if (!turnAtBeacon)
        {
            //turn right
            servo1.setValue(1260);
            Thread.sleep(1200);
        }
        else{
            return;
        }
        if(!turnAtBeacon)
        {
        //turn back left
        servo1.setValue(1800);
        Thread.sleep(1000);
        }
        else {
            return;
        }
        
           //straighten
            servo1.setValue(1550);
        
    }
    
     public void TurnLeft() throws InterruptedException//2
    {
        System.out.println("turn left");
                
        servo2.setValue(1500);
        servo1.setValue(1800);
        Thread.sleep(1600);
        //turn left
        
        servo2.setValue(1600);
        Thread.sleep(500);
        //stop
        
        servo1.setValue(1260);
        Thread.sleep(500);
        //wheel turn right
        
        servo2.setValue(1850);
        Thread.sleep(700);
        //backword right
        
        servo2.setValue(1600);
        Thread.sleep(500);
        //stop
        
        servo1.setValue(1800);
        Thread.sleep(1250);
        //wheel turn left
        
        servo2.setValue(1500);
        Thread.sleep(1600);
        //turn left (back to straight) 

        servo1.setValue(1540);
        Thread.sleep(1000);
       //go straight
       
       servo2.setValue(1600);
       //stop 
    }
     
    public void switchReleased(SwitchEvent se) {
         
    }

    public void switchPressed(SwitchEvent se) {
}
   
    private void pause (long time) { 
        try { 
            Thread.currentThread().sleep(time); 
        } catch (InterruptedException ex) { /* ignore */ } 
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