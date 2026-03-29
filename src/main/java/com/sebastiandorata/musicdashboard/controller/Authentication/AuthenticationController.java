package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.controller.Dashboard.DashboardController;
import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationController {

    @Lazy  @Autowired private AuthenticationService authenticationService;
    @Autowired         private UserSessionService    sessionService;
    @Getter @Setter
    @Lazy  @Autowired private DashboardController dashboardController;


    private final TextField       usernameField  = new TextField();
    private final TextField       emailField     = new TextField();
    private final PasswordField   passwordField  = new PasswordField();
    private final Label           feedbackLabel  = new Label();

    @PostConstruct
    public void register() {
        MainController.registerAuth(this);
    }

    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/login.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
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

        VBox card = buildCard();

        page.getChildren().addAll(appTitle, subtitle, card);
        return new Scene(page, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private VBox buildCard() {
        VBox card = new VBox(12);
            card.getStyleClass().add("login-card");
            card.setAlignment(Pos.TOP_LEFT);

            Label cardTitle = new Label("Sign in");
                cardTitle.getStyleClass().add("login-card-title");


            usernameField.setPromptText("Username");
            usernameField.getStyleClass().add("username-Field");

            emailField.setPromptText("Email (sign up only)");
            emailField.getStyleClass().add("email-Field");

            passwordField.setPromptText("Password");
            passwordField.getStyleClass().add("password-Field");


            passwordField.setOnAction(e -> handleLogin());
            usernameField.setOnAction(e -> handleLogin());


            feedbackLabel.setWrapText(true);
            feedbackLabel.setVisible(false);
            feedbackLabel.setManaged(false);


        Button loginBtn = new Button("Sign in");
            loginBtn.getStyleClass().add("login-btn-primary");
            loginBtn.setMaxWidth(Double.MAX_VALUE);
            loginBtn.setOnAction(e -> handleLogin());

        HBox divider = buildDivider();

        Button signupBtn = new Button("Create account");
            signupBtn.getStyleClass().add("login-btn-secondary");
            signupBtn.setMaxWidth(Double.MAX_VALUE);
            signupBtn.setOnAction(e -> handleSignup());

        card.getChildren().addAll(
                cardTitle,
                fieldGroup("Username", usernameField),
                fieldGroup("Email", emailField),
                fieldGroup("Password", passwordField),
                feedbackLabel,
                loginBtn,
                divider,
                signupBtn
        );
        return card;
    }


    private VBox fieldGroup(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("login-field-label");
        VBox group = new VBox(4, lbl, field);
        return group;
    }


    private HBox buildDivider() {
        Region lineLeft  = new Region();
        Region lineRight = new Region();
        lineLeft.getStyleClass().add("login-divider-line");
        lineRight.getStyleClass().add("login-divider-line");
        HBox.setHgrow(lineLeft,  Priority.ALWAYS);
        HBox.setHgrow(lineRight, Priority.ALWAYS);
        lineLeft.setMaxWidth(Double.MAX_VALUE);
        lineRight.setMaxWidth(Double.MAX_VALUE);

        Label or = new Label("or");
        or.getStyleClass().add("login-divider-label");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(lineLeft, or, lineRight);
        return row;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showFeedback("Please enter both username and password.", false);
            return;
        }

        try {
            var userOptional = authenticationService.login(username, password);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                showFeedback("Welcome back, " + user.getUsername() + "!", true);
                sessionService.setCurrentUser(user);
                dashboardController.show();
            } else {
                showFeedback("Incorrect username or password.", false);
            }
        } catch (Exception e) {
            showFeedback("Login failed: " + e.getMessage(), false);
        }
    }

    private void handleSignup() {
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showFeedback("Please enter a username and password.", false);
            return;
        }
        if (password.length() < 6) {
            showFeedback("Password must be at least 6 characters.", false);
            return;
        }

        try {
            authenticationService.register(email, password, username);
            showFeedback("Account created! You can now sign in.", true);
            passwordField.clear();
        } catch (IllegalArgumentException e) {
            showFeedback(e.getMessage(), false);
        } catch (Exception e) {
            showFeedback("Registration failed: " + e.getMessage(), false);
        }
    }



    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().setAll(success ? "login-success-label" : "login-error-label");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }
}

