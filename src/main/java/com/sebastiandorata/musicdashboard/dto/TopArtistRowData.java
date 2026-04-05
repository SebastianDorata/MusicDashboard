package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsRowFactory;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopArtistsViewModel;
/**
 * Display DTO representing a single row in the Top Artists analytics view.
 *
 * <p>Holds the artist display name and formatted listening time. The
 * {@code artist} field may be {@code null} for placeholder rows inserted
 * when fewer than five artists have valid listening data, in which case
 * both fields display "—".</p>
 */
public class TopArtistRowData {
    public Artist artist;
    public String artistName;
    public String listeningTime;



    /**
     * Data transfer object representing a single row in the top artists
     * analytics view and the dashboard top artists panel.
     *
     * <p>Consumed by {@link AnalyticsRowFactory#createArtistRow}
     * to render an artist row in both the dashboard panel and the analytics modal. Built by {@link TopArtistsViewModel}.
     *
     * <p>Placeholder rows use a {@code null} artist reference and display "—"
     * for both {@code artistName} and {@code listeningTime}, ensuring panels
     * always render a complete list of the expected size.
     *
     * @param artist the {@link Artist} entity reference used by click handlers to navigate to the artist's
     *                      discography; {@code null} for placeholder rows
     * @param artistName the display name of the artist, or "—" for placeholder rows; empty string for unplayed artists in the analytics modal
     * @param listeningTime a formatted listening time string such as "2h 15m", or "—" for placeholder rows
     */
    public TopArtistRowData(Artist artist, String artistName, String listeningTime) {
        this.artist = artist;
        this.artistName = artistName;
        this.listeningTime = listeningTime;
    }
}
