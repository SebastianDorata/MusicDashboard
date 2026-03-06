package com.sebastiandorata.musicdashboard.Controller;

import com.sebastiandorata.musicdashboard.util.Utils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuthenticationController {
    private Label welcomeLable = new Label("Welcome to your Music Dashboard");
    private TextField usernameField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private  Button loginButton = new Button("Login");
    private Label signupLable = new Label("Don't have an accont? Click here");


    public void show(){// this method will be called every time we want to switch views
        Scene scene = createScene();
        scene.getStylesheets().add(getClass().getResource("/login.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
        MainController.switchViews(scene);
    }

    private Scene createScene(){
        VBox mainContainerBox = new VBox();//main div
        mainContainerBox.getStyleClass().addAll("main-background");// To style the 'div' in the css page
        mainContainerBox.setAlignment(Pos.TOP_CENTER);

        welcomeLable.getStyleClass().addAll("header");
        VBox loginFormBox = createLoginFormBox();

        mainContainerBox.getChildren().addAll(welcomeLable,loginFormBox);// When the page runs, these two objects will be displayed
        return new Scene(mainContainerBox, Utils.APP_Width, Utils.APP_Height);
    }

    private VBox createLoginFormBox(){
        VBox loginFormVBox = new VBox(74);// Creating login VBox obj of subtype VBox. Each node in the VBox will a spacing of 74px
        // Style each node in the VBox login page
        loginFormVBox.setAlignment(Pos.CENTER);

        usernameField.getStyleClass().addAll("username-Field");
            usernameField.setPromptText("Enter Username:");
        passwordField.getStyleClass().addAll("password-Field");
            passwordField.setPromptText("Enter Password:");
        loginButton.getStyleClass().addAll("btn-blue", "cursor");
        signupLable.getStyleClass().addAll("btn-blue", "cursor");






        loginFormVBox.getChildren().addAll(usernameField,passwordField,loginButton,signupLable);// nested div
        return loginFormVBox;
    }

}
