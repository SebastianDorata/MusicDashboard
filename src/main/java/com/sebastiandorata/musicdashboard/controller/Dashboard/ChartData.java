package com.sebastiandorata.musicdashboard.controller.Dashboard;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class ChartData {
    public Map<YearMonth, Integer> currentYearData;
    public Map<YearMonth, Integer> previousYearData;
    public Map<YearMonth, Integer> highestYearData;
    public Integer selectedYear;
    public Integer previousYear;
    public Integer highestYear;
    public List<Integer> allYears;
    public boolean hasPreviousYear;
    public boolean isPreviousYearHighest;

    public ChartData(
            Map<YearMonth, Integer> currentYearData,
            Map<YearMonth, Integer> previousYearData,
            Map<YearMonth, Integer> highestYearData,
            Integer selectedYear,
            Integer previousYear,
            Integer highestYear,
            List<Integer> allYears,
            boolean hasPreviousYear,
            boolean isPreviousYearHighest) {
        this.currentYearData = currentYearData;
        this.previousYearData = previousYearData;
        this.highestYearData = highestYearData;
        this.selectedYear = selectedYear;
        this.previousYear = previousYear;
        this.highestYear = highestYear;
        this.allYears = allYears;
        this.hasPreviousYear = hasPreviousYear;
        this.isPreviousYearHighest = isPreviousYearHighest;
    }
}