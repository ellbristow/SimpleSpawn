package me.ellbristow.SimpleSpawn;

import java.io.File;
import java.sql.*;
import java.util.HashMap;

public class SQLBridge {

    private static SimpleSpawn plugin;
    private Connection conn;
    private File sqlFile;
    private Statement statement;
    private HashMap<Integer, HashMap<String, Object>> rows = new HashMap<Integer, HashMap<String, Object>>();
    private int numRows = 0;
    
    public SQLBridge (SimpleSpawn instance) {
        plugin = instance;
        sqlFile = new File(plugin.getDataFolder() + File.separator + plugin.getName() + ".db");
    }
    
    public synchronized Connection getConnection() {
        if (conn == null) {
            return open();
        }
        return conn;
    }
    
    public synchronized Connection open() {    	
    	plugin.getLogger().fine("sqlite connection needs to be established");
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + sqlFile.getAbsolutePath());
            return conn;
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        }
        return null;
    }
    
    public synchronized void close() {
        if (conn != null) {
        	plugin.getLogger().fine("sqlite connection needs to be closed");
            try {
                conn.close();
            } catch (Exception e) {
                plugin.getLogger().severe(e.getMessage());
            }
        }
    }
    
    public boolean checkTable(String tableName) {
        DatabaseMetaData dbm;
        try {
            dbm = getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, tableName, null);
            if (tables.next()) {
            	tables.close();
                return true;
            }
            else {
            	tables.close();
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
            return false;
        }
    }
    
    public boolean createTable(String tableName, String[] columns, String[] dims) {
        try {
            statement = getConnection().createStatement();
            String query = "CREATE TABLE " + tableName + "(";
            for (int i = 0; i < columns.length; i++) {
                if (i!=0) {
                    query += ",";
                }
                query += columns[i] + " " + dims[i];
            }
            query += ")";
            statement.execute(query);
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        } 
        return true;
    }

    public ResultSet query(String query) {        
        ResultSet results = null;
        try {
            statement = getConnection().createStatement();
            results = statement.executeQuery(query);
            return results;
        } catch (Exception e) {
            if (!e.getMessage().contains("not return ResultSet") || (e.getMessage().contains("not return ResultSet") && query.startsWith("SELECT"))) {
                plugin.getLogger().severe(e.getMessage());
            }
            try {
                results.close();
                statement.close();
            } catch (Exception ex) {
            }
        }
        if (results != null) {
            try {
                results.close();
                statement.close();
            } catch (Exception ex) {
            }
        }
        return null;
    }
    
    public HashMap<Integer, HashMap<String, Object>> select(String fields, String tableName, String where, String group, String order) {
        if ("".equals(fields) || fields == null) {
            fields = "*";
        }
        String query = "SELECT " + fields + " FROM " + tableName;
        if (!"".equals(where) && where != null) {
            query += " WHERE " + where;
        }
        if (!"".equals(group) && group != null) {
            query += " GROUP BY " + group;
        }
        if (!"".equals(order) && order != null) {
            query += " ORDER BY " + order;
        }

        ResultSet results = null;
        try {
            rows.clear();
            numRows = 0;
            results = query(query);
                        
            if (results != null) {
                int columns = results.getMetaData().getColumnCount();
                String columnNames = "";
                for (int i = 1; i <= columns; i++) {
                    if (!"".equals(columnNames)) {
                        columnNames += ",";
                    }
                    columnNames += results.getMetaData().getColumnName(i);
                }
                String[] columnArray = columnNames.split(",");
                numRows = 0;
                while (results.next()) {
                    HashMap<String, Object> thisColumn = new HashMap<String, Object>();
                    for (String columnName : columnArray) {
                        thisColumn.put(columnName, results.getObject(columnName));
                    }
                    rows.put(numRows, thisColumn);
                    numRows++;
                }
                results.close();
                statement.close();
                return rows;
            } else {
                return null;
            }
        } catch (Exception e) { 
            plugin.getLogger().severe(e.getMessage());
            if (results != null) {
                try {
                    results.close();
                    statement.close();
                } catch (Exception ex) {
                }
            }
        }
        return null;
    }
}
