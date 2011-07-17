package net.kuehldesign.shoebox.instance;

import java.io.File;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;

public class ShoeboxInstance {
    File directory;
    
    // constructor
    public ShoeboxInstance(File directory) {
        this.directory = directory;
    }
    
    // public methods
    public boolean instanceExistsHere() {
        return getDatabaseFile().exists();
    }
    
    public void initialize() throws InstanceAlreadyExistsHereException {
        if (instanceExistsHere()) {
            throw new InstanceAlreadyExistsHereException();
        }
    }
    
    // private methods
    private File getDatabaseFile() {
        return new File(getDirectoryPath() + "shoebox.db");
    }
    
    private String getDirectoryPath() {
        return directory.getAbsolutePath() + File.separator;
    }
}
