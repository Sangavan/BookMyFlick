package com.example.bookmyflick;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerNowShowing;
    private RecyclerView recyclerUpcoming;
    private MovieAdapter nowShowingAdapter;
    private MovieAdapter upcomingAdapter;

    private List<Movie> allNowShowingMovies;
    private List<Movie> allUpcomingMovies;

    // Secret Admin gate state
    private static final int ADMIN_TAP_THRESHOLD = 5;
    private static final long ADMIN_TAP_WINDOW_MS = 2000L;
    private int adminTapCount = 0;
    private long adminTapWindowStart = 0L;

    private final BroadcastReceiver moviesChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshFromDatabase();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure a prebuilt DB from assets is copied on first run (if provided)
        MovieDbHelper.copyPrebuiltDbIfNeeded(this);

        setupToolbar();
        setupLists();
        setupSearch();
        setupActions();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Hidden multi-tap on toolbar to open Admin (with PIN)
        toolbar.setOnClickListener(v -> handleAdminSecretTap());
    }

    private void setupLists() {
        recyclerNowShowing = findViewById(R.id.recycler_now_showing);
        recyclerUpcoming = findViewById(R.id.recycler_upcoming);

        recyclerNowShowing.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // initial data; actual refresh in onResume from DB if available
        allNowShowingMovies = limitToFive(getNowShowingMovies());
        allUpcomingMovies = limitToFive(getUpcomingMovies());

        nowShowingAdapter = new MovieAdapter(this, new ArrayList<>(allNowShowingMovies));
        upcomingAdapter = new MovieAdapter(this, new ArrayList<>(allUpcomingMovies));

        recyclerNowShowing.setAdapter(nowShowingAdapter);
        recyclerUpcoming.setAdapter(upcomingAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromDatabase();
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

    private void refreshFromDatabase() {
        MovieDbHelper db = new MovieDbHelper(this);
        // Seed from assets if empty for cross-device portability
        db.seedFromAssetsIfEmpty();

        List<Movie> now = db.readByCategory("now");
        List<Movie> up = db.readByCategory("upcoming");

        allNowShowingMovies = limitToFive(now);
        nowShowingAdapter.updateList(new ArrayList<>(allNowShowingMovies));

        allUpcomingMovies = limitToFive(up);
        upcomingAdapter.updateList(new ArrayList<>(allUpcomingMovies));
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("search movies");

        AutoCompleteTextView searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        searchAutoComplete.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterMovies(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMovies(newText);
                return true;
            }
        });
    }

    private void setupActions() {
        findViewById(R.id.btn_now_showing_see_all).setOnClickListener(v -> openAllMovies());
        findViewById(R.id.btn_upcoming_see_all).setOnClickListener(v -> openAllMovies());

        if (findViewById(R.id.btn_nav_home) != null) {
            findViewById(R.id.btn_nav_home).setOnClickListener(v -> {
                // Already on home
            });
        }
        if (findViewById(R.id.btn_nav_search) != null) {
            findViewById(R.id.btn_nav_search).setOnClickListener(v -> {
                SearchView sv = findViewById(R.id.search_view);
                if (sv != null) {
                    sv.requestFocusFromTouch();
                }
            });
        }
    }

    private void filterMovies(String query) {
        String normalized = query == null ? "" : query.toLowerCase().trim();

        MovieDbHelper db = new MovieDbHelper(this);
        List<Movie> fullNow = db.readByCategory("now");
        List<Movie> fullUpcoming = db.readByCategory("upcoming");

        List<Movie> filteredNowShowing = new ArrayList<>();
        List<Movie> filteredUpcoming = new ArrayList<>();

        if (TextUtils.isEmpty(normalized)) {
            filteredNowShowing.addAll(fullNow);
            filteredUpcoming.addAll(fullUpcoming);
        } else {
            for (Movie movie : fullNow) {
                if (movie.getTitle().toLowerCase().contains(normalized)) {
                    filteredNowShowing.add(movie);
                }
            }
            for (Movie movie : fullUpcoming) {
                if (movie.getTitle().toLowerCase().contains(normalized)) {
                    filteredUpcoming.add(movie);
                }
            }
        }

        nowShowingAdapter.updateList(limitToFive(filteredNowShowing));
        upcomingAdapter.updateList(limitToFive(filteredUpcoming));

    }

    private List<Movie> limitToFive(List<Movie> source) {
        if (source == null) return new ArrayList<>();
        int size = Math.min(source.size(), 5);
        return new ArrayList<>(source.subList(0, size));
    }

    private List<Movie> getNowShowingMovies() {
        List<Movie> list = new ArrayList<>();
        list.add(new Movie("Thalaivan Thalaivi", R.drawable.thalaivanthalaivi, "TAMIL"));
        list.add(new Movie("Maargan", R.drawable.maargan, "TAMIL"));
        list.add(new Movie("Avengers", R.drawable.avengers, "ENGLISH"));
        list.add(new Movie("Batman", R.drawable.batman, "ENGLISH"));
        list.add(new Movie("Spiderman", R.drawable.spiderman, "TAMIL"));
        return list;
    }

    private List<Movie> getUpcomingMovies() {
        List<Movie> list = new ArrayList<>();
        list.add(new Movie("Jana Nayagan", R.drawable.jananayagan, "TAMIL"));
        list.add(new Movie("Superman", R.drawable.superman, "TAMIL"));
        list.add(new Movie("Ironman", R.drawable.ironman, "ENGLISH"));
        list.add(new Movie("Thor", R.drawable.thor, "HINDI"));
        return list;
    }

    private void openAllMovies() {
        Intent intent = new Intent(this, AllMoviesActivity.class);
        ArrayList<Movie> combined = new ArrayList<>();
        combined.addAll(allNowShowingMovies);
        combined.addAll(allUpcomingMovies);
        intent.putExtra("all_movies", combined);
        startActivity(intent);
    }

    private void handleAdminSecretTap() {
        long now = SystemClock.elapsedRealtime();
        if (now - adminTapWindowStart > ADMIN_TAP_WINDOW_MS) {
            adminTapWindowStart = now;
            adminTapCount = 0;
        }
        adminTapCount++;
        if (adminTapCount >= ADMIN_TAP_THRESHOLD) {
            adminTapCount = 0;
            showAdminPinDialog();
        }
    }

    private void showAdminPinDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle("Enter Admin PIN")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String pin = input.getText().toString().trim();
                    if ("2468".equals(pin)) {
                        startActivity(new Intent(this, AdminActivity.class));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
