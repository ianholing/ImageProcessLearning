package com.metodica.imageprocess.imageprocess;

import android.graphics.Bitmap;
import android.hardware.Camera;

import java.text.DecimalFormat;

/**
 * Created by Jacob on 12/16/13.
 */
public abstract class ImageProcessing<imageBaseType> {
    protected int substractionThreshold = 60;
    protected int sobelThreshold = 50;

    protected String resultText;
    protected DecimalFormat numberPresentation = new DecimalFormat("#.##");

    // Image Process RGB Data
    protected byte[] srcImage = null;
    protected byte[] newImage = null;
    protected imageBaseType imageResult = null;

    protected int width;
    protected int height;
    protected int frameSize;
    protected Camera.Parameters camSettings;

    // Default Values
    protected boolean useGreyscale = true;
    protected boolean digitalCount = true;
    protected boolean sobelize = true;
    protected boolean applySobelThreshold = true;
    protected boolean substraction = false;
    protected boolean paintMotion = true;

    ////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    //// SETTERS  AND GETTERS TO CONFIG THE PROCESS \\\\

    ////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    public void setGreyscale(boolean bool) {
        useGreyscale = bool;
    }

    public void setDigitalCount(boolean bool) {
        digitalCount = bool;
    }

    public void setSobelize(boolean bool) {
        sobelize = bool;
    }

    public void setApplySobelThreshold(boolean bool) {
        applySobelThreshold = bool;
    }

    public void setSubstraction(boolean bool) {
        substraction = bool;
    }

    public void setPaintMotion(boolean bool) {
        paintMotion = bool;
    }

    public void setSobelThreshold(int threshold) {
        sobelThreshold = threshold;
    }

    public void setSubstractionThreshold(int threshold) {
        substractionThreshold = threshold;
    }

    public boolean getGreyscale() {
        return useGreyscale;
    }

    public boolean getDigitalCount() {
        return digitalCount;
    }

    public boolean getSobelize() {
        return sobelize;
    }

    public boolean getApplySobelThreshold() {
        return applySobelThreshold;
    }

    public boolean getSubstraction() {
        return substraction;
    }

    public boolean getPaintMotion() {
        return paintMotion;
    }

    public int getSobelThreshold() {
        return sobelThreshold;
    }

    public int getSubstractionThreshold() {
        return substractionThreshold;
    }

    public String getLastInfoText() {
        return resultText;
    }

    public byte[] getPlainImage() {
        return newImage;
    }

    public String addLastInfoText(String text) {
        return resultText += text;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSettings(Camera.Parameters settings) {
        Camera.Size s = settings.getPreviewSize();
        height = s.height;
        width = s.width;
        frameSize = width * height;
        camSettings = settings;

        refreshArrayLongs();
    }

    public void setNewFrame(byte[] data) {
        newImage = data;
    }

    public void refreshKeyFrameValues() {
        resultText += "KEY FRAME REFRESH!\n";
        srcImage = newImage.clone();
    }

    abstract public void refreshArrayLongs();

    abstract public Bitmap getLastImage();

    abstract public void process();

    abstract public void substract(imageBaseType src, imageBaseType dst, int width, int height);

    abstract public void sobelOperator(imageBaseType data, int width, int height);

    abstract public void toGreyscale(imageBaseType rgb, int width, int height);

    abstract public void createMotionSquare(imageBaseType data, int minX, int minY, int maxX, int maxY, int width, int height);
}
