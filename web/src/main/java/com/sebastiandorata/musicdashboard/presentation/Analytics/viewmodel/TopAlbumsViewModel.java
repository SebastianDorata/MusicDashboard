package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.TopAlbumRowData;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningPaginationService;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds paginated top album rows for the Analytics page.
 *
 * <p>Delegates to {@link ListeningPaginationService} and maps {@link Album}
 * entities to {@link TopAlbumRowData} DTOs, which are returned directly to the
 * caller.</p>
 */
@Service
public class TopAlbumsViewModel {

    @Autowired
    private ListeningPaginationService paginationService;

    /**
     * Returns a window of top album rows, synchronously.
     *
     * <p>Time Complexity: O(offset + limit)</p>
     * <p>Space Complexity: O(limit) for the returned rows.</p>
     *
     * @param offset zero-based index of the first row to return
     * @param limit  maximum number of rows to return
     * @return the mapped album rows for the requested window
     */
    public List<TopAlbumRowData> getTopAlbumsWindow(int offset, int limit) {
        return buildTopAlbumsWindow(offset, limit);
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
     * Returns the total number of top albums, synchronously.
     *
     * <p>Time Complexity: O(n)</p>
     * <p>Space Complexity: O(1).</p>
     *
     * @return the total album count in the top albums ranking
     */
    public int getTopAlbumsCount() {
        return paginationService.getTopAlbumsCount();
    }
}
