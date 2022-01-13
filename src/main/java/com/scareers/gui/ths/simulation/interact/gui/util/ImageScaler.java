package com.scareers.gui.ths.simulation.interact.gui.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-04:25:58
 */
public class ImageScaler {
    /**
     * 指定长和宽对图片进行缩放
     *
     * @param img
     * @param width
     * @param height
     * @return
     * @throws IOException
     */
    public static Image zoomBySize(Image img, int width, int height) {
        Image imgScaled = img.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        return imgScaled; // BufferedImage
    }

    public static Image zoomByScale(Image img, double scale) {
        int width = (int) (img.getWidth(null) * scale);
        int height = (int) (img.getHeight(null) * scale);
        return zoomBySize(img, width, height);
    }
}