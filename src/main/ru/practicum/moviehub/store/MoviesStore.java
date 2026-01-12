package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Long, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicLong currentId = new AtomicLong(1);

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public Movie findById(long id) {
        return movies.get(id);
    }

    public Movie save(Movie movie) {
        long id = currentId.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public boolean deleteById(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        currentId.set(1);
    }
}