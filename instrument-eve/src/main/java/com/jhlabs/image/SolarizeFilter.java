/* * Copyright (C) Jerry Huxtable 1998 */package com.jhlabs.image;import java.awt.image.*;/** * A filter which solarizes an image. */public class SolarizeFilter extends TransferFilter {	static final long serialVersionUID = 2284566165608004967L;		protected int transferFunction(int v) {		return v > 127 ? 2*(v-128) : 2*(127-v);	}	public String toString() {		return "Colors/Solarize";	}}