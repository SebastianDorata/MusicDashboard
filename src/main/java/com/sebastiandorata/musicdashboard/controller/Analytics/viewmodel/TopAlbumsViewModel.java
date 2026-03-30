package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.ListeningPaginationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class TopAlbumsViewModel {

    @Autowired
    private ListeningPaginationService paginationService;

    @Autowired
    private DataLoadingService dataLoadingService;

    public static class TopAlbumRowData {
        public String albumTitle;
        public String releaseYear;
        public int songCount;

        public TopAlbumRowData(String albumTitle, String releaseYear, int songCount) {
            this.albumTitle = albumTitle;
            this.releaseYear = releaseYear;
            this.songCount = songCount;
        }
    }

    /**
     * Loads a window of top albums with pagination.
     * Time Complexity: O(offset + limit)
     */
    public void loadTopAlbumsWindow(int offset, int limit, Consumer<List<TopAlbumRowData>> onSuccess) {
        dataLoadingService.loadAsync(() -> buildTopAlbumsWindow(offset, limit), onSuccess);
    }

    private List<TopAlbumRowData> buildTopAlbumsWindow(int offset, int limit) {
        DoublyLinkedList<Album> albums = paginationService.getTopAlbumsWindow(offset, limit);
        return albums.getWindow(offset, limit).stream()
                .map(album -> new TopAlbumRowData(
                        album.getTitle(),
                        album.getReleaseYear() != null ? album.getReleaseYear().toString() : "Unknown",
                        album.getSongs() != null ? album.getSongs().size() : 0
                ))
                .toList();
    }

    /**
     * Gets total count of top albums.
     * Time Complexity: O(n)
     */
    public void loadTopAlbumsCount(Consumer<Integer> onSuccess) {
        dataLoadingService.loadAsync(paginationService::getTopAlbumsCount, onSuccess);
    }
}