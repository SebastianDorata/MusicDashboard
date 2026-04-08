package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.TopAlbumRowData;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningPaginationService;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
/**
 * Loads paginated top album rows for the Analytics page.
 *
 * <p>Delegates to
 * {@link ListeningPaginationService}
 * and maps {@link Album} entities
 * to {@link TopAlbumRowData} DTOs asynchronously via
 * {@link DataLoadingService}.</p>
 */
@Service
public class TopAlbumsViewModel {

    @Autowired
    private ListeningPaginationService paginationService;

    @Autowired
    private DataLoadingService dataLoadingService;

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
                        album.getSongs() != null ? album.getSongs().size() : 0,
                        album
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