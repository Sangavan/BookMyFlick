package com.example.bookmyflick;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    private EditText inputTitle;
    private EditText inputLanguage;
    private EditText inputImageResId;
    private EditText inputDetailImageResId;
    private EditText inputCast;
    private EditText inputYear;
    private EditText inputDirector;
    private EditText inputRating;
    private EditText inputCategory; // now | upcoming

    private MovieDbHelper dbHelper;

    public static final String ACTION_MOVIES_CHANGED = "com.example.bookmyflick.MOVIES_CHANGED";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        dbHelper = new MovieDbHelper(this);

        inputTitle = findViewById(R.id.input_title);
        inputLanguage = findViewById(R.id.input_language);
        inputImageResId = findViewById(R.id.input_image_res_id);
        inputDetailImageResId = findViewById(R.id.input_detail_image_res_id);
        inputCast = findViewById(R.id.input_cast);
        inputYear = findViewById(R.id.input_year);
        inputDirector = findViewById(R.id.input_director);
        inputRating = findViewById(R.id.input_rating);
        inputCategory = findViewById(R.id.input_category);

        Button btnAdd = findViewById(R.id.btn_add);
        Button btnUpdate = findViewById(R.id.btn_update);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnCleanDupes = findViewById(R.id.btn_clean_duplicates);
        Button btnBack = findViewById(R.id.btn_back);

        btnAdd.setOnClickListener(v -> addMovie());
        btnUpdate.setOnClickListener(v -> updateMovie());
        btnDelete.setOnClickListener(v -> deleteMovie());
        btnCleanDupes.setOnClickListener(v -> cleanDuplicates());
        btnBack.setOnClickListener(v -> finish());
    }

    private void cleanDuplicates() {
        int removed = dbHelper.deleteDuplicateTitles();
        if (removed > 0) sendBroadcast(new android.content.Intent(ACTION_MOVIES_CHANGED));
        Toast.makeText(this, removed > 0 ? "Duplicates removed" : "No duplicates", Toast.LENGTH_SHORT).show();
    }

    private void addMovie() {
        String title = inputTitle.getText().toString().trim();
        String language = inputLanguage.getText().toString().trim();
        String posterName = inputImageResId.getText().toString().trim();
        String detailImgStr = inputDetailImageResId.getText().toString().trim();
        String cast = inputCast.getText().toString().trim();
        String yearStr = inputYear.getText().toString().trim();
        String director = inputDirector.getText().toString().trim();
        String ratingStr = inputRating.getText().toString().trim();
        String category = inputCategory.getText().toString().trim().toLowerCase();

        Integer imgId = resolveImageResId(posterName);
        if (!validate(title, language, posterName, category)) return;
        if (imgId == null || imgId == 0) {
            Toast.makeText(this, "Invalid image (use drawable name)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.existsByTitleIgnoreCase(title)) {
            Toast.makeText(this, "Movie already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        long id = dbHelper.insertMovieByName(title, language, posterName, category);
        if (id != -1) {
            dbHelper.updateNames(title, posterName, TextUtils.isEmpty(detailImgStr) ? null : detailImgStr);
            Integer year = TextUtils.isEmpty(yearStr) ? null : safeInt(yearStr);
            Float rating = TextUtils.isEmpty(ratingStr) ? null : safeFloat(ratingStr);
            dbHelper.updateMovieAll(title, null, null, null, null, TextUtils.isEmpty(cast) ? null : cast, year, TextUtils.isEmpty(director) ? null : director, rating);
        }
        Toast.makeText(this, id == -1 ? "Insert failed" : "Inserted", Toast.LENGTH_SHORT).show();
        if (id != -1) sendBroadcast(new android.content.Intent(ACTION_MOVIES_CHANGED));
    }

    private void updateMovie() {
        String title = inputTitle.getText().toString().trim();
        String language = inputLanguage.getText().toString().trim();
        String posterName = inputImageResId.getText().toString().trim();
        String detailImgStr = inputDetailImageResId.getText().toString().trim();
        String cast = inputCast.getText().toString().trim();
        String yearStr = inputYear.getText().toString().trim();
        String director = inputDirector.getText().toString().trim();
        String ratingStr = inputRating.getText().toString().trim();
        String category = inputCategory.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(posterName) || !TextUtils.isEmpty(detailImgStr)) {
            dbHelper.updateNames(title, TextUtils.isEmpty(posterName) ? null : posterName, TextUtils.isEmpty(detailImgStr) ? null : detailImgStr);
        }
        Integer year = TextUtils.isEmpty(yearStr) ? null : safeInt(yearStr);
        Float rating = TextUtils.isEmpty(ratingStr) ? null : safeFloat(ratingStr);
        int rows = dbHelper.updateMovieAll(title,
                TextUtils.isEmpty(language) ? null : language,
                null,
                TextUtils.isEmpty(category) ? null : category,
                null,
                TextUtils.isEmpty(cast) ? null : cast,
                year,
                TextUtils.isEmpty(director) ? null : director,
                rating);
        Toast.makeText(this, rows > 0 ? "Updated" : "Not found", Toast.LENGTH_SHORT).show();
        if (rows > 0) sendBroadcast(new android.content.Intent(ACTION_MOVIES_CHANGED));
    }

    private void deleteMovie() {
        String title = inputTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
            return;
        }
        int rows = dbHelper.deleteMovie(title);
        Toast.makeText(this, rows > 0 ? "Deleted" : "Not found", Toast.LENGTH_SHORT).show();
        if (rows > 0) sendBroadcast(new android.content.Intent(ACTION_MOVIES_CHANGED));
    }

    private boolean validate(String title, String language, String imgStr, String category) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(language) || TextUtils.isEmpty(imgStr) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return false;
        }
        Integer resolved = resolveImageResId(imgStr);
        if (resolved == null || resolved == 0) {
            Toast.makeText(this, "Image must be valid drawable name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!category.equals("now") && !category.equals("upcoming")) { Toast.makeText(this, "Category: now|upcoming", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private Integer resolveImageResId(String imgStr) {
        try {
            int asInt = Integer.parseInt(imgStr);
            try {
                getResources().getResourceName(asInt);
                return asInt;
            } catch (Exception e) {
                return null;
            }
        } catch (NumberFormatException ignored) {}
        int resolved = getResources().getIdentifier(imgStr, "drawable", getPackageName());
        return resolved == 0 ? null : resolved;
    }

    private Integer safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private Float safeFloat(String s) {
        try { return Float.parseFloat(s); } catch (Exception e) { return null; }
    }
}


