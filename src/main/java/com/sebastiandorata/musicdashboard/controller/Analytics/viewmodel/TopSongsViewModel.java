package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.ListeningPaginationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class TopSongsViewModel {

    @Autowired
    private ListeningPaginationService paginationService;

    @Autowired
    private DataLoadingService dataLoadingService;

    public static class TopSongRowData {
        public String songTitle;
        public String artistName;
        public int durationSeconds;

        public TopSongRowData(String songTitle, String artistName, int durationSeconds) {
            this.songTitle = songTitle;
            this.artistName = artistName;
            this.durationSeconds = durationSeconds;
        }
    }

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
                        !song.getArtists().isEmpty() ? song.getArtists().get(0).getName() : "Unknown",
                        song.getDuration()
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