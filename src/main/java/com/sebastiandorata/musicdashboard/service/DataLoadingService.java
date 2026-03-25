package com.sebastiandorata.musicdashboard.service;

import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Supplier;


@Service
public class DataLoadingService {

    public <T> void loadAsync(Supplier<T> loader, Consumer<T> onSuccess, Consumer<Exception> onError) {
        new Thread(() -> {
            try {
                T result = loader.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        }).start();
    }


    public <T> void loadAsync(Supplier<T> loader, Consumer<T> onSuccess) {
        loadAsync(
                loader,
                onSuccess,
                e -> System.err.println("Data load failed: " + e.getMessage())
        );
    }
}