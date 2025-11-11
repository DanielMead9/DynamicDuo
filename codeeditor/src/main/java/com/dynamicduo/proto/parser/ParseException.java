package com.dynamicduo.proto.parser;

/** Thrown when the parser hits unexpected tokens. */
public class ParseException extends Exception {
    private final int line;

    public ParseException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getLine() { return line; }
}
