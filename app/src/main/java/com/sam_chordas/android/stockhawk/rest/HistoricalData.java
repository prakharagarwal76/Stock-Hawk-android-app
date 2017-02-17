package com.sam_chordas.android.stockhawk.rest;

/**
 * Created by prakharagarwal on 7/10/2016.
 */
public class HistoricalData {

    public String date;
    public double close;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }


    public HistoricalData() {
        super();
    }

       public HistoricalData(String date, double close) {
        super();
        this.date = date;
        this.close = close;
    }
    public HistoricalData(HistoricalData historicalData) {
        super();
        this.date = historicalData.date;
        this.close = historicalData.close;
    }
}
