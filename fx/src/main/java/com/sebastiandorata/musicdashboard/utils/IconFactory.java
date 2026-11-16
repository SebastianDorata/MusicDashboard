package com.sebastiandorata.musicdashboard.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Factory for loading application icons from the {@code /icons/} resource directory.
 *
 * <p>Icons are loaded and wrapped in a configured {@link ImageView}. All icons are expected to be in
 * {@code src/main/resources/icons/} and named {@code <iconName>.png}.
 *
 * <p>If an icon cannot be found or loaded, an empty {@link ImageView}
 * is returned so the UI degrades without throwing an exception.
 */
public class IconFactory {

    /**
     * @param iconName the name of the icon file without the {@code .png} extension
     * @param size     the width and height in pixels to apply to the image view
     * @return a configured {@link ImageView}, or an empty one if the icon could not be loaded
     */
    public static ImageView createIcon(String iconName, int size) {
        try {
            String path = "/icons/" + iconName + ".png";
            System.out.println("Loading icon: " + path); // Debug

            var stream = IconFactory.class.getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Icon stream is null for: " + path);
                return new ImageView();
            }

            Image image = new Image(stream);
            ImageView icon = new ImageView(image);
            icon.setFitWidth(size);
            icon.setFitHeight(size);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            return icon;
        } catch (Exception e) {
            System.err.println("Icon not found: " + iconName);
            e.printStackTrace();
            return new ImageView(); // Empty icon
        }
    }
}