package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.StatCardsViewModel;
import com.sebastiandorata.musicdashboard.service.UserSessionService;

/**
 * All stat-card values derived from a single database fetch.
 * Consumed by {@link StatCardsViewModel} instead of calling individual getters separately.
 *
 * <p>{@code todayAvgSessionSeconds} is the duration of the current in-memory
 * session (login time to now), sourced from
 * {@link UserSessionService#getSessionDurationSeconds()}.
 * No database involvement. Session history is lost on application restart.
 *
 * @param todayMinutes total valid listening time today in minutes
 * @param todayAvgSessionSeconds elapsed seconds since the user logged in, used as the average session duration stat
 * @param todayTopSong the most played {@link Song} today, or {@code null} if no valid plays exist
 * @param todayTopAlbum the most played {@link Album} today, or {@code null} if no valid plays exist
 * @param weeklyTopSong the most played {@link Song} this week, or {@code null} if no valid plays exist
 * @param weeklyTopAlbum the most played {@link Album} this week, or {@code null} if no valid plays exist
 */
public record StatSnapshot(int todayMinutes, int todayAvgSessionSeconds, Song todayTopSong,
                           Album todayTopAlbum, Song weeklyTopSong, Album weeklyTopAlbum) {
}