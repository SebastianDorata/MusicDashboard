package com.sebastiandorata.musicdashboard.presentation.helpers;

import javafx.stage.Screen;
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
    private final double baseUnit;

    public PlayerConfig(PlayerSize size) {
        this.size = size;
        double screenH = Screen.getPrimary().getVisualBounds().getHeight();
        this.baseUnit = screenH / 1080.0; // 1.0 on a 1080p screen

        switch (size) {
            case LARGE -> {
                this.fontSizeMultiplier = 1.0;
                this.iconSizeMultiplier = 1.0;
                this.spacingMultiplier  = 1.0;
            }
            case SMALL -> {
                this.fontSizeMultiplier = 0.6;
                this.iconSizeMultiplier = 0.6;
                this.spacingMultiplier  = 0.5;
            }
            default -> {
                this.fontSizeMultiplier = 1.0;
                this.iconSizeMultiplier = 1.0;
                this.spacingMultiplier  = 1.0;
            }
        }
    }



    public int getTitleFontSize()    {
        return (int) (30  * fontSizeMultiplier * baseUnit);
    }
    public int getArtistFontSize()   {
        return (int) (14  * fontSizeMultiplier * baseUnit);
    }
    public int getTimeFontSize()     {
        return (int) (16  * fontSizeMultiplier * baseUnit);
    }
    public int getPlayPauseIconSize(){
        return (int) (22  * iconSizeMultiplier * baseUnit);
    }
    public int getNavIconSize()      {
        return (int) (20  * iconSizeMultiplier * baseUnit);
    }
    public double getControlSpacing(){
        return        10  * spacingMultiplier  * baseUnit;
    }
    public double getInfoSectionSpacing() {
        return    6  * spacingMultiplier  * baseUnit;  }


}

