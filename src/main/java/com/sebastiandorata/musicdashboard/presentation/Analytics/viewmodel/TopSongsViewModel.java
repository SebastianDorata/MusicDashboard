package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.TopSongRowData;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningPaginationService;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
/**
 * Loads paginated top song rows for the Analytics page.
 *
 * <p>Delegates to
 * {@link ListeningPaginationService}
 * and maps {@link Song} entities
 * to {@link TopSongRowData} DTOs asynchronously via {@link DataLoadingService}.</p>
 */
@Service
public class TopSongsViewModel {

    @Autowired
    private ListeningPaginationService paginationService;

    @Autowired
    private DataLoadingService dataLoadingService;

    /**
     * Loads a window of top songs with pagination.
     * Time Complexity: O(offset + limit)
     */
    public void loadTopSongsWindow(int offset, int limit, Consumer<List<TopSongRowData>> onSuccess) {
        dataLoadingService.loadAsync(() -> buildTopSongsWindow(offset, limit), onSuccess);
    }

    private List<TopSongRowData> buildTopSongsWindow(int offset, int limit) {
        DoublyLinkedList<Song> songs = paginationService.getTopSongsWindow(offset, limit);
        return songs.getWindow(offset, limit).stream()
                .map(song -> new TopSongRowData(
                        song.getTitle(),
                        !song.getArtists().isEmpty() ? song.getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist") : "Unknown",
                        song.getDuration(),
                        song
                ))
                .toList();
    }

    /**
     * Gets total count of top songs.
     * Time Complexity: O(n)
     */
    public void loadTopSongsCount(Consumer<Integer> onSuccess) {
        dataLoadingService.loadAsync(paginationService::getTopSongsCount, onSuccess);
    }
}