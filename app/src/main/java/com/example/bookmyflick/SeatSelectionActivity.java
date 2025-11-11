package com.example.bookmyflick;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeatSelectionActivity extends AppCompatActivity {

    GridLayout seatLayout;
    Button btnBookNow;
    EditText etSeatCount;
    TextView txtMovieName;
    String selectedMovie;
    String selectedDate;
    String selectedTheatre;
    String selectedTime;
    int selectedSeats = 0;
    int maxSeatCount = 0;

    Set<Integer> soldSeatIndexes = new HashSet<>();
    Set<String> selectedSeatLabels = new HashSet<>();
    List<String> initiallyBookedSeatLabels = new ArrayList<>();
    MovieDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Seat Selection");
        }
        View backArrow = findViewById(R.id.ivBackArrow);
        if (backArrow != null) backArrow.setOnClickListener(v -> onBackPressed());

        selectedMovie = getIntent().getStringExtra("movie");
        if (selectedMovie == null) selectedMovie = getIntent().getStringExtra("selectedMovie");
        selectedDate = getIntent().getStringExtra("date");
        selectedTheatre = getIntent().getStringExtra("theatre");
        selectedTime = getIntent().getStringExtra("time");

        txtMovieName = findViewById(R.id.txtMovieName);
        etSeatCount = findViewById(R.id.etSeatCount);
        seatLayout = findViewById(R.id.seatLayout);
        btnBookNow = findViewById(R.id.btnBookNow);
        btnBookNow.setEnabled(false);

        if (txtMovieName != null) {
            String header = "Movie: " + (selectedMovie != null ? selectedMovie : "-") +
                    "\nDate: " + (selectedDate != null ? selectedDate : "-") +
                    "\nTheatre: " + (selectedTheatre != null ? selectedTheatre : "-") +
                    "\nTime: " + (selectedTime != null ? selectedTime : "-");
            txtMovieName.setText(header);
        }

        createSeatStatusIndicators();
        dbHelper = new MovieDbHelper(this);
        loadSoldSeats();
        btnBookNow.setOnClickListener(v -> onBookNowClicked());
    }

    private void createSeatStatusIndicators() {
        LinearLayout statusLayout = findViewById(R.id.seatStatusLayout);
        addStatusIndicator(statusLayout, 0xFF2E7D32, "Selected"); // green
        addStatusIndicator(statusLayout, 0xFFC62828, "Sold");     // red
        addStatusIndicator(statusLayout, 0xFF9E9E9E, "Available"); // gray
    }

    private void addStatusIndicator(LinearLayout layout, int color, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(10, 5, 10, 5);

        View colorBox = new View(this);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(40, 40);
        colorBox.setLayoutParams(boxParams);
        colorBox.setBackgroundColor(color);

        TextView text = new TextView(this);
        text.setText(" " + label);
        text.setTextSize(16);

        item.addView(colorBox);
        item.addView(text);
        layout.addView(item);
    }

    private void loadSoldSeats() {
        soldSeatIndexes.clear();
        initiallyBookedSeatLabels.clear();
        if (selectedMovie != null && selectedDate != null && selectedTheatre != null && selectedTime != null) {
            initiallyBookedSeatLabels = dbHelper.getBookedSeats(selectedMovie, selectedDate, selectedTheatre, selectedTime);
        }
        createSeats();
    }

    private void createSeats() {
        int rows = 8;
        int cols = 7;
        char[] rowLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};

        seatLayout.removeAllViews();
        for (int i = 0; i < rows * cols; i++) {
            int row = i / cols;
            int col = i % cols;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row), GridLayout.spec(col)
            );
            int seatSize = dpToPx(36);
            int seatMargin = dpToPx(6);
            params.width = seatSize;
            params.height = seatSize;
            params.setMargins(seatMargin, seatMargin, seatMargin, seatMargin);

            TextView seat = new TextView(this);
            seat.setLayoutParams(params);
            seat.setGravity(Gravity.CENTER);
            seat.setTextSize(12);
            seat.setTextColor(Color.WHITE);

            String seatLabel = rowLetters[row] + String.valueOf(col + 1);
            seat.setText(seatLabel);
            seat.setContentDescription("Seat " + seatLabel);

            boolean isBooked = initiallyBookedSeatLabels.contains(seatLabel);
            if (isBooked) {
                seat.setBackgroundColor(0xFFC62828); // red
                seat.setEnabled(false);
            } else {
                seat.setBackgroundColor(0xFF9E9E9E); // gray available
                seat.setPadding(8, 8, 8, 8);

                final boolean[] isSelected = {false};
                seat.setOnClickListener(v -> {
                    String input = etSeatCount.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Please enter seat count first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    maxSeatCount = Integer.parseInt(input);
                    if (isSelected[0]) {
                        seat.setBackgroundColor(0xFF9E9E9E);
                        isSelected[0] = false;
                        selectedSeats--;
                        selectedSeatLabels.remove(seatLabel);
                    } else {
                        if (selectedSeats >= maxSeatCount) {
                            Toast.makeText(this, "Max " + maxSeatCount + " seats", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        seat.setBackgroundColor(0xFF2E7D32); // green
                        isSelected[0] = true;
                        selectedSeats++;
                        selectedSeatLabels.add(seatLabel);
                    }
                    updateBookNowEnabled();
                });
            }

            seatLayout.addView(seat);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateBookNowEnabled() {
        boolean enable = maxSeatCount > 0 && selectedSeats == maxSeatCount;
        btnBookNow.setEnabled(enable);
    }

    private void onBookNowClicked() {
        if (maxSeatCount == 0) {
            Toast.makeText(this, "Enter seat count first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSeats != maxSeatCount) {
            Toast.makeText(this, "Select exactly " + maxSeatCount + " seats", Toast.LENGTH_SHORT).show();
            return;
        }
        String seats = String.join(", ", new ArrayList<>(selectedSeatLabels));
        StringBuilder message = new StringBuilder();
        message.append("Movie: ").append(selectedMovie != null ? selectedMovie : "-").append('\n');
        message.append("Date: ").append(selectedDate != null ? selectedDate : "-").append('\n');
        message.append("Theatre: ").append(selectedTheatre != null ? selectedTheatre : "-").append('\n');
        message.append("Time: ").append(selectedTime != null ? selectedTime : "-").append('\n');
        message.append("Seats: ").append(seats);
        new AlertDialog.Builder(this)
                .setTitle("Proceed to payment")
                .setMessage(message.toString())
                .setPositiveButton("Proceed", (dialog, which) -> {
                    android.content.Intent intent = new android.content.Intent(this, TicketWalletActivity.class);
                    intent.putExtra("movie", selectedMovie);
                    intent.putExtra("date", selectedDate);
                    intent.putExtra("theatre", selectedTheatre);
                    intent.putExtra("time", selectedTime);
                    intent.putExtra("seats", seats);
                    intent.putExtra("seatCount", maxSeatCount);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
