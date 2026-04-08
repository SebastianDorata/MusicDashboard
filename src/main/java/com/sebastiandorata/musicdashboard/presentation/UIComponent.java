package com.sebastiandorata.musicdashboard.presentation;

import com.sebastiandorata.musicdashboard.presentation.shared.StyledView;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstract base class for Spring-managed JavaFX UI components.
 *
 * <p>Injects {@link DataLoadingService}
 * and {@link UserSessionService} and
 * exposes protected convenience methods so subclasses can load data
 * asynchronously and retrieve the current user ID without repeating
 * boilerplate. Error logging is centralized through
 * {@link com.sebastiandorata.musicdashboard.utils.AppUtils#logError(String, String, Exception)}.</p>
 */
public abstract class UIComponent implements StyledView {

    @Autowired
    protected DataLoadingService dataLoadingService;

    @Autowired
    protected UserSessionService userSessionService;



    protected <T> void loadDataAsync(Supplier<T> loader, Consumer<T> onSuccess) {
        dataLoadingService.loadAsync(loader, onSuccess);
    }

    protected Long getCurrentUserId() {
        return userSessionService.getCurrentUserId();
    }


    @Override
    public List<String> getStylesheets() {
        return List.of(); // Default.
    }

}