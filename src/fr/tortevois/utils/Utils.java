package fr.tortevois.utils;

import org.json.simple.JSONObject;

import java.io.File;

public final class Utils {

    public final static int OUT = 1;
    public final static int ERR = 2;

    /**
     * Clearing the console (not working in IntelliJ)
     */
    public static void clearScreen() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                Runtime.getRuntime().exec("cls");
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    /**
     * Print a trace on the console
     *
     * @param debug : this boolean flag is true if we want to print something
     * @param where : print on the OUT or ERR console
     * @param str   : the string to display
     */
    public static void printTrace(boolean debug, int where, String str) {
        if (debug) {
            switch (where) {
                case OUT:
                    System.out.println(str);
                    break;
                case ERR:
                    System.err.println(str);
                    break;
            }
        }
    }

    /**
     * Print a title
     *
     * @param str : the string to display
     */
    public static void printTitle(String str) {
        StringBuilder sb = new StringBuilder("-- " + str + " ");
        for (int i = sb.length(); i <= 50; i++) {
            sb.append("-");
        }
        System.out.println(sb.toString());
    }

    /**
     * Check if the String could be parsed into a Integer
     *
     * @param str : the string to analyse
     * @return The boolean status
     */
    public static boolean isNaN(String str) {
        try {
            Integer.parseInt(str);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Check if a file exists
     *
     * @param fileName : the filename to check
     * @return The boolean status
     */
    public static boolean isFileExist(String fileName) {
        File file = getFile(fileName);
        return (file.exists() && file.isFile());
    }

    /**
     * Get the filename with the absolute project path
     *
     * @param fileName : the filename to complete
     * @return The complete filename's string
     */
    private static String getPath(String fileName) {
        final File f = new File("");
        return f.getAbsolutePath() + File.separator + fileName;
    }

    /**
     * Get a new file instance of the filename parameter
     *
     * @param fileName : the filename to convert
     * @return The new file instance of the filename parameter
     */
    public static File getFile(String fileName) {
        return new File(getPath(fileName));
    }

    /**
     * Extract the command from a string command
     *
     * @param input : the string command to parse
     * @return The command
     */
    public static String parseCommand(String input) {
        String[] command = input.split(" ");
        return command[0];
    }

    /**
     * Extract the args from a string command
     *
     * @param input: the string command to parse
     * @return The args
     */
    public static String[] parseArgs(String input) {
        String[] command = input.split(" ");
        String[] args;
        if (command.length > 1) {
            args = new String[(command.length - 1)];
            System.arraycopy(command, 1, args, 0, command.length - 1);
        } else {
            args = new String[0];
        }
        return args;
    }

    /**
     * A funny loading progress bar
     */
    public static void loading() {
        /*
        try {
            System.out.print("Loading ");
            for (int i = 0; i < 10; i++) {
                Thread.sleep(250);
                System.out.print(".");
            }
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
        */
        System.out.print("Loading ..........");
        System.out.println("");
    }

    // JSON tools
    /**
     * Return an Integer from the JSON Object at key
     *
     * @param key  : The key to extract
     * @param json : The JSON Object to analyze
     * @return The integer value
     */
    public static int jsonGetToInteger(String key, JSONObject json) {
        return (int) ((long) json.get(key));
    }
}
