package com.example.bookmyflick;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MovieDbHelper extends SQLiteOpenHelper {

    static final String DB_NAME = "movies.db";
    static final int DB_VERSION = 7;

    static final String TABLE_MOVIES = "movies";
    static final String COL_ID = "_id";
    static final String COL_TITLE = "title";
    static final String COL_LANGUAGE = "language";
    static final String COL_CATEGORY = "category"; // now | upcoming
    static final String COL_CAST = "cast"; // optional plain text
    static final String COL_YEAR = "year"; // optional integer
    static final String COL_DIRECTOR = "director"; // optional
    static final String COL_RATING = "rating"; // optional float 0..10
    static final String COL_POSTER_NAME = "poster_name"; // drawable name
    static final String COL_DETAIL_NAME = "detail_name"; // drawable name

    // Theatres
    static final String TABLE_THEATRES = "theatres";
    static final String COL_T_NAME = "name";
    static final String COL_T_LOCATION = "location";

    // Shows
    static final String TABLE_SHOWS = "shows";
    static final String COL_S_ID = "_id";
    static final String COL_S_MOVIE = "movie";
    static final String COL_S_DATE = "date";
    static final String COL_S_THEATRE = "theatre";
    static final String COL_S_TIME = "time";

    // Bookings table to persist sold seats per show
    static final String TABLE_BOOKINGS = "bookings";
    static final String COL_B_ID = "_id";
    static final String COL_B_MOVIE = "movie";
    static final String COL_B_DATE = "date";
    static final String COL_B_THEATRE = "theatre";
    static final String COL_B_TIME = "time";
    static final String COL_B_SEAT = "seat";

    // Payments table to store successful transactions
    static final String TABLE_PAYMENTS = "payments";
    static final String COL_P_ID = "_id";
    static final String COL_P_MOVIE = "movie";
    static final String COL_P_DATE = "date";
    static final String COL_P_THEATRE = "theatre";
    static final String COL_P_TIME = "time";
    static final String COL_P_SEATS = "seats"; // comma separated
    static final String COL_P_SEAT_COUNT = "seat_count";
    static final String COL_P_AMOUNT = "amount"; // integer amount
    static final String COL_P_EMAIL = "email";
    static final String COL_P_PHONE = "phone";
    static final String COL_P_NAME_ON_CARD = "name_on_card";
    static final String COL_P_CARD_LAST4 = "card_last4";
    static final String COL_P_CREATED_AT = "created_at";

    private final Context appContext;

    MovieDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.appContext = context.getApplicationContext();
    }

    // Copy a prebuilt DB from assets on first run (if present)
    public static void copyPrebuiltDbIfNeeded(Context context) {
        try {
            java.io.File dbPath = context.getDatabasePath(DB_NAME);
            if (dbPath.exists()) return;
            java.io.File parent = dbPath.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (InputStream is = context.getAssets().open(DB_NAME);
                 java.io.OutputStream os = new java.io.FileOutputStream(dbPath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_MOVIES + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_TITLE + " TEXT NOT NULL UNIQUE, " +
                        COL_LANGUAGE + " TEXT NOT NULL, " +
                        COL_CATEGORY + " TEXT NOT NULL, " +
                        COL_CAST + " TEXT, " +
                        COL_YEAR + " INTEGER, " +
                        COL_DIRECTOR + " TEXT, " +
                        COL_RATING + " REAL, " +
                        COL_POSTER_NAME + " TEXT, " +
                        COL_DETAIL_NAME + " TEXT" +
                        ")"
        );
        // Theatres
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_THEATRES + " (" +
                        COL_T_NAME + " TEXT PRIMARY KEY, " +
                        COL_T_LOCATION + " TEXT)"
        );
        // Shows
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_SHOWS + " (" +
                        COL_S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_S_MOVIE + " TEXT NOT NULL, " +
                        COL_S_DATE + " TEXT NOT NULL, " +
                        COL_S_THEATRE + " TEXT NOT NULL, " +
                        COL_S_TIME + " TEXT NOT NULL)"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_movie_date ON " + TABLE_SHOWS + "(" + COL_S_MOVIE + "," + COL_S_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_theatre ON " + TABLE_SHOWS + "(" + COL_S_THEATRE + ")");

        // Bookings
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKINGS + " (" +
                        COL_B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_B_MOVIE + " TEXT NOT NULL, " +
                        COL_B_DATE + " TEXT NOT NULL, " +
                        COL_B_THEATRE + " TEXT NOT NULL, " +
                        COL_B_TIME + " TEXT NOT NULL, " +
                        COL_B_SEAT + " TEXT NOT NULL, " +
                        "UNIQUE(" + COL_B_MOVIE + "," + COL_B_DATE + "," + COL_B_THEATRE + "," + COL_B_TIME + "," + COL_B_SEAT + ") ON CONFLICT IGNORE)"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_lookup ON " + TABLE_BOOKINGS + "(" + COL_B_MOVIE + "," + COL_B_DATE + "," + COL_B_THEATRE + "," + COL_B_TIME + ")");

        // Payments
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_PAYMENTS + " (" +
                        COL_P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_P_MOVIE + " TEXT NOT NULL, " +
                        COL_P_DATE + " TEXT NOT NULL, " +
                        COL_P_THEATRE + " TEXT NOT NULL, " +
                        COL_P_TIME + " TEXT NOT NULL, " +
                        COL_P_SEATS + " TEXT NOT NULL, " +
                        COL_P_SEAT_COUNT + " INTEGER NOT NULL, " +
                        COL_P_AMOUNT + " INTEGER NOT NULL, " +
                        COL_P_EMAIL + " TEXT, " +
                        COL_P_PHONE + " TEXT, " +
                        COL_P_NAME_ON_CARD + " TEXT, " +
                        COL_P_CARD_LAST4 + " TEXT, " +
                        COL_P_CREATED_AT + " TEXT DEFAULT (datetime('now'))" +
                        ")"
        );
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_payments_lookup ON " + TABLE_PAYMENTS + "(" + COL_P_MOVIE + "," + COL_P_DATE + "," + COL_P_THEATRE + "," + COL_P_TIME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN cast TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN year INTEGER"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN director TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN rating REAL"); } catch (Exception ignored) {}
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN poster_name TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_MOVIES + " ADD COLUMN detail_name TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 4) {
            // handled via table rebuild previously; skip for brevity here
        }
        if (oldVersion < 5) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_THEATRES + " (" +
                            COL_T_NAME + " TEXT PRIMARY KEY, " +
                            COL_T_LOCATION + " TEXT)"
            );
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_SHOWS + " (" +
                            COL_S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COL_S_MOVIE + " TEXT NOT NULL, " +
                            COL_S_DATE + " TEXT NOT NULL, " +
                            COL_S_THEATRE + " TEXT NOT NULL, " +
                            COL_S_TIME + " TEXT NOT NULL)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_movie_date ON " + TABLE_SHOWS + "(" + COL_S_MOVIE + "," + COL_S_DATE + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shows_theatre ON " + TABLE_SHOWS + "(" + COL_S_THEATRE + ")");
        }
        if (oldVersion < 6) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKINGS + " (" +
                            COL_B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COL_B_MOVIE + " TEXT NOT NULL, " +
                            COL_B_DATE + " TEXT NOT NULL, " +
                            COL_B_THEATRE + " TEXT NOT NULL, " +
                            COL_B_TIME + " TEXT NOT NULL, " +
                            COL_B_SEAT + " TEXT NOT NULL, " +
                            "UNIQUE(" + COL_B_MOVIE + "," + COL_B_DATE + "," + COL_B_THEATRE + "," + COL_B_TIME + "," + COL_B_SEAT + ") ON CONFLICT IGNORE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookings_lookup ON " + TABLE_BOOKINGS + "(" + COL_B_MOVIE + "," + COL_B_DATE + "," + COL_B_THEATRE + "," + COL_B_TIME + ")");
        }
        if (oldVersion < 7) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_PAYMENTS + " (" +
                            COL_P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COL_P_MOVIE + " TEXT NOT NULL, " +
                            COL_P_DATE + " TEXT NOT NULL, " +
                            COL_P_THEATRE + " TEXT NOT NULL, " +
                            COL_P_TIME + " TEXT NOT NULL, " +
                            COL_P_SEATS + " TEXT NOT NULL, " +
                            COL_P_SEAT_COUNT + " INTEGER NOT NULL, " +
                            COL_P_AMOUNT + " INTEGER NOT NULL, " +
                            COL_P_EMAIL + " TEXT, " +
                            COL_P_PHONE + " TEXT, " +
                            COL_P_NAME_ON_CARD + " TEXT, " +
                            COL_P_CARD_LAST4 + " TEXT, " +
                            COL_P_CREATED_AT + " TEXT DEFAULT (datetime('now'))" +
                            ")"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_payments_lookup ON " + TABLE_PAYMENTS + "(" + COL_P_MOVIE + "," + COL_P_DATE + "," + COL_P_THEATRE + "," + COL_P_TIME + ")");
        }
    }

    long insertMovieByName(String title, String language, String posterName, String category) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_LANGUAGE, language);
        values.put(COL_CATEGORY, category);
        values.put(COL_POSTER_NAME, posterName);
        return getWritableDatabase().insert(TABLE_MOVIES, null, values);
    }

    int updateNames(String title, String posterName, String detailName) {
        ContentValues values = new ContentValues();
        if (posterName != null && !posterName.trim().isEmpty()) values.put(COL_POSTER_NAME, posterName);
        if (detailName != null && !detailName.trim().isEmpty()) values.put(COL_DETAIL_NAME, detailName);
        return getWritableDatabase().update(TABLE_MOVIES, values, COL_TITLE + "=?", new String[]{title});
    }

    int updateMovieAll(String title, String language, Integer unusedImageResId, String category, Integer unusedDetailImageResId, String cast,
                       Integer year, String director, Float rating) {
        ContentValues values = new ContentValues();
        if (language != null) values.put(COL_LANGUAGE, language);
        if (category != null) values.put(COL_CATEGORY, category);
        if (cast != null) values.put(COL_CAST, cast);
        if (year != null) values.put(COL_YEAR, year);
        if (director != null) values.put(COL_DIRECTOR, director);
        if (rating != null) values.put(COL_RATING, rating);
        return getWritableDatabase().update(TABLE_MOVIES, values, COL_TITLE + "=?", new String[]{title});
    }

    int deleteMovie(String title) {
        return getWritableDatabase().delete(TABLE_MOVIES, COL_TITLE + "=?", new String[]{title});
    }

    List<Movie> readByCategory(String category) {
        List<Movie> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_MOVIES, null, COL_CATEGORY + "=?", new String[]{category}, null, null, COL_ID + " DESC");
        try {
            int idxTitle = c.getColumnIndexOrThrow(COL_TITLE);
            int idxLang = c.getColumnIndexOrThrow(COL_LANGUAGE);
            int idxPosterName = c.getColumnIndex(COL_POSTER_NAME);
            int idxDetailName = c.getColumnIndex(COL_DETAIL_NAME);
            int idxCast = c.getColumnIndex(COL_CAST);
            int idxYear = c.getColumnIndex(COL_YEAR);
            int idxDirector = c.getColumnIndex(COL_DIRECTOR);
            int idxRating = c.getColumnIndex(COL_RATING);
            while (c.moveToNext()) {
                String posterName = (idxPosterName >= 0) ? c.getString(idxPosterName) : null;
                int img = resolveDrawableId(posterName);
                String detailName = (idxDetailName >= 0) ? c.getString(idxDetailName) : null;
                Integer detail = (detailName != null) ? resolveDrawableId(detailName) : null;
                String cast = (idxCast >= 0) ? c.getString(idxCast) : null;
                Integer year = (idxYear >= 0 && !c.isNull(idxYear)) ? c.getInt(idxYear) : null;
                String director = (idxDirector >= 0) ? c.getString(idxDirector) : null;
                Float rating = (idxRating >= 0 && !c.isNull(idxRating)) ? c.getFloat(idxRating) : null;
                list.add(new Movie(c.getString(idxTitle), img, c.getString(idxLang), detail, cast, year, director, rating));
            }
        } finally { c.close(); }
        return list;
    }

    List<Movie> readAll() {
        List<Movie> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_MOVIES, null, null, null, null, null, COL_ID + " DESC");
        try {
            int idxTitle = c.getColumnIndexOrThrow(COL_TITLE);
            int idxLang = c.getColumnIndexOrThrow(COL_LANGUAGE);
            int idxPosterName = c.getColumnIndex(COL_POSTER_NAME);
            int idxDetailName = c.getColumnIndex(COL_DETAIL_NAME);
            int idxCast = c.getColumnIndex(COL_CAST);
            int idxYear = c.getColumnIndex(COL_YEAR);
            int idxDirector = c.getColumnIndex(COL_DIRECTOR);
            int idxRating = c.getColumnIndex(COL_RATING);
            while (c.moveToNext()) {
                String posterName = (idxPosterName >= 0) ? c.getString(idxPosterName) : null;
                int img = resolveDrawableId(posterName);
                String detailName = (idxDetailName >= 0) ? c.getString(idxDetailName) : null;
                Integer detail = (detailName != null) ? resolveDrawableId(detailName) : null;
                String cast = (idxCast >= 0) ? c.getString(idxCast) : null;
                Integer year = (idxYear >= 0 && !c.isNull(idxYear)) ? c.getInt(idxYear) : null;
                String director = (idxDirector >= 0) ? c.getString(idxDirector) : null;
                Float rating = (idxRating >= 0 && !c.isNull(idxRating)) ? c.getFloat(idxRating) : null;
                list.add(new Movie(c.getString(idxTitle), img, c.getString(idxLang), detail, cast, year, director, rating));
            }
        } finally { c.close(); }
        return list;
    }

    Movie readByTitle(String title) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_MOVIES + " WHERE lower(" + COL_TITLE + ") = lower(?) ORDER BY " + COL_ID + " DESC LIMIT 1",
                new String[]{title}
        );
        try {
            if (c.moveToFirst()) {
                int idxTitle = c.getColumnIndexOrThrow(COL_TITLE);
                int idxLang = c.getColumnIndexOrThrow(COL_LANGUAGE);
                int idxPosterName = c.getColumnIndex(COL_POSTER_NAME);
                int idxDetailName = c.getColumnIndex(COL_DETAIL_NAME);
                int idxCast = c.getColumnIndex(COL_CAST);
                int idxYear = c.getColumnIndex(COL_YEAR);
                int idxDirector = c.getColumnIndex(COL_DIRECTOR);
                int idxRating = c.getColumnIndex(COL_RATING);

                String t = c.getString(idxTitle);
                String lang = c.getString(idxLang);
                String posterName = (idxPosterName >= 0) ? c.getString(idxPosterName) : null;
                int img = resolveDrawableId(posterName);
                String detailName = (idxDetailName >= 0) ? c.getString(idxDetailName) : null;
                Integer detail = (detailName != null) ? resolveDrawableId(detailName) : null;
                String cast = (idxCast >= 0) ? c.getString(idxCast) : null;
                Integer year = (idxYear >= 0 && !c.isNull(idxYear)) ? c.getInt(idxYear) : null;
                String director = (idxDirector >= 0) ? c.getString(idxDirector) : null;
                Float rating = (idxRating >= 0 && !c.isNull(idxRating)) ? c.getFloat(idxRating) : null;
                return new Movie(t, img, lang, detail, cast, year, director, rating);
            }
            return null;
        } finally { c.close(); }
    }

    boolean existsByTitleIgnoreCase(String title) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM " + TABLE_MOVIES + " WHERE lower(" + COL_TITLE + ") = lower(?) LIMIT 1",
                new String[]{title}
        );
        try { return c.moveToFirst(); } finally { c.close(); }
    }

    int deleteDuplicateTitles() {
        return getWritableDatabase().delete(TABLE_MOVIES,
                COL_ID + " NOT IN (SELECT MIN(" + COL_ID + ") FROM " + TABLE_MOVIES + " GROUP BY lower(" + COL_TITLE + "))",
                null);
    }

    private int resolveDrawableId(String name) {
        if (name == null || name.trim().isEmpty()) return 0;
        try { return appContext.getResources().getIdentifier(name, "drawable", appContext.getPackageName()); }
        catch (Exception e) { return 0; }
    }

    void seedFromAssetsIfEmpty() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_MOVIES, null);
        try { if (c.moveToFirst() && c.getInt(0) > 0) return; } finally { c.close(); }
        try (InputStream is = appContext.getAssets().open("movies_seed.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.optJSONArray("movies"); if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("title");
                String language = o.optString("language");
                String category = o.optString("category", "now");
                String posterName = o.optString("posterName");
                String detailName = o.optString("detailName", null);
                String cast = o.optString("cast", null);
                Integer year = o.has("year") ? o.optInt("year") : null;
                String director = o.optString("director", null);
                Float rating = o.has("rating") ? (float) o.optDouble("rating") : null;
                if (!existsByTitleIgnoreCase(title)) {
                    insertMovieByName(title, language, posterName, category);
                    updateNames(title, posterName, detailName);
                    updateMovieAll(title, null, null, null, null, cast, year, director, rating);
                }
            }
        } catch (Exception ignored) {}
    }

    // Theatres API
    void seedTheatresIfEmpty() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_THEATRES, null);
        try {
            if (c.moveToFirst() && c.getInt(0) > 0) return;
        } finally { c.close(); }
        Map<String, String> theatres = new LinkedHashMap<>();
        theatres.put("Rajah", "Jaffna");
        theatres.put("CineCity Cinema", "Maradana");
        theatres.put("CPVR Cinema", "One Galle Face Mall");
        theatres.put("Regal Cinema", "Jaffna");
        theatres.put("Samantha Cinema", "Dematagoda");
        for (Map.Entry<String, String> e : theatres.entrySet()) {
            ContentValues v = new ContentValues();
            v.put(COL_T_NAME, e.getKey());
            v.put(COL_T_LOCATION, e.getValue());
            db.insertWithOnConflict(TABLE_THEATRES, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    void ensureShowsForMovieDate(String movie, String date) {
        seedTheatresIfEmpty();
        List<String> theatres = getAllTheatres();
        List<String> defaultTimes = Arrays.asList("10:00 AM", "01:30 PM", "06:00 PM", "09:00 PM");
        SQLiteDatabase db = getWritableDatabase();
        for (String theatre : theatres) {
            Cursor c = db.query(TABLE_SHOWS, null,
                    COL_S_MOVIE + "=? AND " + COL_S_DATE + "=? AND " + COL_S_THEATRE + "=?",
                    new String[]{movie, date, theatre}, null, null, null);
            boolean exists;
            try { exists = c.moveToFirst(); } finally { c.close(); }
            if (!exists) {
                for (String t : defaultTimes) {
                    ContentValues v = new ContentValues();
                    v.put(COL_S_MOVIE, movie);
                    v.put(COL_S_DATE, date);
                    v.put(COL_S_THEATRE, theatre);
                    v.put(COL_S_TIME, t);
                    db.insert(TABLE_SHOWS, null, v);
                }
            }
        }
    }

    List<String> getAllTheatres() {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_THEATRES, new String[]{COL_T_NAME}, null, null, null, null, COL_T_NAME);
        try {
            while (c.moveToNext()) list.add(c.getString(0));
        } finally { c.close(); }
        return list;
    }

    List<String> getTheatresForMovieAndDate(String movie, String date) {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT " + COL_S_THEATRE + " FROM " + TABLE_SHOWS +
                        " WHERE " + COL_S_MOVIE + "=? AND " + COL_S_DATE + "=? ORDER BY " + COL_S_THEATRE,
                new String[]{movie, date}
        );
        try {
            while (c.moveToNext()) list.add(c.getString(0));
        } finally { c.close(); }
        return list;
    }

    List<String> getShowTimes(String movie, String date, String theatre) {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_SHOWS, new String[]{COL_S_TIME},
                COL_S_MOVIE + "=? AND " + COL_S_DATE + "=? AND " + COL_S_THEATRE + "=?",
                new String[]{movie, date, theatre}, null, null, COL_S_TIME);
        try {
            while (c.moveToNext()) list.add(c.getString(0));
        } finally { c.close(); }
        return list;
    }

    // Bookings API
    List<String> getBookedSeats(String movie, String date, String theatre, String time) {
        List<String> seats = new ArrayList<>();
        Cursor c = getReadableDatabase().query(
                TABLE_BOOKINGS,
                new String[]{COL_B_SEAT},
                COL_B_MOVIE + "=? AND " + COL_B_DATE + "=? AND " + COL_B_THEATRE + "=? AND " + COL_B_TIME + "=?",
                new String[]{movie, date, theatre, time},
                null, null, null
        );
        try {
            while (c.moveToNext()) seats.add(c.getString(0));
        } finally { c.close(); }
        return seats;
    }

    void bookSeats(String movie, String date, String theatre, String time, List<String> seatLabels) {
        if (seatLabels == null || seatLabels.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String seat : seatLabels) {
                ContentValues v = new ContentValues();
                v.put(COL_B_MOVIE, movie);
                v.put(COL_B_DATE, date);
                v.put(COL_B_THEATRE, theatre);
                v.put(COL_B_TIME, time);
                v.put(COL_B_SEAT, seat);
                db.insertWithOnConflict(TABLE_BOOKINGS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void savePayment(String movie, String date, String theatre, String time,
                     List<String> seatLabels, int seatCount, int amount,
                     String email, String phone, String nameOnCard, String cardLast4) {
        ContentValues v = new ContentValues();
        v.put(COL_P_MOVIE, movie);
        v.put(COL_P_DATE, date);
        v.put(COL_P_THEATRE, theatre);
        v.put(COL_P_TIME, time);
        String seatsCsv = seatLabels != null ? android.text.TextUtils.join(", ", seatLabels) : "";
        v.put(COL_P_SEATS, seatsCsv);
        v.put(COL_P_SEAT_COUNT, seatCount);
        v.put(COL_P_AMOUNT, amount);
        v.put(COL_P_EMAIL, email);
        v.put(COL_P_PHONE, phone);
        v.put(COL_P_NAME_ON_CARD, nameOnCard);
        v.put(COL_P_CARD_LAST4, cardLast4);
        getWritableDatabase().insert(TABLE_PAYMENTS, null, v);
    }

    Cursor getLatestPaymentByEmail(String email) {
        return getReadableDatabase().query(
                TABLE_PAYMENTS,
                null,
                COL_P_EMAIL + "=?",
                new String[]{email},
                null, null,
                COL_P_ID + " DESC",
                "1"
        );
    }

    int getPosterResIdForMovieTitle(String title) {
        if (title == null) return 0;
        Cursor c = getReadableDatabase().query(TABLE_MOVIES,
                new String[]{COL_POSTER_NAME}, COL_TITLE + "=?", new String[]{title},
                null, null, null, "1");
        try {
            if (c.moveToFirst()) {
                String posterName = c.getString(0);
                return resolveDrawableId(posterName);
            }
        } finally { c.close(); }
        return 0;
    }
}


