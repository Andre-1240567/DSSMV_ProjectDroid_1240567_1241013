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
import pt.isep.dssmv.projectdroid.model.WatchProvider;

import java.util.ArrayList;
import java.util.List;

public class WatchProviderAdapter extends RecyclerView.Adapter<WatchProviderAdapter.ProviderViewHolder> {

    private final List<WatchProvider> providers = new ArrayList<>();

    public void setProviders(List<WatchProvider> newProviders) {
        this.providers.clear();
        if (newProviders != null) {
            this.providers.addAll(newProviders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watch_provider, parent, false);
        return new ProviderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        WatchProvider provider = providers.get(position);
        holder.bind(provider);
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class ProviderViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageViewLogo;
        private final TextView textViewName;
        private final TextView textViewAccessType;

        public ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewLogo = itemView.findViewById(R.id.imageViewProviderLogo);
            textViewName = itemView.findViewById(R.id.textViewProviderName);
            textViewAccessType = itemView.findViewById(R.id.textViewAccessType);
        }

        public void bind(WatchProvider provider) {
            textViewName.setText(provider.getName());

            String type = provider.getAccessType();
            if ("flatrate".equalsIgnoreCase(type)) {
                textViewAccessType.setText("Stream");
            } else if ("rent".equalsIgnoreCase(type)) {
                textViewAccessType.setText("Rent");
            } else if ("buy".equalsIgnoreCase(type)) {
                textViewAccessType.setText("Buy");
            } else {
                textViewAccessType.setText(type != null ? type : "");
            }

            String logoUrl = provider.getLogoPath();
            if (logoUrl != null && !logoUrl.startsWith("http")) {
                logoUrl = "https://image.tmdb.org/t/p/w154" + logoUrl;
            }

            Glide.with(itemView.getContext())
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_error)
                    .into(imageViewLogo);
        }
    }
}
