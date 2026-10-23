package pt.isep.dssmv.projectdroid.network;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.network.dto.FsqPlaceDto;

import static org.junit.jupiter.api.Assertions.*;

public class FsqPlaceDtoTest {

    private final Gson gson = new Gson();

    @Test
    public void testModernPlacesApiResponseDeserialization() {
        String json = "{\n" +
                "  \"fsq_place_id\": \"modern_123\",\n" +
                "  \"name\": \"Cinema Trindade\",\n" +
                "  \"distance\": 450,\n" +
                "  \"latitude\": 41.1504,\n" +
                "  \"longitude\": -8.6095,\n" +
                "  \"location\": {\n" +
                "    \"formatted_address\": \"Rua do Almada 412, Porto\"\n" +
                "  }\n" +
                "}";

        FsqPlaceDto dto = gson.fromJson(json, FsqPlaceDto.class);

        assertNotNull(dto);
        assertEquals("modern_123", dto.getFsqId());
        assertEquals("Cinema Trindade", dto.getName());
        assertEquals(450, dto.getDistance());
        assertEquals(41.1504, dto.getLatitude(), 0.0001);
        assertEquals(-8.6095, dto.getLongitude(), 0.0001);
        assertEquals("Rua do Almada 412, Porto", dto.getDisplayAddress());
    }

    @Test
    public void testLegacyPlacesApiResponseDeserialization() {
        String json = "{\n" +
                "  \"fsq_id\": \"legacy_456\",\n" +
                "  \"name\": \"NOS NorteShopping\",\n" +
                "  \"distance\": 1200,\n" +
                "  \"geocodes\": {\n" +
                "    \"main\": {\n" +
                "      \"latitude\": 41.1800,\n" +
                "      \"longitude\": -8.6500\n" +
                "    }\n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"address\": \"Rua Sara Afonso\"\n" +
                "  }\n" +
                "}";

        FsqPlaceDto dto = gson.fromJson(json, FsqPlaceDto.class);

        assertNotNull(dto);
        assertEquals("legacy_456", dto.getFsqId());
        assertEquals("NOS NorteShopping", dto.getName());
        assertEquals(1200, dto.getDistance());
        assertEquals(41.1800, dto.getLatitude(), 0.0001);
        assertEquals(-8.6500, dto.getLongitude(), 0.0001);
        assertEquals("Rua Sara Afonso", dto.getDisplayAddress());
    }

    @Test
    public void testEmptyAndFallbackCoordinatesAndAddress() {
        String json = "{\n" +
                "  \"fsq_place_id\": \"minimal_789\",\n" +
                "  \"name\": \"Independent Cinema\"\n" +
                "}";

        FsqPlaceDto dto = gson.fromJson(json, FsqPlaceDto.class);

        assertNotNull(dto);
        assertEquals("minimal_789", dto.getFsqId());
        assertEquals("Independent Cinema", dto.getName());
        assertEquals(0.0, dto.getLatitude(), 0.0001);
        assertEquals(0.0, dto.getLongitude(), 0.0001);
        assertEquals("", dto.getDisplayAddress());
    }
}
