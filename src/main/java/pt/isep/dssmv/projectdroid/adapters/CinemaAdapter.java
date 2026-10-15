package pt.isep.dssmv.projectdroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.model.Cinema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CinemaAdapter extends RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder> {

    public interface OnDirectionsClickListener {
        void onDirectionsClick(Cinema cinema);
    }

    private final List<Cinema> cinemas = new ArrayList<>();
    private final OnDirectionsClickListener directionsClickListener;

    public CinemaAdapter(OnDirectionsClickListener directionsClickListener) {
        this.directionsClickListener = directionsClickListener;
    }

    public void setCinemas(List<Cinema> newCinemas) {
        this.cinemas.clear();
        if (newCinemas != null) {
            this.cinemas.addAll(newCinemas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CinemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cinema, parent, false);
        return new CinemaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CinemaViewHolder holder, int position) {
        Cinema cinema = cinemas.get(position);
        holder.bind(cinema, directionsClickListener);
    }

    @Override
    public int getItemCount() {
        return cinemas.size();
    }

    static class CinemaViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewName;
        private final TextView textViewDistance;
        private final TextView textViewAddress;
        private final Button buttonDirections;

        public CinemaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewCinemaName);
            textViewDistance = itemView.findViewById(R.id.textViewCinemaDistance);
            textViewAddress = itemView.findViewById(R.id.textViewCinemaAddress);
            buttonDirections = itemView.findViewById(R.id.buttonGetDirections);
        }

        public void bind(Cinema cinema, OnDirectionsClickListener listener) {
            textViewName.setText(cinema.getName());

            int distance = cinema.getDistanceMeters();
            if (distance > 0) {
                if (distance >= 1000) {
                    double km = distance / 1000.0;
                    textViewDistance.setText(String.format(Locale.getDefault(), "%.1f km", km));
                } else {
                    textViewDistance.setText(String.format(Locale.getDefault(), "%d m", distance));
                }
                textViewDistance.setVisibility(View.VISIBLE);
            } else {
                textViewDistance.setVisibility(View.GONE);
            }

            String address = cinema.getAddress();
            if (address != null && !address.trim().isEmpty()) {
                textViewAddress.setText(address);
                textViewAddress.setVisibility(View.VISIBLE);
            } else {
                textViewAddress.setVisibility(View.GONE);
            }

            buttonDirections.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDirectionsClick(cinema);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDirectionsClick(cinema);
                }
            });
        }
    }
}
