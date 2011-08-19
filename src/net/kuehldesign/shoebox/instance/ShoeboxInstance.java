package net.kuehldesign.shoebox.instance;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;
import net.kuehldesign.shoebox.exception.UnableToConnectToDatabaseException;
import net.kuehldesign.shoebox.exception.UnableToFindFileException;
import net.kuehldesign.shoebox.exception.UnableToInitializeInstanceHereException;
import net.kuehldesign.shoebox.exception.UnableToLoadInstanceException;
import net.kuehldesign.shoebox.exception.UnableToMoveFileException;

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
            new File(getDirectoryPath() + "stored").mkdir();
            
            establishConnection();
            
            Statement statement = getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE meta (key TEXT NOT NULL UNIQUE, value TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE tags (title TEXT NOT NULL UNIQUE, max_age INTEGER NOT NULL DEFAULT 0, delete_after INTEGER NOT NULL DEFAULT 0, accept_all INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE files (date_added INTEGER NOT NULL, name TEXT NOT NULL UNIQUE, deleted INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE file_tags (file_id INTEGER NOT NULL, tag_id INTEGER NOT NULL)");
            
            addProperty("initialized", (new Date()).toString());
            
            statement.close();
        } catch (UnableToConnectToDatabaseException ex) {
            ex.printStackTrace();
            throw new UnableToInitializeInstanceHereException();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new UnableToInitializeInstanceHereException();
        }
    }
    
    public void addProperty(String key, String value) throws SQLException {
        PreparedStatement insertPropertyStatement = getConnection().prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)");

        insertPropertyStatement.setString(1, key);
        insertPropertyStatement.setString(2, value);

        insertPropertyStatement.executeUpdate();

        insertPropertyStatement.close();
    }
    
    public void setConfigured() throws SQLException {
        addProperty("configured", (new Date()).toString());
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
    
    public LinkedList<String> storeFile(File fileToStore) throws UnableToMoveFileException, SQLException {
        PreparedStatement storeFileStatement = getConnection().prepareStatement("INSERT INTO files (date_added, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmssS");
        String storeName = dateFormat.format(new Date());
        
        int currentTime = (int) ((new Date()).getTime() / 1000);
        
        storeFileStatement.setInt(1, currentTime);
        storeFileStatement.setString(2, storeName);
        
        storeFileStatement.executeUpdate();
        
        ResultSet insertKeys = storeFileStatement.getGeneratedKeys();
        
        if (! insertKeys.next()) {
            throw new SQLException("Unable to find inserted key.");
        }
        
        int fileID = insertKeys.getInt(1);
        
        insertKeys.close();
        storeFileStatement.close();
        
        File newFileLocation = new File(getDirectoryPath() + "stored/" + storeName);
        
        if (! fileToStore.renameTo(newFileLocation)) {
            throw new UnableToMoveFileException();
        }
        
        // now get the the file object for tags, etc
        ShoeboxStoredFile file = null;
        
        try {
            file = getStoredFileByID(fileID);
        } catch (UnableToFindFileException ex) {
            throw new SQLException("Unable to find file based on inserted key.");
        }
        
        LinkedList<String> tagsGiven = addTagsForFile(file);
        
        return tagsGiven;
    }
    
    public LinkedList<ShoeboxStoredFile> getFilesWithoutTags() throws SQLException {
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT rowid, * FROM files WHERE files.rowid NOT IN(SELECT file_tags.file_id FROM file_tags)");
        
        LinkedList<ShoeboxStoredFile> filesWithoutTags = new LinkedList();
        
        while (results.next()) {
            filesWithoutTags.add(getFileFromResults(results));
        }
        
        results.close();
        statement.close();
        
        return filesWithoutTags;
    }
    
    public LinkedList<Integer> getExpiredTags() throws SQLException {
        LinkedList<Integer> expiredTags = new LinkedList();
        
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT file_tags.rowid AS tag_uid FROM file_tags, files, tags WHERE files.rowid = file_tags.file_id AND files.deleted = 0 AND tag_id = tags.rowid AND " + ((int) (new Date().getTime() / 1000)) + " > tags.delete_after + files.date_added");
        
        while (results.next()) {
            expiredTags.add(results.getInt("tag_uid"));
        }
        
        return expiredTags;
    }
    
    public int removeExpiredTags() throws SQLException {
        Statement statement = getConnection().createStatement();
        int expiredTags = statement.executeUpdate("DELETE FROM file_tags WHERE file_tags.rowid IN(SELECT file_tags.rowid AS tag_uid FROM file_tags, files, tags WHERE files.rowid = file_tags.file_id AND files.deleted = 0 AND tag_id = tags.rowid AND " + ((int) (new Date().getTime() / 1000)) + " > tags.delete_after + files.date_added)");
        statement.close();
        
        return expiredTags;
    }
    
    public void removeFile(ShoeboxStoredFile storedFile) throws SQLException {
        File file = new File(getDirectoryPath() + "stored/" + storedFile.getName());
        file.delete();
        
        Statement statement = getConnection().createStatement();
        statement.executeUpdate("DELETE FROM files WHERE rowid = " + storedFile.getID());
        statement.close();
    }
    
    public LinkedList<String> addTagsForFile(ShoeboxStoredFile file) throws SQLException {
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT rowid, * FROM tags");
        
        LinkedList<String> tagsGiven = new LinkedList();
        
        while (results.next()) {
            ShoeboxTag tag = new ShoeboxTag(results.getString("title"), results.getInt("max_age"), results.getInt("delete_after"), results.getBoolean("accept_all"));
            tag.setID(results.getInt("rowid"));
            
            boolean shouldGiveTag = false;
            
            if (tag.acceptsAll()) {
                shouldGiveTag = true;
            } else {
                // how old is the last file this tag was given to?
                Statement fileStatement = getConnection().createStatement();
                ResultSet fileResults = fileStatement.executeQuery("SELECT MAX(files.date_added) AS last_added FROM (file_tags LEFT JOIN files on((file_tags.file_id = files.rowid))) WHERE tag_id = " + tag.getID());
                
                if (fileResults.next()) {
                    int lastAdded = fileResults.getInt("last_added");
                    int currentTime = (int) ((new Date().getTime()) / 1000);
                    int age = currentTime - lastAdded;
                    
                    if (age > tag.getMaxAge()) {
                        shouldGiveTag = true;
                    }
                } else {
                    shouldGiveTag = true;
                }
                
                fileResults.close();
                fileStatement.close();
            }
            
            if (shouldGiveTag) {
                tagsGiven.add(tag.getTitle());
                addTag(file.getID(), tag.getID());
            }
        }
        
        results.close();
        statement.close();
        
        return tagsGiven;
    }
    
    public void addTag(int fileID, int tagID) throws SQLException {
        Statement statement = getConnection().createStatement();
        statement.executeUpdate("INSERT INTO file_tags (file_id, tag_id) VALUES (" + fileID + ", " + tagID + ")");
        statement.close();
    }
    
    public ShoeboxStoredFile getStoredFileByID(int fileID) throws SQLException, UnableToFindFileException {
        Statement statement = getConnection().createStatement();
        ResultSet results = statement.executeQuery("SELECT rowid, * FROM files WHERE rowid = " + fileID);
        
        if (! results.next()) {
            throw new UnableToFindFileException();
        }
        
        ShoeboxStoredFile storedFile = getFileFromResults(results);
        
        results.close();
        statement.close();
        
        return storedFile;
    }
    
    // private methods
    private ShoeboxStoredFile getFileFromResults(ResultSet results) throws SQLException {
        long addedEpoch = results.getInt("date_added");
        addedEpoch = addedEpoch * 1000;
        Date addedOn = new Date(addedEpoch);
        
        String name = results.getString("name");
        boolean deleted = results.getBoolean("deleted");
        
        return new ShoeboxStoredFile(results.getInt("rowid"), addedOn, name, deleted);
    }
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
