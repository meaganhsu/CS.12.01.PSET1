package com.example.cs1201pset;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    static TextView fileName;
    static TextView wordCountLabel;
    static TextView wordCountNum;
    static TextView sentCountLabel;
    static TextView sentCountNum;
    static TextView uniqueWordsLabel;
    static TextView uniqueWordsNum;
    static TextView uniqueWordsList;
    static TextView top5Label;
    static ListView top5List;
    static TextView wordPairLabel;
    static ListView wordPairList;
    static ScrollView parent;
    static ScrollView child;
    static int wordCount;
    static int sentenceCount;
    static int uniqueCount;
    static ArrayList<String> messyTxt;       // unorganised arraylist of text (each line is an item)
    static ArrayList<String> text;     // arraylist of ALL words in the file
    static ArrayList<Integer> frequencies;     // parallel arraylist of the frequencies of each word
    static ArrayList<String> words;        // parallel arraylist of all possible words in the file
    static String unique;         // string of words with the frequency of 1 (unique)
    static ArrayList<String[]> wordPairs;       // arraylist of all consecutive word pairs
    static ArrayList<Integer> pairFreq;       // parallel arraylist of frequency of word pairs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sentenceCount = 0;
        text = new ArrayList<>();
        words = new ArrayList<>();
        frequencies = new ArrayList<>();
        messyTxt = new ArrayList<>();
        AssetManager assets = this.getAssets();
        wordPairs = new ArrayList<>();
        pairFreq = new ArrayList<>();

        // files are read differently depending on file type (txt or pdf)
        if (StartingScreen.getFileName().endsWith("pdf")) {
            // https://www.geeksforgeeks.org/how-to-extract-data-from-pdf-file-in-android/
            try {
                PdfReader reader = new PdfReader(StartingScreen.getFileName());
                int n = reader.getNumberOfPages();

                for (int i = 0; i < n; i++) {
                    messyTxt.add(PdfTextExtractor.getTextFromPage(reader, i+1).trim() + "\n");
                }

                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(assets.open(StartingScreen.getFileName()));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    messyTxt.add(line);
                }

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

//        analyse();
//        sortWordPairs();

        Thread thread = new Thread() {
            @Override
            public void run() {
                analyse();
                sortWordPairs();
            }
        };

        thread.start();

        // initialising all components
        fileName = findViewById(R.id.fileName);
        fileName.setText(StartingScreen.getFileName());     // getting user-selected file name

        parent = findViewById(R.id.parent);
        child = findViewById(R.id.child);

        parent.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                Log.v("PARENT", "PARENT TOUCH");
                findViewById(R.id.child).getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });

        child.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                Log.v("CHILD", "CHILD TOUCH");
                // Disallow the touch request for parent scroll on touch of
                // child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        wordCountLabel = findViewById(R.id.wordCountLabel);
        sentCountLabel = findViewById(R.id.sentCountLabel);
        uniqueWordsLabel = findViewById(R.id.uniqueWordsLabel);
        top5Label = findViewById(R.id.top5Label);
        wordPairLabel = findViewById(R.id.wordPairLabel);

        wordCountNum = findViewById(R.id.wordCountNum);
        sentCountNum = findViewById(R.id.sentCountNum);
        uniqueWordsNum = findViewById(R.id.uniqueWordsNum);

        wordCountNum.setText(Integer.toString(wordCount));
        sentCountNum.setText(Integer.toString(sentenceCount));
        uniqueWordsNum.setText(Integer.toString(uniqueCount));

        // unique words list view
        unique = "";
        for (int i = 0 ; i < frequencies.size(); i++) {
            if (frequencies.get(i) != 1) break;       // only including words that appear once in the file
            else unique += (i+1) + ". " + words.get(i) + "\n\n";
        }
        uniqueWordsList = findViewById(R.id.uniqueWordsList);
        uniqueWordsList.setText(unique);

        // most common words list view
        top5List = findViewById(R.id.top5List);
        String[] mostCommon5 = new String[5];

        int x = 0;
        for (int i = words.size()-1; i > words.size()-6; i--) {
            mostCommon5[x] = (x+1) + ". " + words.get(i) + "     [" + frequencies.get(i) + " occurrences.]";
            x++;
        }
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mostCommon5);
        top5List.setAdapter(adapter2);

        // word pair list view
        x = 0;
        wordPairList = findViewById(R.id.wordPairList);
        String[] wordPair5 = new String[5];
        for (int i = wordPairs.size()-1; i > wordPairs.size()-6; i--) {
            String temp = Arrays.toString(wordPairs.get(i)).substring(1,Arrays.toString(wordPairs.get(i)).length()-1);
            wordPair5[x] = (x+1) + ". " + temp + "     [" + pairFreq.get(i) + " occurrences.]";
            x++;
        }

        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, wordPair5);
        wordPairList.setAdapter(adapter3);
    }

    private void analyse() {
        // turning messy text (each item is a line) into text array (each item is a word)
        String line;
        for (int j = 0; j < messyTxt.size(); j++) {     // reading txt into arraylist
            line = messyTxt.get(j);
            sentenceCount += countSentences(line);

            line = line.replaceAll("[^\\w\\s'-]", "").toLowerCase();

            String[] temp = line.split("\\s+");
            for (int i = 0; i < temp.length; i++) {
                // credit: https://www.baeldung.com/java-string-number-presence
                if (temp[i].isEmpty() || temp[i].matches(".*\\d.*")) continue;
                if (temp[i].equals("i")) temp[i] = "I";

                text.add(temp[i]);
            }
        }

        wordCount = text.size();

        // if excluding common words, then common words will not be included in unique words
        if (StartingScreen.getExcludeCW()) {
            String[] commonWords = {
                    "the", "of", "to", "and", "a", "in", "is", "it", "you", "that",
                    "he", "was", "for", "on", "are", "with", "as", "I", "his", "they",
                    "be", "at", "one", "have", "this", "from", "or", "had", "by", "not",
                    "but", "what", "we", "can", "out", "other", "were", "all", "there",
                    "when", "up", "your", "how", "said", "an", "each", "she", "which",
                    "do", "their", "if", "will", "way", "about", "many", "then", "them",
                    "would", "like", "so", "these", "her", "thing", "him", "has", "more",
                    "could", "go", "come", "did", "no", "most", "my", "know", "than",
                    "who", "may", "been", "now"};
            ArrayList<String> clean = new ArrayList<>();

            for (int i = 0; i < text.size(); i++) {
                if (!isCW(text.get(i), commonWords)) clean.add(text.get(i));
            }

            text.clear();
            text = clean;
        }

        // counting frequencies of unique words
        int x = 0;
        for (int i = 0; i < text.size(); i++) {
            if (!words.contains(text.get(i))) {
                words.add(text.get(i));
                frequencies.add(1);
            } else {
                x = frequencies.get(words.indexOf(text.get(i))) + 1;
                frequencies.set(words.indexOf(text.get(i)), x);
            }
        }

        // sort parallel arrays
        for (int i = 0; i < frequencies.size(); i++) {
            for (int j = 0; j < frequencies.size()-1; j++) {
                if (frequencies.get(j) > frequencies.get(j+1)) {
                    int temp = frequencies.get(j);
                    frequencies.set(j, frequencies.get(j+1));
                    frequencies.set(j+1, temp);

                    String temp2 = words.get(j);
                    words.set(j, words.get(j+1));
                    words.set(j+1, temp2);
                }
            }
        }

        uniqueCount = Collections.frequency(frequencies, 1);    // frequency of unique words (words only repeating once/non-repeating)
    }

    private void sortWordPairs() {
        // populating array
        for (int i = 0; i < text.size()-1; i++) {
            String[] pair = {text.get(i), text.get(i+1)};
            int index = findIndex(wordPairs, pair);

            if (index == -1) {     // new word pair
                wordPairs.add(pair);
                pairFreq.add(1);
            } else pairFreq.set(index, pairFreq.get(index)+1);      // add 1 to frequency for existing pair
        }

        // sorting parallel arrays
        for (int i = 0; i < wordPairs.size(); i++) {
            for (int j = 0; j < wordPairs.size()-1; j++) {
                if (pairFreq.get(j) > pairFreq.get(j+1)) {
                    int temp = pairFreq.get(j);
                    pairFreq.set(j, pairFreq.get(j+1));
                    pairFreq.set(j+1, temp);

                    String[] temp2 = wordPairs.get(j);
                    wordPairs.set(j, wordPairs.get(j+1));
                    wordPairs.set(j+1, temp2);
                }
            }
        }
    }

    // helper methods
    private int findIndex(ArrayList<String[]> wordPairs, String[] pair) {
        for (int i = 0; i < wordPairs.size(); i++) {
            if (Arrays.equals(wordPairs.get(i), pair)) return i;
        }

        return -1;   // if pair doesn't exist
    }

    private boolean isCW(String word, String[] commonWords) {
        for (int j = 0; j < commonWords.length; j++) {
            if (word.equals(commonWords[j])) return true;   // checks if a word is a common word
        }

        return false;
    }

    private int countSentences(String str) {
        int cnt = 0;
        boolean sentence = false;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '!' || str.charAt(i) == '?' || str.charAt(i) == '.') {
                if (sentence) {
                    cnt++;
                    sentence = false;   // now outside of sentence
                }

                // checking for consecutive punctuation (eg. ?!)
                while (i + 1 < str.length() && (str.charAt(i+1) == '!' || str.charAt(i+1) == '?' || str.charAt(i+1) == '.')) {
                    i++;  // skipping to next char
                }
            } else if (!Character.isWhitespace(str.charAt(i))) sentence = true;
        }

        if (sentence) cnt++;      // if last char was part of a sentence

        return cnt;
    }
}