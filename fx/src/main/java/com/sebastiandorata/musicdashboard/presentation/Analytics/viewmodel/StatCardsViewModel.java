package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.StatSnapshot;
import com.sebastiandorata.musicdashboard.dto.StatCardsData;
import com.sebastiandorata.musicdashboard.service.handlers.DailyListeningStatsService;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * ViewModel for the dashboard stat cards.
 * Calls buildStatSnapshot() once, which fetches history once and derives all
 * six values in memory. Zero extra queries.
 *
 * Time Complexity: O(n). One DB fetch via buildStatSnapshot()
 * Space Complexity: O(1). Fixed-size StatCardsData result
 */
@Service
public class StatCardsViewModel {

    @Autowired
    private DailyListeningStatsService dailyListeningStatsService;

    @Autowired
    private DataLoadingService dataLoadingService;


    public void loadStatCardsData(Consumer<StatCardsData> onSuccess) {
        dataLoadingService.loadAsync(this::buildStatCardsData, onSuccess);
    }

    private StatCardsData buildStatCardsData() {
        StatSnapshot snap = dailyListeningStatsService.buildStatSnapshot();
        String playbackValue;
        String playbackUnit;
        if (snap.todayMinutes() >= 60) {
            playbackValue = String.valueOf(snap.todayMinutes() / 60);
            playbackUnit  = "Hours";
        } else {
            playbackValue = String.valueOf(snap.todayMinutes());
            playbackUnit  = "Minutes";
        }

        // Format average session
        String avgValue;
        String avgUnit;
        if (snap.todayAvgSessionSeconds() >= 60) {
            avgValue = String.valueOf(snap.todayAvgSessionSeconds() / 60);
            avgUnit  = "m";
        } else {
            avgValue = String.valueOf(snap.todayAvgSessionSeconds());
            avgUnit  = "s";
        }

        return new StatCardsData(
                playbackValue, playbackUnit,
                avgValue, avgUnit,
                snap.todayTopSong() != null ? snap.todayTopSong().getTitle()   : "—",
                snap.todayTopAlbum() != null ? snap.todayTopAlbum().getTitle()  : "—",
                snap.weeklyTopSong() != null ? snap.weeklyTopSong().getTitle()  : "—",
                snap.weeklyTopAlbum() != null ? snap.weeklyTopAlbum().getTitle() : "—",
                snap.todayTopSong(), snap.todayTopAlbum(),
                snap.weeklyTopSong(), snap.weeklyTopAlbum()
        );
    }
}