package com.sebastiandorata.musicdashboard.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class IconFactory {

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
