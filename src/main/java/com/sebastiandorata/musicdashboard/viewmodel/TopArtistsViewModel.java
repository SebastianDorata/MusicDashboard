package com.sebastiandorata.musicdashboard.viewmodel;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.service.ArtistListeningTimeService;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Service
public class TopArtistsViewModel {

    @Autowired
    private ArtistListeningTimeService artistListeningTimeService;

    @Autowired
    private DataLoadingService dataLoadingService;


    public static class TopArtistRowData {
        public String artistName;
        public String listeningTime;

        public TopArtistRowData(String artistName, String listeningTime) {
            this.artistName = artistName;
            this.listeningTime = listeningTime;
        }
    }

    /**
     * Time Complexity: O(n log 5) = O(n) effectively
     * Space Complexity: O(5) = O(1)
     */
    public void loadTopArtistsData(Consumer<List<TopArtistRowData>> onSuccess) {
        dataLoadingService.loadAsync(
                this::buildTopArtistsData,
                onSuccess
        );
    }


    private List<TopArtistRowData> buildTopArtistsData() {

        List<ArtistListeningTimeService.ArtistTimeData> topArtists =
                artistListeningTimeService.getTopArtistsAllTime(5);

        List<TopArtistRowData> result = new ArrayList<>();
        for (ArtistListeningTimeService.ArtistTimeData data : topArtists) {
            result.add(new TopArtistRowData(
                    data.artist.getName(),
                    data.formatTime()
            ));
        }


        while (result.size() < 5) {
            result.add(new TopArtistRowData("—", "—"));
        }

        return result;
    }


    public String formatArtistTime(Artist artist) {
        if (artist == null) return "—";
        Integer seconds = artistListeningTimeService.getArtistTotalSeconds(artist);
        return formatSeconds(seconds);
    }


    private String formatSeconds(Integer seconds) {
        if (seconds == null || seconds == 0) return "—";

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return "0m";
        }
    }
}