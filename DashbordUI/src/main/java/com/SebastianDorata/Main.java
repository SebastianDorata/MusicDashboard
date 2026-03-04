package com.SebastianDorata;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(new VBox());
        stage.setTitle("Hello World");
        stage.setScene(scene);
        stage.show();
    }
}


