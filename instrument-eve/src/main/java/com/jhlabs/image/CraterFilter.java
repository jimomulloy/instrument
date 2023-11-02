/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.util.*;import java.awt.image.*;import com.jhlabs.math.*;public class CraterFilter extends WholeImageFilter implements java.io.Serializable {	static final long serialVersionUID = 6491871753122667752L;		private Colormap colormap = new LinearColormap();	private Random randomGenerator;	private long seed = 567;	private int numCraters = 25000;	private float depthPower = 1.0f;	private float depthBias = 0.707107f;	private float depthScaling = 1.0f;	private boolean spherical = false;	public CraterFilter() {		randomGenerator = new Random();	}	public void setNumCraters(int numCraters) {		this.numCraters = numCraters;	}	public int getNumCraters() {		return numCraters;	}	public void setDepthPower(float depthPower) {		this.depthPower = depthPower;	}	public float getDepthPower() {		return depthPower;	}	public void setDepthBias(float depthBias) {		this.depthBias = depthBias;	}	public float getDepthBias() {		return depthBias;	}	public void setDepthScaling(float depthScaling) {		this.depthScaling = depthScaling;	}	public float getDepthScaling() {		return depthScaling;	}	public void setColormap(Colormap colormap) {		this.colormap = colormap;	}		public Colormap getColormap() {		return colormap;	}		public void setSpherical(boolean spherical) {		this.spherical = spherical;	}	public boolean isSpherical() {		return spherical;	}	public void randomize() {		seed = new Date().getTime();	}		private float random(float low, float high) {		return low+(high-low) * randomGenerator.nextFloat();	}		public void imageComplete(int status) {		if (status == IMAGEERROR || status == IMAGEABORTED) {			consumer.imageComplete(status);			return;		}try{		int width = originalSpace.width;		int height = originalSpace.height;		int[] outPixels = new int[width * height];		randomGenerator.setSeed(seed);		int i, j, x, y;		i = 0;		for (y = 0; y < height; y++)			for (x = 0; x < width; x++)				outPixels[i++] = 32767;		for (i = 0; i < numCraters; i++) {			float g;			int cx = (int)random(0.0f, width-1);			int cy = (int)random(0.0f, height-1);			int gx, gy;			int amptot = 0, axelev;			int npatch = 0;			g = (float)Math.sqrt(1 / ((float)Math.PI * (1 - random(0, 0.9999f))));			if (g < 3) {				// A very small crater				for (y = Math.max(0, cy - 1); y <= Math.min(height - 1, cy + 1); y++) {					int sx = Math.max(0, cx - 1);					int a = y*width+sx;					for (x = sx; x <= Math.min(width - 1, cx + 1); x++) {						amptot += outPixels[a++];						npatch++;					}				}				axelev = amptot / npatch;				/* Perturb the mean elevation by a small random factor. */				x = (g >= 1) ? ((randomGenerator.nextInt() >> 8) & 3) - 1 : 0;				x *= depthScaling;				outPixels[width*cy+cx] = axelev + x;			} else {				gx = (int)Math.max(2, g / 3);				gy = (int)Math.max(2, g / 3);				for (y = Math.max(0, cy - gy); y <= Math.min(height - 1, cy + gy); y++) {					int sx = Math.max(0, cx - gx);					int a = y*width+sx;					for (x = sx; x <= Math.min(width - 1, cx + gx); x++) {						amptot += outPixels[a++];						npatch++;					}				}				axelev = amptot / npatch;				gy = (int)Math.max(2, g);				g = gy;				gx = (int)Math.max(2, g);				for (y = Math.max(0, cy - gy); y <= Math.min(height - 1, cy + gy); y++) {					int sx = Math.max(0, cx - gx);					int ax = y*width+sx;					float dy = (cy - y) / (float) gy;					float dysq = dy * dy;					for (x = sx; x <= Math.min(width - 1, cx + gx); x++) {						float dx = ((cx - x) / (float) gx),						   cd = (dx * dx) + dysq,						   cd2 = cd * 2.25f,						   tcz = depthBias - (float)Math.sqrt(Math.abs(1 - cd2)),						   cz = Math.max((cd2 > 1) ? 0.0f : -10f, tcz),						   roll, iroll;						int av;						cz *= (float)Math.pow(g, depthPower);						if (dy == 0 && dx == 0 && ((int) cz) == 0)						   cz = cz < 0 ? -1 : 1;						cz *= depthScaling;						float rollmin = 0.9f;						roll = (((1 / (1 - Math.min(rollmin, cd))) /							 (1 / (1 - rollmin))) - (1 - rollmin)) / rollmin;						iroll = 1 - roll;						av = (int)((axelev + cz) * iroll + (outPixels[ax] + cz) * roll);						av = Math.max(1000, Math.min(64000, av));						outPixels[ax++] = av;					}				}			}		}		float ImageGamma = 0.5f;		float dgamma = 1.0f;		int slopemin = -52, slopemax = 52;		i = Math.max((slopemax - slopemin) + 1, 1);		float[] slopemap = new float[i];		for (i = slopemin; i <= slopemax; i++) {			slopemap[i - slopemin] = i > 0 ?				(0.5f + 0.5f *				(float)Math.pow(Math.sin(ImageMath.HALF_PI * i / slopemax),					   dgamma * ImageGamma)) :				(0.5f - 127.0f *				(float)Math.pow(Math.sin(ImageMath.HALF_PI * i / slopemin),					   dgamma * ImageGamma));		}		if (colormap != null) {			int index = 0;			for (y = 0; y < height; y++) {				int last = outPixels[index];				for (x = 0; x < width; x++) {					int t = outPixels[index];					j = t-last;					j = Math.min(Math.max(slopemin, j), slopemax);					outPixels[index] = colormap.getColor(slopemap[j-slopemin]);					last = t;//					outPixels[index] = colormap.getColor(outPixels[index]/65535.0);					index++;				}			}		}		consumer.setPixels(0, 0, width, height, defaultRGBModel, outPixels, 0, width);		consumer.imageComplete(status);		inPixels = null;}catch(Exception e){e.printStackTrace();}	}	public String toString() {		return "Texture/Crater...";	}	}