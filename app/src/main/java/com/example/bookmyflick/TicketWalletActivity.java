package com.example.bookmyflick;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class TicketWalletActivity extends AppCompatActivity {

    EditText etEmail, etPhone, etCardNumber, etNameOnCard, etExpiryMM, etExpiryYY, etCvv;
    TextView tvSeats, tvAmount;
    Button btnCancel, btnPay;

    int totalAmount = 0;

    String selectedMovie, selectedDate, selectedTheatre, selectedTime;
    List<String> selectedSeats;
    MovieDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_wallet);
        dbHelper = new MovieDbHelper(this);

        etEmail = findViewById(R.id.ticket_wallet_et_email);
        etPhone = findViewById(R.id.ticket_wallet_et_phone);
        etCardNumber = findViewById(R.id.ticket_wallet_et_card_number);
        etNameOnCard = findViewById(R.id.ticket_wallet_et_name_on_card);
        etExpiryMM = findViewById(R.id.ticket_wallet_et_expiry_mm);
        etExpiryYY = findViewById(R.id.ticket_wallet_et_expiry_yy);
        etCvv = findViewById(R.id.ticket_wallet_et_cvv);

        tvSeats = findViewById(R.id.ticket_wallet_tv_seats);
        tvAmount = findViewById(R.id.ticket_wallet_tv_amount);

        btnCancel = findViewById(R.id.ticket_wallet_btn_cancel);
        btnPay = findViewById(R.id.ticket_wallet_btn_pay);

        selectedMovie = getIntent().getStringExtra("movie");
        selectedDate = getIntent().getStringExtra("date");
        selectedTheatre = getIntent().getStringExtra("theatre");
        selectedTime = getIntent().getStringExtra("time");
        String seatsStr = getIntent().getStringExtra("seats");
        int seatCount = getIntent().getIntExtra("seatCount", 0);

        if (seatsStr != null && !seatsStr.isEmpty()) {
            tvSeats.setText(seatsStr);
            selectedSeats = Arrays.asList(seatsStr.split(", "));
            totalAmount = seatCount * 1000;
            tvAmount.setText("Rs. " + totalAmount);
        } else {
            tvSeats.setText("No seats selected");
            totalAmount = 0;
            tvAmount.setText("Rs. 0");
        }

        btnCancel.setOnClickListener(v -> finish());

        btnPay.setOnClickListener(v -> {
            if (validateInputs()) {
                new AlertDialog.Builder(TicketWalletActivity.this)
                        .setTitle("Payment Confirmation")
                        .setMessage("Proceed with payment?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Persist bookings (mark seats as sold)
                            if (selectedMovie != null && selectedDate != null && selectedTheatre != null && selectedTime != null && selectedSeats != null) {
                                dbHelper.bookSeats(selectedMovie, selectedDate, selectedTheatre, selectedTime, selectedSeats);
                                // Persist payment details
                                String email = etEmail.getText().toString().trim();
                                String phone = etPhone.getText().toString().trim();
                                String nameOnCard = etNameOnCard.getText().toString().trim();
                                String card = etCardNumber.getText().toString().trim();
                                String last4 = card.length() >= 4 ? card.substring(card.length() - 4) : card;
                                dbHelper.savePayment(
                                        selectedMovie,
                                        selectedDate,
                                        selectedTheatre,
                                        selectedTime,
                                        selectedSeats,
                                        seatCount,
                                        totalAmount,
                                        email,
                                        phone,
                                        nameOnCard,
                                        last4
                                );
                            }
                            Toast.makeText(TicketWalletActivity.this, "Payment saved!", Toast.LENGTH_SHORT).show();
                            // Navigate to ticket details page
                            Intent details = new Intent(TicketWalletActivity.this, DetailActivity.class);
                            details.putExtra("email", etEmail.getText().toString().trim());
                            startActivity(details);
                            finish();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            etEmail.setError("Enter valid email (must contain @)");
            etEmail.requestFocus();
            return false;
        }

        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone) || phone.length() != 10 || !phone.matches("\\d{10}")) {
            etPhone.setError("Enter valid 10-digit phone number");
            etPhone.requestFocus();
            return false;
        }

        String card = etCardNumber.getText().toString().trim();
        if (!card.matches("\\d{16}")) {
            etCardNumber.setError("Card number must be exactly 16 digits");
            etCardNumber.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etNameOnCard.getText().toString().trim())) {
            etNameOnCard.setError("Enter name on card");
            etNameOnCard.requestFocus();
            return false;
        }

        String mm = etExpiryMM.getText().toString().trim();
        String yy = etExpiryYY.getText().toString().trim();
        if (TextUtils.isEmpty(mm) || TextUtils.isEmpty(yy)) {
            etExpiryMM.setError("Expiry required");
            etExpiryMM.requestFocus();
            return false;
        }
        try {
            int month = Integer.parseInt(mm);
            if (month < 1 || month > 12) {
                etExpiryMM.setError("Month must be between 01 and 12");
                etExpiryMM.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etExpiryMM.setError("Invalid month");
            etExpiryMM.requestFocus();
            return false;
        }

        String cvv = etCvv.getText().toString().trim();
        if (!cvv.matches("\\d{3}")) {
            etCvv.setError("CVV must be exactly 3 digits");
            etCvv.requestFocus();
            return false;
        }

        return true;
    }
}
