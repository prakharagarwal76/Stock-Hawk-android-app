package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  public static final int STATUS_OK = 0;
  public static final int STATUS_ERROR_JSON = 1;
  public static final int STATUS_SERVER_ERROR = 2;
  public static final int STATUS_SERVER_DOWN = 3;
  public static final int STATUS_NO_NETWORK = 4;
  public static final int STATUS_UNKNOWN = 5;

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try {
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
              + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      setStockStatus(mContext, STATUS_UNKNOWN);
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
              new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
              null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
                  URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          setStockStatus(mContext, STATUS_UNKNOWN);
        }
      } else if (initQueryCursor != null) {
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++) {
          mStoredSymbols.append("\"" +
                  initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          setStockStatus(mContext, STATUS_UNKNOWN);
        }
      }
    } else if (params.getTag().equals("add")) {
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        setStockStatus(mContext, STATUS_UNKNOWN);
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    urlString = urlStringBuilder.toString();
    try {
      getResponse = fetchData(urlString);
      Log.i("Example", "onRunTask: "  + urlString);
      result = GcmNetworkManager.RESULT_SUCCESS;
      try {
        ContentValues contentValues = new ContentValues();
        // update ISCURRENT to 0 (false) so new data is current
        if (isUpdate) {
          contentValues.put(QuoteColumns.ISCURRENT, 0);
          mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                  null, null);
        }

          ArrayList<ContentProviderOperation> batchOperations = Utils.quoteJsonToContentVals(getResponse);


        if(getResponse!=null) {
          //JSONObject jsonObject
          if (batchOperations != null && batchOperations.size() != 0) {
            // if(batchOperations.get(0))
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    batchOperations);
          }
        } else {
            //The stock doesn't exist.

            Intent intent = new Intent();
            intent.setAction("com.sam_chordas.android.stockhawk.ui.MyStocksActivity.STOCK_NOT_FOUND");
            mContext.sendBroadcast(intent);

          }


      } catch (RemoteException | OperationApplicationException e) {
        setStockStatus(mContext, STATUS_ERROR_JSON);
      } catch (JSONException e) {
        e.printStackTrace();
        setStockStatus(mContext, STATUS_ERROR_JSON);
      }
    } catch (IOException e) {
      e.printStackTrace();
      setStockStatus(mContext, STATUS_SERVER_DOWN);
    }

      return result;
  }

  static public void setStockStatus(Context context,int stockStatus) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sp.edit();
    editor.putInt(context.getString(R.string.stockStatus), stockStatus);
    editor.apply();
  }

}
