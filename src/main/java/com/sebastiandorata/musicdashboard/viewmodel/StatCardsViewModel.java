package com.sebastiandorata.musicdashboard.viewmodel;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.DailyListeningStatsService;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Time Complexity: O(n) where n = total playbacks (multiple O(n) stats computed)
 * Space Complexity: O(1) - fixed-size data structure regardless of input
 */
@Service
public class StatCardsViewModel {

    @Autowired
    private DailyListeningStatsService dailyListeningStatsService;

    @Autowired
    private DataLoadingService dataLoadingService;

    /**
     * Fields represent formatted strings ready for display:
     * PlaybackDurationValue/Unit: "2" / "Hours" or "45" / "Minutes"
     * AverageSessionValue/Unit: "18" / "m" or "45" / "s"
     * Names: Song/Album/Artist titles or "—" if none available
     */
    public static class StatCardsData {

        public String playbackDurationValue;
        public String playbackDurationUnit;


        public String averageSessionValue;
        public String averageSessionUnit;
        public String todayTopSongName;
        public String todayTopAlbumName;
        public String weeklyTopSongName;
        public String weeklyTopAlbumName;


        public StatCardsData(String playbackDurationValue, String playbackDurationUnit,
                             String averageSessionValue, String averageSessionUnit,
                             String todayTopSongName, String todayTopAlbumName,
                             String weeklyTopSongName, String weeklyTopAlbumName) {
            this.playbackDurationValue = playbackDurationValue;
            this.playbackDurationUnit = playbackDurationUnit;
            this.averageSessionValue = averageSessionValue;
            this.averageSessionUnit = averageSessionUnit;
            this.todayTopSongName = todayTopSongName;
            this.todayTopAlbumName = todayTopAlbumName;
            this.weeklyTopSongName = weeklyTopSongName;
            this.weeklyTopAlbumName = weeklyTopAlbumName;
        }
    }

    /**
     * Time Complexity: O(n) where n = total playback history
     * Space Complexity: O(1) for return value
     */
    public void loadStatCardsData(Consumer<StatCardsData> onSuccess) {
        dataLoadingService.loadAsync(
                this::buildStatCardsData,
                onSuccess
        );
    }


    private StatCardsData buildStatCardsData() {

        Integer totalMinutesToday = dailyListeningStatsService.getTodayListeningTimeMinutes();
        Integer avgSessionSeconds = dailyListeningStatsService.getTodayAverageSessionSeconds();

        Song todaySong = dailyListeningStatsService.getTodayTopSong();
        Album todayAlbum = dailyListeningStatsService.getTodayTopAlbum();

        Song weeklySong = dailyListeningStatsService.getWeeklyTopSong();
        Album weeklyAlbum = dailyListeningStatsService.getWeeklyTopAlbum();


        String playbackValue;
        String playbackUnit;
        if (totalMinutesToday >= 60) {

            playbackValue = String.valueOf(totalMinutesToday / 60);
            playbackUnit = "Hours";
        } else {

            playbackValue = String.valueOf(totalMinutesToday);
            playbackUnit = "Minutes";
        }


        String avgValue;
        String avgUnit;
        if (avgSessionSeconds >= 60) {

            avgValue = String.valueOf(avgSessionSeconds / 60);
            avgUnit = "m";
        } else {

            avgValue = String.valueOf(avgSessionSeconds);
            avgUnit = "s";
        }


        String topSongName = todaySong != null ? todaySong.getTitle() : "—";
        String topAlbumName = todayAlbum != null ? todayAlbum.getTitle() : "—";
        String weeklySongName = weeklySong != null ? weeklySong.getTitle() : "—";
        String weeklyAlbumName = weeklyAlbum != null ? weeklyAlbum.getTitle() : "—";


        return new StatCardsData(playbackValue, playbackUnit,avgValue,avgUnit,topSongName,topAlbumName,weeklySongName,weeklyAlbumName);
    }
}