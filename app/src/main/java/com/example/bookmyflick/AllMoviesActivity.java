package com.example.bookmyflick;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AllMoviesActivity extends AppCompatActivity {

    private RecyclerView recyclerAllMovies;
    private MovieAdapter movieAdapter;
    private List<Movie> allMovies;

    private final BroadcastReceiver moviesChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MovieDbHelper db = new MovieDbHelper(AllMoviesActivity.this);
            allMovies = new ArrayList<>(db.readAll());
            movieAdapter.updateList(new ArrayList<>(allMovies));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_movies);

        setupList();
        setupData();
        setupSearch();
        setupNavigation();
    }

    private void setupList() {
        recyclerAllMovies = findViewById(R.id.recycler_all_movies);
        recyclerAllMovies.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void setupData() {
        // Load all movies from DB
        MovieDbHelper db = new MovieDbHelper(this);
        allMovies = new ArrayList<>(db.readAll());
        movieAdapter = new MovieAdapter(this, new ArrayList<>(allMovies), R.layout.item_movie_grid);
        recyclerAllMovies.setAdapter(movieAdapter);
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.search_view_all);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("search movies");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAllMovies(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAllMovies(newText);
                return true;
            }
        });
    }

    private void setupNavigation() {
        // Bottom nav actions
        View btnHome = findViewById(R.id.btn_nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> finish()); // go back to home (MainActivity)
        }

        View btnSearch = findViewById(R.id.btn_nav_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                SearchView sv = findViewById(R.id.search_view_all);
                if (sv != null) sv.requestFocusFromTouch();
            });
        }

        // Back button (top-left)
        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> onBackPressed());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list from DB in case Admin changed data
        MovieDbHelper db = new MovieDbHelper(this);
        allMovies = new ArrayList<>(db.readAll());
        movieAdapter.updateList(new ArrayList<>(allMovies));
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AdminActivity.ACTION_MOVIES_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(moviesChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(moviesChangedReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(moviesChangedReceiver);
    }

    private void filterAllMovies(String query) {
        String normalized = query == null ? "" : query.toLowerCase().trim();
        List<Movie> filtered = new ArrayList<>();
        if (TextUtils.isEmpty(normalized)) {
            filtered.addAll(allMovies);
        } else {
            for (Movie movie : allMovies) {
                if (movie.getTitle().toLowerCase().contains(normalized)) {
                    filtered.add(movie);
                }
            }
        }
        movieAdapter.updateList(filtered);
    }

}


