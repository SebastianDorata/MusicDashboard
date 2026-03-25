package com.sebastiandorata.musicdashboard.viewmodel;


public class YearComparisonData {
    public Integer previousYear;
    public Integer highestYear;
    public boolean hasPreviousYear;
    public boolean hasHighestYear;

    public YearComparisonData(Integer previousYear, Integer highestYear,
                              boolean hasPreviousYear, boolean hasHighestYear) {
        this.previousYear = previousYear;
        this.highestYear = highestYear;
        this.hasPreviousYear = hasPreviousYear;
        this.hasHighestYear = hasHighestYear;
    }
}