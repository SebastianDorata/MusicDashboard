package com.sebastiandorata.musicdashboard.presentation.shared;


import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.StatCardsViewModel;
import com.sebastiandorata.musicdashboard.presentation.UIComponent;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Factory for the dashboard stat cards row.
 *
 * <p>The two Consumer parameters are intentionally kept. DashboardController
 * passes them and removing them would break compilation there.</p>
 */
@Component
public class CardFactory extends UIComponent {

    @Autowired
    private MusicPlayerService musicPlayerService;

    @Autowired
    private StatCardsViewModel statCardsViewModel;

    /**
     * Survives between createStatCards() calls solely to deregister the previous
     * MusicPlayerService listener. Holds no UI state.
     */
    private ChangeListener<Song> songChangeListener;
    private static final double SCALE = Screen.getPrimary().getVisualBounds().getHeight() / 1080.0;


    public HBox createStatCards(Consumer<Album> onAlbumClicked, Consumer<Song>  onSongPlayed) {
        HBox cards = new HBox(12);

        // Local labels so the closure always targets live nodes
        Label playbackValueLabel   = new Label("0");
        Label playbackUnitLabel    = new Label("Minutes");
        Label avgSessionValueLabel = new Label("0");
        Label avgSessionUnitLabel  = new Label("min/session");
        Label todaySongLabel       = new Label("—");
        Label todayAlbumLabel      = new Label("—");
        Label weeklySongLabel      = new Label("—");
        Label weeklyAlbumLabel     = new Label("—");

        // Single-element arrays so lambdas can update entity references
        Song[]  todayTopSong   = {null};
        Album[] todayTopAlbum  = {null};
        Song[]  weeklyTopSong  = {null};
        Album[] weeklyTopAlbum = {null};

        VBox card1 = createCombinedPlaybackAvgCard(playbackValueLabel, playbackUnitLabel, avgSessionValueLabel, avgSessionUnitLabel);
        VBox card2 = createNamedStatCard("Top Song Today", todaySongLabel);
        VBox card3 = createNamedStatCard("Top Album Today", todayAlbumLabel);
        VBox card4 = createNamedStatCard("Top Song This Week",  weeklySongLabel);
        VBox card5 = createNamedStatCard("Top Album This Week", weeklyAlbumLabel);

        for (VBox card : new VBox[]{card1, card2, card3, card4, card5}) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefWidth(0);
        }
        cards.setMaxWidth(Double.MAX_VALUE);
        cards.setFillHeight(true);
        HBox.setHgrow(cards, Priority.ALWAYS);
        cards.getChildren().addAll(card1, card2, card3, card4, card5);

        // Refresh, always works on the labels above
        Runnable refresh = () -> statCardsViewModel.loadStatCardsData(data -> {
            playbackValueLabel.setText(data.playbackDurationValue());
            playbackUnitLabel.setText(data.playbackDurationUnit());
            avgSessionValueLabel.setText(data.averageSessionValue());
            avgSessionUnitLabel.setText(data.averageSessionUnit());

            todayTopSong[0]   = data.todayTopSong();
            todayTopAlbum[0]  = data.todayTopAlbum();
            weeklyTopSong[0]  = data.weeklyTopSong();
            weeklyTopAlbum[0] = data.weeklyTopAlbum();

            setupLabel(todaySongLabel, data.todayTopSongName(), todayTopSong[0],onAlbumClicked, onSongPlayed);
            setupLabel(todayAlbumLabel, data.todayTopAlbumName(), todayTopAlbum[0], onAlbumClicked, null);
            setupLabel(weeklySongLabel, data.weeklyTopSongName(), weeklyTopSong[0], onAlbumClicked, onSongPlayed);
            setupLabel(weeklyAlbumLabel, data.weeklyTopAlbumName(), weeklyTopAlbum[0], onAlbumClicked, null);
        });

        refresh.run();

        // Deregister the previous listener before adding a new one
        if (songChangeListener != null) {
            musicPlayerService.currentSongProperty().removeListener(songChangeListener);
        }
        songChangeListener = (obs, oldSong, newSong) -> refresh.run();
        musicPlayerService.currentSongProperty().addListener(songChangeListener);

        return cards;
    }

    private VBox createCombinedPlaybackAvgCard(Label playbackValueLabel, Label playbackUnitLabel,
                                               Label avgSessionValueLabel,
                                               Label avgSessionUnitLabel) {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("dashboard-stat-card", "wt-smmd-bld","panels");
        Label playbackTitle = new Label("Playback Duration Today");

        playbackTitle.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title","dashboard-stat-name ");


        card.getChildren().add(playbackTitle);
        Region separator = new Region();
        separator.setPrefHeight(3);
        card.getChildren().addAll(separator);

        playbackValueLabel.getStyleClass().add("txt-white-bld-thirty");
        playbackUnitLabel.getStyleClass().add("dashboard-stat-unit");


        HBox dayLabel = new HBox(6, playbackValueLabel, playbackUnitLabel);
        dayLabel.getStyleClass().add("dashboard-stat-day");

        card.getChildren().add(dayLabel);





        Label avgTitle = new Label("Average Listening Period");
        avgTitle.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title");

        Label avgPrefix = new Label("Avg");
        avgPrefix.getStyleClass().add("txt-white-sm");

        avgSessionValueLabel.getStyleClass().add("txt-white-bld-thirty");
        avgSessionUnitLabel.getStyleClass().add("dashboard-stat-unit");

        HBox avgValueRow = new HBox(6, avgPrefix, avgSessionValueLabel, avgSessionUnitLabel);
        avgValueRow.setAlignment(Pos.CENTER_LEFT);

        VBox avgSection = new VBox(4, avgTitle, avgValueRow);
        avgSection .setAlignment(Pos.CENTER);
        VBox.setVgrow(avgSection , Priority.ALWAYS);

        card.getChildren().addAll(avgSection);
        return card;
    }

    private VBox createNamedStatCard(String titleText, Label valueLabel) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("dashboard-stat-card", "panels");

        Label title = new Label(titleText);
        title.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        valueLabel.getStyleClass().addAll("dashboard-stat-name", "txt-white-md-bld");
        valueLabel.setWrapText(true);
        valueLabel.setAlignment(Pos.CENTER);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        VBox valueContainer = new VBox(valueLabel);
        valueContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(valueContainer, Priority.ALWAYS);

        card.getChildren().addAll(title, valueContainer);
        return card;
    }

    // ── User Library grid cards ───────────────────────────────────
    // Both methods apply the shared ".library-card" style class so every grid
    // view (Albums tab, Favourites, Artist detail) looks identical.
    // Sizing and hover effects live entirely in library.css under .library-card;
    // call sites must NOT add extra size overrides on top of the returned VBox.

    public static VBox createAlbumCard(Album album, MusicPlayerService musicPlayerService) {
        VBox card = buildBaseCard();

        ImageView art = buildCardArt();
        if (album.getAlbumArtPath() != null) {
            try { art.setImage(new Image("file:" + album.getAlbumArtPath(), true)); }
            catch (Exception ignored) {}
        }

        Label title = buildCardTitle(album.getTitle());

        String year = album.getReleaseYear() != null
                ? String.valueOf(album.getReleaseYear()) : "—";
        Label subtitle = buildCardSubtitle(year);

        card.getChildren().addAll(art, title, subtitle);
        card.setOnMouseClicked(e -> {
            if (album.getSongs() != null && !album.getSongs().isEmpty()) {
                musicPlayerService.playSong(album.getSongs().stream().findFirst().orElse(null));
            }
        });
        return card;
    }

    public static VBox createSongCard(Song song, MusicPlayerService musicPlayerService) {
        VBox card = buildBaseCard();

        ImageView art = buildCardArt();
        if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
            try { art.setImage(new Image("file:" + song.getAlbum().getAlbumArtPath(), true)); }
            catch (Exception ignored) {}
        }

        Label title = buildCardTitle(song.getTitle());

        String artistName = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().stream().findFirst().map(Artist::getName).orElse("Unknown Artist")
                : "Unknown Artist";
        Label subtitle = buildCardSubtitle(artistName);

        card.getChildren().addAll(art, title, subtitle);
        card.setOnMouseClicked(e -> musicPlayerService.playSong(song));
        return card;
    }


    private static VBox buildBaseCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("library-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setCursor(Cursor.HAND);
        return card;
    }

    private static ImageView buildCardArt() {
        ImageView iv = new ImageView();
        iv.getStyleClass().add("library-card-art");
        iv.setPreserveRatio(true);
        return iv;
    }

    private static Label buildCardTitle(String text) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.getStyleClass().add("library-card-title");
        lbl.setWrapText(true);
        return lbl;
    }

    private static Label buildCardSubtitle(String text) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.getStyleClass().add("library-card-subtitle");
        lbl.setWrapText(true);
        return lbl;
    }

    /**
     * <p>Sets text and wires single/double-click behaviour on a stat label.
     * Always clears the previous handler first to prevent a stale
     * PauseTransition closure from a prior refresh firing on the new entity.</p>
     */
    private void setupLabel(Label label, String text, Object entity,
                            Consumer<Album> onAlbumNavigate,
                            Consumer<Song>  onSongPlay) {
        // Clear previous state unconditionally
        label.setOnMouseClicked(null);
        label.setTooltip(null);
        label.getStyleClass().remove("Cursor-Hand");

        String display = (text != null && !text.equals("—")) ? text : "—";
        label.setText(display);

        if (entity == null || display.equals("—")) return;

        label.getStyleClass().add("Cursor-Hand");

        if (entity instanceof Song) {
            label.setTooltip(new Tooltip("Click: view album  |  Double-click: play song"));
        } else if (entity instanceof Album) {
            label.setTooltip(new Tooltip("Click to view album"));
        }

        PauseTransition clickDelay = new PauseTransition(Duration.millis(300));

        label.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            if (event.getClickCount() == 2) {
                clickDelay.stop();
                if (entity instanceof Song s && onSongPlay != null) {
                    onSongPlay.accept(s);
                }
            } else if (event.getClickCount() == 1) {
                clickDelay.setOnFinished(e -> handleSingleClick(entity, onAlbumNavigate));
                clickDelay.playFromStart();
            }
        });
    }

    private void handleSingleClick(Object entity, Consumer<Album> onAlbumNavigate) {
        if (entity == null || onAlbumNavigate == null) return;
        Album target = null;
        if (entity instanceof Album a)     target = a;
        else if (entity instanceof Song s) target = s.getAlbum();
        if (target != null) onAlbumNavigate.accept(target);
    }
}
