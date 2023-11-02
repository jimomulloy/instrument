/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.awt.*;import java.awt.image.*;/** * Given a binary image, this filter converts it to its outline, replacing all interior pixels with the 'new' color. */public class OutlineFilter extends BinaryFilter {	public OutlineFilter() {		newColor = 0xffffffff;	}	public void imageComplete(int status) {		if (status == IMAGEERROR || status == IMAGEABORTED) {			consumer.imageComplete(status);			return;		}		int width = originalSpace.width;		int height = originalSpace.height;		int index = 0;		int[] outPixels = new int[width * height];		for (int y = 0; y < height; y++) {			for (int x = 0; x < width; x++) {				int pixel = inPixels[y*width+x];				if (blackFunction.isBlack(pixel)) {					int neighbours = 0;					for (int dy = -1; dy <= 1; dy++) {						int iy = y+dy;						int ioffset;						if (0 <= iy && iy < height) {							ioffset = iy*width;							for (int dx = -1; dx <= 1; dx++) {								int ix = x+dx;								if (!(dy == 0 && dx == 0) && 0 <= ix && ix < width) {									int rgb = inPixels[ioffset+ix];									if (blackFunction.isBlack(rgb))										neighbours++;								} else									neighbours++;							}						}					}										if (neighbours == 9)						pixel = newColor;				}				outPixels[index++] = pixel;			}		}		consumer.setPixels(0, 0, width, height, defaultRGBModel, outPixels, 0, width);		consumer.imageComplete(status);		inPixels = null;	}	public String toString() {		return "Binary/Outline...";	}}