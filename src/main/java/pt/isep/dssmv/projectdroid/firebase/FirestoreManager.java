package pt.isep.dssmv.projectdroid.firebase;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.model.MovieList;
import pt.isep.dssmv.projectdroid.model.Rating;
import pt.isep.dssmv.projectdroid.model.SavedMovie;
import pt.isep.dssmv.projectdroid.model.WatchStatus;

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

    public com.google.firebase.firestore.ListenerRegistration attachMovieListsListener(String userId, FirestoreCallback<List<MovieList>> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure("Invalid user ID");
            return null;
        }

        return db.collection("users")
                .document(userId)
                .collection("lists")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<MovieList> result = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            String id = doc.getString("listId");
                            String name = doc.getString("name");
                            Timestamp timestamp = doc.getTimestamp("createdAt");
                            Date createdAt = (timestamp != null) ? timestamp.toDate() : new Date();

                            if (id != null && name != null) {
                                try {
                                    result.add(new MovieList(id, name, createdAt));
                                } catch (InvalidDataException ignored) {
                                }
                            }
                        }
                    }
                    callback.onSuccess(result);
                });
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

    public void addMovieToList(String userId, String listId, SavedMovie movie, FirestoreCallback<Void> callback) {
        if (userId == null || listId == null || movie == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("internalId", movie.getInternalId());
        data.put("tmdbId", movie.getTmdbId());
        data.put("title", movie.getTitle());
        data.put("releaseYear", movie.getReleaseYear());
        data.put("overview", movie.getOverview() != null ? movie.getOverview() : "");
        data.put("status", movie.getStatus() != null ? movie.getStatus().name() : WatchStatus.PLAN_TO_WATCH.name());
        data.put("posterPath", movie.getPosterPath() != null ? movie.getPosterPath() : "");
        data.put("createdAt", new Date());

        if (movie.getRating() != null) {
            data.put("ratingScore", (double) movie.getRating().getScore());
            data.put("ratingComment", movie.getRating().getComment());
            data.put("ratingId", movie.getRating().getRatingId());
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .document(movie.getInternalId())
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public ListenerRegistration attachMoviesInListListener(String userId, String listId, FirestoreCallback<List<SavedMovie>> callback) {
        if (userId == null || listId == null) {
            callback.onFailure("Invalid parameters");
            return null;
        }

        return db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<SavedMovie> movies = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String internalId = doc.getString("internalId");
                            Long tmdbIdLong = doc.getLong("tmdbId");
                            int tmdbId = (tmdbIdLong != null) ? tmdbIdLong.intValue() : 1;
                            String title = doc.getString("title");

                            if (internalId != null && title != null) {
                                try {
                                    SavedMovie movie = new SavedMovie(internalId, tmdbId, title);
                                    Long yearLong = doc.getLong("releaseYear");
                                    if (yearLong != null) {
                                        movie.setReleaseYear(yearLong.intValue());
                                    }
                                    movie.setOverview(doc.getString("overview"));
                                    movie.setPosterPath(doc.getString("posterPath"));

                                    String statusStr = doc.getString("status");
                                    if (statusStr != null) {
                                        try {
                                            movie.setStatus(WatchStatus.valueOf(statusStr));
                                        } catch (IllegalArgumentException ignored) {
                                            movie.setStatus(WatchStatus.PLAN_TO_WATCH);
                                        }
                                    }

                                    Double scoreDouble = doc.getDouble("ratingScore");
                                    String comment = doc.getString("ratingComment");
                                    String ratingId = doc.getString("ratingId");
                                    if (scoreDouble != null) {
                                        try {
                                            String rId = (ratingId != null) ? ratingId : internalId;
                                            Rating rating = new Rating(rId, scoreDouble.floatValue(), comment != null ? comment : "");
                                            movie.setRating(rating);
                                        } catch (Exception ignored) {
                                        }
                                    }

                                    movies.add(movie);
                                } catch (InvalidDataException ignored) {
                                    // Skip corrupted movie data
                                }
                            }
                        }
                    }
                    callback.onSuccess(movies);
                });
    }

    public void getMoviesInList(String userId, String listId, FirestoreCallback<List<SavedMovie>> callback) {
        if (userId == null || listId == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SavedMovie> movies = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String internalId = doc.getString("internalId");
                        Long tmdbIdLong = doc.getLong("tmdbId");
                        int tmdbId = (tmdbIdLong != null) ? tmdbIdLong.intValue() : 1;
                        String title = doc.getString("title");

                        if (internalId != null && title != null) {
                            try {
                                SavedMovie movie = new SavedMovie(internalId, tmdbId, title);
                                Long yearLong = doc.getLong("releaseYear");
                                if (yearLong != null) {
                                    movie.setReleaseYear(yearLong.intValue());
                                }
                                movie.setOverview(doc.getString("overview"));
                                movie.setPosterPath(doc.getString("posterPath"));

                                String statusStr = doc.getString("status");
                                if (statusStr != null) {
                                    try {
                                        movie.setStatus(WatchStatus.valueOf(statusStr));
                                    } catch (IllegalArgumentException ignored) {
                                        movie.setStatus(WatchStatus.PLAN_TO_WATCH);
                                    }
                                }

                                Double scoreDouble = doc.getDouble("ratingScore");
                                String comment = doc.getString("ratingComment");
                                String ratingId = doc.getString("ratingId");
                                if (scoreDouble != null) {
                                    try {
                                        String rId = (ratingId != null) ? ratingId : internalId;
                                        Rating rating = new Rating(rId, scoreDouble.floatValue(), comment != null ? comment : "");
                                        movie.setRating(rating);
                                    } catch (Exception ignored) {
                                    }
                                }

                                movies.add(movie);
                            } catch (InvalidDataException ignored) {
                                // Skip corrupted movie data
                            }
                        }
                    }
                    callback.onSuccess(movies);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteMovieFromList(String userId, String listId, String internalId, FirestoreCallback<Void> callback) {
        if (userId == null || listId == null || internalId == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .document(internalId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void rateMovieInList(String userId, String listId, String internalId, Rating rating, FirestoreCallback<Void> callback) {
        if (userId == null || listId == null || internalId == null || rating == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("ratingScore", (double) rating.getScore());
        updates.put("ratingComment", rating.getComment());
        updates.put("ratingId", rating.getRatingId());

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .document(internalId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateMovieStatusInList(String userId, String listId, String internalId, WatchStatus newStatus, FirestoreCallback<Void> callback) {
        if (userId == null || listId == null || internalId == null || newStatus == null) {
            callback.onFailure("Invalid parameters");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("lists")
                .document(listId)
                .collection("movies")
                .document(internalId)
                .update("status", newStatus.name())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
