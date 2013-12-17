package com.metodica.imageprocess.imageprocess;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

public class ImageProcessingYUV extends ImageProcessing<byte[]> {
    public ImageProcessingYUV() {}

    public ImageProcessingYUV(byte[] data, Camera.Parameters settings) {
        setSettings(settings);
        setNewFrame(data);
        imageResult = data;
    }

    public Bitmap getLastImage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(
                imageResult,
                camSettings.getPreviewFormat(),
                width,
                height,
                null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public void refreshArrayLongs() {

        // I DON'T KNOW IF THIS FIT ON EVERY PHONE BUT IT'S GOOD FOR N21
        // WHICH IS YUV FORMAT FOR MY PHONE

        newImage = new byte[((frameSize / 2) * 3)];
        imageResult = new byte[((frameSize / 2) * 3)];
        srcImage = newImage;
    }

    // For YUV Image
    public void process() {
        imageResult = newImage.clone();
        try
        {
            resultText = "";
            if (useGreyscale) toGreyscale(imageResult, width, height);
            if (sobelize) sobelOperator(imageResult, width, height);
            if (substraction) {
                byte[] oldresult = srcImage.clone();
                if (useGreyscale) toGreyscale(oldresult, width, height);
                if (sobelize) sobelOperator(oldresult, width, height);

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

    public void substract(byte [] src, byte[] dst, int width, int height) {
        int srcY, srcU, srcV, dstY, dstU, dstV, subsY, subsU, subsV, subs = 0;
        int minY = height, maxY = 0, minX = width, maxX = 0;
        double sum = 0;

        for (int i = 0, ij=0; i < height; i++) {
            for (int j = 0; j < width; j++,ij++) {
                if (!useGreyscale) {
                    srcY = dst[ij];
                    srcU = dst[frameSize + (i >> 1) * width + (j & ~1) + 0];
                    srcV = dst[frameSize + (i >> 1) * width + (j & ~1) + 1];

                    dstY = src[ij];
                    dstU = src[frameSize + (i >> 1) * width + (j & ~1) + 0];
                    dstV = src[frameSize + (i >> 1) * width + (j & ~1) + 1];

                    subsY = Math.abs(srcY - dstY);
                    subsU = Math.abs(srcU - dstU);
                    subsV = Math.abs(srcV - dstV);

                    subs = subsY + subsU + subsV;
                } else {
                    srcY = dst[ij];
                    dstY = src[ij];
                    // WORK WITH UNSIGNED BYTES ;)
                    subs = subsY = Math.abs((srcY & 0xFF) - (dstY  & 0xFF));
                    subsU = subsV = 127;
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

                    src[i*width+j] = (byte)subsY;                               // Y
                } else
                    src[i*width+j] = 0;                                         // Y

                src[frameSize + (i >> 1) * width + (j & ~1) + 0] = (byte)subsU;	// U
                src[frameSize + (i >> 1) * width + (j & ~1) + 1] = (byte)subsV;	// V
            }
        }

        // CALCULATE PERCENTS AND RESULTS INFO
        int maxChange = frameSize;
        int maxPixelChange = 127;
        int YUVChannelsNumber = 3;

        if (!digitalCount) {
            if (useGreyscale)   maxChange *= maxPixelChange;
            else                maxChange *= maxPixelChange * YUVChannelsNumber;
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

    public void sobelOperator(byte[] data, int width, int height) {
        long initTime = System.currentTimeMillis();
        int i, j, pos = 1 * width + 1;
        byte y, u, v;

        byte[] sobelFrame = data.clone();

        for(i = 1; i < (height-2); i++)
        {
            for(j = 1; j < (width-2); j++)
            {
                y = sobelFrame[pos];
                if (!useGreyscale) {
                    u = getSobelPixel(sobelFrame, i, j, pos, width, "U");
                    v = getSobelPixel(sobelFrame, i, j, pos, width, "V");
                }
                else u = v = y = getSobelPixel(sobelFrame, i, j, pos, width, "Y");

                // PAINT
                if (useGreyscale) data[pos] = y;
                else paintPixel(data, j - 1, i - 1, width, height, frameSize, y, u, v);
                pos++;
            }
        }

        resultText += "Sobel Operator Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms\n";
    }

    private byte getSobelPixel(byte[] data, int i, int j, int pos, int width, String requestedData) {
        int posw1, poswn1, pos1, posn1, posnw1, posw, posnw, posnwn1;
        int sobelX, sobelY, sobelFinal;
        int UVoffset = 0;
        int w = width;
        int positiveValue = 0xFF;
        int negativeValue = 0;

        //  "& 0xFF" CONVERT BYTE TO UNSIGNED VALUE
        if (requestedData.equalsIgnoreCase("Y")) {
            posw1 = data[pos+w+1] & 0xFF;
            poswn1 = data[pos+w-1] & 0xFF;
            pos1 = data[pos+1] & 0xFF;
            posn1 = data[pos-1] & 0xFF;
            posnw1 = data[pos-w+1] & 0xFF;
            posnwn1 = data[pos-w-1] & 0xFF;
            posw = data[pos+w] & 0xFF;
            posnw = data[pos-w] & 0xFF;
        } else {
            if (requestedData.equalsIgnoreCase("V")) UVoffset = 1;
            posw1 = (0xff & (data[frameSize + ((i+1) >> 1) * width + ((j+1) & ~1) + UVoffset]));
            poswn1 = (0xff & (data[frameSize + ((i+1) >> 1) * width + ((j-1) & ~1) + UVoffset]));
            pos1 = (0xff & (data[frameSize + (i >> 1) * width + ((j+1) & ~1) + UVoffset]));
            posn1 = (0xff & (data[frameSize + (i >> 1) * width + ((j-1) & ~1) - ((j & ~1)*2) + UVoffset]));
            posnw1 = (0xff & (data[frameSize + ((i-1) >> 1) * width + ((j+1) & ~1) + UVoffset]));
            posnwn1 = (0xff & (data[frameSize + ((i-1) >> 1) * width + ((j-1) & ~1) + UVoffset]));
            posw = (0xff & (data[frameSize + ((i+1) >> 1) * width + (j & ~1) + UVoffset]));
            posnw = (0xff & (data[frameSize + ((i-1) >> 1) * width + (j & ~1) + UVoffset]));

            positiveValue = (0xff & (data[frameSize + (i >> 1) * width + (j & ~1) + UVoffset]));
            positiveValue = 0;
            negativeValue = 0xFF;
        }

        sobelX = posw1 - poswn1
                + pos1 + pos1
                - posn1 - posn1
                + posnw1 - posnwn1;
        sobelY = posw1 + posw
                + posw + poswn1
                - posnw1 - posnw
                - posnw - posnwn1;

        sobelFinal = (sobelX + sobelY) / 2;
        if (sobelFinal > 255) sobelFinal = 255;

        if (applySobelThreshold) {
            if(sobelFinal <  (sobelThreshold))
                sobelFinal = negativeValue;
            if(sobelFinal >= (sobelThreshold))
                sobelFinal = positiveValue;
        }

        return (byte)(sobelFinal & 0xFF);
    }

    public void toGreyscale(byte[] rgb, int width, int height) {
        long initTime = System.currentTimeMillis();
        int y, u ,v, mid;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
//                rgb[i*width+j] = (byte)0xFF;                                     // Y
                rgb[frameSize + (i >> 1) * width + (j & ~1) + 0] = 127;	// U
                rgb[frameSize + (i >> 1) * width + (j & ~1) + 1] = 127;	// V
            }
        }

        resultText += "GreyScale Transform Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms\n";
    }

    public void createMotionSquare(byte[] data, int minX, int minY, int maxX, int maxY,
                                     int width, int height) {
        int frameSize = width * height;
        for (int i = minX; i < maxX; i++)
            paintPixel(data, i, minY, width, height, frameSize, (byte)127, (byte)-75, (byte)127);
        for (int i = minX; i < maxX; i++)
            paintPixel(data, i, maxY, width, height, frameSize, (byte)127, (byte)-75, (byte)127);
        for (int i = minY; i < maxY; i++)
            paintPixel(data, minX, i, width, height, frameSize, (byte)127, (byte)-75, (byte)127);
        for (int i = minY; i < maxY; i++)
            paintPixel(data, maxX, i, width, height, frameSize, (byte)127, (byte)-75, (byte)127);
    }

    private void paintPixel(
            byte[] data, int x, int y, int width, int height, int frameSize,
            byte colorY, byte colorU, byte colorV) {
        data[y*width + x] = colorY;
        data[frameSize + (y >> 1) * width + (x & ~1) + 0] = colorU;	//U
        data[frameSize + (y >> 1) * width + (x & ~1) + 1] = colorV;	//V
    }
}
