package com.aquariusinteractive.util

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode

import javax.imageio.ImageIO
import java.awt.*
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp

import static java.lang.Math.PI

/**
 * User: Michael Bishop
 * Date: 5/12/14
 * Time: 2:42 PM
 *
 * Groovy performance enhanced implementation of pHash based off of
 * https://code.google.com/p/ironchef-team21/source/browse/ironchef_team21/src/ImagePHash.java?r=75856e07bb89645d0e56820d6e79f8219a06bfb7
 *
 */

/**
 * TODO http://nekkidphpprogrammer.blogspot.fi/2014/02/yes-it-is-perfect-now.html
 * TODO http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html
 */
@CompileStatic
class ImagePHash2 {

    static {
        ImageIO.scanForPlugins()
        ImageIO.setUseCache(false)
    }

    private int size = 32;
    private int smallerSize = 8;

    public ImagePHash2() {
        initCoefficients();
    }

    public ImagePHash2(int size, int smallerSize) {
        this.size = size;
        this.smallerSize = smallerSize;

        initCoefficients();
    }

    /**
     * Hamming distance optimized for binary strings
     * @param s1
     * @param s2
     * @return distance
     */
    @TypeChecked(TypeCheckingMode.SKIP)
    public int distance(final String s1, final String s2) {

        if (s1.length() == s2.length()) {
            BitSet one = BitSet.valueOf(s1 as byte[])
            BitSet two = BitSet.valueOf(s2 as byte[])
            return one.xor(two).toByteArray().count(1) as int
        } else {
            throw new UnsupportedOperationException("String must be of same length")
        }
    }

    // Returns a 'binary string' (like. 001010111011100010) which is easy to do a hamming distance on.
    public String getHash(final InputStream is) throws Exception {
        BufferedImage img = ImageIO.read(is);

        /* 1. Reduce size.
         * Like Average Hash, pHash starts with a small image.
         * However, the image is larger than 8x8; 32x32 is a good size.
         * This is really done to simplify the DCT computation and not
         * because it is needed to reduce the high frequencies.
         */
        img = resize(img, size, size);

        /* 2. Reduce color.
         * The image is reduced to a grayscale just to further simplify
         * the number of computations.
         */
        img = grayscale(img);

        double[][] vals = new double[size][size];

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                vals[x][y] = getBlue(img, x, y);
            }
        }

        is?.close()

        /* 3. Compute the DCT.
         * The DCT separates the image into a collection of frequencies
         * and scalars. While JPEG uses an 8x8 DCT, this algorithm uses
         * a 32x32 DCT.
         */
//        long start = System.currentTimeMillis();
        double[][] dctVals = applyDCT(vals);
//        System.out.println("DCT: " + (System.currentTimeMillis() - start));

        /* 4. Reduce the DCT.
         * This is the magic step. While the DCT is 32x32, just keep the
         * top-left 8x8. Those represent the lowest frequencies in the
         * picture.
         */
        /* 5. Compute the average value.
         * Like the Average Hash, compute the mean DCT value (using only
         * the 8x8 DCT low-frequency values and excluding the first term
         * since the DC coefficient can be significantly different from
         * the other values and will throw off the average).
         */
        double total = 0;

        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                total += dctVals[x][y];
            }
        }
        total -= dctVals[0][0];


        double avg = total / (double) ((smallerSize * smallerSize) - 1);

        /* 6. Further reduce the DCT.
         * This is the magic step. Set the 64 hash bits to 0 or 1
         * depending on whether each of the 64 DCT values is above or
         * below the average value. The result doesn't tell us the
         * actual low frequencies; it just tells us the very-rough
         * relative scale of the frequencies to the mean. The result
         * will not vary as long as the overall structure of the image
         * remains the same; this can survive gamma and color histogram
         * adjustments without a problem.
         */
        StringBuilder hash = new StringBuilder(64)

        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                if (x != 0 && y != 0) {
                    hash.append((dctVals[x][y] > avg ? "1" : "0"))
                }
            }
        }

        return hash;
    }

    private static BufferedImage resize(final BufferedImage image, final int width,
                                        final int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();

        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    private static ColorConvertOp colorConvert = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    private static BufferedImage grayscale(final BufferedImage img) {
        colorConvert.filter(img, img);
        return img;
    }

    private static int getBlue(final BufferedImage img, final int x, final int y) {
        return (img.getRGB(x, y)) & 0xff;
    }

    // DCT function stolen from http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java

    private double[] c;

    private void initCoefficients() {
        c = new double[size];
        c[0] = 1 / Math.sqrt(2.0);

        for (int i = 1; i < size; i++) {
            c[i] = 1;
        }
    }


    private double[][] applyDCT(final double[][] f) {
        final int N = size;
        final double doubleN = 2.0 * N
        final double[][] F = new double[N][N];

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += Math.cos(((2 * i + 1) / (doubleN)) * u * PI) *
                               Math.cos(((2 * j + 1) / (doubleN)) * v * PI) * (f[i][j]);
                    }
                }
                sum *= ((c[u] * c[v]) / 4d);
                F[u][v] = sum;
            }
        }

        return F;
    }
}


