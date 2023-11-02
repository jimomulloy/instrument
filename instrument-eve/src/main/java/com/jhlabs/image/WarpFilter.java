/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.awt.*;import java.awt.event.*;import java.awt.image.*;/** * A filter for warping images using the gridwarp algorithm. * You need to supply two warp grids, one for the source image and * one for the destination image. The image will be warped so that * a point in the source grid moves to its counterpart in the destination * grid. */public class WarpFilter extends WholeImageFilter {	static final long serialVersionUID = 1299148944426051330L;	private WarpGrid sourceGrid;	private WarpGrid destGrid;	private int frames = 1;	/**	 * Create a WarpFilter.	 */	public WarpFilter() {	}		/**	 * Create a WarpFilter with two warp grids.	 * @param sourceGrid the source grid	 * @param destGrid the destination grid	 */	public WarpFilter(WarpGrid sourceGrid, WarpGrid destGrid) {		this.sourceGrid = sourceGrid;		this.destGrid = destGrid;			}		/**	 * Set the source warp grid.	 * @param sourceGrid the source grid	 */	public void setSourceGrid(WarpGrid sourceGrid) {		this.sourceGrid = sourceGrid;	}	/**	 * Get the source warp grid.	 * @return the source grid	 */	public WarpGrid getSourceGrid() {		return sourceGrid;	}	/**	 * Set the destination warp grid.	 * @param destGrid the destination grid	 */	public void setDestGrid(WarpGrid destGrid) {		this.destGrid = destGrid;	}	/**	 * Get the destination warp grid.	 * @return the destination grid	 */	public WarpGrid getDestGrid() {		return destGrid;	}	public void setFrames(int frames) {		this.frames = frames;	}	public int getFrames() {		return frames;	}	protected void transformSpace(Rectangle r) {		r.width *= frames;	}	public void imageComplete(int status) {		if (status == IMAGEERROR || status == IMAGEABORTED) {			consumer.imageComplete(status);			return;		}		int width = originalSpace.width;		int height = originalSpace.height;		int[] outPixels = new int[width * height];				if (frames <= 1) {			sourceGrid.warp(inPixels, width, height, sourceGrid, destGrid, outPixels);			consumer.setPixels(0, 0, width, height, defaultRGBModel, outPixels, 0, width);		} else {			WarpGrid newGrid = new WarpGrid(sourceGrid.rows, sourceGrid.cols, width, height);			for (int i = 0; i < frames; i++) {				float t = (float)i/(frames-1);				sourceGrid.lerp(t, destGrid, newGrid);				sourceGrid.warp(inPixels, width, height, sourceGrid, newGrid, outPixels);				consumer.setPixels(i*width, 0, width, height, defaultRGBModel, outPixels, 0, width);			}		}		consumer.imageComplete(status);		inPixels = null;	}	public int[] getPixels(Image image, int width, int height) {		int[] pixels = new int[width * height];				pixels = new int[width * height];		PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);		try {			pg.grabPixels();		} catch (InterruptedException e) {			System.err.println("interrupted waiting for pixels!");			return null;		}		if ((pg.status() & ImageObserver.ABORT) != 0) {			System.err.println("image fetch aborted or errored");			return null;		}		return pixels;	}	public void morph(int[] srcPixels, int[] destPixels, int[] outPixels, WarpGrid srcGrid, WarpGrid destGrid, int width, int height, float t) {		WarpGrid newGrid = new WarpGrid(srcGrid.rows, srcGrid.cols, width, height);		srcGrid.lerp(t, destGrid, newGrid);		srcGrid.warp(srcPixels, width, height, srcGrid, newGrid, outPixels);		int[] destPixels2 = new int[width * height];		destGrid.warp(destPixels, width, height, destGrid, newGrid, destPixels2);		crossDissolve(outPixels, destPixels2, width, height, t);	}	public void crossDissolve(int[] pixels1, int[] pixels2, int width, int height, float t) {		int index = 0;		for (int y = 0; y < height; y++) {			for (int x = 0; x < width; x++) {				pixels1[index] = ImageMath.mixColors(t, pixels1[index], pixels2[index]);				index++;			}		}	}		public String toString() {		return "Distort/Mesh Warp...";	}}