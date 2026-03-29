package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class UIComponent {

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


    protected void logError(String componentName, String message, Exception exception) {
        AppUtils.logError(componentName, message, exception);
    }

}