package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sjha3 on 9/9/16.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {

    private static final int MESSAGE_DOWNLOAD = 0;
    private static final String TAG = "ThumbnailDownloader";

    Handler mHandler;
    Handler mResponseHandler;
    Listener<Token> mListener;

    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(final Listener<Token> listener) {
        mListener = listener;
    }


    public ThumbnailDownloader(final Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    final Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for the url:" + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    public void queueThumbnail(final Token token, final String url) {
        Log.i(TAG, "Got an URL: " + url);
        requestMap.put(token, url);
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }

    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            if (url == null)
                return;

            final byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url) {
                        return;
                    }
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });
        } catch (final IOException ioe) {
            Log.e(TAG, "Error downloading image");
        }
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }


}
