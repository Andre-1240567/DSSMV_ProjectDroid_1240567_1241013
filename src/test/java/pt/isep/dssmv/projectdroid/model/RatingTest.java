package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;
import pt.isep.dssmv.projectdroid.exceptions.InvalidRatingException;

import static org.junit.jupiter.api.Assertions.*;

public class RatingTest {

    @Test
    public void testValidRatingCreation() throws InvalidDataException, InvalidRatingException {
        Rating rating = new Rating("r1", 4.5f, "Great movie!");

        assertEquals("r1", rating.getRatingId());
        assertEquals(4.5f, rating.getScore());
        assertEquals("Great movie!", rating.getComment());
        assertNotNull(rating.getCreatedAt());
    }

    @Test
    public void testScoreBelowMinimumThrowsException() {
        assertThrows(InvalidRatingException.class, () -> {
            new Rating("r1", 0.5f, "Bad");
        });
    }

    @Test
    public void testScoreAboveMaximumThrowsException() {
        assertThrows(InvalidRatingException.class, () -> {
            new Rating("r1", 5.5f, "Awesome");
        });
    }

    @Test
    public void testBoundaryScoresAreValid() throws InvalidDataException, InvalidRatingException {
        Rating minRating = new Rating("r1", 1.0f, "Min score");
        Rating maxRating = new Rating("r2", 5.0f, "Max score");

        assertEquals(1.0f, minRating.getScore());
        assertEquals(5.0f, maxRating.getScore());
    }

    @Test
    public void testEmptyRatingIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new Rating("  ", 3.0f, "Nice");
        });
    }
}
