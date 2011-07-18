package net.kuehldesign.shoebox.cli;

import java.io.File;
import java.sql.SQLException;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;
import net.kuehldesign.shoebox.exception.UnableToInitializeInstanceHereException;
import net.kuehldesign.shoebox.exception.UnableToLoadInstanceException;
import net.kuehldesign.shoebox.instance.ShoeboxInstance;

public class ShoeboxInterface {
    public static void main(String[] args) {
        String command = null;
        
        try {
            command = args[0];
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("You must specify a command. Try \"help\".");
            System.exit(100);
        }
        
        File workingDirectory = new File("");
        
        // is there an extra parameter for directory?
        if (args.length >= 2) {
            String newDirectory = args[1];
            
            if (newDirectory.endsWith(File.separator)) {
                newDirectory = newDirectory.substring(0, newDirectory.length() - 1);
            }
            
            workingDirectory = new File(newDirectory);
            
            if (workingDirectory.exists() && ! workingDirectory.isDirectory()) {
                System.err.println("The directory specified already exists as a file.");
                System.exit(101);
            }
            
            if (! workingDirectory.exists()) {
                workingDirectory.mkdirs();
            }
        }
        
        ShoeboxInstance instance = null;
        
        try {
            instance = new ShoeboxInstance(workingDirectory);
        } catch (UnableToLoadInstanceException ex) {
            System.err.println("This Shoebox instance is malformed.");
            System.exit(104);
        }
        
        // branch out for each possible command
        if (command.equals("init")) {
            try {
                instance.initialize();
            } catch (InstanceAlreadyExistsHereException ex) {
                System.err.println("There is already a Shoebox instance here.");
                System.exit(102);
            } catch (UnableToInitializeInstanceHereException ex) {
                System.err.println("Unable to initialize instance here.");
                System.exit(103);
            }
        } else if (command.equals("status")) {
            try {
                if (instance.instanceExistsHere()) {
                    System.out.println("No instance exists here.");
                } else if (! instance.isConfigured()) {
                    System.out.println("Instance exists but has not been configured.");
                } else {
                    System.out.println("Instance is configured.");
                    // TODO: more instance information
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.err.println("Unable to determine status of instance.");
                System.exit(105);
            }
        }
    }
}
