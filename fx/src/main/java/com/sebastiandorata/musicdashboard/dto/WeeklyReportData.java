package com.sebastiandorata.musicdashboard.dto;

/**
 * Display record holding the pre-formatted data for a single weekly report card.
 *
 * <p>Fields include the ISO week number, total songs played, total listening
 * minutes, and the string names of the top song, artist, album, and genre
 * ("—" when not available).</p>
 */
public class WeeklyReportData {
    public int week;
    public int totalSongs;
    public int totalMinutes;
    public String topSongName;
    public String topArtistName;
    public String topAlbumName;
    public String topGenreName;

    public WeeklyReportData(int week, int totalSongs, int totalMinutes,
                            String topSongName, String topArtistName,
                            String topAlbumName, String topGenreName) {
        this.week = week;
        this.totalSongs = totalSongs;
        this.totalMinutes = totalMinutes;
        this.topSongName = topSongName;
        this.topArtistName = topArtistName;
        this.topAlbumName = topAlbumName;
        this.topGenreName = topGenreName;
    }
}
