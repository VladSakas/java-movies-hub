package ru.practicum.moviehub.model;

import java.time.Year;

public class MovieSpec {
    public static final int MIN_YEAR = 1888;
    public static final int MAX_TITLE_LENGTH = 100;

    public static int getMaxYear() {
        return Year.now().getValue() + 1;
    }
}