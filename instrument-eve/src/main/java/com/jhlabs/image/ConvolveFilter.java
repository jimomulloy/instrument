/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.awt.*;import java.awt.image.*;/** * A filter which applies a convolution kernel to an image. * @author Jerry Huxtable */public class ConvolveFilter extends WholeImageFilter {	static final long serialVersionUID = 2239251672685254626L;		protected Kernel kernel = null;	public boolean alpha = true;//FIXME	/**	 * Construct a filter with a null kernel. This is only useful if you're going to change the kernel later on.	 */	public ConvolveFilter() {		this(new float[9]);	}	/**	 * Construct a filter with the given 3x3 kernel.	 * @param matrix an array of 9 floats containing the kernel	 */	public ConvolveFilter(float[] matrix) {		this(new Kernel(3, 3, matrix));	}		/**	 * Construct a filter with the given kernel.	 * @param rows	the number of rows in the kernel	 * @param cols	the number of columns in the kernel	 * @param matrix	an array of rows*cols floats containing the kernel	 */	public ConvolveFilter(int rows, int cols, float[] matrix) {		this(new Kernel(rows, cols, matrix));	}		/**	 * Construct a filter with the given 3x3 kernel.	 * @param matrix an array of 9 floats containing the kernel	 */	public ConvolveFilter(Kernel kernel) {		this.kernel = kernel;		}	public void setKernel(Kernel kernel) {		this.kernel = kernel;	}	public Kernel getKernel() {		return kernel;	}	public void imageComplete(int status) {		if (status == IMAGEERROR || status == IMAGEABORTED) {			consumer.imageComplete(status);			return;		}		int width = originalSpace.width;		int height = originalSpace.height;		int[] outPixels = new int[width * height];		convolve(kernel, inPixels, outPixels, width, height, alpha);		consumer.setPixels(0, 0, width, height, defaultRGBModel, outPixels, 0, width);		consumer.imageComplete(status);		inPixels = null;	}	public static void convolve(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height) {		convolve(kernel, inPixels, outPixels, width, height, true);	}		public static void convolve(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha) {		if (kernel.rows == 1)			convolveH(kernel, inPixels, outPixels, width, height, alpha);		else if (kernel.cols == 1)			convolveV(kernel, inPixels, outPixels, width, height, alpha);		else			convolveHV(kernel, inPixels, outPixels, width, height, alpha);	}		/**	 * Convolve with a 2D kernel	 */	public static void convolveHV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha) {		int index = 0;		float[] matrix = kernel.matrix;		int rows = kernel.rows;		int cols = kernel.cols;		int rows2 = rows/2;		int cols2 = cols/2;		for (int y = 0; y < height; y++) {			for (int x = 0; x < width; x++) {				float r = 0, g = 0, b = 0, a = 0;				for (int row = -rows2; row <= rows2; row++) {					int iy = y+row;					int ioffset;					if (0 <= iy && iy < height)						ioffset = iy*width;					else						ioffset = y*width;					int moffset = cols*(row+rows2)+cols2;					for (int col = -cols2; col <= cols2; col++) {						float f = matrix[moffset+col];						if (f != 0) {							int ix = x+col;							if (!(0 <= ix && ix < width))								ix = x;							int rgb = inPixels[ioffset+ix];							a += f * ((rgb >> 24) & 0xff);							r += f * ((rgb >> 16) & 0xff);							g += f * ((rgb >> 8) & 0xff);							b += f * (rgb & 0xff);						}					}				}				int ia = alpha ? PixelUtils.clamp((int)(a+0.5)) : 0xff;				int ir = PixelUtils.clamp((int)(r+0.5));				int ig = PixelUtils.clamp((int)(g+0.5));				int ib = PixelUtils.clamp((int)(b+0.5));				outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;			}		}	}	/**	 * Convolve with a kernel consisting of one row	 */	public static void convolveH(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha) {		int index = 0;		float[] matrix = kernel.matrix;		int cols = kernel.cols;		int cols2 = cols/2;		for (int y = 0; y < height; y++) {			int ioffset = y*width;			for (int x = 0; x < width; x++) {				float r = 0, g = 0, b = 0, a = 0;				int moffset = cols2;				for (int col = -cols2; col <= cols2; col++) {					float f = matrix[moffset+col];					if (f != 0) {						int ix = x+col;						if (!(0 <= ix && ix < width))							ix = x;						int rgb = inPixels[ioffset+ix];						a += f * ((rgb >> 24) & 0xff);						r += f * ((rgb >> 16) & 0xff);						g += f * ((rgb >> 8) & 0xff);						b += f * (rgb & 0xff);					}				}				int ia = alpha ? PixelUtils.clamp((int)(a+0.5)) : 0xff;				int ir = PixelUtils.clamp((int)(r+0.5));				int ig = PixelUtils.clamp((int)(g+0.5));				int ib = PixelUtils.clamp((int)(b+0.5));				outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;			}		}	}	/**	 * Convolve with a kernel consisting of one column	 */	public static void convolveV(Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha) {		int index = 0;		float[] matrix = kernel.matrix;		int rows = kernel.rows;		int rows2 = rows/2;		for (int y = 0; y < height; y++) {			for (int x = 0; x < width; x++) {				float r = 0, g = 0, b = 0, a = 0;				for (int row = -rows2; row <= rows2; row++) {					int iy = y+row;					int ioffset;					if (0 <= iy && iy < height)						ioffset = iy*width;					else						ioffset = y*width;					float f = matrix[row+rows2];					if (f != 0) {						int rgb = inPixels[ioffset+x];						a += f * ((rgb >> 24) & 0xff);						r += f * ((rgb >> 16) & 0xff);						g += f * ((rgb >> 8) & 0xff);						b += f * (rgb & 0xff);					}				}				int ia = alpha ? PixelUtils.clamp((int)(a+0.5)) : 0xff;				int ir = PixelUtils.clamp((int)(r+0.5));				int ig = PixelUtils.clamp((int)(g+0.5));				int ib = PixelUtils.clamp((int)(b+0.5));				outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;			}		}	}	public String toString() {		return "Blur/Convolve...";	}}