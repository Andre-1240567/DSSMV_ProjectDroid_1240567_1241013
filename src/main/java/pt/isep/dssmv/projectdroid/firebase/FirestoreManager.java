package pt.isep.dssmv.projectdroid.firebase;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.MovieList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static FirestoreManager instance;
    private final FirebaseFirestore db;

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    private FirestoreManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public void createMovieList(String userId, MovieList list, FirestoreCallback<Void> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("listId", list.getListId());
        data.put("name", list.getName());
        data.put("createdAt", (list.getCreatedAt() != null) ? list.getCreatedAt() : new Date());

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(list.getListId())
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUserMovieLists(String userId, FirestoreCallback<List<MovieList>> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovieList> result = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getString("listId");
                        String name = doc.getString("name");
                        Timestamp timestamp = doc.getTimestamp("createdAt");
                        Date createdAt = (timestamp != null) ? timestamp.toDate() : new Date();

                        if (id != null && name != null) {
                            try {
                                result.add(new MovieList(id, name, createdAt));
                            } catch (InvalidDataException ignored) {
                                // Skip corrupted documents
                            }
                        }
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteMovieList(String userId, String listId, FirestoreCallback<Void> callback) {
        if (userId == null || listId == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
