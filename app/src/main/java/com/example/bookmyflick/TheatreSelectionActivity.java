package com.example.bookmyflick;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TheatreSelectionActivity extends AppCompatActivity {

    private Button btnSelectDate;
    private TextView tvSelectedDate;
    private ExpandableListView elvTheatres;
    private SimpleExpandableListAdapter adapter;
    private List<Map<String, String>> groupData = new ArrayList<>();
    private List<List<Map<String, String>>> childData = new ArrayList<>();
    private List<String> currentTheatres = new ArrayList<>();

    private String selectedDate = "";
    private String selectedMovie = "";

    private MovieDbHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theatre_selection);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Theatre Selection");

        // Back arrow at top-left inside layout
        android.view.View backArrow = findViewById(R.id.ivBackArrow);
        if (backArrow != null) backArrow.setOnClickListener(v -> onBackPressed());

        db = new MovieDbHelper(this);
        db.seedTheatresIfEmpty();

        selectedMovie = getIntent().getStringExtra("selectedMovie");
        TextView tvSelectedMovie = findViewById(R.id.tvSelectedMovie);
        if (tvSelectedMovie != null) {
            tvSelectedMovie.setText("Movie: " + (selectedMovie != null ? selectedMovie : "-"));
        }

        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        elvTheatres = findViewById(R.id.elvTheatres);

        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Bottom nav actions
        android.view.View btnHome = findViewById(R.id.btn_nav_home);
        if (btnHome != null) btnHome.setOnClickListener(v -> finish());
        android.view.View btnSearch = findViewById(R.id.btn_nav_search);
        if (btnSearch != null) btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(this, AllMoviesActivity.class));
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(sel.getTime());
                    tvSelectedDate.setText("Selected Date: " + selectedDate);
                    // Ensure shows exist then load theatres from DB
                    new Thread(() -> {
                        db.ensureShowsForMovieDate(selectedMovie, selectedDate);
                        List<String> theatres = db.getTheatresForMovieAndDate(selectedMovie, selectedDate);
                        runOnUiThread(() -> bindTheatres(theatres));
                    }).start();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        Calendar max = Calendar.getInstance();
        max.add(Calendar.MONTH, 3);
        dp.getDatePicker().setMaxDate(max.getTimeInMillis());
        dp.show();
    }

    private void bindTheatres(List<String> theatres) {
        currentTheatres = theatres == null ? new ArrayList<>() : theatres;
        groupData = buildGroupData(currentTheatres);
        childData = new ArrayList<>();
        for (int i = 0; i < currentTheatres.size(); i++) {
            childData.add(new ArrayList<>());
        }
        adapter = new SimpleExpandableListAdapter(
                this,
                groupData,
                android.R.layout.simple_expandable_list_item_1,
                new String[]{"THEATRE"},
                new int[]{android.R.id.text1},
                childData,
                android.R.layout.simple_expandable_list_item_1,
                new String[]{"TIME"},
                new int[]{android.R.id.text1}
        );
        elvTheatres.setAdapter(adapter);
        elvTheatres.setOnGroupExpandListener(groupPosition -> {
            if (childData.get(groupPosition).isEmpty()) {
                String theatre = currentTheatres.get(groupPosition);
                // Load times from DB
                new Thread(() -> {
                    List<String> times = db.getShowTimes(selectedMovie, selectedDate, theatre);
                    List<Map<String, String>> rows = new ArrayList<>();
                    if (times != null && !times.isEmpty()) {
                        for (String t : times) {
                            Map<String, String> map = new HashMap<>();
                            map.put("TIME", t);
                            rows.add(map);
                        }
                    } else {
                        Map<String, String> map = new HashMap<>();
                        map.put("TIME", "No show times");
                        rows.add(map);
                    }
                    runOnUiThread(() -> {
                        childData.set(groupPosition, rows);
                        adapter.notifyDataSetChanged();
                    });
                }).start();
            }
        });
        elvTheatres.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            String theatre = currentTheatres.get(groupPosition);
            String time = childData.get(groupPosition).get(childPosition).get("TIME");
            if (time != null && !time.equals("No show times")) {
                Intent intent = new Intent(this, SeatSelectionActivity.class);
                intent.putExtra("movie", selectedMovie);
                intent.putExtra("date", selectedDate);
                intent.putExtra("theatre", theatre);
                intent.putExtra("time", time);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private List<Map<String, String>> buildGroupData(List<String> theatres) {
        List<Map<String, String>> data = new ArrayList<>();
        for (String theatre : theatres) {
            Map<String, String> map = new HashMap<>();
            map.put("THEATRE", theatre);
            data.add(map);
        }
        return data;
    }
}


