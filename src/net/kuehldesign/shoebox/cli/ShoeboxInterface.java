package net.kuehldesign.shoebox.cli;

import java.io.File;

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
        
        if (command.equals("init")) {
            System.out.println("current directory: " + workingDirectory.getAbsolutePath());
        }
    }
}
