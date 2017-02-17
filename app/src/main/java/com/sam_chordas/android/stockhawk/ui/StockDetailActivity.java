package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.HistoricalData;
import com.sam_chordas.android.stockhawk.rest.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class StockDetailActivity extends AppCompatActivity {

    private static final String TAG = StockDetailActivity.class.getSimpleName();

    LineChart lineChart;

    String symbol = "";
    String name = "";
    String currency = "";
    String lasttradedate = "";
    String daylow = "";
    String dayhigh = "";
    String yearlow = "";
    String yearhigh = "";
    String earningsshare = "";
    String marketcapitalization = "";
    TextView tv_symbol,tv_name,tv_currency,tv_lasttradedate,tv_daylow,tv_dayhigh,tv_yearlow,tv_yearhigh,tv_earningsshare,tv_marketcapitalization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lineChart=(LineChart)findViewById(R.id.lineChart_activity_line_graph);
        tv_name=(TextView)findViewById(R.id.name);
        tv_symbol=(TextView)findViewById(R.id.symbol);
        tv_currency=(TextView)findViewById(R.id.currency);
        tv_lasttradedate=(TextView)findViewById(R.id.lasttradedate);
        tv_daylow=(TextView)findViewById(R.id.daylow);
        tv_dayhigh=(TextView) findViewById(R.id.dayhigh);
        tv_yearlow=(TextView)findViewById(R.id.yearlow);
        tv_yearhigh=(TextView)findViewById(R.id.yearhigh);
        tv_earningsshare=(TextView)findViewById(R.id.earningsshare);
        tv_marketcapitalization=(TextView)findViewById(R.id.marketcaptalization);

        Intent i = getIntent();
        symbol = getIntent().getStringExtra("symbol_name");
        name = getIntent().getStringExtra("name");
        currency = getIntent().getStringExtra("currency");
        lasttradedate = getIntent().getStringExtra("lasttradedate");
        daylow = getIntent().getStringExtra("daylow");
        dayhigh = getIntent().getStringExtra("dayhigh");
        yearlow = getIntent().getStringExtra("yearlow");
        yearhigh = getIntent().getStringExtra("yearhigh");
        earningsshare = getIntent().getStringExtra("earningsshare");
        marketcapitalization = getIntent().getStringExtra("marketcaptalization");

        tv_name.setText(name);
        tv_symbol.setText(symbol);
        tv_currency.setText(currency);
        tv_lasttradedate.setText(lasttradedate);
        tv_daylow.setText(daylow);
        tv_dayhigh.setText(dayhigh);
        tv_yearlow.setText(yearlow);
        tv_yearhigh.setText(yearhigh);
        tv_earningsshare.setText(earningsshare);
        tv_marketcapitalization.setText(marketcapitalization);

        Log.d( "onCreate: ",symbol+" "+name);

     String url="http://chartapi.finance.yahoo.com/instrument/1.0/" +symbol+ "/chartdata;type=quote;range=1y/json";
        RequestQueue queue = Volley.newRequestQueue(this);


        StringRequest request= new StringRequest(Request.Method.GET,url,new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {


                String json = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
                try {
                    //Log.d( "doInBackground: ",json);
                    drawGraph(getHistoricalDatafromJson(json));
                } catch (JSONException e) {
                    Log.e("error", e.getMessage(), e);
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG , error.toString());
            }
        }) ;

        queue.add(request);
    }


    public  void drawGraph(HistoricalData[] historicalDatas)
    {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xvalues = new ArrayList<>();

        for (int i = 0; i < historicalDatas.length; i++) {
            HistoricalData historicalData= new HistoricalData(historicalDatas[i]);
            double yValue = historicalData.close;
            xvalues.add(Utils.convertDate(historicalData.date));
            entries.add(new Entry((float) yValue, i));
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelsToSkip(5);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.rgb(182,182,182));

        YAxis left = lineChart.getAxisLeft();
        left.setEnabled(true);
        left.setLabelCount(10, true);
        left.setTextColor(Color.rgb(182,182,182));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextSize(16f);
        lineChart.setDrawGridBackground(true);
        lineChart.setGridBackgroundColor(Color.rgb(25,118,210));
        lineChart.setDescriptionColor(Color.WHITE);
        lineChart.setDescription("Last 12 Months Stock Comparison");

        //String name= getResources().getString(R.string.stock);
        LineDataSet dataSet = new LineDataSet(entries, name);
        LineData lineData = new LineData(xvalues, dataSet);

        lineChart.animateX(2500);
        lineChart.setData(lineData);
    }

    private HistoricalData[] getHistoricalDatafromJson(String HistoricalJsonString)
            throws JSONException {
        final String OWM_SERIES = "series";
        final String OWM_DATE = "Date";
        final String OWM_CLOSE = "close";


        JSONObject moviesJson = new JSONObject(HistoricalJsonString);
        JSONArray series_data = moviesJson.getJSONArray(OWM_SERIES);
        HistoricalData[] resultObjects = new HistoricalData[series_data.length()];

        for (int i = 0; i < series_data.length(); i++) {

            JSONObject singleObject = series_data.getJSONObject(i);
            String date = singleObject.getString(OWM_DATE);
            double close = singleObject.getDouble(OWM_CLOSE);
            resultObjects[i] = new HistoricalData(date,close);
        }

        return resultObjects;
    }


   /* public class FetchHistoricalData extends AsyncTask<Void, Void, HistoricalData[]> {
        private final String LOG_TAG = FetchHistoricalData.class.getName();

        private HistoricalData[] getHistoricalDatafromJson(String HistoricalJsonString)
                throws JSONException {
            final String OWM_SERIES = "series";
            final String OWM_DATE = "Date";
            final String OWM_CLOSE = "close";

            JSONObject moviesJson = new JSONObject(HistoricalJsonString);
            JSONArray series_data = moviesJson.getJSONArray(OWM_SERIES);
            HistoricalData[] resultObjects = new HistoricalData[series_data.length()];

            for (int i = 0; i < series_data.length(); i += 10) {
                JSONObject singleObject = series_data.getJSONObject(i);
                String date = singleObject.getString(OWM_DATE);
                double close = singleObject.getDouble(OWM_CLOSE);
                resultObjects[i] = new HistoricalData(date,close);
            }

            return resultObjects;
        }

        @Override
        protected HistoricalData[] doInBackground(Void... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String historicalJsonString = null;

            try {

                URL url = new URL( "http://chartapi.finance.yahoo.com/instrument/1.0/" +symbol+ "/chartdata;type=quote;range=1y/json");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                historicalJsonString = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                Log.d( "doInBackground: ",historicalJsonString);
                return getHistoricalDatafromJson(historicalJsonString);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(HistoricalData[] result) {
            if (result != null) {
                //moviesAdapter.clear();
                //moviesAdapter.addAll(result);
                //moviesAdapter.notifyDataSetChanged();
            }
        }
    }
*/
}
