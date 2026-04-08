package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.StatCardsViewModel;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;

/**
 * Display DTO for the five dashboard stat cards.
 *
 * <p>Carries formatted strings for the playback duration value and unit,
 * average session value and unit, and the top song and album names for
 * today and this week. Also holds direct entity references for click
 * navigation to albums and playback of songs.</p>
 */
public record StatCardsData(String playbackDurationValue, String playbackDurationUnit, String averageSessionValue,
                            String averageSessionUnit, String todayTopSongName, String todayTopAlbumName,
                            String weeklyTopSongName, String weeklyTopAlbumName, Song todayTopSong, Album todayTopAlbum,
                            Song weeklyTopSong, Album weeklyTopAlbum) {
    /**
     * Data transfer object carrying all formatted display strings and entity
     * references needed to populate the five dashboard stat cards.
     *
     * <p>Built by {@link StatCardsViewModel#loadStatCardsData}
     * from a {@link StatSnapshot} and consumed
     * by {@link CardFactory#createStatCards}.
     *
     * <p>String fields are pre-formatted for direct display in Label nodes.
     * Entity reference fields are retained alongside their display strings
     * so that click handlers can navigate to the corresponding album or play the
     * corresponding song without a second database lookup.
     *
     * @param playbackDurationValue formatted numeric value for today's playback duration
     * @param playbackDurationUnit  unit string for today's playback duration, either "Hours" or "Minutes"
     * @param averageSessionValue   formatted numeric value for the average session length
     * @param averageSessionUnit    unit string for the average session, either "m" or "s"
     * @param todayTopSongName      display name of today's most played song, or "—"
     * @param todayTopAlbumName     display name of today's most played album, or "—"
     * @param weeklyTopSongName     display name of this week's most played song, or "—"
     * @param weeklyTopAlbumName    display name of this week's most played album, or "—"
     * @param todayTopSong          entity reference for today's top song; {@code null} if none
     * @param todayTopAlbum         entity reference for today's top album; {@code null} if none
     * @param weeklyTopSong         entity reference for this week's top song; {@code null} if none
     * @param weeklyTopAlbum        entity reference for this week's top album; {@code null} if none
     */
    public StatCardsData {
    }
}
