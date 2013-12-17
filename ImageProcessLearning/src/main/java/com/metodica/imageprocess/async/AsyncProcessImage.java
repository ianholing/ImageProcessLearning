package com.metodica.imageprocess.async;

import android.os.AsyncTask;

import com.metodica.imageprocess.imageprocess.ImageProcessingRGB;

/**
 * Created by Jacob on 9/9/13.
 */
public class AsyncProcessImage extends AsyncTask<Void, Void, Boolean> {
    private ImageProcessingRGB engine = null;
    private byte[] data = null;
    long initTime;

    public AsyncProcessImage(ImageProcessingRGB engine, byte[] data) {
        this.engine = engine;
        this.data = data;
    }

    @Override
    protected void onPreExecute() {
        // This method works in UI Thread
    }

    @Override
    protected Boolean doInBackground(Void... v) {
        initTime = System.currentTimeMillis();
        // This works in parallel
        try {
            engine.setNewFrame(data);
            engine.process();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result)
    {
        engine.addLastInfoText("Frame Process Time: " + ((System.currentTimeMillis() - initTime)) +"ms");
    }
}
