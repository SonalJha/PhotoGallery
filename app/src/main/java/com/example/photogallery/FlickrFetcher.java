package com.example.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by sjha3 on 9/8/16.
 */
public class FlickrFetcher {

    public static final String TAG = "FlickrFetcher";
    public static final String PERF_SEARCH_QUERY = "searchQuery";
    public static final String ENDPOINT = "https://api.flickr.com/services/rest";
    public static final String API_KEY = "468b7c425a0fceac3dd658f2fd3e250b";
    public static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    public static final String PARAM_EXTRAS = "extras";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";


    public static final String EXTRA_SMALL_URL = "url_s";
    private static final String XML_PHOTO = "photo";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_TEXT = "text";


    byte[] getUrlBytes(final String urlSpec) throws IOException {
        final URL url = new URL(urlSpec);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            int bytesRead = 0;
            final byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrl(final String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public ArrayList<GalleryItem> downloadGalleryItems(final String url) {
        final ArrayList<GalleryItem> items = new ArrayList<>();
        try {
            final String xmlString = getUrl(url);
            Log.i(TAG, "Received xml:" + xmlString);
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));

            parseItems(items, parser);
        } catch (final IOException ioe) {
            Log.e(TAG, "failed to fetch items", ioe);
        } catch (final XmlPullParserException xppe) {
            Log.e(TAG, "failed to fetch items", xppe);
        }
        return items;
    }

    public ArrayList<GalleryItem> fetchItems() {
        final String url = Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method", METHOD_GET_RECENT).appendQueryParameter("api_key", API_KEY).appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL).build().toString();
        return downloadGalleryItems(url);
    }

    public ArrayList<GalleryItem> search(final String query) {
        final String url = Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("method", METHOD_SEARCH).appendQueryParameter("api_key", API_KEY).appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL).appendQueryParameter(PARAM_TEXT, query).build().toString();
        return downloadGalleryItems(url);
    }

    void parseItems(final ArrayList<GalleryItem> items, final XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
                final String id = parser.getAttributeValue(null, "id");
                final String caption = parser.getAttributeValue(null, "title");
                final String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);

                final GalleryItem item = new GalleryItem();
                item.setId(id);
                item.setCaption(caption);
                item.setUrl(smallUrl);

                items.add(item);
            }
            eventType = parser.next();
        }

    }
}
