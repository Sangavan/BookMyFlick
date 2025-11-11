package com.example.bookmyflick;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class DetailActivity extends AppCompatActivity {

    private MovieDbHelper dbHelper;

    private TextView tvEmail, tvMovie, tvTheatre, tvDate, tvTime, tvSeat, tvAmount, tvOrderId;
    private ImageView ivBackground;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView homeBtn = findViewById(R.id.homeButton);
        ImageView searchBtn = findViewById(R.id.searchButton);
        if (homeBtn != null) homeBtn.setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, MainActivity.class));
            finish();
        });
        if (searchBtn != null) searchBtn.setOnClickListener(v -> {
            startActivity(new Intent(DetailActivity.this, AllMoviesActivity.class));
            finish();
        });

        dbHelper = new MovieDbHelper(this);

        tvEmail = findViewById(R.id.txtEmail);
        tvMovie = findViewById(R.id.txtMovieName);
        tvTheatre = findViewById(R.id.txtTheatre);
        tvDate = findViewById(R.id.txtDate);
        tvTime = findViewById(R.id.txtTime);
        tvSeat = findViewById(R.id.txtSeat);
        tvAmount = findViewById(R.id.txtAmount);
        tvOrderId = findViewById(R.id.txtOrderId);
        ivBackground = findViewById(R.id.imgBackground);

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            loadLatestPayment(email);
        }
    }

    private void loadLatestPayment(String email) {
        Cursor c = dbHelper.getLatestPaymentByEmail(email);
        if (c != null && c.moveToFirst()) {
            String movie = c.getString(c.getColumnIndexOrThrow("movie"));
            String theatre = c.getString(c.getColumnIndexOrThrow("theatre"));
            String date = c.getString(c.getColumnIndexOrThrow("date"));
            String time = c.getString(c.getColumnIndexOrThrow("time"));
            String seats = c.getString(c.getColumnIndexOrThrow("seats"));
            int amount = c.getInt(c.getColumnIndexOrThrow("amount"));

            String orderId = "ORD-" + String.format("%06d", new Random().nextInt(999999));

            tvEmail.setText(email);
            tvMovie.setText(movie);
            tvTheatre.setText(theatre);
            tvDate.setText(date);
            tvTime.setText(time);
            tvSeat.setText(seats);
            tvAmount.setText("Rs. " + amount);
            tvOrderId.setText(orderId);

            int posterRes = dbHelper.getPosterResIdForMovieTitle(movie);
            if (posterRes != 0) {
                ivBackground.setImageBitmap(BitmapFactory.decodeResource(getResources(), posterRes));
            } else {
                ivBackground.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
        if (c != null) c.close();
    }
}
