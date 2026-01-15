package comul01.eve;

import java.awt.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.media.util.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.util.*;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.RenderableOp;
import javax.media.jai.*;

/*
* Class EFrame
*/
public class EFrame {

	public static final int RED = 2;
	public static final int GREEN = 1;
	public static final int BLUE = 0;

	int[] pixelData = null;
	byte[] byteData = null;
	int[] pixelAge = null;
	private int currentPixel;
	private BufferedImage bi, bimg;
	private Image image;
	private BufferToImage btoi;
	private ImageToBuffer itob;
	private Buffer buffer;
	private Format format;
	private VideoFormat videoFormat;
	private RGBFormat rgbFormat;
	private int bufferLength;
	private int length;
	private int offset;
	private long seqnum;
	private Dimension size;
	private int pixelStride;
	private int lineStride;
	private int age;
	private int maxDataLength;
	private float frameRate;
	private int flipped;
	private int endian;
	private int height;
	private int width;
	private PlanarImage planarImage=null;

	static public Color getColor(int band) {
		if (band == RED ) return Color.red;
		if (band == GREEN) return Color.green;
		if (band == BLUE) return Color.blue;
		return Color.white;
	}
	public EFrame() {
		super();
	}

	public EFrame(Buffer buffer) {
		super();
		this.buffer = new Buffer();
		this.buffer.copy(buffer);
		format = buffer.getFormat();
		if (format instanceof RGBFormat) {
			rgbFormat = (RGBFormat)format;
			size = rgbFormat.getSize();
			pixelStride = rgbFormat.getPixelStride();
			lineStride = rgbFormat.getLineStride();
			maxDataLength = rgbFormat.getMaxDataLength();
			frameRate = rgbFormat.getFrameRate();
			flipped = rgbFormat.getFlipped();
			endian = rgbFormat.getEndian();
			if (size != null) {
				width = size.width;
				height = size.height;
				maxDataLength = size.width * size.height * pixelStride;
				if (lineStride < size.width * pixelStride)
					lineStride = size.width * pixelStride;
				if (flipped != Format.FALSE)
					flipped = Format.FALSE;
			}
		}
		videoFormat = (VideoFormat)buffer.getFormat();
		bufferLength = buffer.getLength();
		length = buffer.getLength()/pixelStride;
		offset = buffer.getOffset();
		seqnum = buffer.getSequenceNumber();
		byte[] getData = (byte[])buffer.getData();
		byteData = new byte[buffer.getLength()];
		for (int i = offset; i < buffer.getLength() ;i++) {
			byteData[i] = getData[i];
		}

		resetPixelAge();

	}

	public void attachPlanarImage(PlanarImage image) {

		planarImage = image;
	}

	public PlanarImage dettachPlanarImage() {
		PlanarImage image = planarImage;
		planarImage = null;
		return image;
	}

	public boolean isPlanarImage() {

		return (planarImage != null);
	}

	public Buffer getBuffer() {
		refreshBuffer();
		return buffer;
	}

	public void setFormat(Format format) {
		this.format = (Format)format.clone();
		if (format instanceof RGBFormat) {
			rgbFormat = (RGBFormat)format;
			size = rgbFormat.getSize();
			pixelStride = rgbFormat.getPixelStride();
			lineStride = rgbFormat.getLineStride();
			maxDataLength = rgbFormat.getMaxDataLength();
			frameRate = rgbFormat.getFrameRate();
			flipped = rgbFormat.getFlipped();
			endian = rgbFormat.getEndian();
			if (size != null) {
				width = size.width;
				height = size.height;
				maxDataLength = size.width * size.height * pixelStride;
				if (lineStride < size.width * pixelStride)
					lineStride = size.width * pixelStride;
				if (flipped != Format.FALSE)
					flipped = Format.FALSE;
			}
		}
	}

	public int getBufferlength() {
		return bufferLength;
	}


	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	public int getPixelStride() {
		return pixelStride;
	}
	public int getLineStride() {
		return lineStride;
	}


	public void resetPixelAge() {
		pixelAge = new int[getLength()];
	}

	public int getPixelAge(int p) {
		return pixelAge[p];
	}

	public void setPixelAge(int p, int age) {
		pixelAge[p] = age;
	}

	public String toString() {
		return ("length: "+length+" offset: "+offset+" seqnum: "+seqnum+" pixelStride: "+pixelStride+" lineStride "+lineStride) ;
	}

	public Format getFormat() {
		return format;
	}

	public VideoFormat getVideoFormat() {
		return videoFormat;
	}


	public int getLength() {
		return length;
	}

	public int getOffset() {
		return offset;
	}

	public long getSeqnum() {
		return seqnum;
	}

	public byte[] getData() {
		return byteData;
	}

	public byte[] getDataReverse() {
		int len = byteData.length;
		byte[] reverse = new byte[len];
		for (int y=0; y<height; y++) {
			for (int x=0; x<width*pixelStride; x+=pixelStride) {
				for (int p=0; p<pixelStride; p++) {
					reverse[(x+(pixelStride-1-p))+((height-1-y)*lineStride)] = byteData[(x+p)+(y*lineStride)];
				}
			}
		}
		return reverse;
	}


	public int[] getPixelData() {
		return pixelData;
	}

	public int getBufferLength() {
		return bufferLength;
	}

	public void refreshBuffer(){
		buffer.setData(getData());
	}


	public BufferedImage getBufferedImage() {
		buffer.setData(getData());
		BufferToImage btoi = new BufferToImage(videoFormat);
		Image image = btoi.createImage(buffer);
		if (image == null) {
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
		bi.createGraphics().drawImage(image ,0 ,0 , null);
		return bi;
	}

	public Graphics2D getGraphics() {
		buffer.setData(getData());
		BufferToImage btoi = new BufferToImage(videoFormat);
		Image image = btoi.createImage(buffer);
		if (image == null) {
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			return bi.createGraphics();
		}
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(image ,0 ,0 , null);
		return g2d;
	}

	public Image getImage() {
		buffer.setData(getData());
		if (videoFormat == null) return null;
		BufferToImage btoi = new BufferToImage(videoFormat);
		Image image = btoi.createImage(buffer);
		return image;
	}

	public RenderedOp getRenderedOp() {
		Image image = getImage();
		if (image == null) {
			// Fallback: create image directly from byte data if BufferToImage fails
			image = createImageFromBytes();
		}
		if (image == null) return null;
		RenderedOp rop = JAI.create("AWTImage", image);
		return rop;
	}

	/**
	 * Creates an Image directly from byte data, bypassing BufferToImage.
	 * This is a fallback for when FMJ's BufferToImage doesn't work with certain formats.
	 * Handles vertical flipping since video data is often stored bottom-up.
	 */
	private Image createImageFromBytes() {
		if (byteData == null || width <= 0 || height <= 0) return null;

		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] pixels = new int[width * height];

		// Read pixels row by row, flipping vertically (bottom-up to top-down)
		for (int y = 0; y < height; y++) {
			int srcY = height - 1 - y;  // Read from bottom row first
			for (int x = 0; x < width; x++) {
				int srcIdx = (srcY * width + x) * 3;
				if (srcIdx + 2 < byteData.length) {
					int b = byteData[srcIdx] & 0xFF;
					int g = byteData[srcIdx + 1] & 0xFF;
					int r = byteData[srcIdx + 2] & 0xFF;
					pixels[y * width + x] = (r << 16) | (g << 8) | b;
				}
			}
		}

		bi.setRGB(0, 0, width, height, pixels, 0, width);
		return bi;
	}

	public RenderedImage getRenderedImage() {
		Image image = getImage();
		if (image == null) {
			image = createImageFromBytes();
		}
		if (image == null) return null;
		RenderedImage rimg = (RenderedImage)JAI.create("AWTImage", image);
		return rimg;
	}


	public PlanarImage getPlanarImage() {
		Image image = getImage();
		if (image == null) {
			image = createImageFromBytes();
		}
		if (image == null) return null;
		PlanarImage rop = (PlanarImage)JAI.create("AWTImage", image);
		return rop;
	}

	public void setRasterPixels(int[] pixels) {
		for (int i = 0; i < pixels.length && i < bufferLength; i++ ) {
			byteData[bufferLength-1-i] = (byte)pixels[i];
		}
		buffer.setData(getData());
	}

	public void setRaster2(Raster raster) {
		try {
			for (int x = 0; x < getWidth(); x++ ) {
				for (int y = 0; y < getHeight() ; y++) {
					for( int b = 0; b < 3; b++ ) {
						setPixel(b, x, getHeight()-1-y, raster.getSampleDouble(x, y, 2-b));
					}
				}
			}
		} catch (Exception e) {
		}
		buffer.setData(getData());
	}

	// was like setRaster2 ??
	// screws up jaiscale ??
	public void setRaster(Raster raster) {
		try {
			for (int x = 0; x < getWidth(); x++ ) {
				for (int y = 0; y < getHeight() ; y++) {
					for( int b = 0; b < 3; b++ ) {
						if (x < raster.getWidth() && y < raster.getHeight()) {
							setPixel(b, x, getHeight()-1-y, raster.getSampleDouble(x, y, 2-b));
						} else {
							setPixel(b, x, getHeight()-1-y, 0);
						}
					}
				}
			}
		} catch (Exception e) {
		}
		buffer.setData(getData());
	}


	public void setRaster1(Raster raster) {
		try {
			for (int x = 0; x < getWidth(); x++ ) {
				for (int y = 0; y < getHeight() ; y++) {
					for( int b = 0; b < 3; b++ ) {
						if (x < raster.getWidth() && y < raster.getHeight()) {
							setPixel(b, x, getHeight()-1-y, raster.getSampleDouble(x, y, 2-b));
						} else {
							setPixel(b, x, getHeight()-1-y, 0);
						}
					}
				}
			}
		} catch (Exception e) {
		}
		buffer.setData(getData());
	}

	public void clearBuffer() {
		for (int i = 0; i < byteData.length; i++ ) {
			byteData[i] = 0;
		}
		buffer.setData(getData());
	}

	public void setImage(Image image) {
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
		bi.createGraphics().drawImage(image ,0 ,0 , null);
		Raster raster = bi.getRaster();
		setRaster(raster);
	}

	public void setImage1(Image image) {
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
		bi.createGraphics().drawImage(image ,0 ,0 , null);
		Raster raster = bi.getRaster();
		setRaster1(raster);
	}

	public void setBufferedImage(BufferedImage bimg) {
		int[] intarray = null;
		int iw = bimg.getWidth();
		int ih = bimg.getHeight();
		//int[] pixels = bimg.getRaster().getPixels(0, 0, iw, ih, intarray);
		Raster raster = bimg.getRaster();
		setRaster(raster);
	}

	public void setRenderedOp(RenderedOp rop) {
		Raster raster = rop.getData();
		setRaster(raster);
	}

	public void setRenderedImage(RenderedImage rop) {
		Raster raster = rop.getData();
		setRaster(raster);
	}

	public void setPlanarImage(PlanarImage rop) {
		Raster raster = rop.getData();
		setRaster(raster);
	}


	public void setRenderedOpPixels(RenderedOp rop) {
		int[] intarray = null;
		int iw = rop.getWidth();
		int ih = rop.getHeight();
		int[] pixels = rop.getData().getPixels(0, 0, iw, ih, intarray);
		setRasterPixels(pixels);
	}

	public void composite(Image broll, Image mask, double[] alpha, int mode, int xoffset, int yoffset) {
		composite(broll,mask,alpha,mode,xoffset,yoffset,false,false);
	}

	public void composite(Image broll, Image mask, double[] alpha, int mode, int xoffset, int yoffset, boolean option1, boolean option2) {
		int wb=0, hb=0, wm=0, hm=0;
		BufferedImage bib=null, bim=null;
		Raster rb=null, rm=null;
		if (broll != null) {
			wb = broll.getWidth(null);
			hb = broll.getHeight(null);
			bib = new BufferedImage(wb, hb, BufferedImage.TYPE_INT_RGB);
			bib.createGraphics().drawImage(broll ,0 ,0 , null);
			rb = bib.getRaster();
			System.out.println("EFrame.composite: broll=" + wb + "x" + hb +
				", frame=" + getWidth() + "x" + getHeight() +
				", offset=" + xoffset + "," + yoffset +
				", alpha=[" + alpha[0] + "," + alpha[1] + "," + alpha[2] + "]" +
				", mode=" + mode + ", opt1=" + option1 + ", opt2=" + option2);
		} else {
			System.out.println("EFrame.composite: broll is NULL");
		}

		if (mask != null) {
			wm = mask.getWidth(null);
			hm = mask.getHeight(null);
			bim = new BufferedImage(wm, hm, BufferedImage.TYPE_INT_RGB);
			bim.createGraphics().drawImage(mask ,0 ,0 , null);
			rm = bim.getRaster();

		}

		double pixela=0.0, pixelb=0.0, pixelm=0.0, pixel1=0.0, pixel2=0.0, pixels=0.0, pmax=0.0, trans=0.0;
		try {

			if (option1) {

				for (int x = 0; x < getWidth(); x++ ) {
					for (int y = 0; y < getHeight() ; y++) {
						pixels = getPixelDouble(0,x,y)+getPixelDouble(1,x,y)+getPixelDouble(2,x,y);
						if (pixels >pmax) pmax=pixels;
					}
				}
			} else if (option2) {

				for (int x = 0; x < wb; x++ ) {
					for (int y = 0; y < hb ; y++) {
						pixels = rb.getSampleDouble(x,y,0)+rb.getSampleDouble(x,y,0)+rb.getSampleDouble(x,y,0);
						if (pixels >pmax) pmax=pixels;
					}
				}
			}

			for (int x = 0; x < getWidth()-xoffset; x++ ) {
				for (int y = 0; y < getHeight()-yoffset ; y++) {
					for( int b = 0; b < 3; b++ ) {
						pixela = 0.0;
						pixelb = 0.0;
						pixelm = 0.0;
						pixela = getPixelDouble(b,x+xoffset,y+yoffset)* alpha[0];
						if (rb != null && x <wb && y < hb) {
							pixelb = rb.getSampleDouble(x, hb-1-y, 2-b)*alpha[1];
						}

						if (rm != null && x <wm && y < hm) {
							pixelm = rm.getSampleDouble(x, hm-1-y, 2-b);
							if (mode == 0) {
								pixela = pixela*(1.0 - alpha[2]*(255.0 - pixelm)/255.0);
								pixelb = pixelb*(1.0 - alpha[2]*(pixelm)/255.0);
							} 
							/*else if (mode == 1) {
								//if (pixela > pixelb) {
								//	pixelb = 0;
								//} else {
								//	pixela = 0;
								//}
							} else if (mode == 2) {
								if (pixela > pixelb) {
									pixela = 0;
								} else {
									pixelb = 0;
								}
							}*/
						}


						if (option1 & !option2) {
							pixels = getPixelDouble(0,x,y)+getPixelDouble(1,x,y)+getPixelDouble(2,x,y);
							trans = pixels/pmax;
							pixela = pixela*(trans);
							pixelb = pixelb*(1.0 - trans);
						} else if (option2 & !option1) {
							pixels = rb.getSampleDouble(x,hb-1-y,0)+rb.getSampleDouble(x,hb-1-y,0)+rb.getSampleDouble(x,hb-1-y,0);
							trans = pixels/pmax;
							pixelb = pixelb*(trans);
							pixela = pixela*(1.0 - trans);
						}


						if (!(option1 & option2)) {
							if (mode == 0) {
								pixela = pixela + pixelb;
								if (pixela > 255)
									pixela = 255;
							} else if (mode == 1) {
								if (pixela < 255 * alpha[2]) {
									pixela = pixelb + pixela;
									if (pixela > 255)
										pixela = 255;
								}
							} else if (mode == 2) {
								//if (pixelb > pixela){
								//	pixela = pixelb;
								//}
								pixel1 = 0.0;
								//pixel2 = pixela;
								//pixela = pixelb;
								if (rb != null && x < wb && y < hb) {
									for (int b1 = 0; b1 < 3; b1++) {
										pixel1 = rb.getSampleDouble(x, hb - 1
												- y, 2 - b1);
										//pixel1 =
										// getPixelDouble(b1,x+xoffset,y+yoffset);
										if (pixel1 > 255 * alpha[2]) {
											pixela = pixelb;
										}
									}
								}

							}
						} else if ((option1 & option2)){
							if (mode == 0) {
								
								pixela = pixela * pixelb;
								if (pixela > 255)
									pixela = 255;
							} else if (mode == 1) {
								pixela = pixela / pixelb;
								
							} else if (mode == 2) {
								pixela = pixela - pixelb;
								if (pixela < 0)
									pixela = -pixela;
								
							}
							
						}
						setPixel(b, x + xoffset, y + yoffset, pixela);
					}
				}
			}
		} catch (Exception e) {
		}
		buffer.setData(getData());
	}

	public void setPixel(int b, int x, int y, double i) {
		if (i > 255)
			i = 255;
		if (i < 0)
			i = 0;
		setPixel(b, x, y, (byte) i);
	}

	public void setPixel(int b, int p, double i) {
		if (i > 255)
			i = 255;
		if (i < 0)
			i = 0;
		setPixel(b, p, (byte)i);
	}

	public void setPixel(int b, int x, int y, int i) {
		if (i > 255) i = 255;
		if (i < 0) i = 0;
		setPixel(b, x, y, (byte)i);
	}
	public void setPixelHSI(int x, int y, float[] v) {

		int rgb = Color.HSBtoRGB(v[0],v[1],v[2]);

		int red = rgb<<8;
		red = red>>>24 & 0xFF;

		int green = rgb<<12;
		green = green>>>24 & 0xFF;

		int blue = rgb<<24;
		blue = blue>>>24 & 0xFF;

		setPixel(RED, x, y, (byte)red);
		setPixel(GREEN, x, y, (byte)green);
		setPixel(BLUE, x, y, (byte)blue);

	}

	public void setPixel(int b, int p, int i) {
		if (i > 255) i = 255;
		if (i < 0) i = 0;
		setPixel(b, p, (byte)i);
	}

	public void setPixel(int b, int x, int y, byte i) {
		if ((byteData.length <= (x*pixelStride + y*lineStride+b))||((x*pixelStride + y*lineStride+b)<0)) return;
		if (size != null && x > width-1) x = width-1;
		if (x<0) x = 0;
		if (size != null && y > height-1) y = height-1;
		if (y<0) y = 0;
		currentPixel = x + y*lineStride/pixelStride;
		byteData[x*pixelStride + y*lineStride+b] = (byte)i;
	}

	public void setPixel(int b, int p, byte i) {
		if ((byteData.length <= (p*pixelStride + b))||((p*pixelStride + b)<0)) return;
		currentPixel = p;
		byteData[p*pixelStride+b] = (byte)i;
	}

	public void setPixel(int p, int i) {

		if ((byteData.length <= (p*pixelStride+pixelStride))||((p*pixelStride+pixelStride)<0)) return;
		currentPixel = p;
		byteData[p*pixelStride+RED] = (byte)((i >> 16 ) & 0xFF);
		byteData[p*pixelStride+GREEN] = (byte)((i >> 8 ) & 0xFF);
		byteData[p*pixelStride+BLUE] = (byte)((i >> 0 ) & 0xFF);
	}

	public int getPixel(int p) {
		currentPixel = p;
		int i, ir, ig, ib;

		ir = 0;
		i = 0;
		i = byteData[p*pixelStride+RED]<< 8;
		ir = i>>>8 & 0xFF;

		ig = 0;
		i = 0;
		i = byteData[p*pixelStride+GREEN]<< 8;
		ig = i>>>8 & 0xFF;

		ib = 0;
		i = 0;
		i = byteData[p*pixelStride+BLUE]<< 8;
		ib = i>>>8 & 0xFF;

		return (i & 0xFF000000) | (ir<<16) | (ig<<8 ) | (ib<<0) ;

	}

	public boolean comparePixel ( EFrame other, int p, int bandDiff, int totalDiff){

		int tr = getPixel(RED, p);
		int tb = getPixel(BLUE, p);
		int tg = getPixel(GREEN, p);
		int or = other.getPixel(RED, p);
		int ob = other.getPixel(BLUE, p);
		int og = other.getPixel(GREEN, p);
		int dr, dg, db;
		if (tr > or) dr = tr - or;
		else dr = or - tr;
		if (tb > ob) db = tb - ob;
		else db = ob - tb;
		if (tg > og) dg = tg - og;
		else dg = og - tg;

		if (dr > bandDiff || dg > bandDiff || db > bandDiff) return false;

		if ((dr+dg+db) > totalDiff) return false;

		return true;
	}

	public byte getPixelByte(int b, int x, int y) {
		if ((byteData.length <= (x*pixelStride + y*lineStride+b))||((x*pixelStride + y*lineStride+b)<0)) return 0;
		currentPixel = x+y*lineStride/pixelStride;
		return byteData[x*pixelStride + y*lineStride+b];
	}

	public byte getPixelByte(int b, int p) {
		if ((byteData.length <= (p*pixelStride+b))||((p*pixelStride+b)<0)) return 0;
		currentPixel = p;
		return byteData[p*pixelStride+b];
	}

	public int getPixelInt(int b, int x, int y) {
		byte bytePixel = getPixelByte(b, x, y);
		int temp, intPixel;
		temp = bytePixel<<8;
		intPixel = temp>>>8 & 0xFF;
		return intPixel;
	}

	public int getPixelInt(int b, int p) {
		byte bytePixel = getPixelByte(b, p);
		int temp, intPixel;
		temp = bytePixel<<8;
		intPixel = temp>>>8 & 0xFF;
		return intPixel;
	}

	public double getPixelDouble(int b, int x, int y) {
		byte bytePixel = getPixelByte(b, x, y);
		int temp, intPixel;
		temp = bytePixel<<8;
		intPixel = temp>>>8 & 0xFF;
		return (double)intPixel;
	}

	public float getPixelHue(int x, int y) {

	    int[] rgb = new int[3];

		rgb[0] = getPixelInt(RED, x, y);
		rgb[1] = getPixelInt(GREEN,  x, y);
		rgb[2] = getPixelInt(BLUE,  x, y);

    	float[] values = Color.RGBtoHSB(rgb[0],rgb[1],rgb[2], null);
    	return values[0];
	}
	public float getPixelSat(int x, int y) {

	    int[] rgb = new int[3];

		rgb[0] = getPixelInt(RED, x, y);
		rgb[1] = getPixelInt(GREEN,  x, y);
		rgb[2] = getPixelInt(BLUE,  x, y);

    	float[] values = Color.RGBtoHSB(rgb[0],rgb[1],rgb[2], null);
    	return values[1];
	}
	public float getPixelInten(int x, int y) {

	    int[] rgb = new int[3];

		rgb[0] = getPixelInt(RED, x, y);
		rgb[1] = getPixelInt(GREEN,  x, y);
		rgb[2] = getPixelInt(BLUE,  x, y);

    	float[] values = Color.RGBtoHSB(rgb[0],rgb[1],rgb[2], null);
    	return values[2];
	}

	public double getPixelDouble(int b, int p) {
		byte bytePixel = getPixelByte(b, p);
		int temp, intPixel;
		temp = bytePixel<<8;
		intPixel = temp>>>8 & 0xFF;
		return (double)intPixel;
	}

	public int getPixel(int b, int x, int y) {
		return getPixelInt(b, x , y);
	}

	public int getPixel(int b, int p) {
		return getPixelInt(b, p);
	}

	public double getPixelGrey(int x, int y) {
		double greyValue =(0.299*getPixelDouble(RED, x , y) +
			0.587*getPixelDouble(GREEN, x, y) +
			0.114*getPixelDouble(BLUE, x, y));
		if (greyValue > 255.0) return 255.0;
		return greyValue;
	}

	public EFrame copy() {

		EFrame copyEFrame = new EFrame(buffer);
		System.arraycopy(pixelAge, 0, copyEFrame.pixelAge, 0 , pixelAge.length);

		return copyEFrame;
	}

	public void setBuffer(Buffer buffer) {
		this.buffer = buffer;
		byteData = (byte[])buffer.getData();
		format = buffer.getFormat();
		if (format instanceof RGBFormat) {
			rgbFormat = (RGBFormat)format;
			size = rgbFormat.getSize();
			pixelStride = rgbFormat.getPixelStride();
			lineStride = rgbFormat.getLineStride();
		}

		videoFormat = (VideoFormat)buffer.getFormat();
		length = buffer.getLength()/pixelStride;
		offset = buffer.getOffset();
		seqnum = buffer.getSequenceNumber();

		}

}


