package com.sebastiandorata.musicdashboard.controller;
import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.Utils.Utils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;


    @Getter
    @Setter
    @Autowired
    private DashboardController dashboardController;


    private Label welcomeLabel = new Label("Welcome to your Music Dashboard");
    private TextField usernameField = new TextField();
    private TextField emailField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private Button loginButton = new Button("Login");
    private Label signupLabel = new Label("Don't have an account? Click here");

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
        VBox mainContainerBox = new VBox();
        mainContainerBox.getStyleClass().addAll("main-background");
        mainContainerBox.setAlignment(Pos.TOP_CENTER);

        welcomeLabel.getStyleClass().addAll("header");
        VBox loginFormBox = createLoginFormBox();

        mainContainerBox.getChildren().addAll(welcomeLabel, loginFormBox);
        return new Scene(mainContainerBox, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }

    private VBox createLoginFormBox() {
        VBox loginFormVBox = new VBox(20);
        loginFormVBox.setAlignment(Pos.CENTER);

        usernameField.getStyleClass().addAll("username-Field");
        usernameField.setPromptText("Enter Username:");

        passwordField.getStyleClass().addAll("password-Field");
        passwordField.setPromptText("Enter Password:");

        loginButton.getStyleClass().addAll("btn-blue", "cursor");

        signupLabel.getStyleClass().addAll("btn-blue", "cursor");

       //ADD EVENT HANDLERS
        loginButton.setOnAction(event -> handleLogin());
        signupLabel.setOnMouseClicked(event -> handleSignup());

        loginFormVBox.getChildren().addAll(usernameField, emailField, passwordField, loginButton, signupLabel);
        return loginFormVBox;
    }


    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        try {

            var userOptional = authenticationService.login(username, password);

            if (userOptional.isPresent()) {
                showSuccess("Welcome back, " + userOptional.get().getUsername() + "!");

                //Move to Dashboard page
                navigateToDashboardPage();


            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSignup() {
        String username = usernameField.getText().trim();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        try {
            authenticationService.register(email, password, username);

            showSuccess("Registration successful! You can now login.");
            passwordField.clear();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void navigateToDashboardPage() {
        dashboardController.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}






