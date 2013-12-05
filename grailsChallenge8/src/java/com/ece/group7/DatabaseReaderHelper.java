package com.ece.group7;

import java.util.ArrayList;
import java.sql.*;

/*
 * Helper class to grab data from SQL 
 */
public class DatabaseReaderHelper {

    /*Method to grab temperature, time, id as custom object from SQL database*/
    public static ArrayList<Float> getCoords(int sunspotId) {
        
        // a is the sunspot ID, we save every mote's data separately//
        ArrayList<Float> dataList = new ArrayList<Float>();
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:file:/Users/calvinflegal/Developer/ec544/Challenge8/insertLocation-onDesktop/spotData.db");

            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            if (sunspotId == 1) {
                ResultSet rs = stmt.executeQuery("SELECT X,Y "
                        + "FROM OURDATA "
                        + "WHERE ID = '0014.4F01.0000.7FEE';");
                
                while (rs.next()) {
                    Float x =new Float(rs.getFloat("X"));
                    Float y =new Float(rs.getFloat("Y"));
                    dataList.add(x);
                    dataList.add(y);
                }
                
                rs.close();
                stmt.close();
                c.close();
            } 
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");

        return dataList;
    }

}

