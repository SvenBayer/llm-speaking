package de.sven.bayer.speaking_llm.component;

import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// In TextSplitter.java
@Component
public class TextSplitter {

    public List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                //sentence = "╚╚╚╚" + sentence + "╚╚╚╚";
                sentences.add(sentence);
            }
        }
        return sentences;
    }
}
