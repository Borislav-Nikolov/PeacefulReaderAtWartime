package reader;

import java.io.IOException;

public class ReaderDemo {
    public static void main(String[] args) {
        try {
            TheReader.processText("WarAndPeace.txt", 7, "мир", "война");
        } catch (IOException ex) {
            System.out.println("There was a problem: " + ex.getMessage());
        }
    }
}
