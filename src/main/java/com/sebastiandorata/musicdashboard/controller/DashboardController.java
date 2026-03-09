package com.sebastiandorata.musicdashboard.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class DashboardController {

    private Button myLibrary = new Button();
    private Button importFiles = new Button();
    private Button myPlaysits = new Button();
    private Button accountSettings = new Button();
    private TextField searchBar = new TextField();
    private Label accountUsername = new Label();
    private Button accountPlanStatus = new Button("Basic Plan");

    //Data needed on the home page left to right.

    //left VBox N/A

    // Main Dash center HBox area
            // Now playing and progress duration bar with current position in playback. Min,Sec played and Min,Sec remaining.

    //Info box 1/5
            //Playback duration today
                //Total time and average listening session.

    //Info box 2/5
            //Top artist today.


    //Info box 3/5
            //Top album today.

    //Info box 4/5
            //Top artist this week.


    //Info box 5/5
            //Top album this week.

    //Bottom HBox of all-time stats
            //Total playback hours for the current year. Jan 1st to present.
            // Change in playback hours of current year compared against previous year, at the current date.
            // Change in playback hours of current year compared against the year with the highest playback hours, at the current date.

    //Right VBox of top 5 artists of all time
            //left column artist name.
            // Right column total time listend to.

}
