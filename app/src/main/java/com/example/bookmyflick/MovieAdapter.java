package com.example.bookmyflick;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Simple RecyclerView adapter rendering movie poster, language and title.
 * Supports swapping item layout (card vs grid) via constructor overload.
 */
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private final Context context;
    private final List<Movie> movieList;
    private final int itemLayoutResId;

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
        this.itemLayoutResId = R.layout.item_movie_card;
    }

    public MovieAdapter(Context context, List<Movie> movieList, int itemLayoutResId) {
        this.context = context;
        this.movieList = movieList;
        this.itemLayoutResId = itemLayoutResId;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutToInflate = itemLayoutResId == 0 ? R.layout.item_movie_card : itemLayoutResId;
        View view = LayoutInflater.from(context).inflate(layoutToInflate, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        // Safely set image with fallback to launcher icon if invalid ID in DB
        int resId = movie.getImageResId();
        try {
            holder.movieImage.setImageDrawable(AppCompatResources.getDrawable(context, resId));
        } catch (Resources.NotFoundException | NullPointerException e) {
            holder.movieImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_launcher_foreground));
        }
        holder.movieTitle.setText(movie.getTitle());
        holder.movieLanguage.setText(movie.getLanguage());

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, MovieDetailActivity.class);
            intent.putExtra("movie", movie);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                context.startActivity(intent);
            } catch (Exception ignored) {}
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    /**
     * Replace current dataset and refresh the list.
     */
    public void updateList(List<Movie> newList) {
        movieList.clear();
        movieList.addAll(newList);
        notifyDataSetChanged();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        final ImageView movieImage;
        final TextView movieTitle;
        final TextView movieLanguage;

        MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage = itemView.findViewById(R.id.movie_image);
            movieTitle = itemView.findViewById(R.id.movie_title);
            movieLanguage = itemView.findViewById(R.id.movie_language);
        }
    }
}
