package com.example.bookmyflick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

public class MovieDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        View toolbar = findViewById(R.id.toolbar_detail);
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvLanguage = findViewById(R.id.tv_detail_language);
        TextView tvCast = findViewById(R.id.tv_detail_cast_value);
        TextView tvYear = findViewById(R.id.tv_detail_year_value);
        TextView tvDirector = findViewById(R.id.tv_detail_director_value);
        TextView tvRating = findViewById(R.id.tv_detail_rating_value);
        View btnBook = findViewById(R.id.btn_book_movie);
        ImageView ivPoster = findViewById(R.id.iv_detail_poster);
        ImageView ivDetail = findViewById(R.id.iv_detail_secondary);

        Movie movie = (Movie) getIntent().getSerializableExtra("movie");
        if (movie != null) {
            MovieDbHelper db = new MovieDbHelper(this);
            Movie latest = db.readByTitle(movie.getTitle());
            if (latest != null) movie = latest;
        }
        if (movie != null) {
            tvTitle.setText(movie.getTitle());
            tvLanguage.setText(movie.getLanguage());
            ivPoster.setVisibility(View.GONE);
            if (movie.getDetailImageResId() != null) {
                try {
                    ivDetail.setImageDrawable(AppCompatResources.getDrawable(this, movie.getDetailImageResId()));
                    ivDetail.setVisibility(View.VISIBLE);
                } catch (Exception ignored) {
                    ivDetail.setVisibility(View.GONE);
                }
            } else {
                ivDetail.setVisibility(View.GONE);
            }
            if (movie.getCast() != null && !movie.getCast().trim().isEmpty()) {
                tvCast.setText(movie.getCast());
            } else if (tvCast != null) {
                tvCast.setText("Not available");
            }
            if (movie.getYear() != null && tvYear != null) tvYear.setText(String.valueOf(movie.getYear())); else if (tvYear != null) tvYear.setText("Not available");
            if (movie.getDirector() != null && !movie.getDirector().trim().isEmpty() && tvDirector != null) tvDirector.setText(movie.getDirector()); else if (tvDirector != null) tvDirector.setText("Not available");
            if (movie.getRating() != null && tvRating != null) tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f/10", movie.getRating())); else if (tvRating != null) tvRating.setText("Not available");
        }
        if (btnBook != null) {
            Movie finalMovie = movie;
            btnBook.setOnClickListener(v -> {
                if (finalMovie != null) {
                    Intent intent = new Intent(this, TheatreSelectionActivity.class);
                    intent.putExtra("selectedMovie", finalMovie.getTitle());
                    startActivity(intent);
                }
            });
        }

        // No bottom nav on details per latest request
    }
}


