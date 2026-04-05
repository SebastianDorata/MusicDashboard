package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsRowFactory;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopSongsViewModel;
/**
 * Display DTO representing a single row in the Top Songs analytics view.
 *
 * <p>Holds the song title, primary artist name, duration in seconds, and
 * a direct reference to the {@link Song}
 * entity so that clicking the row starts playback immediately.</p>
 */
public class TopSongRowData {
    public String songTitle;
    public String artistName;
    public int durationSeconds;
    public Song song;


    /**
     * Data transfer object representing a single row in the top songs
     * analytics view.
     *
     * <p>Consumed by {@link AnalyticsRowFactory#createTopSongRow}
     * to render a song row in both the preview section and the paginated modal.
     * Built by {@link TopSongsViewModel}.
     *
     * @param songTitle the title of the song
     * @param artistName the primary artist name for the song
     * @param durationSeconds the total duration of the song in seconds
     * @param song the {@link Song} entity reference used by the click handler to start playback
     */
    public TopSongRowData(String songTitle, String artistName, int durationSeconds, Song song) {
        this.songTitle = songTitle;
        this.artistName = artistName;
        this.durationSeconds = durationSeconds;
        this.song = song;
    }
}
