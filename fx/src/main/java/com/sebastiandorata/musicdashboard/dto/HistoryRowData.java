package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsRowFactory;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.ListeningHistoryViewModel;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
/**
 * Display DTO representing a single row in the Listening History view.
 *
 * <p>Holds the song title, primary artist name, a human-readable relative
 * timestamp (e.g. "3m ago", "Yesterday"), the duration played in seconds,
 * and a direct reference to the {@link Song}
 * entity so that clicking the row can start playback immediately.</p>
 */
public class HistoryRowData {
    public String songTitle;
    public String artistName;
    public String playedAt;
    public int durationSeconds;
    public Song song;


    /**
     * Data transfer object representing a single row in the listening history analytics view.
     *
     * <p>Consumed by {@link AnalyticsRowFactory#createHistoryRow}
     * to render a history row in both the preview section and the paginated modal.
     * Built by {@link ListeningHistoryViewModel}.
     *
     * @param songTitle the title of the played song
     * @param artistName the primary artist name for the played song
     * @param playedAt a readable time string formatted by {@link AppUtils#formatRelativeTime}
     * @param durationSeconds the number of seconds the song was played for this record
     * @param song  the {@link Song} entity reference used by the click handler to start playback; may be {@code null} for unknown songs
     */
    public HistoryRowData(String songTitle, String artistName, String playedAt,
                          int durationSeconds, Song song) {
        this.songTitle = songTitle;
        this.artistName = artistName;
        this.playedAt = playedAt;
        this.durationSeconds = durationSeconds;
        this.song = song;
    }
}
