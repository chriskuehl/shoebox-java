package net.kuehldesign.shoebox.instance;

public class ShoeboxTag {
    private String title;
    private int maxAge;
    private int deleteAfter;
    private boolean acceptsAll;
    private int id;
    
    public ShoeboxTag(String title, int maxAge, int deleteAfter, boolean acceptsAll) {
        this.title = title;
        this.maxAge = maxAge;
        this.deleteAfter = deleteAfter;
        this.acceptsAll = acceptsAll;
    }
    
    // getters
    public boolean acceptsAll() {
        return acceptsAll;
    }

    public int getDeleteAfter() {
        return deleteAfter;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public String getTitle() {
        return title;
    }
    
    public int getID() {
        return id;
    }

    // setters
    public void setID(int id) {
        this.id = id;
    }
    
    public void setAcceptsAll(boolean acceptsAll) {
        this.acceptsAll = acceptsAll;
    }

    public void setDeleteAfter(int deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
