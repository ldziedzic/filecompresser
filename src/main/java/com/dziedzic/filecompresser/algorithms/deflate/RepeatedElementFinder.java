package com.dziedzic.filecompresser.algorithms.deflate;

import com.google.common.collect.Multimap;

public class RepeatedElementFinder {
    private byte[] content;
    private Multimap<Integer, Integer> hashDictionary;
    private int i;
    private int key;
    private int maxMatchedElements;
    private int indexOfMatchedSubstring;

    public RepeatedElementFinder(byte[] content, Multimap<Integer, Integer> hashDictionary, int i, int key, int maxMatchedElements, int indexOfMatchedSubstring) {
        this.content = content;
        this.hashDictionary = hashDictionary;
        this.i = i;
        this.key = key;
        this.maxMatchedElements = maxMatchedElements;
        this.indexOfMatchedSubstring = indexOfMatchedSubstring;
    }

    public int getMaxMatchedElements() {
        return maxMatchedElements;
    }

    public int getIndexOfMatchedSubstring() {
        return indexOfMatchedSubstring;
    }

    public RepeatedElementFinder invoke() {
        int j = 0;
        for (Integer element : hashDictionary.get(key)) {
            int positionFromDictionary = element;
            int positionFromContent = i;
            int matchedElements = 0;

            int maxElementToSearch = 10000;
            if (j >= maxElementToSearch)
                break;

            int maxLength = 257; // RFC1951 limit
            while (positionFromContent < content.length && content[positionFromContent] == content[positionFromDictionary] && matchedElements < maxLength) {
                positionFromContent++;
                positionFromDictionary++;
                matchedElements++;

            }
            if (matchedElements > maxMatchedElements) {
                maxMatchedElements = matchedElements;
                indexOfMatchedSubstring = element;
            }
            j++;
        }
        return this;
    }
}
