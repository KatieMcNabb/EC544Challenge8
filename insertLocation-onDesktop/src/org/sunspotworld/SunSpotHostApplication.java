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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
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
    private static final int PACKETS_PER_SECOND     = 1; 
    private static final int PACKET_INTERVAL        = 3000 / PACKETS_PER_SECOND; 
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID; 
    private int channel = 21; 
    private int power = 32;  
        
    private void run() throws Exception {
        IRadioPolicyManager rpm = RadioFactory.getRadioPolicyManager();
        rpm.setChannelNumber(channel); 
        rpm.setPanId(PAN_ID); 
        rpm.setOutputPower(power - 32); 
    RadiogramConnection rCon = (RadiogramConnection)Connector.open("radiogram://:" + HOST_PORT); 
        rCon.setTimeout(PACKET_INTERVAL - 5); 
        Radiogram rdg = (Radiogram)rCon.newDatagram(rCon.getMaximumLength()); 
        
        // Loop to read datagrams
        while (true) {
            try {
                
               rdg.reset(); 
                rCon.receive(rdg);
                boolean turnAtBeacon = rdg.readBoolean(); // src MAC address 
                boolean didStartDriving = rdg.readBoolean(); // src MAC address
                long beaconAddr = rdg.readLong();
                System.out.println("turnAtBeacon: " + turnAtBeacon + " DidStartDrving: "
                        + didStartDriving + " beacon addr: " + beaconAddr);
                Timer timer = new Timer();
                
            } catch (Exception e) {
                System.err.println("Caught " + e +  " while reading sensor samples.");
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
            try{
            sqlStatement.executeUpdate(sql);
            }catch(Exception e){
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
    public static void insertInitialDataTable()
    {
        java.sql.Connection insertConnection = null;
        Statement insertStatement = null;
        
        try 
        {
            /*Create connection with database*/
            Class.forName("org.sqlite.JDBC");
            insertConnection = DriverManager.getConnection("jdbc:sqlite:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");
            System.out.println("Opened database successfully");
            
            /*Create sql statement*/
            insertStatement = insertConnection.createStatement();
            
            try{
            String sql = "INSERT INTO OURDATA(ID,X,Y)" + 
                    "VALUES('0014.4F01.0000.7FEE',10,10);";
            insertStatement.executeUpdate(sql);
            
            String sql2 = "INSERT INTO OURDATA(ID,X,Y)" + 
                    "VALUES('0014.4F01.0000.7FEE',0.7,0.7);";
            insertStatement.executeUpdate(sql2);
            
            String sql3 = "INSERT INTO OURDATA(ID,X,Y)" + 
                    "VALUES('0014.4F01.0000.7FEE',0.9,0.9);";
            insertStatement.executeUpdate(sql3);
            }catch(Exception e)
            {
                System.err.println("Table already contains default data...continuing");
            }
            insertStatement.close();
            insertConnection.close();
            
        }catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
    
    public static float queryTable(int spotNumber) {
        java.sql.Connection queryConnection = null;
        Statement queryStatement = null;

        try {
            /*Create connection with database*/
            Class.forName("org.sqlite.JDBC");
            queryConnection = DriverManager.getConnection("jdbc:sqlite:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");
            System.out.println("Opened database successfully");

            /*Create sql statement*/
            queryStatement = queryConnection.createStatement();

            if (spotNumber == 1) {
                String sql = "SELECT X "
                        + "FROM OURDATA "
                        + "WHERE ID = '0014.4F01.0000.7FEE';";
                ResultSet myResult = queryStatement.executeQuery(sql);
                float data = myResult.getFloat("X");
                
                myResult.close();
                queryStatement.close();
                queryConnection.close();

                return data;
                
            } else if (spotNumber == 2) {
                String sql = "SELECT ONOFF "
                        + "FROM OURDATA "
                        + "WHERE ID = '0014.4F01.0000.4120';";
                
                ResultSet myResult = queryStatement.executeQuery(sql);
                int data = myResult.getInt("ONOFF");
                
                myResult.close();
                queryStatement.close();
                queryConnection.close();

                return data;
                

            } else {
                String sql = "SELECT ONOFF "
                        + "FROM OURDATA "
                        + "WHERE ID = '0014.4F01.0000.765E';";

                ResultSet myResult = queryStatement.executeQuery(sql);
                int data = myResult.getInt("ONOFF");
                
                myResult.close();
                queryStatement.close();
                queryConnection.close();

                return data;
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return 0;
        }
    }
}