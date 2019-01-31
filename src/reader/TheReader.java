package reader;

import java.io.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class TheReader {
    private static AtomicInteger word1Counter = new AtomicInteger(0);
    private static AtomicInteger word2Counter = new AtomicInteger(0);
    private static AtomicInteger commaCounter = new AtomicInteger(0);
    private static ConcurrentHashMap<String, Integer> allWordsCount = new ConcurrentHashMap<>();

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
                addWordsToMap(lowCase);
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

    static void processText(String filePath, int numberOfThreads, String word1, String word2) throws IOException {
        long start = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringBuilder allText = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            allText.append("\n").append(line);
        }
        int partRange = allText.length() / numberOfThreads;
        int partStart = 0;
        int partEnd = partRange;
        while (partEnd < allText.length()) {
            while (!(partEnd > allText.length()) && allText.charAt(partEnd) != ' ') {
                partEnd++;
            }
            String text = allText.substring(partStart, partEnd);
            executor.execute(new Counter(text, word1, word2));
            partStart = partEnd;
            partEnd = partStart + partRange;
        }
        String text = allText.substring(partStart);
        executor.execute(new Counter(text, word1, word2));
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
        for (Map.Entry<String, Integer> wordsCount : allWordsCount.entrySet()) {
            System.out.println(wordsCount.getKey() + " - " + wordsCount.getValue());
        }
        System.out.println("Word 1 occurrence: " + word1Counter);
        System.out.println("Word 2 occurrence: " + word2Counter);
        System.out.println("Commas: " + commaCounter);
        createFilesForDiffWordLength();
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
                        Character.toString(line.charAt(wordIdx - 1)).matches("[\\p{L}+]")) {
                    line = removeWord(line, wordIdx, indexAfterWord);
                    continue;
                }
                // and, unless word is the last word,
                if (indexAfterWord != line.length() &&
                        // check if next character is not a letter;
                        Character.toString(line.charAt(indexAfterWord)).matches("[\\p{L}+]")) {
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
    private static void addWordsToMap(String line) {
        line = line.replaceAll("[\\pP]", " ");
        line = line.replaceAll("[\\d+]", " ");
        line = line.replaceAll(" +", " ").trim();
        String[] words = line.split( " ");
        for (String word : words) {
            if (!allWordsCount.containsKey(word)) {
                allWordsCount.put(word, 0);
            }
            allWordsCount.put(word, allWordsCount.get(word) + 1);
        }
    }

    private static void createFilesForDiffWordLength() throws IOException {
        for (String word : allWordsCount.keySet()) {
            String fileName = "" + word.length() + "-letterWords.txt";
            File file = new File(fileName);
            FileWriter fw = new FileWriter(file, true);
            fw.write(word);
            fw.write(String.format("%n"));
        }
    }
}
