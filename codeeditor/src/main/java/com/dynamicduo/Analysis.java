package com.dynamicduo;

public class Analysis {
    private String analysis = "";

    public Analysis(String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            analysis += messages[i] + "\n\n";
        }
    }

    public String getAnalysis() {
        return analysis;
    }

}
