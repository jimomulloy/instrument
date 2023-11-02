/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.awt.image.*;import java.util.*;import com.jhlabs.math.*;public class CausticsFilter extends WholeImageFilter {	private float scale = 32;	private float angle = 0.0f;	public int brightness = 10;	public float amount = 1.0f;	public float turbulence = 1.0f;	public float dispersion = 0.0f;	public float time = 0.0f;	private int samples = 2;	private float s, c;	public CausticsFilter() {	}	public void setScale(float scale) {		this.scale = scale;	}	public float getScale() {		return scale;	}	public void setBrightness(int brightness) {		this.brightness = brightness;	}	public int getBrightness() {		return brightness;	}	public void setTurbulence(float turbulence) {		this.turbulence = turbulence;	}	public float getTurbulence() {		return turbulence;	}	public void setAmount(float amount) {		this.amount = amount;	}		public float getAmount() {		return amount;	}		public void setDispersion(float dispersion) {		this.dispersion = dispersion;	}		public float getDispersion() {		return dispersion;	}		public void setTime(float time) {		this.time = time;	}		public float getTime() {		return time;	}		public void setSamples(int samples) {		this.samples = samples;	}		public int getSamples() {		return samples;	}		public void imageComplete(int status) {		if (status == IMAGEERROR || status == IMAGEABORTED) {			consumer.imageComplete(status);			return;		}		Random random = new Random(0);		s = (float)Math.sin(0.1);		c = (float)Math.cos(0.1);		int srcWidth = originalSpace.width;		int srcHeight = originalSpace.height;		int outWidth = transformedSpace.width;		int outHeight = transformedSpace.height;		int index = 0;		int[] pixels = new int[outWidth * outHeight];		for (int y = 0; y < outHeight; y++) {			for (int x = 0; x < outWidth; x++) {				pixels[index++] = 0xfe799fff;			}		}				int v = brightness/samples;		if (v == 0)			v = 1;		float rs = 1.0f/scale;		float d = 0.95f;		index = 0;		for (int y = 0; y < outHeight; y++) {			for (int x = 0; x < outWidth; x++) {				for (int s = 0; s < samples; s++) {					float sx = x+random.nextFloat();					float sy = y+random.nextFloat();					float nx = sx*rs;					float ny = sy*rs;					float xDisplacement, yDisplacement;					float focus = 0.1f+amount;					xDisplacement = evaluate(nx-d, ny) - evaluate(nx+d, ny);					yDisplacement = evaluate(nx, ny+d) - evaluate(nx, ny-d);					if (dispersion > 0) {						for (int c = 0; c < 3; c++) {							float ca = (1+c*dispersion);							float srcX = sx + scale*focus * xDisplacement*ca;							float srcY = sy + scale*focus * yDisplacement*ca;							if (srcX < 0 || srcX >= outWidth-1 || srcY < 0 || srcY >= outHeight-1) {							} else {								int i = ((int)srcY)*outWidth+(int)srcX;								if (false) {									float fx = srcX-(int)srcX;									float fy = srcY-(int)srcY;									if (srcX >= 1) {										pixels[i-1] = add(pixels[i-1], brightness*fx*(1-fy), c);										if (srcY >= 1)											pixels[i-outWidth-1] = add(pixels[i-outWidth-1], brightness*fx*fy, c);									}									if (srcY >= 1)										pixels[i-outWidth] = add(pixels[i-outWidth], brightness*(1-fx)*fy, c);									pixels[i] = add(pixels[i], brightness*(1-fx)*(1-fy), c);								} else {									int rgb = pixels[i];									int r = (rgb >> 16) & 0xff;									int g = (rgb >> 8) & 0xff;									int b = rgb & 0xff;									if (c == 2)										r += v;									else if (c == 1)										g += v;									else										b += v;									if (r > 255)										r = 255;									if (g > 255)										g = 255;									if (b > 255)										b = 255;									pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;								}							}						}					} else {						float srcX = sx + scale*focus * xDisplacement;						float srcY = sy + scale*focus * yDisplacement;						if (srcX < 0 || srcX >= outWidth-1 || srcY < 0 || srcY >= outHeight-1) {						} else {							int i = ((int)srcY)*outWidth+(int)srcX;							if (false) {								float fx = srcX-(int)srcX;								float fy = srcY-(int)srcY;								if (srcX >= 1) {									pixels[i-1] = add(pixels[i-1], brightness*fx*(1-fy));									if (srcY >= 1)										pixels[i-outWidth-1] = add(pixels[i-outWidth-1], brightness*fx*fy);								}								if (srcY >= 1)									pixels[i-outWidth] = add(pixels[i-outWidth], brightness*(1-fx)*fy);								pixels[i] = add(pixels[i], brightness*(1-fx)*(1-fy));							} else {								int rgb = pixels[i];								int r = (rgb >> 16) & 0xff;								int g = (rgb >> 8) & 0xff;								int b = rgb & 0xff;								r += v;								g += v;								b += v;								if (r > 255)									r = 255;								if (g > 255)									g = 255;								if (b > 255)									b = 255;								pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;							}						}					}				}			}		}		consumer.setPixels(0, 0, outWidth, outHeight, defaultRGBModel, pixels, 0, outWidth);		consumer.imageComplete(status);		inPixels = null;		pixels = null;	}	private static int add(int rgb, float brightness) {		int r = (rgb >> 16) & 0xff;		int g = (rgb >> 8) & 0xff;		int b = rgb & 0xff;		r += brightness;		g += brightness;		b += brightness;		if (r > 255)			r = 255;		if (g > 255)			g = 255;		if (b > 255)			b = 255;		return 0xff000000 | (r << 16) | (g << 8) | b;	}		private static int add(int rgb, float brightness, int c) {		int r = (rgb >> 16) & 0xff;		int g = (rgb >> 8) & 0xff;		int b = rgb & 0xff;		if (c == 2)			r += brightness;		else if (c == 1)			g += brightness;		else			b += brightness;		if (r > 255)			r = 255;		if (g > 255)			g = 255;		if (b > 255)			b = 255;		return 0xff000000 | (r << 16) | (g << 8) | b;	}		public static float turbulence2(float x, float y, float time, float octaves) {		float value = 0.0f;		float remainder;		float lacunarity = 2.0f;		float f = 1.0f;		int i;				// to prevent "cascading" effects		x += 371;		y += 529;				for (i = 0; i < (int)octaves; i++) {			value += Noise.noise3(x, y, time) / f;			x *= lacunarity;			y *= lacunarity;			f *= 2;		}		remainder = octaves - (int)octaves;		if (remainder != 0)			value += remainder * Noise.noise3(x, y, time) / f;		return value;	}	protected float evaluate(float x, float y) {		float xt = s*x + c*time;		float tt = c*x - c*time;		float f = turbulence == 0.0 ? Noise.noise3(xt, y, tt) : turbulence2(xt, y, tt, turbulence);		return f;	}		public String toString() {		return "Texture/Caustics...";	}	}