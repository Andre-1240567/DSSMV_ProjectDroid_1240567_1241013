package pt.isep.dssmv.projectdroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.network.TmdbMovieDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TmdbMovieAdapter extends RecyclerView.Adapter<TmdbMovieAdapter.ViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(TmdbMovieDto movie);
    }

    public interface OnMovieAddClickListener {
        void onMovieAddClick(TmdbMovieDto movie);
    }

    private final List<TmdbMovieDto> movies = new ArrayList<>();
    private final OnMovieClickListener movieClickListener;
    private final OnMovieAddClickListener addClickListener;

    public TmdbMovieAdapter(OnMovieClickListener movieClickListener, OnMovieAddClickListener addClickListener) {
        this.movieClickListener = movieClickListener;
        this.addClickListener = addClickListener;
    }

    public TmdbMovieAdapter(OnMovieAddClickListener addClickListener) {
        this(null, addClickListener);
    }

    public void addMovies(List<TmdbMovieDto> newMovies) {
        if (newMovies != null && !newMovies.isEmpty()) {
            int startPos = this.movies.size();
            this.movies.addAll(newMovies);
            notifyItemRangeInserted(startPos, newMovies.size());
        }
    }

    public void clear() {
        this.movies.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tmdb_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TmdbMovieDto movie = movies.get(position);

        holder.textViewTitle.setText(movie.getTitle());

        if (movie.getReleaseYear() > 0) {
            holder.textViewYear.setText(String.valueOf(movie.getReleaseYear()));
            holder.textViewYear.setVisibility(View.VISIBLE);
        } else {
            holder.textViewYear.setVisibility(View.GONE);
        }

        if (movie.getVoteAverage() > 0) {
            holder.textViewVoteAverage.setText(String.format(Locale.getDefault(), "⭐ %.1f", movie.getVoteAverage()));
            holder.textViewVoteAverage.setVisibility(View.VISIBLE);
        } else {
            holder.textViewVoteAverage.setVisibility(View.GONE);
        }

        if (!movie.getOverview().isEmpty()) {
            holder.textViewOverview.setText(movie.getOverview());
            holder.textViewOverview.setVisibility(View.VISIBLE);
        } else {
            holder.textViewOverview.setVisibility(View.GONE);
        }

        if (movie.getFullPosterUrl() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(movie.getFullPosterUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.imageViewPoster);
        } else {
            holder.imageViewPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (movieClickListener != null) {
                movieClickListener.onMovieClick(movie);
            } else if (addClickListener != null) {
                addClickListener.onMovieAddClick(movie);
            }
        });

        holder.buttonAddToList.setOnClickListener(v -> {
            if (addClickListener != null) {
                addClickListener.onMovieAddClick(movie);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageViewPoster;
        public final TextView textViewTitle;
        public final TextView textViewYear;
        public final TextView textViewVoteAverage;
        public final TextView textViewOverview;
        public final Button buttonAddToList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPoster = itemView.findViewById(R.id.imageViewPoster);
            textViewTitle = itemView.findViewById(R.id.textViewMovieTitle);
            textViewYear = itemView.findViewById(R.id.textViewMovieYear);
            textViewVoteAverage = itemView.findViewById(R.id.textViewMovieVoteAverage);
            textViewOverview = itemView.findViewById(R.id.textViewMovieOverview);
            buttonAddToList = itemView.findViewById(R.id.buttonAddToList);
        }
    }
}
