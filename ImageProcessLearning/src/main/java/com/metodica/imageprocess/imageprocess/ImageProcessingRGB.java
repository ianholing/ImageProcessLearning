package com.metodica.imageprocess.imageprocess;

import android.graphics.Bitmap;
import android.hardware.Camera;

import com.metodica.imageprocess.SupportLibrary.GeneralImageProcess;

public class ImageProcessingRGB extends ImageProcessing<int[]> {
    protected int[] oldargb = null;
    protected int[] argb = null;

    public ImageProcessingRGB() {}

    public ImageProcessingRGB(byte[] data, Camera.Parameters settings) {
        setSettings(settings);
        setNewFrame(data);
    }

    public Bitmap getLastImage() {
        return Bitmap.createBitmap(
                imageResult,
                width,
                height,
                Bitmap.Config.ARGB_8888);
    }

    public void refreshArrayLongs() {
        argb = new int[frameSize];
        imageResult = new int[frameSize];
        oldargb = argb;
    }

    // For YUV Image
    public void setNewFrame(byte[] data) {
        long initTime = System.currentTimeMillis();
        super.setNewFrame(data);
        argb = GeneralImageProcess.NV21ToRGB(data, false, width, height, frameSize);
        resultText += "RGB Transform Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms\n";
    }

    // For YUV Image
    public void process() {
        imageResult = argb.clone();
        try
        {
            resultText = "";
            if (useGreyscale) toGreyscale(imageResult, width, height);
            if (sobelize) sobelOperator(imageResult, width, height);
            if (substraction) {
                int[] oldresult = oldargb.clone();
                if (useGreyscale) toGreyscale(oldargb, width, height);
                if (sobelize)  sobelOperator(oldresult, width, height);

                substract(
                        imageResult,
                        oldresult,
                        width,
                        height);

            }
        }
        catch(Exception e)
        {
            // Log/trap rendering errors
            e.printStackTrace();
        }
    }

    public void substract(int [] src, int [] dst, int width, int height) {
        int srcR, srcG, srcB, dstR, dstG, dstB, subsR, subsG, subsB, subs = 0;
        int minY = height, maxY = 0, minX = width, maxX = 0;
        double sum = 0;

        for (int i = 0, ij=0; i < height; i++) {
            for (int j = 0; j < width; j++,ij++) {
                if (!useGreyscale) {
                    srcR = (dst[ij] & 0x00ff0000) >> 16;
                    srcG = (dst[ij] & 0x0000ff00) >> 8;
                    srcB = (dst[ij] & 0x000000ff);

                    dstR = (src[ij] & 0x00ff0000) >> 16;
                    dstG = (src[ij] & 0x0000ff00) >> 8;
                    dstB = (src[ij] & 0x000000ff);

                    subsR = Math.abs(srcR - dstR);
                    subsG = Math.abs(srcG - dstG);
                    subsB = Math.abs(srcB - dstB);

                    subs = subsR + subsG + subsB;
                } else {
                    srcB = (dst[ij] & 0x000000ff);
                    dstB = (src[ij] & 0x000000ff);
                    subs = subsR = subsG = subsB = Math.abs(srcB - dstB);
                }

                if (digitalCount) {
                    if (subs > substractionThreshold) sum++;
                    // This could be done sepparating RGB channels too like this:
                    // making a maxChange = maxChange * 3;
//                    if (subsR > threshold) sum++;
//                    if (subsG > threshold) sum++;
//                    if (subsB > threshold) sum++;
                } else sum += subs;

                if (subs > substractionThreshold) {
                    if (paintMotion && subs > substractionThreshold) {
                        if (i < minY) minY = i;
                        if (i > maxY) maxY = i;
                        if (j < minX) minX = j;
                        if (j > maxX) maxX = j;
                    }

                    src[ij] = 0xFF000000 | (subsR << 16) | (subsG << 8) | subsB;
                } else
                    src[ij] = 0xFF000000;
            }
        }

        // CALCULATE PERCENTS AND RESULTS INFO
        int maxChange = frameSize;
        int maxPixelChange = 255;
        int RGBChannelsNumber = 3;

        if (!digitalCount) {
            if (useGreyscale)   maxChange *= maxPixelChange;
            else                maxChange *= maxPixelChange * RGBChannelsNumber;
        }

        if (paintMotion) createMotionSquare(src, minX, minY, maxX, maxY, width, height);

        String text = "Substract Image PixelCount = " + sum + "\n";
        text +="Substract Image Percentaje: " +
                numberPresentation.format(sum * 100 / maxChange) + "%)\n";
        resultText += text;
    }

//    // CANNY EDGE
//    public void cannyEdgeProcess(byte[] data, Camera c)
//    {
//        try
//        {
//            YUV_NV21_TO_RGB(data, false);
//
//            CannyEdgeDetector detector = new CannyEdgeDetector();
////            detector.setLowThreshold(0.5f);
////            detector.setHighThreshold(1f);
//            detector.setSourceImg(
//                    Bitmap.createBitmap(
//                            argb,
//                            width,
//                            height,
//                            Bitmap.Config.ARGB_8888));
//            detector.process();
//
//            Bitmap edges = detector.getEdgesImg();
//            sobelViewB.setImageBitmap(edges);
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//    }

    public void sobelOperator(int[] data, int width, int height) {
        long initTime = System.currentTimeMillis();
        int x, y, pos = 1 * width + 1;
        int r, g, b;

        // I need a clone to get contiguous pixels through getSobelPixel()
        int[] sobelFrame = data.clone();

        for(y = 1; y < (height-1); y++)
        {
            for(x = 1; x < (width-1); x++)
            {
                r = getSobelPixel(sobelFrame, pos, width, 0x00ff0000, 16);
                if (!useGreyscale) {
                    g = getSobelPixel(sobelFrame, pos, width, 0x0000ff00, 8);
                    b = getSobelPixel(sobelFrame, pos, width, 0x000000ff, 0);
                } else
                    g = b = r;
                // Transparent Background
//                    mFrameSobel[pos] = 0x00000000 | (sobelFinal << 16) | (sobelFinal << 8) | sobelFinal;

                // Black Background
                data[pos] = 0xFF000000 | (r << 16) | (g << 8) | b;

                pos++;
            }
        }

        resultText += "Sobel Operator Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms\n";
    }

    private int getSobelPixel(int[] data, int pos, int w, int mask, int offset) {
        int posw1, poswn1, pos1, posn1, posnw1, posw, posnw, posnwn1;
        int sobelX, sobelY, sobelFinal;

        posw1 = (data[pos+w+1] & mask) >> offset;
        poswn1 = (data[pos+w-1]& mask) >> offset;
        pos1 = (data[pos+1]& mask) >> offset;
        posn1 = (data[pos-1]& mask) >> offset;
        posnw1 = (data[pos-w+1]& mask) >> offset;
        posnwn1 = (data[pos-w-1]& mask) >> offset;
        posw = (data[pos+w]& mask) >> offset;
        posnw = (data[pos-w]& mask) >> offset;

        sobelX = posw1 - poswn1
                + pos1 + pos1
                - posn1 - posn1
                + posnw1 - posnwn1;
        sobelY = posw1 + posw
                + posw + poswn1
                - posnw1 - posnw
                - posnw - posnwn1;

        sobelFinal = (sobelX + sobelY) / 2;

        // Threshold
        if (applySobelThreshold) {
            if(sobelFinal <  sobelThreshold)
                sobelFinal = 0;
            if(sobelFinal >= sobelThreshold)
                sobelFinal = 255;
        }

        return sobelFinal;
    }

    public void toGreyscale(int[] rgb, int width, int height) {
        long initTime = System.currentTimeMillis();
        int r, g ,b, mid;

        int a = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                r = (rgb[a] & 0x00ff0000) >> 16;
                g = (rgb[a] & 0x0000ff00) >> 8;
                b = (rgb[a] & 0x000000ff);

                mid = (r + g + b) / 3;

                // Black Background
                rgb[a] = 0xff000000 | (mid << 16) | (mid << 8) | mid;

                // Transparent Background
//                ret[a] = 0xFF000000 | (mid << 16) | (mid << 8) | mid;

                a++;
            }
        }

        resultText += "GreyScale Transform Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms\n";
    }

    public void createMotionSquare(int[] data, int minX, int minY, int maxX, int maxY, int width, int height) {
        for (int i = minX; i < maxX;i++) paintPixel(data, i, minY);
        for (int i = minX; i < maxX;i++) paintPixel(data, i, maxY);
        for (int i = minY; i < maxY;i++) paintPixel(data, minX, i);
        for (int i = minY; i < maxY;i++) paintPixel(data, maxX, i);
    }

    private void paintPixel(int[] data, int x, int y) {
        data[y*width + x] = 0xff000000 | (255 << 16) | (0 << 8) | 0;
    }

    public void refreshKeyFrameValues() {
        super.refreshKeyFrameValues();
        oldargb = argb.clone();
    }
}
