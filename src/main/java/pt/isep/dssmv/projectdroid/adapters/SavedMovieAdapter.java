package pt.isep.dssmv.projectdroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.WatchStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavedMovieAdapter extends RecyclerView.Adapter<SavedMovieAdapter.ViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(SavedMovie movie);
    }

    public interface OnMovieDeleteClickListener {
        void onMovieDelete(SavedMovie movie);
    }

    public interface OnMovieRateClickListener {
        void onMovieRate(SavedMovie movie);
    }

    public interface OnMovieShareClickListener {
        void onMovieShare(SavedMovie movie);
    }

    private final List<SavedMovie> movies = new ArrayList<>();
    private final OnMovieClickListener clickListener;
    private final OnMovieDeleteClickListener deleteListener;
    private final OnMovieRateClickListener rateListener;
    private final OnMovieShareClickListener shareListener;

    public SavedMovieAdapter(OnMovieClickListener clickListener,
                             OnMovieDeleteClickListener deleteListener,
                             OnMovieRateClickListener rateListener) {
        this(clickListener, deleteListener, rateListener, null);
    }

    public SavedMovieAdapter(OnMovieClickListener clickListener,
                             OnMovieDeleteClickListener deleteListener,
                             OnMovieRateClickListener rateListener,
                             OnMovieShareClickListener shareListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.rateListener = rateListener;
        this.shareListener = shareListener;
    }

    public void setMovies(List<SavedMovie> newMovies) {
        this.movies.clear();
        if (newMovies != null) {
            this.movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    public void addMovie(SavedMovie movie) {
        if (movie != null) {
            this.movies.add(0, movie);
            notifyItemInserted(0);
        }
    }

    public void removeMovie(String internalId) {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getInternalId().equals(internalId)) {
                movies.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateMovie(SavedMovie movie) {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getInternalId().equals(movie.getInternalId())) {
                movies.set(i, movie);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedMovie movie = movies.get(position);

        holder.textViewTitle.setText(movie.getTitle());

        String posterPath = movie.getPosterPath();
        if (posterPath != null && !posterPath.trim().isEmpty()) {
            String fullUrl = posterPath.startsWith("http")
                    ? posterPath
                    : "https://image.tmdb.org/t/p/w500" + posterPath;

            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_error)
                    .into(holder.imageViewPoster);
        } else {
            holder.imageViewPoster.setImageResource(R.drawable.ic_movie_placeholder);
        }

        if (movie.getReleaseYear() > 0) {
            holder.textViewYear.setText(String.valueOf(movie.getReleaseYear()));
            holder.textViewYear.setVisibility(View.VISIBLE);
        } else {
            holder.textViewYear.setVisibility(View.GONE);
        }

        WatchStatus status = movie.getStatus() != null ? movie.getStatus() : WatchStatus.PLAN_TO_WATCH;
        holder.textViewStatus.setText(formatWatchStatus(status));

        Rating rating = movie.getRating();
        if (rating != null && rating.getScore() > 0) {
            holder.textViewRating.setText(String.format(Locale.getDefault(), "★ %.1f / 5.0", rating.getScore()));
            if (rating.getComment() != null && !rating.getComment().trim().isEmpty()) {
                holder.textViewComment.setText(rating.getComment());
                holder.textViewComment.setVisibility(View.VISIBLE);
            } else {
                holder.textViewComment.setVisibility(View.GONE);
            }
        } else {
            holder.textViewRating.setText(R.string.no_rating_yet);
            holder.textViewComment.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMovieClick(movie);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onMovieDelete(movie);
            }
        });

        holder.buttonRate.setOnClickListener(v -> {
            if (rateListener != null) {
                rateListener.onMovieRate(movie);
            }
        });

        if (holder.buttonShare != null) {
            holder.buttonShare.setOnClickListener(v -> {
                if (shareListener != null) {
                    shareListener.onMovieShare(movie);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    private String formatWatchStatus(WatchStatus status) {
        return switch (status) {
            case WATCHING -> "Watching";
            case WATCHED -> "Watched";
            case FAVORITE -> "Favorite";
            default -> "Plan to Watch";
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageViewPoster;
        public final TextView textViewTitle;
        public final TextView textViewYear;
        public final TextView textViewStatus;
        public final TextView textViewRating;
        public final TextView textViewComment;
        public final ImageButton buttonDelete;
        public final ImageButton buttonShare;
        public final Button buttonRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPoster = itemView.findViewById(R.id.imageViewSavedMoviePoster);
            textViewTitle = itemView.findViewById(R.id.textViewMovieTitle);
            textViewYear = itemView.findViewById(R.id.textViewMovieYear);
            textViewStatus = itemView.findViewById(R.id.textViewMovieStatus);
            textViewRating = itemView.findViewById(R.id.textViewMovieRating);
            textViewComment = itemView.findViewById(R.id.textViewMovieComment);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteMovie);
            buttonShare = itemView.findViewById(R.id.buttonShareMovie);
            buttonRate = itemView.findViewById(R.id.buttonRateMovie);
        }
    }
}
