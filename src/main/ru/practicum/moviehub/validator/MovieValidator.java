package ru.practicum.moviehub.validator;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieSpec;
import java.util.ArrayList;
import java.util.List;

public class MovieValidator {

    public static List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("Тело запроса не может быть пустым");
            return errors;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MovieSpec.MAX_TITLE_LENGTH) {
            errors.add("название не должно превышать " + MovieSpec.MAX_TITLE_LENGTH + " символов");
        }

        if (movie.getYear() == null) {
            errors.add("год фильма должен быть указан");
        } else {
            if (movie.getYear() < MovieSpec.MIN_YEAR) {
                errors.add("год должен быть не меньше " + MovieSpec.MIN_YEAR);
            }
            if (movie.getYear() > MovieSpec.getMaxYear()) {
                errors.add("год должен быть не больше " + MovieSpec.getMaxYear());
            }
        }

        return errors;
    }
}