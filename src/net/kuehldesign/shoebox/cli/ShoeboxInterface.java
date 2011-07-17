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
        
        File workingDirectory = new File(".");
        
        if (command.equals("init")) {
            System.out.println("current directory: " + workingDirectory.getAbsolutePath());
        }
    }
}
