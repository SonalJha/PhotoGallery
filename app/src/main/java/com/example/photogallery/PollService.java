package com.example.photogallery;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by sjha3 on 9/10/16.
 */
public class PollService extends IntentService {

    private static final String TAG = "PollService";

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final boolean isNetworkAvailable = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo() != null;
        if (!isNetworkAvailable)
            return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String query = prefs.getString(FlickrFetcher.PERF_SEARCH_QUERY, null);
        final String lastResultId = prefs.getString(FlickrFetcher.PREF_LAST_RESULT_ID, null);

        final ArrayList<GalleryItem> items;
        if (query != null)
            items = new FlickrFetcher().search(query);
        else
            items = new FlickrFetcher().fetchItems();

        if (items.size() == 0)
            return;

        // String result

        Log.i(TAG, "Received an intent " + intent);
    }
}
