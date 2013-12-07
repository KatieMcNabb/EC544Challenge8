/*
 * BeaconProximity.java
 *
 * Created on Nov 21, 2013 12:30:00 PM;
 * Author Qilong Tang qilong@bu.edu
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
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
 *
 * The manifest specifies this class
 *
 * @author Yuting Zhang <ytzhang@bu.edu>
 */
public class BeaconProximity extends MIDlet {

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private IAnalogInput proximity = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A2];
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private static final String VERSION = "1.0";
    // CHANNEL_NUMBER  default as 26, each group set their own correspondingly
    private static final int CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BROADCAST_PORT = "161";
    private static final int PACKETS_PER_SECOND = 1;
    private static final int PACKET_INTERVAL = 200 / PACKETS_PER_SECOND;
    //   private static AODVManager aodv = AODVManager.getInstance();
    private int channel = 21;
    private int power = 32;                             // Start with max transmit power
    private boolean recvDo = true;
    private long myAddr = 0;
    private long TimeStamp;
    private ITriColorLED statusLED = leds.getLED(0);
    private ITriColorLED receiveLED =leds.getLED(7);
    private ITriColorLED sendLED =leds.getLED(6);
    private boolean ledsInUse = false;
    private LEDColor red = new LEDColor(50, 0, 0);
    private LEDColor green = new LEDColor(0, 50, 0);
    private LEDColor blue = new LEDColor(0, 0, 50);
    private double dis;
    private boolean Turn;
    private double p1 = -10.76;
    private double p2 = 67.71;
    private double p3 = -163.9;
    private double p4 = 169.3;

    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");

        new Thread() {

            public void run() {
                DataCollect();
            }
        }.start();                      // spawn a thread to transmit packets
        new Thread() {

            public void run() {
                xmitLoop();
            }
        }.start();                      // spawn a thread to receive packets
    }

    private void DataCollect() {
        while (true) {
            try {
                double vol = proximity.getVoltage();
                //System.out.println("Output voltage = " + vol + " V");
                //dis = 18.67 / (vol + 0.167);
                dis = p1 * (pow(vol, 3)) + p2 * (pow(vol, 2)) + p3 * vol + p4;
                System.out.println("Distance = " + dis + " cm");
                Utils.sleep(1000);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            /*if (sw1.isClosed()) {
            notifyDestroyed();                      // cause the MIDlet to exit
            }*/
        }
    }

    private void recvLoop() {
        RadiogramConnection rcvConn = null;
        RadiogramConnection txConn = null;
        recvDo = true;
        int nothing = 0;
        while (recvDo) {
            try {
                System.out.println("Ready to get response.");
                rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + BROADCAST_PORT);
                rcvConn.setTimeout(PACKET_INTERVAL - 5);
                Radiogram rdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());
                while (recvDo) {
                    try {
                        rdg.reset();
                        rcvConn.receive(rdg);           // listen for a packet
                        statusLED.setColor(green);
                        statusLED.setOn();
                        long srcAddr = rdg.readLong(); // src MAC address
                        long srcTime = rdg.readLong(); // src's timestamp
                        boolean srcRequest = rdg.readBoolean(); // src's REQUEST BIT
                        int pow = rdg.readInt();
                        receiveLED.setColor(green);
                        Utils.sleep(500);
                        receiveLED.setOn();
                        Utils.sleep(500);
                        sendLED.setOff();
                        //if receive a query, give response
                        if (true) {
                            try {
                                txConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
                                TimeStamp = System.currentTimeMillis();
                                xdg.reset();
                                xdg.writeLong(myAddr); // own MAC address
                                xdg.writeLong(TimeStamp); // current timestamp
                                if(dis<50)
                                    Turn = true;
                                else Turn = false;
                                xdg.writeBoolean(Turn);
                                xdg.writeBoolean(false);//current distance messured
                                //xdg.writeInt(power); // own power
                                
                                System.out.println("dis:    "+dis+" turn"+Turn);
                                
                                txConn.send(xdg);
                                sendLED.setColor(blue);
                                Utils.sleep(200);
                                sendLED.setOn();
                                Utils.sleep(200);
                                sendLED.setOff();
                                long delay = (TimeStamp + PACKET_INTERVAL - System.currentTimeMillis()) - 2;
                                if (delay > 0) {
                                    pause(delay);
                                }
                                leds.getLED(7).setOff();
                            } catch (IOException ex) {
                                // ignore
                            } finally {
                                if (txConn != null) {
                                    try {
                                        txConn.close();
                                    } catch (IOException ex) {
                                    }
                                }
                            }
                        }

                        String srcID = IEEEAddress.toDottedHex(srcAddr);
                        nothing = 0;
                    } catch (TimeoutException tex) {        // timeout - display no packet received
                        System.out.println("Time out.");
                        statusLED.setColor(red);
                        statusLED.setOn();
                        nothing++;
                        if (nothing > 2 * PACKETS_PER_SECOND && !ledsInUse) {
                            for (int ledint = 1; ledint <= 7; ledint++) { // if nothing received eventually turn off LEDs
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
                    } catch (IOException ex) {
                    }
                }
            }
        }
    }

    private void xmitLoop () {
 //       ILed led = Spot.getInstance().getGreenLed();
        System.out.println("xmit loop entered ");
        RadiogramConnection txConn = null;
        //xmitDo = true;
        while (true) {
            try {
                System.out.println("xmitting!!!!!!!!!!!!!!!!!!! ");
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());

                    receiveLED.setColor(green);
                    receiveLED.setOn();
                    TimeStamp = System.currentTimeMillis();

                    xdg.reset();
                    xdg.writeLong(myAddr); // own MAC address
//                    xdg.writeLong(TimeStamp); // current timestamp
//                    xdg.writeBoolean(xmitDo);
//                    
//                    xdg.writeInt(power);
                    xdg.writeBoolean(false);
                    xdg.writeBoolean(true);
                    if(dis<50)
                        Turn = true;
                    else Turn = false;
                    xdg.writeBoolean(Turn);
                    txConn.send(xdg);
//                    led.setOff();
                    pause(100);
                    receiveLED.setOff();   
                    long delay = (TimeStamp+ PACKET_INTERVAL- System.currentTimeMillis());
                    if (delay > 0) {
                        pause(delay);
                    }
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
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        initialize();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(myAddr));

        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setColor(LEDColor.GREEN);
            leds.getLED(i).setOn();
        }
        Utils.sleep(500);
        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setOff();
        }
        Utils.sleep(500);
        run();
    }

    /**
     * Initialize any needed variables.
     */
    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        statusLED.setColor(red);     // Red = not active
        statusLED.setOn();
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        //    AODVManager rp = Spot.getInstance().
    }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
     * Pause for a specified time.
     *
     * @param time the number of milliseconds to pause
     */
    private void pause(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException ex) { /* ignore */ }
    }

    /**
     * Called if the MIDlet is terminated by the system. It is not called if
     * MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setOff();
        }
    }

    public double pow(double x, double y) {
        int den = 1024; //declare the denominator to be 1024  
        /*Conveniently 2^10=1024, so taking the square root 10  
        times will yield our estimate for n.¡¡In our example  
        n^3=8^2n^1024 = 8^683.*/
        int num = (int) (y * den); // declare numerator
        int iterations;
        iterations = 10;
        double n = Double.MAX_VALUE; /* we initialize our
         * estimate, setting it to max*/
        while (n >= Double.MAX_VALUE && iterations > 1) {
            /*¡¡We try to set our estimate equal to the right
             * hand side of the equation (e.g., 8^2048).¡¡If this
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
