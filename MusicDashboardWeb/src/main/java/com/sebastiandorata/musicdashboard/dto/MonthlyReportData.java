package com.sebastiandorata.musicdashboard.dto;

import java.time.Month;

/**
 * Display record holding the pre-formatted data for a single monthly report card.
 *
 * <p>Fields include the month number and display name, total songs played,
 * total listening minutes, and the string names of the top song, artist,
 * album, and genre ("—" when not available).</p>
 */
public class MonthlyReportData {
    public int month;
    public String monthName;
    public int totalSongs;
    public int totalMinutes;
    public String topSongName;
    public String topArtistName;
    public String topAlbumName;
    public String topGenreName;

    /**
     * Creates the display record for one monthly report card.
     *
     * @param month         month number, 1 to 12; also used to derive {@code monthName}
     * @param totalSongs    total songs played in the month
     * @param totalMinutes  total listening time in the month, in minutes
     * @param topSongName   name of the most-played song, or "—"
     * @param topArtistName name of the most-played artist, or "—"
     * @param topAlbumName  name of the most-played album, or "—"
     * @param topGenreName  name of the most-played genre, or "—"
     */
    public MonthlyReportData(int month, int totalSongs, int totalMinutes,
                             String topSongName, String topArtistName,
                             String topAlbumName, String topGenreName) {
        this.month = month;
        this.monthName = Month.of(month).name();
        this.totalSongs = totalSongs;
        this.totalMinutes = totalMinutes;
        this.topSongName = topSongName;
        this.topArtistName = topArtistName;
        this.topAlbumName = topAlbumName;
        this.topGenreName = topGenreName;
    }
}
