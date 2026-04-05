package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsRowFactory;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopAlbumsViewModel;
/**
 * Display DTO representing a single row in the Top Albums analytics view.
 *
 * <p>Holds the album title, release year, song count, and a direct
 * reference to the {@link Album}
 * entity so that clicking the row navigates to the album detail view
 * in My Library.</p>
 */
public class TopAlbumRowData {
    public String albumTitle;
    public String releaseYear;
    public int songCount;
    public Album album;



    /**
     * Data transfer object representing a single row in the top albums
     * analytics view.
     *
     * <p>Consumed by {@link AnalyticsRowFactory#createTopAlbumRow}
     * to render an album row in both the preview section and the paginated modal.
     * Built by {@link TopAlbumsViewModel}.
     *
     * @param albumTitle  the title of the album
     * @param releaseYear the album's release year as a string, or "Unknown" if absent
     * @param songCount the number of songs in the album
     * @param album the {@link Album} entity reference used by the click handler to navigate to the album drill-down view in My Library
     */
    public TopAlbumRowData(String albumTitle, String releaseYear, int songCount, Album album) {
        this.albumTitle = albumTitle;
        this.releaseYear = releaseYear;
        this.songCount = songCount;
        this.album = album;
    }
}
