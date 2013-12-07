package org.sunspotworld; 
  
import com.sun.spot.peripheral.Spot; 
import com.sun.spot.peripheral.TimeoutException; 
import com.sun.spot.peripheral.radio.IRadioPolicyManager; 
import com.sun.spot.peripheral.radio.RadioFactory; 
import com.sun.spot.resources.Resources; 
import com.sun.spot.resources.transducers.ISwitch; 
import com.sun.spot.resources.transducers.ITriColorLED; 
import com.sun.spot.resources.transducers.LEDColor; 
import com.sun.spot.resources.transducers.ITriColorLEDArray; 
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection; 
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.SwitchEvent;
  
import java.io.IOException; 
import javax.microedition.io.Connector; 
import javax.microedition.io.Datagram; 
import javax.microedition.midlet.MIDlet; 
import javax.microedition.midlet.MIDletStateChangeException; 
  

/*Code for spot that is the remote*/
/* switch 1 tells car to drive self
 * switch 2 controls override
   * L(1) green = telling car to drive self
   * L(6) green = acting as remote for car
*/

public class RemoteSunSpotApplication extends MIDlet implements ISwitchListener{ 
  
    private static final String VERSION = "1.0"; 
    // CHANNEL_NUMBER  default as 26, each group set their own correspondingly 
    //private static final int CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL;  
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID; 
    private static final String BROADCAST_PORT      = "161"; 
    private static final int PACKETS_PER_SECOND     = 1; 
    private static final int PACKET_INTERVAL        = 3000 / PACKETS_PER_SECOND; 
      
    private int channel = 21; 
    private int power = 32;                             // Start with max transmit power 
      
    private ISwitch sw1 = (ISwitch)Resources.lookup(ISwitch.class, "SW1"); 
    private ISwitch sw2 = (ISwitch)Resources.lookup(ISwitch.class, "SW2"); 
    private ITriColorLEDArray leds = (ITriColorLEDArray)Resources.lookup(ITriColorLEDArray.class); 
    private ITriColorLED statusLED = leds.getLED(0); 
    
    private IAccelerometer3D accel = (IAccelerometer3D)Resources.lookup(IAccelerometer3D.class); 
  
    private LEDColor red   = new LEDColor(50,0,0); 
    private LEDColor green = new LEDColor(0,50,0); 
    private LEDColor blue  = new LEDColor(0,0,50); 
    private double xTilt;
    private double yTilt;
    
    private boolean override = false;
    private boolean driveSelf = false;
    
    private long myAddr = 0; // own MAC addr (ID) 
    private long save_addr[] = {0,0,0,0,0,0};// save all the MAC linked to spot 
    
      
    private boolean xmitDo = true; 
      
        /*transmit driving instructions to car*/
        private void xmitLoop () { 
        RadiogramConnection txConn = null; 
        xmitDo = true; 
        
        while (xmitDo) { 
            try { 
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT); 
                //txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted 
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength()); 
                while (xmitDo) { 
  
                    xdg.reset(); 
                    xdg.writeLong(myAddr); // own MAC address 
                    xdg.writeDouble(xTilt); // xtilt
                    xdg.writeDouble(yTilt); // ytilt 
                    xdg.writeBoolean(override);
                    xdg.writeBoolean(driveSelf);
                    txConn.send(xdg); 
                          
                if (override)
                {
                    System.out.println("overriding");
                    
                }
                if (driveSelf)
                {
                    System.out.println("driveSelf");
                }
                System.out.println("xtilt is" + xTilt);
                System.out.println("ytilt is" + yTilt);
                pause(300);
                }
            }
                catch (IOException ex) { 
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
      
      
    private void pause (long time) { 
        try { 
            Thread.currentThread().sleep(time); 
        } catch (InterruptedException ex) { /* ignore */ } 
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
        sw2.addISwitchListener(this);
        sw1.addISwitchListener(this);
    } 
        
    /** 
     * Main application run loop. 
     */
    private void run() { 
  
        
        new Thread() { 
            public void run () { 
                xmitLoop(); 
            } 
        }.start();                      // spawn a thread to transmit packets 
 
      new Thread() { 
            public void run () { 
                LEDLoop(); 
            } 
        }.start();
      
    } 
   
  private void LEDLoop() 
  {
      while (true)
      {
        try {
            xTilt = accel.getTiltX();
            yTilt = accel.getTiltY();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
                
                if (xTilt > 0)
                    {
                        leds.getLED(0).setColor(blue);
                        leds.getLED(0).setOn();
                        leds.getLED(7).setOff();
                    }
                    else
                    {
                        leds.getLED(7).setColor(red);
                        leds.getLED(7).setOn();
                        leds.getLED(0).setOff();
                    }
      }
  }
  
  
   /*handle switch pressed event
    switch 2 controls override
    switch 1 tells car to drive self
    */
    public void switchPressed(SwitchEvent Sw) {
    
      if (Sw.getSwitch().equals(sw2) && override == false)
      {
          /*overriding autonomous control*/
          leds.getLED(6).setColor(green);
          leds.getLED(6).setOn();
          override = true;
      }
      else if (Sw.getSwitch().equals(sw2))
      {
          leds.getLED(6).setOff();
          override = false;
          
      }
      else if (Sw.getSwitch().equals(sw1) && driveSelf == false)
      {
          
          leds.getLED(1).setColor(green);
          leds.getLED(1).setOn();
          driveSelf = true;
      }
      else if (Sw.getSwitch().equals(sw1))
      {
          leds.getLED(1).setOff();
          driveSelf = false;
          
      }
      
  
    }
      
    /** 
     * MIDlet call to start our application. 
     */
    protected void startApp() throws MIDletStateChangeException { 
    // Listen for downloads/commands over USB connection 
    new com.sun.spot.service.BootloaderListenerService().getInstance().start(); 
        initialize(); 
        run(); 
    } 
  
    /** 
     * This will never be called by the Squawk VM. 
     */
    protected void pauseApp() { 
        // This will never be called by the Squawk VM 
    } 
  
    /** 
     * Called if the MIDlet is terminated by the system. 
     * @param unconditional If true the MIDlet must cleanup and release all resources. 
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException { 
    } 


    /*remain not implemented*/
    public void switchReleased(SwitchEvent se) {
     
    }
  
}