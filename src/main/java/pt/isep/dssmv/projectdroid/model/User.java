package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.Date;
import java.util.Objects;

public class User {

    private String userId;
    private String name;
    private String email;
    private Date registrationDate;

    public User() {
        this.registrationDate = new Date();
    }

    public User(String userId, String name, String email) throws InvalidDataException {
        this(userId, name, email, new Date());
    }

    public User(String userId, String name, String email, Date registrationDate) throws InvalidDataException {
        setUserId(userId);
        setName(name);
        setEmail(email);
        this.registrationDate = (registrationDate != null) ? registrationDate : new Date();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) throws InvalidDataException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidDataException("User ID cannot be empty");
        }
        this.userId = userId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidDataException {
        if (name == null || name.trim().length() < 2) {
            throw new InvalidDataException("Name must have at least 2 characters");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) throws InvalidDataException {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new InvalidDataException("Invalid email format");
        }
        this.email = email.trim().toLowerCase();
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = (registrationDate != null) ? registrationDate : new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}
