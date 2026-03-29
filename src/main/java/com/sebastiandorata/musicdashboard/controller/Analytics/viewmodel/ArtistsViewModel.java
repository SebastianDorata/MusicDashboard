package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.service.ArtistListeningTimeService;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Service
public class ArtistsViewModel {

    @Autowired
    private ArtistListeningTimeService artistListeningTimeService;

    @Autowired
    private DataLoadingService dataLoadingService;


    public static class TopArtistRowData {
        public Artist artist; // null for placeholder rows when fewer than 5 artists exist
        public String artistName;
        public String listeningTime;

        public TopArtistRowData(Artist artist, String artistName, String listeningTime) {
            this.artist = artist;
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
                    data.artist,
                    data.artist.getName(),
                    data.formatTime()
            ));
        }

        while (result.size() < 5) {
            result.add(new TopArtistRowData(null, "—", "—"));
        }

        return result;
    }

}