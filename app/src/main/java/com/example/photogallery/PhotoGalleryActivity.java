package com.example.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    public Fragment createFragment() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onNewIntent(final Intent intent) {
        final PhotoGalleryFragment fragment = (PhotoGalleryFragment) getSupportFragmentManager().findFragmentById(R.id.FragmentContainer);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(FlickrFetcher.PERF_SEARCH_QUERY, query).commit();
        }
        fragment.updateItems();
    }
}
