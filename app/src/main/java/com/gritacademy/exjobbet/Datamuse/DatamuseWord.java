package com.gritacademy.exjobbet.Datamuse;

public class DatamuseWord {
    private String word; // The word returned by the API
    private float score; // Score indicating relevance (optional)

    // Getters and setters
    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}