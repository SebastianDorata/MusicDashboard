package com.sebastiandorata.musicdashboard.viewmodel;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;


public class ChartData {
    public Map<YearMonth, Integer> currentYearData;
    public Map<YearMonth, Integer> previousYearData;
    public Integer selectedYear;
    public Integer previousYear;
    public Integer highestYear;
    public List<Integer> allYears;

    public ChartData(
            Map<YearMonth, Integer> currentYearData,
            Map<YearMonth, Integer> previousYearData,
            Integer selectedYear,
            Integer previousYear,
            Integer highestYear,
            List<Integer> allYears) {
        this.currentYearData = currentYearData;
        this.previousYearData = previousYearData;
        this.selectedYear = selectedYear;
        this.previousYear = previousYear;
        this.highestYear = highestYear;
        this.allYears = allYears;
    }
}