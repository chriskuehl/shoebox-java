package net.kuehldesign.shoebox.instance;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;
import net.kuehldesign.shoebox.exception.UnableToConnectToDatabaseException;
import net.kuehldesign.shoebox.exception.UnableToInitializeInstanceHereException;
import net.kuehldesign.shoebox.exception.UnableToLoadInstanceException;

public class ShoeboxInstance {
    private File directory;
    private Connection connection;
    
    // constructor
    public ShoeboxInstance(File directory) throws UnableToLoadInstanceException {
        this.directory = directory;
        
        if (instanceExistsHere()) {
            try {
                establishConnection();
                // TODO: load instance information
            } catch (UnableToConnectToDatabaseException ex) {
                ex.printStackTrace();
                throw new UnableToLoadInstanceException();
            }
        }
    }
    
    // public methods
    public boolean instanceExistsHere() {
        return getDatabaseFile().exists();
    }
    
    public boolean isConfigured() throws SQLException {
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT * FROM meta WHERE key = 'configured'");
        
        boolean configured = results.next();
        
        results.close();
        statement.close();
        
        return configured;
    }
    
    public void initialize() throws InstanceAlreadyExistsHereException, UnableToInitializeInstanceHereException {
        if (instanceExistsHere()) {
            throw new InstanceAlreadyExistsHereException();
        }
        
        try {
            new File(getDirectoryPath()).mkdirs();
            
            establishConnection();
            
            Statement statement = getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE meta (key TEXT NOT NULL UNIQUE DEFAULT '', value TEXT NOT NULL DEFAULT '')");
            statement.executeUpdate("CREATE TABLE tags (title TEXT NOT NULL UNIQUE DEFAULT '', max_age INTEGER NOT NULL DEFAULT 0, delete_after INTEGER NOT NULL DEFAULT 0, accept_all INTEGER NOT NULL DEFAULT 0)");
            
            PreparedStatement insertPropertyStatement = getConnection().prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)");
            
            insertPropertyStatement.setString(1, "initialized");
            insertPropertyStatement.setString(2, (new Date()).toString());
            
            insertPropertyStatement.executeUpdate();
            
            insertPropertyStatement.close();
            statement.close();
        } catch (UnableToConnectToDatabaseException ex) {
            ex.printStackTrace();
            throw new UnableToInitializeInstanceHereException();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new UnableToInitializeInstanceHereException();
        }
    }
    
    public void setConfigured() throws SQLException {
        PreparedStatement insertPropertyStatement = getConnection().prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)");

        insertPropertyStatement.setString(1, "configured");
        insertPropertyStatement.setString(2, (new Date()).toString());

        insertPropertyStatement.executeUpdate();
        insertPropertyStatement.close();
    }
    
    public void deleteTag(int tagIDToDelete) throws SQLException {
        Statement statement = getConnection().createStatement();
        statement.executeUpdate("DELETE FROM tags WHERE rowid = " + tagIDToDelete);
        statement.close();
    }
    
    public LinkedList<ShoeboxTag> getTags() throws SQLException {
        LinkedList<ShoeboxTag> tags = new LinkedList();
        
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT rowid, * FROM tags");
        
        while (results.next()) {
            ShoeboxTag tag = new ShoeboxTag(results.getString("title"), results.getInt("max_age"), results.getInt("delete_after"), results.getBoolean("accept_all"));
            tag.setID(results.getInt("rowid"));
            
            tags.add(tag);
        }
        
        results.close();
        statement.close();
        
        return tags;
    }
    
    public ShoeboxTag getTag(int tagID) throws SQLException {
        ShoeboxTag tag = null;
        
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT rowid, * FROM tags WHERE rowid = " + tagID);
        
        if (results.next()) {
            tag = new ShoeboxTag(results.getString("title"), results.getInt("max_age"), results.getInt("delete_after"), results.getBoolean("accept_all"));
            tag.setID(results.getInt("rowid"));
        }
        
        results.close();
        statement.close();
        
        return tag;
    }
    
    public void updateTag(int tagToModify, String newTitle, int newMaxAge, int newDeleteAfter, boolean newAcceptAll) throws SQLException {
        PreparedStatement updateTagStatement = getConnection().prepareStatement("UPDATE tags SET title = ?, max_age = ?, delete_after = ?, accept_all = ? WHERE rowid = ?");
        
        updateTagStatement.setString(1, newTitle);
        updateTagStatement.setInt(2, newMaxAge);
        updateTagStatement.setInt(3, newDeleteAfter);
        updateTagStatement.setBoolean(4, newAcceptAll);
        updateTagStatement.setInt(5, tagToModify);
        
        updateTagStatement.executeUpdate();
        updateTagStatement.close();
    }
    
    public void addTag(ShoeboxTag tag) throws SQLException {
        PreparedStatement addTagStatement = getConnection().prepareStatement("INSERT INTO tags (title, max_age, delete_after, accept_all) VALUES (?, ?, ?, ?)");

        addTagStatement.setString(1, tag.getTitle());
        addTagStatement.setInt(2, tag.getMaxAge());
        addTagStatement.setInt(3, tag.getDeleteAfter());
        addTagStatement.setBoolean(4, tag.acceptsAll());

        addTagStatement.executeUpdate();

        addTagStatement.close();
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
