package fr.tortevois.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static fr.tortevois.utils.Utils.getFile;

public final class CSV {
    public final static String CSV_SEPARATOR = ";";
    public final static String CSV_EXTENSION = ".csv";

    /**
     * Read a CSV files
     *
     * @param fileName : the raw filename without the path
     * @return a list of all the lines read
     */
    public static List<String> readFile(String fileName) {
        List<String> result = new ArrayList<>();
        try {
            FileReader fr = new FileReader(getFile(fileName));
            BufferedReader br = new BufferedReader(fr);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                result.add(line);
            }
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            System.err.println("CSV FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("CSV IOException");
            e.printStackTrace();
        }
        return result;
    }
}
