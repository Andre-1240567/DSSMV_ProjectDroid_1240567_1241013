package pt.isep.dssmv.projectdroid.network;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.network.dto.TmdbGuestSessionResponse;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingRequest;
import pt.isep.dssmv.projectdroid.network.dto.TmdbRatingResponse;

import static org.junit.jupiter.api.Assertions.*;

public class TmdbRatingDtoTest {

    private final Gson gson = new Gson();

    @Test
    public void testRatingRequestSerialization() {
        TmdbRatingRequest request = new TmdbRatingRequest(8.5);
        assertEquals(8.5, request.getValue());

        String json = gson.toJson(request);
        assertTrue(json.contains("\"value\":8.5") || json.contains("\"value\": 8.5"));

        request.setValue(9.0);
        assertEquals(9.0, request.getValue());
    }

    @Test
    public void testRatingResponseDeserialization() {
        String json = "{\"status_code\": 1, \"status_message\": \"Success.\", \"success\": true}";
        TmdbRatingResponse response = gson.fromJson(json, TmdbRatingResponse.class);

        assertNotNull(response);
        assertEquals(1, response.getStatusCode());
        assertEquals("Success.", response.getStatusMessage());
        assertTrue(response.isSuccess());

        TmdbRatingResponse manual = new TmdbRatingResponse(12, "The item was updated successfully.", true);
        assertEquals(12, manual.getStatusCode());
        assertEquals("The item was updated successfully.", manual.getStatusMessage());
        assertTrue(manual.isSuccess());
    }

    @Test
    public void testGuestSessionResponseDeserialization() {
        String json = "{\"success\": true, \"guest_session_id\": \"xyz789abc\", \"expires_at\": \"2026-10-24 12:00:00 UTC\"}";
        TmdbGuestSessionResponse response = gson.fromJson(json, TmdbGuestSessionResponse.class);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("xyz789abc", response.getGuestSessionId());
        assertEquals("2026-10-24 12:00:00 UTC", response.getExpiresAt());

        TmdbGuestSessionResponse manual = new TmdbGuestSessionResponse(true, "sess123", "2026-12-31");
        assertTrue(manual.isSuccess());
        assertEquals("sess123", manual.getGuestSessionId());
        assertEquals("2026-12-31", manual.getExpiresAt());
    }
}
