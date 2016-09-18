package com.example.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;

/**
 * Created by sjha3 on 9/8/16.
 */
public class PhotoGalleryFragment extends Fragment {
    GridView mGridView;
    private static final String TAG = "PhotoGalleryFragment";
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        final Intent i = new Intent(getActivity(), PollService.class);
        getActivity().startService(i);

        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            public void onThumbnailDownloaded(final ImageView imageView, final Bitmap thumbnail) {
                if (isVisible())
                    imageView.setImageBitmap(thumbnail);
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    public void updateItems() {
        new FetchItemsTask().execute();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        // Pull out the search view
        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        // get the data from our searchable xml as a searchable info

        final SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        final ComponentName name = getActivity().getComponentName();
        final SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
        searchView.setSearchableInfo(searchInfo);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(FlickrFetcher.PERF_SEARCH_QUERY, null).commit();
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup parent, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_photo_gallery, parent, false);
        mGridView = (GridView) v.findViewById(R.id.gridView);
        setUpAdaptor();
        return v;
    }

    void setUpAdaptor() {
        if (getActivity() == null || mGridView == null)
            return;
        if (mItems != null) {
            mGridView.setAdapter(new GalleryItemAdaptor(mItems));
        } else
            mGridView.setAdapter(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(final Void... params) {
            final Activity activity = getActivity();
            if (activity == null)
                return new ArrayList<GalleryItem>();
            final String query = PreferenceManager.getDefaultSharedPreferences(activity).getString(FlickrFetcher.PERF_SEARCH_QUERY, null);
            if (query != null)
                return new FlickrFetcher().search(query);
            return new FlickrFetcher().fetchItems();
        }

        @Override
        protected void onPostExecute(final ArrayList<GalleryItem> items) {
            mItems = items;
            setUpAdaptor();
        }

    }

    private class GalleryItemAdaptor extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdaptor(final ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);

            final ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.new_image);


            final GalleryItem item = getItem(position);
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());


            return convertView;
        }
    }
}
