package com.example.bookmyflick;

import java.io.Serializable;

/**
 * Immutable value object representing a movie item displayed in the app.
 */
public class Movie implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String title;
    private final int imageResId;
    private final String language;
    private final Integer detailImageResId; // optional secondary image for detail page
    private final String cast; // optional cast string
    private final Integer year; // optional release year
    private final String director; // optional director name
    private final Float rating; // optional rating 0..10

    /**
     * Creates a new Movie with minimal fields.
     */
    public Movie(String title, int imageResId, String language) {
        this(title, imageResId, language, null, null, null, null, null);
    }

    /**
     * Creates a new Movie with detail image and cast.
     */
    public Movie(String title, int imageResId, String language, Integer detailImageResId, String cast,
                 Integer year, String director, Float rating) {
        this.title = title;
        this.imageResId = imageResId;
        this.language = language;
        this.detailImageResId = detailImageResId;
        this.cast = cast;
        this.year = year;
        this.director = director;
        this.rating = rating;
    }

    public String getTitle() { return title; }
    public int getImageResId() { return imageResId; }
    public String getLanguage() { return language; }
    public Integer getDetailImageResId() { return detailImageResId; }
    public String getCast() { return cast; }
    public Integer getYear() { return year; }
    public String getDirector() { return director; }
    public Float getRating() { return rating; }
}
