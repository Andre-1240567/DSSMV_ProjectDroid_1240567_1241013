package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;

import java.util.Date;
import java.util.Objects;

public class Rating {

    private String ratingId;
    private float score;
    private String comment;
    private Date createdAt;

    public Rating() {
        this.createdAt = new Date();
    }

    public Rating(String ratingId, float score, String comment) throws InvalidDataException, InvalidRatingException {
        this(ratingId, score, comment, new Date());
    }

    public Rating(String ratingId, float score, String comment, Date createdAt) throws InvalidDataException, InvalidRatingException {
        setRatingId(ratingId);
        setScore(score);
        setComment(comment);
        this.createdAt = (createdAt != null) ? createdAt : new Date();
    }

    public String getRatingId() {
        return ratingId;
    }

    public void setRatingId(String ratingId) throws InvalidDataException {
        if (ratingId == null || ratingId.trim().isEmpty()) {
            throw new InvalidDataException("Rating ID cannot be empty");
        }
        this.ratingId = ratingId.trim();
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) throws InvalidRatingException {
        if (score < 1.0f || score > 5.0f) {
            throw new InvalidRatingException("Score must be between 1.0 and 5.0");
        }
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = (comment != null) ? comment.trim() : "";
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rating rating)) return false;
        return Objects.equals(ratingId, rating.ratingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratingId);
    }

    @Override
    public String toString() {
        return score + "★" + (comment.isEmpty() ? "" : " - " + comment);
    }
}
