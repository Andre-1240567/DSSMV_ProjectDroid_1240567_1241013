package pt.isep.dssmv.projectdroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.network.dto.TmdbMovieDto;

import java.util.ArrayList;
import java.util.List;

public class MovieSearchAdapter extends RecyclerView.Adapter<MovieSearchAdapter.MovieViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(TmdbMovieDto movie);
    }

    private final List<TmdbMovieDto> movies = new ArrayList<>();
    private final OnMovieClickListener clickListener;

    public MovieSearchAdapter(OnMovieClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setMovies(List<TmdbMovieDto> newMovies) {
        this.movies.clear();
        if (newMovies != null) {
            this.movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_search, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        TmdbMovieDto movie = movies.get(position);
        holder.bind(movie, clickListener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageViewPoster;
        private final TextView textViewTitle;
        private final TextView textViewYear;
        private final TextView textViewOverview;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPoster = itemView.findViewById(R.id.imageViewMoviePoster);
            textViewTitle = itemView.findViewById(R.id.textViewMovieTitle);
            textViewYear = itemView.findViewById(R.id.textViewMovieYear);
            textViewOverview = itemView.findViewById(R.id.textViewMovieOverview);
        }

        public void bind(TmdbMovieDto movie, OnMovieClickListener clickListener) {
            textViewTitle.setText(movie.getTitle());

            int year = movie.getReleaseYear();
            if (year > 0) {
                textViewYear.setText(String.valueOf(year));
                textViewYear.setVisibility(View.VISIBLE);
            } else {
                textViewYear.setVisibility(View.GONE);
            }

            String overview = movie.getOverview();
            if (overview != null && !overview.trim().isEmpty()) {
                textViewOverview.setText(overview);
                textViewOverview.setVisibility(View.VISIBLE);
            } else {
                textViewOverview.setVisibility(View.GONE);
            }

            // AC04: Async image loading using Glide with placeholder and error
            Glide.with(itemView.getContext())
                    .load(movie.getFullPosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_error)
                    .into(imageViewPoster);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMovieClick(movie);
                }
            });
        }
    }
}
