package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.controller.DashboardController;
import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.service.LoginAttemptService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Top-level controller for the authentication page.
 *
 * <p>Renders two independent card panels side by side — a login card on the
 * left and a registration card on the right — each with their own input
 * fields, feedback labels, and submit buttons. The two cards share no state
 * with each other.
 *
 * <p>Login is protected against brute force attacks via
 * {@link LoginAttemptService}, which enforces escalating lockout periods
 * and a 5-second delay on every failed attempt. The login attempt runs on
 * a background thread so the delay never blocks the JavaFX UI thread.
 *
 * <p>On successful login, the authenticated {@link User} is stored in
 * {@link UserSessionService} and the application navigates to the
 * {@link DashboardController}.
 */
@Component
public class AuthenticationController {

    @Lazy @Autowired private AuthenticationService authenticationService;
    @Autowired private UserSessionService    sessionService;
    @Getter @Setter @Lazy @Autowired private DashboardController dashboardController;
    @Autowired private LoginAttemptService loginAttemptService;

    private Button loginBtn;
    private final TextField     usernameField      = new TextField();
    private final PasswordField passwordField      = new PasswordField();
    private final Label         feedbackLabel      = new Label();
    private final TextField     signupUsernameField = new TextField();
    private final TextField     signupEmailField    = new TextField();
    private final PasswordField signupPasswordField = new PasswordField();
    private final Label         signupFeedbackLabel = new Label();

    @PostConstruct
    public void register() {
        MainController.registerAuth(this);
    }

    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        VBox page = new VBox();
        page.getStyleClass().add("main-background");
        page.setAlignment(Pos.TOP_CENTER);

        Label appTitle = new Label("Music Dashboard");
        appTitle.getStyleClass().add("header");

        Label subtitle = new Label("Your personal listening stats & library");
        subtitle.getStyleClass().add("login-subtitle");

        HBox cards = new HBox(40);
        cards.setAlignment(Pos.CENTER);
        cards.getChildren().addAll(buildLoginCard(), buildSignupCard());

        page.getChildren().addAll(appTitle, subtitle, cards);
        return new Scene(page, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    /**
     * Builds the left-side login card containing the username/email field,
     * password field, feedback label, and sign-in button.
     *
     * <p>The login button is stored as an instance field so that
     * {@link #handleLogin()} can disable it during a login attempt to
     * prevent double submissions.
     *
     * @return the fully constructed login {@link VBox} card
     */
    private VBox buildLoginCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label cardTitle = new Label("Sign in");
        cardTitle.getStyleClass().add("login-card-title");

        usernameField.setPromptText("Username or Email");
        usernameField.getStyleClass().add("username-Field");

        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-Field");

        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> handleLogin());

        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        loginBtn = new Button("Sign in");
        loginBtn.getStyleClass().add("login-btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin());

        card.getChildren().addAll(
                cardTitle,
                fieldGroup("Username or Email", usernameField),
                fieldGroup("Password", passwordField),
                feedbackLabel,
                loginBtn
        );
        return card;
    }

    /**
     * Builds the right-side registration card containing the username,
     * email, and password fields along with a feedback label and
     * create account button.
     *
     * @return the fully constructed signup {@link VBox} card
     */
    private VBox buildSignupCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label cardTitle = new Label("Create account");
        cardTitle.getStyleClass().add("login-card-title");

        signupUsernameField.setPromptText("Username");
        signupUsernameField.getStyleClass().add("username-Field");

        signupEmailField.setPromptText("Email");
        signupEmailField.getStyleClass().add("email-Field");

        signupPasswordField.setPromptText("Password");
        signupPasswordField.getStyleClass().add("password-Field");

        signupFeedbackLabel.setWrapText(true);
        signupFeedbackLabel.setVisible(false);
        signupFeedbackLabel.setManaged(false);

        Button signupBtn = new Button("Create account");
        signupBtn.getStyleClass().add("login-btn-primary");
        signupBtn.setMaxWidth(Double.MAX_VALUE);
        signupBtn.setOnAction(e -> handleSignup());

        card.getChildren().addAll(
                cardTitle,
                fieldGroup("Username", signupUsernameField),
                fieldGroup("Email", signupEmailField),
                fieldGroup("Password", signupPasswordField),
                signupFeedbackLabel,
                signupBtn
        );
        return card;
    }


    /**
     * Builds a labelled field group consisting of a bold grey label above
     * the given input control, used consistently across both cards.
     *
     * @param labelText the text to display above the field
     * @param field     the input control to place below the label
     * @return a {@link VBox} containing the label and field
     */
    private VBox fieldGroup(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("txt-grey-sm-bld");
        VBox group = new VBox(4, lbl, field);
        return group;
    }


    /**
     * Handles a login submission from the login card.
     *
     * <p>Validates that both fields are non-empty, then dispatches the
     * authentication attempt to a background thread via a plain
     * {@link Thread} so the 5-second failed-attempt delay imposed by
     * {@link LoginAttemptService} does not block the JavaFX UI thread.
     *
     * <p>The login button is disabled for the duration of the attempt
     * to prevent double submissions. All UI updates after the background
     * thread completes are dispatched back to the JavaFX thread via
     * {@link javafx.application.Platform#runLater(Runnable)}.
     *
     * <p>On success, the authenticated user is stored in
     * {@link UserSessionService} and the application navigates to the
     * dashboard. On failure, the remaining attempt count is displayed.
     * On lockout, the lockout message from {@link LoginAttemptService}
     * is displayed directly.
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showLoginFeedback("Please enter your username and password.", false);
            return;
        }

        // Disable the button during the attempt to prevent double submissions
        // Login runs on a background thread so the 5s delay does not freeze the UI
        loginBtn.setDisable(true);
        showLoginFeedback("Signing in...", true);

        new Thread(() -> {
            try {
                Optional<User> userOptional = authenticationService.login(username, password);
                javafx.application.Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        showLoginFeedback("Welcome back, " + user.getUsername() + "!", true);
                        sessionService.setCurrentUser(user);
                        dashboardController.show();
                    } else {
                        int remaining = loginAttemptService.getRemainingAttempts(username);
                        showLoginFeedback(
                                "Incorrect username or password. "
                                        + remaining + " attempt"
                                        + (remaining == 1 ? "" : "s") + " remaining.", false);
                    }
                });
            } catch (IllegalStateException e) {
                javafx.application.Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    showLoginFeedback(e.getMessage(), false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    showLoginFeedback("Login failed: " + e.getMessage(), false);
                });
            }
        }).start();
    }

    /**
     * Handles a registration submission from the signup card.
     *
     * <p>Validates that username and password are non-empty and that the
     * password meets the minimum length requirement of 6 characters before
     * delegating to {@link AuthenticationService#register(String, String, String)}.
     *
     * <p>On success, a confirmation message is shown and the password field
     * is cleared so the user can sign in via the login card. On failure,
     * the error message from the service is displayed on the signup card's
     * feedback label.
     */
    private void handleSignup() {
        String username = signupUsernameField.getText().trim();
        String email    = signupEmailField.getText().trim();
        String password = signupPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showSignupFeedback("Please enter a username and password.", false);
            return;
        }
        if (password.length() < 6) {
            showSignupFeedback("Password must be at least 6 characters.", false);
            return;
        }

        try {
            authenticationService.register(email, password, username);
            showSignupFeedback("Account created! You can now sign in.", true);
            signupPasswordField.clear();
        } catch (IllegalArgumentException e) {
            showSignupFeedback(e.getMessage(), false);
        } catch (Exception e) {
            showSignupFeedback("Registration failed: " + e.getMessage(), false);
        }
    }



    /**
     * Displays a feedback message on the signup card.
     *
     * @param message the text to display
     * @param success {@code true} to apply the success style (green),
     *                {@code false} to apply the error style (red)
     */
    private void showSignupFeedback(String message, boolean success) {
        signupFeedbackLabel.setText(message);
        signupFeedbackLabel.getStyleClass().setAll(
                success ? "login-success-label" : "login-error-label");
        signupFeedbackLabel.setVisible(true);
        signupFeedbackLabel.setManaged(true);
    }

    /**
     * Displays a feedback message on the login card.
     *
     * @param message the text to display
     * @param success {@code true} to apply the success style (green),
     *                {@code false} to apply the error style (red)
     */
    private void showLoginFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().setAll(
                success ? "login-success-label" : "login-error-label");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }
}

