package pt.isep.dssmv.projectdroid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import pt.isep.dssmv.projectdroid.R;
import pt.isep.dssmv.projectdroid.model.MovieList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.ViewHolder> {

    public interface OnListClickListener {
        void onListClick(MovieList movieList);
    }

    public interface OnListDeleteClickListener {
        void onListDelete(MovieList movieList);
    }

    private final List<MovieList> lists = new ArrayList<>();
    private final OnListClickListener clickListener;
    private final OnListDeleteClickListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public MovieListAdapter(OnListClickListener clickListener, OnListDeleteClickListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setLists(List<MovieList> newLists) {
        this.lists.clear();
        if (newLists != null) {
            this.lists.addAll(newLists);
        }
        notifyDataSetChanged();
    }

    public void addList(MovieList list) {
        if (list != null) {
            this.lists.add(0, list);
            notifyItemInserted(0);
        }
    }

    public void removeList(String listId) {
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).getListId().equals(listId)) {
                lists.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieList movieList = lists.get(position);
        holder.textViewName.setText(movieList.getName());

        if (movieList.getCreatedAt() != null) {
            holder.textViewDate.setText("Created: " + dateFormat.format(movieList.getCreatedAt()));
        } else {
            holder.textViewDate.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onListClick(movieList);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onListDelete(movieList);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textViewName;
        public final TextView textViewDate;
        public final ImageButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewItemListName);
            textViewDate = itemView.findViewById(R.id.textViewItemCreatedAt);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteList);
        }
    }
}
