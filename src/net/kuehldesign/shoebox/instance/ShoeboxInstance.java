package net.kuehldesign.shoebox.instance;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;
import net.kuehldesign.shoebox.exception.UnableToConnectToDatabaseException;
import net.kuehldesign.shoebox.exception.UnableToInitializeInstanceHereException;

public class ShoeboxInstance {
    private File directory;
    private Connection connection;
    
    // constructor
    public ShoeboxInstance(File directory) {
        this.directory = directory;
        
        if (instanceExistsHere()) {
            // TODO: load instance information
        }
    }
    
    // public methods
    public boolean instanceExistsHere() {
        return getDatabaseFile().exists();
    }
    
    public void initialize() throws InstanceAlreadyExistsHereException, UnableToInitializeInstanceHereException {
        if (instanceExistsHere()) {
            throw new InstanceAlreadyExistsHereException();
        }
        
        try {
            establishConnection();
        } catch (UnableToConnectToDatabaseException ex) {
            ex.printStackTrace();
            throw new UnableToInitializeInstanceHereException();
        }
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // private methods
    private void establishConnection() throws UnableToConnectToDatabaseException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDatabaseFile().getAbsolutePath());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new UnableToConnectToDatabaseException();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new UnableToConnectToDatabaseException();
        }
    }
    
    private Connection getConnection() {
        return connection;
    }
    
    private File getDatabaseFile() {
        return new File(getDirectoryPath() + "shoebox.db");
    }
    
    private String getDirectoryPath() {
        return directory.getAbsolutePath() + File.separator;
    }
}
