package com.metodica.imageprocess.SupportLibrary;

/**
 * Created by Jacob on 12/16/13.
 */
public class GeneralImageProcess {
    public static int[] NV21ToRGB(byte[] yuv, boolean greyscale, int width, int height, int frameSize) {
        int[] ret = new int[frameSize];

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                // TRASNFORM TO GREYSCALE IN THE SAME PROCESS
                // NOT USED CAUSE APP FLOW DON'T ALLOW BUT VERY USEFUL
                if (greyscale) ret[a] = 0x00000000 | ((r + g + b) / 3);
                else
                    // Black Background
                    ret[a] = 0xff000000 | (r << 16) | (g << 8) | b;

                    // Transparent background
//                    argb[a] = 0x00000000 | (imageR[a] << 16) | (imageG[a] << 8) | imageB[a];

                a++;
            }
        }
        return ret;
    }


    public static int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int[] ret = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else
                    if (r > 262143) r = 262143;

                if (g < 0) g = 0;
                else
                    if (g > 262143) g = 262143;

                if (b < 0) b = 0;
                else
                    if (b > 262143) b = 262143;

                ret[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        return ret;
    }


    /**
     * Converts YUV420 NV21 to ARGB8888 FROM WIKIPEDIA
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a ARGB8888 pixels int array. Where each int is a pixels ARGB.
     */

    // I DON'T USE IT CAUSE IT MAKES SOME STRANGE SHADOW ON THE IMAGE
    public static int[] convertYUV420_NV21toARGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i along Y and the final pixels
        // k along pixels U and V
        for(int i=0, k=0; i < size; i+=2, k+=1) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            v = data[offset+k  ]&0xff;
            u = data[offset+k+1]&0xff;
            v = v-128;
            u = u-128;

            pixels[i  ] = convertYUVtoARGB(y1, u, v);
            pixels[i+1] = convertYUVtoARGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoARGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoARGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoARGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f*u);
        g = y - (int)(0.344f*v + 0.714f*u);
        b = y + (int)(1.772f*v);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (r<<16) | (g<<8) | b;
    }
}
