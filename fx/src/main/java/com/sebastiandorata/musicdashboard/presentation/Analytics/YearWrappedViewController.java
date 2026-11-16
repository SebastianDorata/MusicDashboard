package com.sebastiandorata.musicdashboard.presentation.Analytics;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.YearEndReportViewModel;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Builds the year-end "Wrapped" slideshow for the Analytics page.
 *
 * <p>Assembles ten themed slide cards from
 * {@link com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.YearEndReportViewModel.WrappedData},
 * each with a distinct gradient palette. Handles animated slide transitions
 * (fade + scale via JavaFX {@link javafx.animation.FadeTransition} and
 * {@link javafx.animation.ScaleTransition}), dot-indicator navigation, and
 * a responsive bar chart of monthly listening time that resizes with
 * the scene. Slide dimensions are kept in sync with the live scene via
 * property listeners bound in {@link #buildSlideShow}.</p>
 */
@Component
public class YearWrappedViewController {

    private static final double W_RATIO = 0.850;
    private static final double H_RATIO = 0.790;


    private double currentSlideW = AppUtils.APP_WIDTH  * W_RATIO;
    private double currentSlideH = AppUtils.APP_HEIGHT * H_RATIO;

    @Autowired
    private YearEndReportViewModel yearEndReportViewModel;

    private static final String[] GRADIENTS = {
            "linear-gradient(to bottom right, #1a1a2e, #16213e, #0f3460)",
            "linear-gradient(to bottom right, #0d7377, #14a085, #0d7377)",
            "linear-gradient(to bottom right, #1db954, #1ed760, #0d7377)",
            "linear-gradient(to bottom right, #e94560, #c41e3a, #7a0026)",
            "linear-gradient(to bottom right, #533483, #6a0572, #533483)",
            "linear-gradient(to bottom right, #ff6b35, #f7931e, #c41e3a)",
            "linear-gradient(to bottom right, #005f73, #0a9396, #94d2bd)",
            "linear-gradient(to bottom right, #7209b7, #3a0ca3, #480ca8)",
            "linear-gradient(to bottom right, #1a1a2e, #e94560, #1a1a2e)",
            "linear-gradient(to bottom right, #1db954, #0d7377, #1a1a2e)",
    };

    private int currentSlide = 0;
    private List<VBox> slides;
    private StackPane  slidePane;

    public VBox buildView(int year, VBox centerContent) {
        VBox root = new VBox(0);
        root.setFillWidth(true);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setAlignment(Pos.TOP_CENTER);

        root.getChildren().add(buildLoadingCard(year));

        yearEndReportViewModel.loadWrappedData(year, data -> {
            root.getChildren().clear();
            root.getChildren().add(buildSlideShow(data));
        });

        return root;
    }

    private VBox buildSlideShow(YearEndReportViewModel.WrappedData data) {
        slides       = buildAllSlides(data);
        currentSlide = 0;

        slidePane = new StackPane();
        slidePane.setAlignment(Pos.CENTER);
        slidePane.getChildren().setAll(slides.get(0));

        HBox nav = buildNavRow();

        VBox wrapper = new VBox(16);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setFillWidth(true);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(new Insets(30, 0, 30, 0));
        wrapper.getChildren().addAll(slidePane, nav);

        // Bind to scene size.
        slidePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                resizeSlides(newScene.getWidth(), newScene.getHeight());
                newScene.widthProperty().addListener((o, oldW, newW) ->
                        resizeSlides(newW.doubleValue(), newScene.getHeight()));
                newScene.heightProperty().addListener((o, oldH, newH) ->
                        resizeSlides(newScene.getWidth(), newH.doubleValue()));
            }
        });

        animateSlideIn(slides.get(0));
        return wrapper;
    }

    private void resizeSlides(double sceneW, double sceneH) {
        currentSlideW = sceneW * W_RATIO;
        currentSlideH = sceneH * H_RATIO;

        slidePane.setPrefWidth(currentSlideW);
        slidePane.setMaxWidth(currentSlideW);
        slidePane.setPrefHeight(currentSlideH);
        slidePane.setMaxHeight(currentSlideH);

        if (slides != null) {
            for (VBox slide : slides) {
                slide.setPrefWidth(currentSlideW);
                slide.setMinWidth(currentSlideW);
                slide.setMaxWidth(currentSlideW);
                slide.setPrefHeight(currentSlideH);
                slide.setMinHeight(currentSlideH);
                slide.setMaxHeight(currentSlideH);
            }
        }
    }

    private HBox buildNavRow() {
        Button prev = navButton("←");
        Button next = navButton("→");

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < slides.size(); i++) {
            final int idx = i;
            Label dot = new Label("●");
            dot.getStyleClass().add(i == 0 ? "nav-dot-active" : "nav-dot");
            dot.setOnMouseClicked(ev -> goTo(idx, dots));
            dots.getChildren().add(dot);
        }

        prev.setOnAction(e -> { if (currentSlide > 0) goTo(currentSlide - 1, dots); });
        next.setOnAction(e -> { if (currentSlide < slides.size() - 1) goTo(currentSlide + 1, dots); });

        HBox nav = new HBox(20, prev, dots, next);
        nav.setAlignment(Pos.CENTER);
        return nav;
    }

    private void goTo(int idx, HBox dots) {
        currentSlide = idx;
        slidePane.getChildren().setAll(slides.get(currentSlide));
        animateSlideIn(slides.get(currentSlide));
        updateDots(dots);
    }

    private void updateDots(HBox dots) {
        for (int i = 0; i < dots.getChildren().size(); i++) {
            Label dot = (Label) dots.getChildren().get(i);
            dot.getStyleClass().setAll(i == currentSlide ? "nav-dot-active" : "nav-dot");
        }
    }

    private void animateSlideIn(VBox slide) {
        slide.setOpacity(0);
        slide.setScaleX(0.96);
        slide.setScaleY(0.96);

        FadeTransition ft = new FadeTransition(Duration.millis(300), slide);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), slide);
        st.setFromX(0.96);
        st.setFromY(0.96);
        st.setToX(1.0);
        st.setToY(1.0);

        ft.play();
        st.play();
    }

    private List<VBox> buildAllSlides(YearEndReportViewModel.WrappedData data) {
        List<VBox> list = new ArrayList<>();
        list.add(buildCoverSlide(data.year));
        list.add(buildHoursSlide(data));
        list.add(buildSongsCountSlide(data));
        list.add(buildTopSongHeroSlide(data));
        list.add(buildTop5SongsSlide(data));
        list.add(buildTopArtistHeroSlide(data));
        list.add(buildTop5ArtistsSlide(data));
        list.add(buildTopAlbumGenreSlide(data));
        list.add(buildMonthlySlide(data));
        list.add(buildClosingSlide(data.year));
        return list;
    }

    private VBox buildCoverSlide(int year) {
        VBox slide = newSlide(0);
        slide.getChildren().addAll(spacer(), bigEmoji("🎧"), heroLabel(String.valueOf(year)),
                subLabel("Your " + year + " Wrapped"), spacer(), hintLabel("→ Use the arrows to explore"));
        return slide;
    }

    private VBox buildHoursSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(1);
        slide.getChildren().addAll(spacer(), introLabel("This year you listened for"),
                heroLabel(data.totalHoursFormatted()), accentLabel("hours"),
                subLabel(data.totalMinutes + " total minutes"), spacer(),
                bodyLabel("Your busiest month was " + data.busiestMonthName()));
        return slide;
    }

    private VBox buildSongsCountSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(2);
        slide.getChildren().addAll(spacer(), introLabel("You played"),
                heroLabel(String.valueOf(data.totalSongs)), accentLabel("songs"), spacer(),
                subLabel("That's " + (data.totalSongs / 12) + " songs per month on average"), spacer());
        return slide;
    }

    private VBox buildTopSongHeroSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(3);
        slide.getChildren().addAll(spacer(), introLabel("Your #1 song of " + data.year),
                bigEmoji("🎵"), heroLabel(data.topSongTitle), accentLabel(data.topSongArtist),
                spacer(), bodyLabel("You couldn't stop listening."));
        return slide;
    }

    private VBox buildTop5SongsSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(4);
        VBox list  = rankList(data.topSongs.stream()
                .map(e -> Map.entry(e.getKey().getTitle() + "\n" + firstArtist(e.getKey()), e.getValue()))
                .toList(), "green");
        slide.getChildren().addAll(spacer(), sectionHeader("Top 5 Songs"), list, spacer());
        return slide;
    }

    private VBox buildTopArtistHeroSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(5);
        slide.getChildren().addAll(spacer(), introLabel("Your most-played artist"),
                bigEmoji("🎤"), heroLabel(data.topArtistName), spacer(), bodyLabel("You were a true fan."));
        return slide;
    }

    private VBox buildTop5ArtistsSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(6);
        VBox list  = rankList(data.topArtists.stream()
                .map(e -> Map.entry(e.getKey().getName(), e.getValue()))
                .toList(), "purple");
        slide.getChildren().addAll(spacer(), sectionHeader("Top 5 Artists"), list, spacer());
        return slide;
    }

    private VBox buildTopAlbumGenreSlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(7);
        slide.getChildren().addAll(spacer(), introLabel("Your favourites at a glance"),
                bigEmoji("💿"), accentLabel("Top Album"), heroLabel(data.topAlbumTitle), spacer(12),
                bigEmoji("🎼"), accentLabel("Top Genre"), subLabel(data.topGenreName), spacer());
        return slide;
    }

    private VBox buildMonthlySlide(YearEndReportViewModel.WrappedData data) {
        VBox slide = newSlide(8);
        slide.getChildren().addAll(sectionHeader("Listening by Month"),
                buildBarChart(data.monthlyMinutes, data.year));
        return slide;
    }

    private VBox buildClosingSlide(int year) {
        VBox slide = newSlide(9);
        slide.getChildren().addAll(spacer(), bigEmoji("🎶"), heroLabel("That's a wrap!"),
                subLabel("See you in " + (year + 1)), spacer(), bodyLabel("Thanks for the music."), spacer());
        return slide;
    }

    private VBox buildLoadingCard(int year) {
        VBox card = newSlide(0);
        card.getChildren().addAll(spacer(), bigEmoji("⏳"),
                subLabel("Building your " + year + " Wrapped…"), spacer());
        return card;
    }

    private VBox buildBarChart(Map<YearMonth, Integer> monthly, int year) {
        String[] MONTH_ABBR = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        int    max  = monthly.values().stream().mapToInt(v -> v).max().orElse(1);

        double innerW = currentSlideW - 72;
        double chartW = innerW * 0.80;
        double maxH   = currentSlideH * 0.25;
        double colW   = chartW / 12;
        double barW   = colW * 0.65;

        HBox bars = new HBox(0);
        bars.setAlignment(Pos.CENTER);
        bars.setPrefWidth(chartW);
        bars.setMaxWidth(chartW);

        for (int m = 1; m <= 12; m++) {
            YearMonth ym      = YearMonth.of(year, m);
            int       minutes = monthly.getOrDefault(ym, 0);
            double    h       = Math.max(4, (max > 0 ? (double) minutes / max : 0) * maxH);

            Label val = new Label(minutes > 0 ? (minutes >= 60 ? (minutes / 60) + "h" : minutes + "m") : "");
            val.getStyleClass().add("bar-value-label");
            val.setAlignment(Pos.CENTER);
            val.setMaxWidth(colW);

            Region bar = new Region();
            bar.setPrefHeight(h);
            bar.setPrefWidth(barW);
            bar.setStyle("-fx-background-color: " + (minutes == max ? "#1db954" : "rgba(255,255,255,0.25)")
                    + "; -fx-background-radius: 4 4 0 0;");

            Label lbl = new Label(MONTH_ABBR[m - 1]);
            lbl.getStyleClass().add("bar-month-label");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(colW);

            VBox col = new VBox(4, val, bar, lbl);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setPrefWidth(colW);
            bars.getChildren().add(col);
        }

        Region axisLine = new Region();
        axisLine.getStyleClass().add("chart-axis-line");
        axisLine.setMaxWidth(chartW);

        VBox chart = new VBox(4, bars, axisLine);
        chart.setAlignment(Pos.CENTER);
        chart.getStyleClass().add("chart-container");
        return chart;
    }

    private VBox rankList(List<Map.Entry<String, Long>> items, String accentVariant) {
        VBox list = new VBox(10);
        list.setPadding(new Insets(10, 20, 10, 20));
        String[] medals = {"🥇", "🥈", "🥉", "  4.", "  5."};
        for (int i = 0; i < items.size() && i < 5; i++) {
            HBox row = new HBox(14);
            row.getStyleClass().add("rank-row");

            Label medal = new Label(medals[i]);
            medal.getStyleClass().add("rank-medal");
            medal.setMinWidth(34);

            String[] parts   = items.get(i).getKey().split("\n", 2);
            VBox     textBox = new VBox(1);
            Label    main    = new Label(parts[0]);
            main.getStyleClass().add("rank-title");
            main.setMaxWidth(currentSlideW * 0.5);
            textBox.getChildren().add(main);
            if (parts.length > 1) {
                Label sub = new Label(parts[1]);
                sub.getStyleClass().add("rank-subtitle");
                textBox.getChildren().add(sub);
            }
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label plays = new Label(items.get(i).getValue() + "×");
            plays.getStyleClass().add("rank-plays-" + accentVariant);

            row.getChildren().addAll(medal, textBox, plays);
            list.getChildren().add(row);
        }
        return list;
    }

    private VBox newSlide(int paletteIndex) {
        VBox slide = new VBox(14);
        slide.setAlignment(Pos.CENTER);
        slide.setPadding(new Insets(40, 36, 40, 36));
        slide.getStyleClass().add("slide-card");
        slide.setStyle("-fx-background-color: " + GRADIENTS[paletteIndex % GRADIENTS.length] + ";");
        return slide;
    }

    private Label heroLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-hero");
        l.setMaxWidth(currentSlideW - 72);
        l.setWrapText(true);
        return l;
    }

    private Label accentLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-accent");
        l.setWrapText(true);
        return l;
    }

    private Label introLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.getStyleClass().add("slide-intro");
        return l;
    }

    private Label subLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-sub");
        l.setWrapText(true);
        l.setMaxWidth(currentSlideW - 72);
        return l;
    }

    private Label bodyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-body");
        l.setWrapText(true);
        l.setMaxWidth(currentSlideW - 72);
        return l;
    }

    private Label hintLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-hint");
        return l;
    }

    private Label sectionHeader(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("slide-section-header");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Label bigEmoji(String emoji) {
        Label l = new Label(emoji);
        l.getStyleClass().add("slide-big-emoji");
        return l;
    }

    private Button navButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("wrapped-nav-btn");
        return btn;
    }

    private Region spacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private Region spacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }

    private String firstArtist(Song song) {
        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
            return song.getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist");
        }
        return "Unknown Artist";
    }
}