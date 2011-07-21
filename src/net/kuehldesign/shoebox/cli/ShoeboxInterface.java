package net.kuehldesign.shoebox.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import net.kuehldesign.shoebox.exception.InstanceAlreadyExistsHereException;
import net.kuehldesign.shoebox.exception.UnableToInitializeInstanceHereException;
import net.kuehldesign.shoebox.exception.UnableToLoadInstanceException;
import net.kuehldesign.shoebox.exception.UnableToReadFromConsoleException;
import net.kuehldesign.shoebox.instance.ShoeboxInstance;
import net.kuehldesign.shoebox.instance.ShoeboxTag;

public class ShoeboxInterface {
    public static void main(String[] args) {
        String command = null;
        
        try {
            command = args[0];
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("You must specify a command. Try \"help\".");
            System.exit(100);
        }
        
        InputStreamReader inreader = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inreader);
        
        // branch out for each possible command
        if (command.equals("init")) {
            ShoeboxInstance instance = getInstance(args, 0);
            
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
            ShoeboxInstance instance = getInstance(args, 0);
            
            try {
                if (! instance.instanceExistsHere()) {
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
        } else if (command.equals("configure")) {
            ShoeboxInstance instance = getInstance(args, 0);
            
            try {
                if (! instance.instanceExistsHere()) {
                    System.err.println("No instance exists here.");
                    System.exit(106);
                } else {
                    instance.addTag(new ShoeboxTag("daily", (24 * 60 * 60), (7 * 24 * 60 * 60), true));
                    instance.addTag(new ShoeboxTag("weekly", (7 * 24 * 60 * 60), (30 * 24 * 60 * 60), false));
                    instance.addTag(new ShoeboxTag("monthly", (30 * 24 * 60 * 60), (6 * 30 * 24 * 60 * 60), false));
                    instance.addTag(new ShoeboxTag("biannually", (6 * 30 * 24 * 60 * 60), 0, false));
                    
                    instance.setConfigured();
                    
                    System.out.println("Instance has been configured with default tags. Use tags command to view existing tags.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.err.println("Unable to configure instance.");
                System.exit(107);
            }
        } else if (command.equals("tags")) {
            ShoeboxInstance instance = getInstance(args, 0);
            
            try {
                if (! instance.instanceExistsHere()) {
                    System.err.println("No instance exists here.");
                    System.exit(107);
                } else {
                    LinkedList<ShoeboxTag> tags = instance.getTags();
                    
                    //System.out.println("Title\t\tMax Age\t\tDelete After\t\tAccept All Snapshots");
                    //System.out.println("=====\t\t=======\t\t============\t\t=====================");
                    
                    for (ShoeboxTag tag : tags) {
                        System.out.println(tag.getTitle() + " (" + tag.getID() + "): max_age=" + tag.getMaxAge() + ", delete_after=" + tag.getDeleteAfter() + ", accept_all=" + (tag.acceptsAll() ? "yes" : "no"));
                    }
                    
                    System.out.println("");
                    System.out.println("What would you like to do?");
                    System.out.println("  1) Add another tag");
                    System.out.println("  2) Delete an existing tag");
                    System.out.println("  3) Modify an existing tag");
                    
                    System.out.println("  0) Exit");
                    
                    int choice = getIntFromConsole(reader, "Enter your choice:");
                    
                    switch (choice) {
                        case 1:
                            String title = getLineFromConsole(reader, "Enter the tag title:");
                            int maxAge = getIntFromConsole(reader, "Enter the tag max age (in seconds):");
                            int deleteAfter = getIntFromConsole(reader, "Enter the time to delete snapshots after (in seconds):");
                            
                            String response = "";
                            
                            while (! response.equals("y") && ! response.equals("n")) {
                                response = getLineFromConsole(reader, "Should new snapshots always be given this tag? [y/n]:");
                            }
                            
                            boolean alwaysGive = response.equals("y");
                            instance.addTag(new ShoeboxTag(title, maxAge, deleteAfter, alwaysGive));
                            
                            System.out.println("Tag added.");
                        break;
                        
                        case 2:
                            System.out.println("Which tag do you want to delete?");
                            int tagToDelete = getIntFromConsole(reader, "Enter the tag ID:");
                            
                            if (tagToDelete > 0) {
                                instance.deleteTag(tagToDelete);
                                System.out.println("Tag deleted.");
                            }
                        break;
                            
                        case 3:
                            System.out.println("Which tag do you want to modify?");
                            int tagToModify = getIntFromConsole(reader, "Enter the tag ID:");
                            
                            if (tagToModify > 0) {
                                ShoeboxTag tag = instance.getTag(tagToModify);
                                
                                if (tag == null) {
                                    System.err.println("No tag found with that ID.");
                                    System.exit(109);
                                }
                                
                                String newTitle = getLineFromConsole(reader, "Enter the new tag title [" + tag.getTitle() + "]:");
                                int newMaxAge = getIntFromConsole(reader, "Enter the new tag max age (in seconds) [" + tag.getMaxAge() + "]:");
                                int newDeleteAfter = getIntFromConsole(reader, "Enter the new time to delete snapshots after (in seconds) [" + tag.getDeleteAfter() + "]:");

                                String newResponse = "";

                                while (! newResponse.equals("y") && ! newResponse.equals("n")) {
                                    newResponse = getLineFromConsole(reader, "Should new snapshots always be given this tag? [y/n] [" + (tag.acceptsAll() ? "y" : "n") + "]:");
                                }

                                boolean newAlwaysGive = newResponse.equals("y");
                                instance.updateTag(tagToModify, newTitle, newMaxAge, newDeleteAfter, newAlwaysGive);

                                System.out.println("Tag modified.");
                            }
                        break;
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                System.err.println("Unable to find tags.");
                System.exit(107);
            } catch (UnableToReadFromConsoleException ex) {
                ex.printStackTrace();
                System.err.println("Unable to read from console.");
                System.exit(108);
            }
        } else {
            System.out.println("Unknown command. Try help.");
        }
    }
    
    private static String getLineFromConsole(BufferedReader reader, String prompt) throws UnableToReadFromConsoleException {
        String line = null;

        while (line == null || line.length() <= 0) {
            System.out.print(prompt + " ");

            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new UnableToReadFromConsoleException();
            }
        }

        return line;
    }
    
    public static int getIntFromConsole(BufferedReader reader, String prompt) throws UnableToReadFromConsoleException {
        while (true) {
            try {
                int read = Integer.valueOf(getLineFromConsole(reader, prompt));
                return read;
            } catch (NumberFormatException ex) {
                System.err.println("That wasn't expected. Please try again.");
            }
        }
    }
    
    public static ShoeboxInstance getInstance(String[] args, int extraParams) {
        File workingDirectory = new File("");
        
        // is there an extra parameter for directory?
        if (args.length >= (2 + extraParams)) {
            String newDirectory = args[1];
            
            if (newDirectory.endsWith(File.separator)) {
                newDirectory = newDirectory.substring(0, newDirectory.length() - 1);
            }
            
            workingDirectory = new File(newDirectory);
            
            if (workingDirectory.exists() && ! workingDirectory.isDirectory()) {
                System.err.println("The directory specified already exists as a file.");
                System.exit(101);
            }
        }
        
        ShoeboxInstance instance = null;
        
        try {
            instance = new ShoeboxInstance(workingDirectory);
        } catch (UnableToLoadInstanceException ex) {
            System.err.println("This Shoebox instance is malformed.");
            System.exit(104);
        }
        
        return instance;
    }
}
