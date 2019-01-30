package reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class TheReader {
    private static class Counter implements Runnable {
        private String text;
        private String word1;
        private String word2;
        private Counter(String text, String word1, String word2) {
            this.text = text;
            this.word1 = word1;
            this.word2 = word2;
        }
        @Override
        public void run() {
            Scanner reader = new Scanner(text);
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String lowCase = line.toLowerCase();
                int word1Counter = 0;
                int word2Counter = 0;
                int commaCounter = 0;
                int word1Increments = countWordOccurrence(word1, lowCase);
                int word2Increments = countWordOccurrence(word2, lowCase);
                for (int i = 0; i < word1Increments; i++) {
                    word1Counter++;
                }
                for (int i = 0; i < word2Increments; i++) {
                    word2Counter++;
                }
                for (char c : line.toCharArray()) {
                    if (c == ',') {
                        commaCounter++;
                    }
                }
                TheReader.word1Counter.addAndGet(word1Counter);
                TheReader.word2Counter.addAndGet(word2Counter);
                TheReader.commaCounter.addAndGet(commaCounter);
            }
        }
    }
    private static AtomicInteger word1Counter = new AtomicInteger(0);
    private static AtomicInteger word2Counter = new AtomicInteger(0);
    private static AtomicInteger commaCounter = new AtomicInteger(0);

    static void processText(String filePath, int numberOfThreads, String word1, String word2) {
        long start = System.currentTimeMillis();
        File file = new File(filePath);
        StringBuilder builder = new StringBuilder();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                builder.append("\n").append(sc.nextLine());
            }
            int partStart = 0;
            int partEnd = builder.length() / numberOfThreads;
            while (partEnd < builder.length()) {
                while (!(partEnd > builder.length()) && builder.charAt(partEnd) != ' ') {
                    partEnd++;
                }
                String text = builder.substring(partStart, partEnd);
                executor.execute(new Counter(text, word1, word2));
                partStart = partEnd;
                partEnd *= 2;
            }
            String text = builder.substring(partStart);
            executor.execute(new Counter(text, word1, word2));
        } catch (FileNotFoundException ex) {
            System.out.println("File not found.");
        }
        executor.shutdown();
        while (!executor.isTerminated()) {

        }
        System.out.println("Word 1 occurrence: " + word1Counter);
        System.out.println("Word 2 occurrence: " + word2Counter);
        System.out.println("Commas: " + commaCounter);
        System.out.println(System.currentTimeMillis() - start);

    }

    private static int countWordOccurrence(String word, String line) {
        int wordCounter = 0;
        // if line contains word
        boolean isContained;
        do {
            isContained = false;
            // and, unless word is the first word,
            if (line.contains(word)) {
                isContained = true;
                int wordIdx = line.indexOf(word);
                int indexAfterWord = wordIdx + word.length();
                if (wordIdx != 0 &&
                        // check if former character is not a letter
                        Character.toString(line.charAt(wordIdx - 1)).matches("\\p{L}+")) {
                    line = removeWord(line, wordIdx, indexAfterWord);
                    continue;
                }
                // and, unless word is the last word,
                if (indexAfterWord != line.length() &&
                        // check if next character is not a letter;
                        Character.toString(line.charAt(indexAfterWord)).matches("\\p{L}+")) {
                    line = removeWord(line, wordIdx, indexAfterWord);
                    continue;
                }
                // remove this occurrence of word from line
                line = removeWord(line, wordIdx, indexAfterWord);
                // add to counter of word
                wordCounter++;
            }
        } while (isContained);
        return wordCounter;
    }

    private static String removeWord(String string, int wordIdx, int indexAfterWord) {
        return string.substring(0, wordIdx).concat(string.substring(indexAfterWord));
    }
}
