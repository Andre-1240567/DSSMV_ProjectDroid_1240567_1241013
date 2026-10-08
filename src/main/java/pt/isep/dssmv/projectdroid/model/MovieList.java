package pt.isep.dssmv.projectdroid.model;

import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import java.util.Date;
import java.util.Objects;

public class MovieList {

    private String listId;
    private String name;
    private Date createdAt;

    public MovieList() {
        this.createdAt = new Date();
    }

    public MovieList(String listId, String name) throws InvalidDataException {
        this(listId, name, new Date());
    }

    public MovieList(String listId, String name, Date createdAt) throws InvalidDataException {
        setListId(listId);
        setName(name);
        this.createdAt = (createdAt != null) ? createdAt : new Date();
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) throws InvalidDataException {
        if (listId == null || listId.trim().isEmpty()) {
            throw new InvalidDataException("List ID cannot be empty");
        }
        this.listId = listId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("List name cannot be empty");
        }
        this.name = name.trim();
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
        if (!(o instanceof MovieList that)) return false;
        return Objects.equals(listId, that.listId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listId);
    }

    @Override
    public String toString() {
        return name;
    }
}
