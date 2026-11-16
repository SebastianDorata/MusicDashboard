package com.sebastiandorata.musicdashboard.dto;

/**
 * Display record holding the pre-formatted data for a single weekly report card.
 *
 * <p>Fields include the ISO week number, total songs played, total listening
 * minutes, and the string names of the top song, artist, album, and genre
 * ("—" when not available).</p>
 */
public class WeeklyReportData {
    /** ISO week number of the report. */
    public int week;
    /** Total songs played in the week. */
    public int totalSongs;
    /** Total listening time in the week, in minutes. */
    public int totalMinutes;
    /** Name of the most-played song, or "—". */
    public String topSongName;
    /** Name of the most-played artist, or "—". */
    public String topArtistName;
    /** Name of the most-played album, or "—". */
    public String topAlbumName;
    /** Name of the most-played genre, or "—". */
    public String topGenreName;

    /**
     * Creates the display record for one weekly report card.
     *
     * @param week          ISO week number of the report
     * @param totalSongs    total songs played in the week
     * @param totalMinutes  total listening time in the week, in minutes
     * @param topSongName   name of the most-played song, or "—"
     * @param topArtistName name of the most-played artist, or "—"
     * @param topAlbumName  name of the most-played album, or "—"
     * @param topGenreName  name of the most-played genre, or "—"
     */
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
