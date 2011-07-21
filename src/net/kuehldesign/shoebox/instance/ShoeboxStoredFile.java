package net.kuehldesign.shoebox.instance;

import java.util.Date;

public class ShoeboxStoredFile {
    private int id;
    private Date addedOn;
    private String name;
    private boolean deleted;
    
    public ShoeboxStoredFile(int id, Date addedOn, String name, boolean deleted) {
        this.id = id;
        this.addedOn = addedOn;
        this.name = name;
        this.deleted = deleted;
    }
    
    // getters
    public Date getAddedOn() {
        return addedOn;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }
}
