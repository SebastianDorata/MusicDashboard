package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.TopSongRowData;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningPaginationService;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds paginated top song rows for the Analytics page.
 *
 * <p>Delegates to {@link ListeningPaginationService} and maps {@link Song}
 * entities to {@link TopSongRowData} DTOs, which are returned directly to the
 * caller.</p>
 */
@Service
public class TopSongsViewModel {

    @Autowired private ListeningPaginationService paginationService;

    /**
     * Returns a window of top song rows, synchronously.
     *
     * <p>Time Complexity: O(offset + limit)</p>
     * <p>Space Complexity: O(limit) for the returned rows.</p>
     *
     * @param offset zero-based index of the first row to return
     * @param limit  maximum number of rows to return
     * @return the mapped song rows for the requested window
     */
    public List<TopSongRowData> getTopSongsWindow(int offset, int limit) {
        return buildTopSongsWindow(offset, limit);
    }

    private List<TopSongRowData> buildTopSongsWindow(int offset, int limit) {
        DoublyLinkedList<Song> songs = paginationService.getTopSongsWindow(offset, limit);
        return songs.getWindow(offset, limit).stream()
                .map(song -> new TopSongRowData(
                        song.getTitle(),
                        ArtistUtils.getPrimaryArtistName(song),
                        song.getDuration(),
                        song
                ))
                .toList();
    }

    /**
     * Returns the total number of top songs, synchronously.
     *
     * <p>Time Complexity: O(n)</p>
     * <p>Space Complexity: O(1).</p>
     *
     * @return the total song count in the top songs ranking
     */
    public int getTopSongsCount() {
        return paginationService.getTopSongsCount();
    }
}
