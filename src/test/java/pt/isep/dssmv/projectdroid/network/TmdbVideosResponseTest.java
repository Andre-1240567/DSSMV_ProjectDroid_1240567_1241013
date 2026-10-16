package pt.isep.dssmv.projectdroid.network;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideoDto;
import pt.isep.dssmv.projectdroid.network.dto.TmdbVideosResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TmdbVideosResponseTest {

    @Test
    public void testFindOfficialYouTubeTrailerKeyPrioritizesOfficial() {
        List<TmdbVideoDto> videos = new ArrayList<>();
        videos.add(new TmdbVideoDto("v1", "teaser123", "Teaser", "YouTube", "Teaser", false));
        videos.add(new TmdbVideoDto("v2", "trailer_unofficial", "Fan Trailer", "YouTube", "Trailer", false));
        videos.add(new TmdbVideoDto("v3", "official_trailer_key", "Official Main Trailer", "YouTube", "Trailer", true));

        TmdbVideosResponse response = new TmdbVideosResponse(550, videos);

        assertEquals("official_trailer_key", response.findOfficialYouTubeTrailerKey());
    }

    @Test
    public void testFallbackToUnofficialYouTubeTrailer() {
        List<TmdbVideoDto> videos = new ArrayList<>();
        videos.add(new TmdbVideoDto("v1", "vimeo_trailer", "Vimeo Trailer", "Vimeo", "Trailer", true));
        videos.add(new TmdbVideoDto("v2", "yt_unofficial_trailer", "YouTube Trailer", "YouTube", "Trailer", false));

        TmdbVideosResponse response = new TmdbVideosResponse(550, videos);

        assertEquals("yt_unofficial_trailer", response.findOfficialYouTubeTrailerKey());
    }

    @Test
    public void testNoYouTubeVideosReturnsNull() {
        List<TmdbVideoDto> videos = new ArrayList<>();
        videos.add(new TmdbVideoDto("v1", "vimeo1", "Vimeo Clip", "Vimeo", "Clip", true));

        TmdbVideosResponse response = new TmdbVideosResponse(550, videos);

        assertNull(response.findOfficialYouTubeTrailerKey());
    }

    @Test
    public void testEmptyOrNullResults() {
        TmdbVideosResponse response = new TmdbVideosResponse(550, null);
        assertNull(response.findOfficialYouTubeTrailerKey());
    }
}
