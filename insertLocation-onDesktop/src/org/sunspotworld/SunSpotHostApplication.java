/*
 * SunSpotHostApplication.java
 *
 * Created on Sep 26, 2013 12:47:16 PM;
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.Spot;
import javax.microedition.midlet.MIDletStateChangeException;
import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.RadioPolicyManager;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.*;

/**
 * Host application
 */
/**
 * Host application
 */
public class SunSpotHostApplication {

    // Broadcast port on which we listen for sensor samples
    private static final int HOST_PORT = 163;
    private static final int PACKETS_PER_SECOND = 1;
    private static final int PACKET_INTERVAL = 3000 / PACKETS_PER_SECOND;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private int channel = 21;
    private int power = 32;
    private String firstBeacon = "7fee";
    private String secondBeacon;
    private String thirdBeacon;
    private String fourthBeacon;
    private boolean turnAtBeacon = false;
    private boolean isMoving = false;
    private int[] triggerArray = {0, 0, 0, 0};
    private int xpos = 560;
    private int ypos = 340;
    private static boolean didStartDriving;
    private Timer motionTimer = new Timer();

    private void run() throws Exception {
        IRadioPolicyManager rpm = RadioFactory.getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        new Thread() {
            public void run() {
                recvLoop();
            }
        }.start();
        new Thread() {
            public void run() {
                updateDBLoop();
            }
        }.start();
    }

    public void recvLoop() {
        try {
            RadiogramConnection rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
                    rCon.setTimeout(PACKET_INTERVAL - 5);
                    Radiogram rdg = (Radiogram) rCon.newDatagram(rCon.getMaximumLength());
            while (true) {
                try {
                    
                    rdg.reset();
                    rCon.receive(rdg);
                    turnAtBeacon = rdg.readBoolean(); 
                    if (!didStartDriving)
                    didStartDriving = rdg.readBoolean(); 
                    else
                        rdg.readBoolean();
                    long beaconAddr = rdg.readLong();
                    System.out.println("turnAtBeacon: " + turnAtBeacon + " DidStartDrving: "
                            + didStartDriving + " beacon addr: " + beaconAddr);
                    String hexaddr = Integer.toHexString((int) beaconAddr);
                } catch (IOException ex) {
                    Logger.getLogger(SunSpotHostApplication.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(SunSpotHostApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateDBLoop() {
        while (true) {
            createTable();
            insertInitialDataTable();

            while (true) {
                try {
                        motionTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                //make sure we are moving before updating
                            
                                    if (didStartDriving)ypos -= 2;
                                
                                // update databse with current x,y
                                updateTable();
                            }
                        }, 2 * 1000, 2 * 1000);

                    


                } catch (Exception e) {
                    System.err.println("Caught " + e + " while reading sensor samples.");
                }
            }
        }
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("SendDataDemo");

        SunSpotHostApplication app = new SunSpotHostApplication();

        app.run();
    }

    /* Method to create table with three rows*/
    /*Create table code based on code at: http://www.tutorialspoint.com/sqlite/sqlite_java.htm*/
    public static void createTable() {
        java.sql.Connection createConnect = null;
        Statement sqlStatement = null;

        try {

            /*Create connection with database*/
            Class.forName("org.sqlite.JDBC");
            createConnect = DriverManager.getConnection("jdbc:sqlite:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");
            System.out.println("Opened database successfully");

            /*Create table sql statement*/
            sqlStatement = createConnect.createStatement();
            String sql = "CREATE TABLE OURDATA"
                    + "(ID             VARCHAR   NOT NULL,"
                    + "X           FLOAT    NOT NULL, "
                    + "Y           FLOAT    NOT NULL, "
                    + "CONSTRAINT pri_id_time PRIMARY KEY(ID))";

            /*Execute sql statement and close connection*/
            try {
                sqlStatement.executeUpdate(sql);
            } catch (Exception e) {
                System.err.println("Table already exists...continuing");
            }
            sqlStatement.close();
            createConnect.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Table created successfully");
    }

    /*method to insert table*/
    public static void insertInitialDataTable() {
        java.sql.Connection insertConnection = null;
        Statement insertStatement = null;

        try {
            /*Create connection with database*/
            Class.forName("org.sqlite.JDBC");
            insertConnection = DriverManager.getConnection("jdbc:sqlite:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");
            System.out.println("Opened database successfully");

            /*Create sql statement*/
            insertStatement = insertConnection.createStatement();

            try {
                String sql = "INSERT INTO OURDATA(ID,X,Y)"
                        + "VALUES('1',560,340);";
                insertStatement.executeUpdate(sql);
            } catch (Exception e) {
                System.err.println("Table already contains default data...continuing");
            }
            insertStatement.close();
            insertConnection.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public void updateTable() {
        try {
            java.sql.Connection queryConnection = null;
            Statement queryStatement = null;

            /*Create connection with database*/
            Class.forName("org.sqlite.JDBC");
            queryConnection = DriverManager.getConnection("jdbc:sqlite:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");
            //System.out.println("Opened database successfully");

            /*Create sql statement*/
            queryStatement = queryConnection.createStatement();
            String sql = "UPDATE OURDATA SET X=" + xpos + ", Y=" + ypos
                    + " WHERE ID='1'";
            queryStatement.executeQuery(sql);

            queryStatement.close();
            queryConnection.close();

        } catch (Exception ex) {
            // Logger.getLogger(SunSpotHostApplication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}