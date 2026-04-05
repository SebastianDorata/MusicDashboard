package com.sebastiandorata.musicdashboard.presentation.helpers;

import lombok.Getter;

/**
 * <p>Configuration for player panel sizing.
 * Allows different pages (Dashboard, Analytics) to customize player appearance.</p>
 *
 * <p>SRP: Encapsulates all player sizing concerns.
 * <p>DIP: Controllers depend on this config so that there are no hardcoded values.
 */
public class PlayerConfig {
    /**
     * Defines the two supported rendering sizes for the playback panel.
     *
     * <ul>
     *   <li>{@code LARGE} — used on the Dashboard; full font sizes, icon sizes,
     *       and a progress slider.</li>
     *   <li>{@code SMALL} — used on the Analytics page bottom bar; 60% font and
     *       icon sizes, no progress slider.</li>
     * </ul>
     */
    public enum PlayerSize {LARGE, SMALL}

    @Getter
    private final PlayerSize size;
    private final double fontSizeMultiplier;
    private final double iconSizeMultiplier;
    private final double spacingMultiplier;

    public PlayerConfig(PlayerSize size) {
        this.size = size;

        switch (size) {
            case LARGE -> {
                this.fontSizeMultiplier = 1.0;
                this.iconSizeMultiplier = 1.0;
                this.spacingMultiplier = 1.0;
            }
            case SMALL -> {
                this.fontSizeMultiplier = 0.6;
                this.iconSizeMultiplier = 0.6;
                this.spacingMultiplier = 0.5;
            }
            default -> {
                this.fontSizeMultiplier = 1.0;
                this.iconSizeMultiplier = 1.0;
                this.spacingMultiplier = 1.0;
            }
        }
    }


    public int getTitleFontSize() {
        return (int) (30 * fontSizeMultiplier);
    }

    public int getArtistFontSize() {
        return (int) (14 * fontSizeMultiplier);
    }

    public int getTimeFontSize() {
        return (int) (12 * fontSizeMultiplier);
    }

    public int getPlayPauseIconSize() {
        return (int) (24 * iconSizeMultiplier);
    }

    public int getNavIconSize() {
        return (int) (20 * iconSizeMultiplier);
    }

    public double getControlSpacing() {
        return 12 * spacingMultiplier;
    }

    public double getInfoSectionSpacing() {
        return 6 * spacingMultiplier;
    }
}