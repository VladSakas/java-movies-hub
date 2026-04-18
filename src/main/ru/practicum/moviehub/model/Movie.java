package ru.practicum.moviehub.model;

public class Movie {
    private Integer id;
    private String title;
    private Integer year;

    public Movie(String title, Integer year) {
        this.title = title;
        this.year = year;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }
}