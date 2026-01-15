/* EJEffects.java */
package comul01.eve;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.Collections;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageFunction;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import javax.media.jai.WarpPerspective;
import javax.media.jai.WarpPolynomial;
import javax.media.jai.operator.DFTDescriptor;
import javax.media.jai.operator.MedianFilterDescriptor;
import javax.media.util.BufferToImage;

import com.jhlabs.image.CellularFilter;
import com.jhlabs.image.ContrastFilter;
import com.jhlabs.image.CraterFilter;
import com.jhlabs.image.CrystalizeFilter;
import com.jhlabs.image.DiffusionFilter;
import com.jhlabs.image.DilateFilter;
import com.jhlabs.image.DistanceFilter;
import com.jhlabs.image.DitherFilter;
import com.jhlabs.image.ErodeFilter;
import com.jhlabs.image.FBMFilter;
import com.jhlabs.image.GammaFilter;
import com.jhlabs.image.HSBAdjustFilter;
import com.jhlabs.image.LifeFilter;
import com.jhlabs.image.LightFilter;
import com.jhlabs.image.MarbleFilter;
import com.jhlabs.image.OilFilter;
import com.jhlabs.image.OutlineFilter;
import com.jhlabs.image.PerspectivFilter;
import com.jhlabs.image.PlasmaFilter;
import com.jhlabs.image.PolarFilter;
import com.jhlabs.image.QuiltFilter;
import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.ShadowFilter;
import com.jhlabs.image.SharpenFilter;
import com.jhlabs.image.SkeletonFilter;
import com.jhlabs.image.SolarizeFilter;
import com.jhlabs.image.SparkleFilter;
import com.jhlabs.image.TextureFilter;
import com.jhlabs.image.WeaveFilter;

public class EJEffects {

	EFrameSet eFrameIn = null;

	EFrameSet eFrameFilter = null;

	EFrameSet eFrameOut = null;

	EFrame thisFrame = null;

	EFrame prevFrame = null;

	EFrame interFrame = null;

	EFrame nextFrame = null;

	EFrame sumFrame = null;

	EJCA[][][] lastStates = null;


	EFrame lastStates2 = null;


	EFrame thisFrameA = null;

	EFrame nextFrameA = null;

	EFrame thisFrameB = null;

	EFrame nextFrameB = null;

	private EJMain ejMain = null;

	private EJSettings ejSettings = null;

	private int waitTime = 0;

	private Object convolve = null;

	private EffectContext effectContext = null;

	private EffectParam effectParam = null;

	private EffectModulator effectModulator = null;

	private int[] oMatrix = null;

	private double[] effectParams;

	private long highSeq = 0;

	private long highTime = 0;

	private long accSeq = 0;

	private long accTime = 0;

	public int seqNumber = 0;

	int seqnum = 0;

	boolean skipFlag = false;

	double duration;

	double currentTime;

	float compoRatio;

	WarpImagePath warpPath;

	WaterApplet waterRipple;

	WarpImageGenerator warpGen;

	EJControls ejControls;

	EJCell ejCell = null;

	int saveWidth = 0;

	int saveHeight = 0;

	int effectNumber = 0;

	ArrayList compoEffectList = null;

	/** selected Parameter1 * */
	protected float parameter1 = 1.2F;

	/**
	 * initialize the formats
	 */
	public EJEffects(EJMain ejMain) {

		this.ejMain = ejMain;
		ejSettings = ejMain.getEJSettings();
		ejControls = ejMain.getEJControls();

		EffectRandomiser effectRandomiser;
		for (int i = 0; i < ejSettings.randomiserList.size(); i++) {
			effectRandomiser = (EffectRandomiser) ejSettings.randomiserList
					.get(i);
			effectRandomiser.last = -99;
			effectRandomiser.factor = -1.0;

		}
		this.duration = ejSettings.getDuration();
		int maxDepth = 4;
		EffectContext effectContext = null;
		for (int i = 0; i < ejSettings.effectList.size(); i++) {
			effectContext = (EffectContext) ejSettings.effectList.get(i);
			if (effectContext.depth > maxDepth)
				maxDepth = effectContext.depth;
		}
		this.waitTime = ejSettings.effectTrans.duration;
		eFrameIn = new EFrameSet(maxDepth);
		eFrameFilter = new EFrameSet(maxDepth);
		eFrameOut = new EFrameSet(maxDepth);
		System.out.println("EJEffects after init EFrames");

	}

	public void sleep(int time) {
		try {
			Thread.currentThread().sleep(time);
		} catch (InterruptedException ie) {
		}
	}

	// Utility methods.
	Format matches(Format in, Format outs[]) {
		for (int i = 0; i < outs.length; i++) {
			if (in.matches(outs[i]))
				return outs[i];
		}

		return null;
	}

	byte[] validateByteArraySize(Buffer buffer, int newSize) {
		Object objectArray = buffer.getData();
		byte[] typedArray;
		if (objectArray instanceof byte[]) { // is correct type AND not null
			typedArray = (byte[]) objectArray;
			if (typedArray.length >= newSize) { // is sufficient capacity
				return typedArray;
			}

			byte[] tempArray = new byte[newSize]; // re-alloc array
			System.arraycopy(typedArray, 0, tempArray, 0, typedArray.length);
			typedArray = tempArray;
		} else {
			typedArray = new byte[newSize];
		}

		buffer.setData(typedArray);
		return typedArray;
	}

	public void setEffectNumber(int effectNumber) {
		this.effectNumber = effectNumber;
	}

	public int getEffectNumber() {
		return effectNumber;
	}

	/**
	 * utility: update the output buffer fields
	 */
	protected void updateOutput(Buffer outputBuffer, Format format, int length,
			int offset) {

		outputBuffer.setFormat(format);
		outputBuffer.setLength(length);
		outputBuffer.setOffset(offset);
	}

	/**
	 * Callback to access individual video frames.
	 */
	public void processFrame(Buffer frame) {

		// For demo, we'll just print out the frame #, time &
		// data length.

		Time startTime = ejSettings.effectCompo.grabberA.getBeginTime();
		Time endTime = ejSettings.effectCompo.grabberA.getEndTime();

		if ((frame.getTimeStamp() - startTime.getNanoseconds()) <= 0) {
			frame.setTimeStamp(0);
			seqnum = 1;

		} else {
			frame.setTimeStamp(frame.getTimeStamp()
					- startTime.getNanoseconds());
			seqnum++;
		}

		// Set currentTime and t AFTER adjusting for startTime to avoid negative values
		long t = (long) (frame.getTimeStamp() / 10000000f);
		currentTime = (double) (frame.getTimeStamp());

		frame.setSequenceNumber(seqnum);

		//if (highTime < frame.getTimeStamp()) {
		//	highTime = frame.getTimeStamp();
		//}
		//if (highSeq < frame.getSequenceNumber()) {
		//	highSeq = frame.getSequenceNumber();
		//}

		//if (frame.getSequenceNumber() == 0) {
		//accTime = accTime + highTime +81666000;
		//accSeq = accSeq + highSeq +1;
		//}

		//frame.setTimeStamp(frame.getTimeStamp()+accTime);
		//frame.setSequenceNumber(frame.getSequenceNumber()+accSeq);

		System.out.println("EJEffects Testinge: frame #: "
				+ frame.getTimeStamp() + ", " + +frame.getSequenceNumber()
				+ ", time: " + ((float) t) / 100f + ", " + currentTime
				+ ", len: " + frame.getLength() + ", offset: "
				+ frame.getOffset());

		this.waitTime = ejSettings.effectTrans.duration;

		if (waitTime > 0) {
			sleep(waitTime);
		}

		ejControls.setFilmTime((long) t);
		ejControls.setFilmFrame((long) frame.getSequenceNumber());
		ejControls
				.setFilmProgress((long) ((double) (frame.getTimeStamp() - startTime
						.getNanoseconds()) / (double) (endTime.getNanoseconds() - startTime
						.getNanoseconds())));

		if (frame.getLength() == 0)
			return;
		thisFrame = new EFrame(frame);

		System.out.println("***** Start Processing Frame ****" + " height: "
				+ thisFrame.getHeight() + ", width: " + thisFrame.getWidth());

		nextFrame = new EFrame(frame);
		sumFrame = null;
		interFrame = null;
		eFrameIn.put(thisFrame);
		seqNumber = (int) frame.getSequenceNumber();
		System.out.println("EJEffects.processFrame: frame seqNumber=" + seqNumber + " (from buffer sequenceNumber=" + frame.getSequenceNumber() + ")");

		int i = 0;
		int maxSeq = 0;
		for (i = 0; i < ejSettings.effectList.size(); i++) {
			if (effectNumber != 0)
				i = effectNumber - 1;
			effectContext = (EffectContext) ejSettings.effectList.get(i);
			if (maxSeq < effectContext.seq)
				maxSeq = effectContext.seq;
			if (effectContext.enabled
					&& effectContext.type == EJSettings.PRE_PROCESS
					&& !effectContext.compo) {
				convolve = getConvolve();
				getParameters();
				if (effectParams[0] == 1.0) {
					nextFrame = doEffect(thisFrame);
				} else if (effectParams[0] != 0.0) {
					interFrame = doEffect(thisFrame);
					nextFrame = doJAIAdd(doJAIMultiplyConst(effectParams[0],
							interFrame), doJAIMultiplyConst(
							(1.0 - effectParams[0]), nextFrame));
				}
				if (sumFrame == null) {
					sumFrame = thisFrame.copy();
				} else {
					nextFrame = doJAIAdd(nextFrame, sumFrame);
				}
			}
			if (compoEffectList != null)
				break;
			if (effectNumber != 0)
				break;
		}

		for (i = 0; i < ejSettings.effectList.size(); i++) {
			if (effectNumber != 0)
				i = effectNumber - 1;
			effectContext = (EffectContext) ejSettings.effectList.get(i);
			if (effectContext.enabled
					&& effectContext.type == EJSettings.FILTER_PROCESS
					&& !effectContext.compo) {
				convolve = getConvolve();
				getParameters();
				if (effectParams[0] == 1.0) {
					nextFrame = doEffect(nextFrame);

				} else if (effectParams[0] != 0.0) {
					interFrame = doEffect(nextFrame);
					nextFrame = doJAIAdd(doJAIMultiplyConst(effectParams[0],
							interFrame), doJAIMultiplyConst(
							(1.0 - effectParams[0]), nextFrame));
				}
			}
			if (compoEffectList != null)
				break;
			if (effectNumber != 0)
				break;
		}

		eFrameFilter.put(nextFrame);
		sumFrame = null;
		thisFrame = nextFrame.copy();

		for (i = 0; i < ejSettings.effectList.size(); i++) {
			if (effectNumber != 0)
				i = effectNumber - 1;
			effectContext = (EffectContext) ejSettings.effectList.get(i);
			if (effectContext.enabled
					&& effectContext.type == EJSettings.POST_PROCESS
					&& !effectContext.compo) {
				convolve = getConvolve();
				getParameters();
				if (effectParams[0] == 1.0) {
					nextFrame = doEffect(thisFrame);
				} else if (effectParams[0] != 0.0) {
					interFrame = doEffect(thisFrame);
					nextFrame = doJAIAdd(doJAIMultiplyConst(effectParams[0],
							interFrame), doJAIMultiplyConst(
							(1.0 - effectParams[0]), nextFrame));
				}
				if (sumFrame == null) {
					sumFrame = thisFrame.copy();
				} else {
					nextFrame = doJAIAdd(nextFrame, sumFrame);
				}
			}
			if (compoEffectList != null)
				break;
			if (effectNumber != 0)
				break;
		}

		System.out.println("***** End Processing ************");
		System.out.println("");

		System.gc();

		eFrameOut.put(nextFrame);

		updateOutput(frame, nextFrame.getBuffer().getFormat(), nextFrame
				.getBuffer().getLength(), nextFrame.getBuffer().getOffset());
		validateByteArraySize(frame, nextFrame.getBuffer().getLength());
		frame.setData(nextFrame.getData());

	}

	/**
	 * Callback to access individual video frames.
	 */
	public void processFrame(Buffer frameInA, Buffer frameInB, float compoRatio) {

		this.compoRatio = compoRatio;

		long t = (long) (frameInA.getTimeStamp() / 10000000f);
		currentTime = (double) (frameInA.getTimeStamp());

		System.out.println("EJEffect Compo Processing frame#: "
				+ frameInA.getSequenceNumber() + ", time: " + ((float) t)
				/ 100f + ", current: " + currentTime + ", len: "
				+ frameInA.getLength() + ", offset: " + frameInA.getOffset()
				+ ", ratio: " + compoRatio);

		if (frameInA.getLength() == 0 || frameInA.getLength() == 0)
			return;

		thisFrameA = new EFrame(frameInA);
		thisFrameB = new EFrame(frameInB);

		nextFrameA = new EFrame(frameInA);
		nextFrameB = new EFrame(frameInB);
		eFrameIn.put(thisFrameA);
		seqNumber = (int) frameInA.getSequenceNumber();

		int i = 0;
		for (i = 0; i < ejSettings.effectList.size(); i++) {
			effectContext = (EffectContext) ejSettings.effectList.get(i);

			if (effectContext.enabled
					&& effectContext.type == EJSettings.FILTER_PROCESS
					&& effectContext.compo) {
				getParameters();
				nextFrameA = doComboEffect(nextFrameA, nextFrameB);
				break;
			}
		}

		eFrameFilter.put(nextFrameA);

		System.gc();

		eFrameOut.put(nextFrameA);

		updateOutput(frameInA, nextFrameA.getBuffer().getFormat(), nextFrameA
				.getBuffer().getLength(), nextFrameA.getBuffer().getOffset());
		validateByteArraySize(frameInA, nextFrameA.getBuffer().getLength());
		//frameInA.setData(nextFrameA.getData());
		frameInA.setData(nextFrameA.getDataReverse());

	}

	private void getParameters() {

		effectParams = new double[effectContext.params.size()];
		double pValue = 0, lowVFactor = 0, highVFactor = 0, midVFactor = 0, midPFactor = 0, mid2VFactor = 0, mid2PFactor = 0, startFactor = 0, stopFactor = 0;
		double value = 0, startTime = 0, stopTime = 0, midPTime = 0, mid2PTime = 0;
		for (int i = 0; i < effectContext.params.size(); i++) {
			effectParam = (EffectParam) effectContext.params.get(i);
			int effectValue = effectParam.value;
			effectValue = getRandomParam(effectValue, effectParam.index);

			if (effectParam.modulator == null) {
				effectParams[i] = ((double) (effectValue)) / 100.0;
			} else {
				lowVFactor = ((double) getRandomMod(
						effectParam.modulator.lowValue, 0)) / 100.000;
				highVFactor = ((double) getRandomMod(
						effectParam.modulator.highValue, 1)) / 100.000;
				midPFactor = ((double) getRandomMod(
						effectParam.modulator.midPoint, 2)) / 100.000;
				mid2PFactor = ((double) getRandomMod(
						effectParam.modulator.mid2Point, 3)) / 100.000;
				midVFactor = ((double) getRandomMod(
						effectParam.modulator.midValue, 4)) / 100.000;
				mid2VFactor = ((double) getRandomMod(
						effectParam.modulator.mid2Value, 5)) / 100.000;
				startFactor = ((double) getRandomMod(
						effectParam.modulator.start, 6)) / 100.000;
				stopFactor = ((double) getRandomMod(effectParam.modulator.stop,
						7)) / 100.000;

				if (mid2PFactor < midPFactor) {
					mid2PFactor = ((double) effectParam.modulator.midPoint) / 100.000;
					midPFactor = ((double) effectParam.modulator.mid2Point) / 100.000;
				}
				if (startFactor > midPFactor) {
					startFactor = midPFactor;
				}
				if (stopFactor < mid2PFactor) {
					stopFactor = mid2PFactor;
				}

				startTime = duration * startFactor;
				stopTime = duration * stopFactor;
				midPTime = duration * midPFactor;
				mid2PTime = duration * mid2PFactor;

				pValue = ((double) (effectValue)) / 100.0;
				if (currentTime < startTime) {
					effectParams[i] = lowVFactor * pValue;
				} else if (currentTime > stopTime) {
					effectParams[i] = highVFactor * pValue;
				} else
				// ?? look out for divide by zeroes !!
				if ((currentTime) < midPTime) {
					effectParams[i] = (lowVFactor * pValue + (midVFactor
							* pValue - lowVFactor * pValue)
							* ((currentTime - startTime) / (midPTime - startTime)));
				} else if (currentTime >= mid2PTime) {
					effectParams[i] = (highVFactor * pValue + (mid2VFactor
							* pValue - highVFactor * pValue)
							* ((stopTime - currentTime) / (stopTime - mid2PTime)));
				} else {
					effectParams[i] = (midVFactor * pValue + (mid2VFactor
							* pValue - midVFactor * pValue)
							* ((currentTime - midPTime) / (mid2PTime - midPTime)));

				}
			}
			/*
			 * System.out.println( "modulator lowV " + lowVFactor + ", midV " +
			 * midVFactor + ", highV " + highVFactor + ", midP " + midPFactor + ",
			 * midV " + midVFactor + ", mid2P " + mid2PFactor + ", mid2V " +
			 * mid2VFactor + ", startF " + startFactor + ", stopF " + stopFactor +
			 * ":: evalue " + effectValue + ";; eParam " + effectParams[i] + ",
			 * pValue " + pValue + ":: ct " + currentTime + ", du " + duration + ",
			 * stt " + startTime + ", sot" + stopTime);
			 */
		}
	}

	public int getRandomMod(int value, int item) {

		EffectRandomiser effectRandomiser;
		int retValue = value;
		for (int i = 0; i < ejSettings.randomiserList.size(); i++) {
			effectRandomiser = (EffectRandomiser) ejSettings.randomiserList
					.get(i);
			if (effectRandomiser.modOpt
					&& effectParam.modulator.index + 1 == effectRandomiser.groupNumber
					&& effectRandomiser.itemNumber == item + 1) {
				if (effectRandomiser.effectOpt) {
					retValue = doRandomise1(value, effectRandomiser);
				} else {
					retValue = doRandomise(value, effectRandomiser);
				}
				//System.out.println("randomiser "+value+", "+retValue+",
				// "+effectRandomiser.groupNumber+", "+item);
				return retValue;
			}
		}
		return retValue;
	}

	public int getRandomParam(int value, int item) {

		EffectRandomiser effectRandomiser;
		int retValue = value;
		for (int i = 0; i < ejSettings.randomiserList.size(); i++) {
			effectRandomiser = (EffectRandomiser) ejSettings.randomiserList
					.get(i);
			if (effectRandomiser.paramOpt
					&& effectContext.index + 1 == effectRandomiser.groupNumber
					&& effectRandomiser.itemNumber == item + 1) {
				if (effectRandomiser.effectOpt) {
					retValue = doRandomise1(value, effectRandomiser);
				} else {
					retValue = doRandomise(value, effectRandomiser);
				}

				return retValue;
			}
		}
		return retValue;
	}

	public int doRandomise(int value, EffectRandomiser effectRandomiser) {

		//if (effectRandomiser.itemNumber==2)
		//	System.out.println("randomiser: "+effectRandomiser.range +", "+
		// effectRandomiser.degree+", "+effectRandomiser.rate);
		if (effectRandomiser.last == -99)
			effectRandomiser.last = value;
		int rMax, rMin, dMax, dMin;
		rMax = (int) ((double) value + (double) effectRandomiser.range);
		rMin = (int) ((double) value - (double) effectRandomiser.range);
		if (rMax > 100)
			rMax = 100;
		if (rMin < 0)
			rMin = 0;

		if (effectRandomiser.factor >= 0) {
			effectRandomiser.factor = Math.random();
			dMax = (int) ((double) effectRandomiser.last + ((double) effectRandomiser.rate / 100.0)
					* ((double) effectRandomiser.degree));
			dMin = effectRandomiser.last;
			if (dMax > rMax) {
				dMax = rMax;
				effectRandomiser.factor = -1.0 * effectRandomiser.factor;
			}
			if (dMin < rMin)
				dMin = rMin;

		} else {
			effectRandomiser.factor = Math.random();
			effectRandomiser.factor = -1.0 * effectRandomiser.factor;
			dMax = effectRandomiser.last;
			dMin = (int) ((double) effectRandomiser.last - ((double) effectRandomiser.rate / 100.0)
					* ((double) effectRandomiser.degree));
			if (dMin < rMin) {
				dMin = rMin;
				effectRandomiser.factor = -1.0 * effectRandomiser.factor;
			}
			if (dMax > rMax)
				dMax = rMax;
		}
		if (dMax < rMax)
			rMax = dMax;
		if (dMin > rMin)
			rMin = dMin;
		//effectRandomiser.factor = Math.random();

		effectRandomiser.last = effectRandomiser.last
				+ (int) (effectRandomiser.factor * (double) (rMax - rMin));
		//effectRandomiser.last =
		// (int)((double)(rMax+rMin)/2.0)+(int)(effectRandomiser.factor*(double)(rMax-rMin));
		if (effectRandomiser.last > 100)
			effectRandomiser.last = 100;
		if (effectRandomiser.last < 0)
			effectRandomiser.last = 0;

		if (effectRandomiser.itemNumber == 2)
			System.out.println("randomise " + value + ", "
					+ effectRandomiser.last + ", " + effectRandomiser.factor
					+ ", " + rMax + ", " + rMin + ", " + dMax + ", " + dMin);

		return effectRandomiser.last;
	}

	public int doRandomise1(int value, EffectRandomiser effectRandomiser) {

		//System.out.println("randomise1a "+effectRandomiser.range +", "+
		// effectRandomiser.degree+", "+seqnum);

		int offset = (int) (((double) effectRandomiser.range / 2.0) * Math
				.sin(((double) effectRandomiser.rate / 100.0)
						* ((double) effectRandomiser.degree / 100.0) * 2.0
						* Math.PI * (double) seqnum));

		effectRandomiser.last = value + offset;

		if (effectRandomiser.last < 0)
			effectRandomiser.last = value;
		if (effectRandomiser.last > 100)
			effectRandomiser.last = 100;

		//System.out.println("randomise1 "+value +", "+
		// effectRandomiser.last+", "+offset);

		return effectRandomiser.last;
	}

	private Object getConvolve() {

		String name = effectContext.convolve;

		if (name == null) {
			return null;
		}

		if (name == "edgematrix") {
			return edgeMatrix;
		}
		
		if (name == "edgematrix1") {
			return edgeMatrix1;
		}

		if (name == "edgematrix2") {
			return edgeMatrix1;
		}

		if (name == "embossmatrix") {
			return embossMatrix;
		}

		if (name == "freichen1") {
			return new float[][] { freichen_h_data, freichen_v_data };
		}
		if (name == "prewitt1") {
			return new float[][] { prewitt_h_data, prewitt_v_data };
		}
		if (name == "roberts1") {
			return new float[][] { roberts_h_data, roberts_v_data };
		}

		if (name == "normaldata") {
			return normalData;
		}
		if (name == "blurdata") {
			return blurData;
		}

		if (name == "blurmoredata") {
			return blurMoreData;
		}

		if (name == "sharpendata") {
			return sharpenData;
		}

		if (name == "sharpenmoreata") {
			return sharpenMoreData;
		}

		if (name == "laplacematrix") {
			return laplaceMatrix;
		}

		if (name == "edgedata") {
			return edgeData;
		}

		if (name == "embossdata") {
			return embossData;
		}

		if (name == "cellData") {
			return cellData;
		}

		if (name == "timeconvolve1") {
			return timeConvolve1;
		}
		if (name == "timeprewitt") {
			return timePrewitt;
		}
		if (name == "timeblur") {
			return timeBlur;
		}
		if (name == "timemean") {
			return timeMean;

		}
		if (name == "dilationmatrix") {
			return dilationMatrix;
		}

		if (name == "erosionmatrix") {
			return erosionMatrix;
		}
		if (name == "tophatmatrix") {
			return tophatMatrix;
		}

		if (name == "bcmatrix1") {
			return bcMatrix1;
		}

		if (name == "bcmatrix2") {
			return bcMatrix2;
		}

		if (name == "bcmatrix4") {
			return bcMatrix4;
		}
		if (name == "bcmatrix5") {
			return bcMatrix5;
		}

		if (name == "bcmatrixr") {
			return bcMatrixr;
		}
		if (name == "bcmatrixb") {
			return bcMatrixb;
		}
		if (name == "bcmatrixg") {
			return bcMatrixg;
		}
		return null;

	}

	private EFrame doEffect(EFrame in) {
		EFrame out;

		System.out.println("EJEffects Do effect: frame#-" + seqnum + " ,time-"
				+ currentTime + " ,name-" + effectContext.name + " ,index-"
				+ effectContext.index + " ,mode-" + effectContext.mode
				+ " ,step-" + effectContext.step + " ,depth-"
				+ effectContext.depth + " ,count-" + effectContext.count
				+ " ,convolve-" + effectContext.convolve + " ,type-"
				+ effectContext.type + " ,p0-" + effectParams[0] + " ,p1-"
				+ effectParams[1] + " ,p2-" + effectParams[2] + " ,p3-"
				+ effectParams[3] + " ,p4-" + effectParams[4] + " ,p5-"
				+ effectParams[5] + " ,p6-" + effectParams[6] + " ,p7-"
				+ effectParams[7] + " ,p8-" + effectParams[8] + " ,p9-"
				+ effectParams[9] + " ,o1-" + effectContext.option1 + " ,o2-"
				+ effectContext.option2 + " ,o3-" + effectContext.option3
				+ " ,o4-" + effectContext.option4 + " ,o5-"
				+ effectContext.option5 + " ,o6-" + effectContext.option6
				+ "****"

		);

		String effectName = effectContext.name;
		if ("deltaframes".equals(effectName)) {
			out = doDeltaFrames(in);
			return out;
		}
		if ("deco".equals(effectName)) {
			out = doDitherFrames(in);
			return out;

		}
		if ("passtime".equals(effectName)) {
			out = doPassTime(in);
			return out;
		}
		if ("timeframes".equals(effectName)) {
			out = doTimeFrames(in);
			return out;
		}
		if ("timeframework".equals(effectName)) {
			out = doTimeFrameWork(in);
			return out;
		}
		if ("maxtime".equals(effectName)) {
			out = doMaxTime(in);
			return out;
		}
		if ("mintime".equals(effectName)) {
			out = doMinTime(in);
			return out;
		}
		if ("imagine".equals(effectName)) {
			out = doJAIImagine(in);
			return out;
		}
		if ("jaiscale".equals(effectName)) {
			out = doJAIScale(in);
			return out;
		}
		if ("scale".equals(effectName)) {
			out = doJ2DScale(in);
			return out;
		}
		if ("jaiinvert".equals(effectName)) {
			out = doJAIInvert(in);
			return out;
		}
		if ("intervene".equals(effectName)) {
			out = doIntervene(in);
			return out;
		}
		if ("magnitude".equals(effectName)) {
			out = doMagnitude(in);
			return out;
		}

		if ("framemean".equals(effectName)) {
			out = doFrameMeanFilter(in);
			return out;
		}
		if ("zebra".equals(effectName)) {
			out = doZebra(in);
			return out;
		}
		if ("passfilter".equals(effectName)) {
			out = doFramePassFilter();
			return out;
		}
		if ("dft".equals(effectName)) {
			out = doJAIdft(in);
			return out;
		}
		if ("idft".equals(effectName)) {
			out = doJAIidft(in);
			return out;
		}
		if ("dctidct".equals(effectName)) {
			out = doJAIdctidct(in);
			return out;
		}
		if ("dctfilter".equals(effectName)) {
			out = doJAIdftidft(in);
			return out;
		}
		if ("dct".equals(effectName)) {
			out = doJAIdct(in);
			return out;
		}

		if ("idct".equals(effectName)) {
			out = doJAIidct(in);
			return out;
		}
		if ("bandcombine".equals(effectName)) {
			out = doJAIBandCombine(in);
			return out;
		}
		if ("colourconvert".equals(effectName)) {
			out = doJAIColorConvert(in);
			return out;
		}
		if ("triplethreshold".equals(effectName)) {
			out = doJAIThreshold(in);
			return out;
		}
		if ("jailaplace".equals(effectName)) {
			out = doJAILaplaceFilter(in);
			return out;
		}
		if ("sharpen".equals(effectName)) {
			out = doJAISharpenFilter(in);
			return out;
		}
		if ("emboss".equals(effectName)) {
			//??out = doJAIEmboss(in);
			out = doCell(in);
			return out;
		}
		if ("robertsedge".equals(effectName)) {
			out = doJAIRobertsEdge(in);
			return out;
		}
		if ("freichenedge".equals(effectName)) {
			out = doJAIFreichenEdge(in);
			return out;
		}
		if ("prewittedge".equals(effectName)) {
			out = doJAIPrewittEdge(in);
			return out;
		}
		if ("sobeledge".equals(effectName)) {
			out = doJAISobelEdge(in);
			return out;
		}
		if ("median".equals(effectName)) {
			out = doJAIMedian(in);
			return out;
		}
		if ("timeconvolve".equals(effectName)) {
			out = doTimeConvolve(in);
			return out;
		}
		if ("convolve".equals(effectName)) {
			out = doConvolve(in);
			return out;
		}
		if ("convolvespread".equals(effectName)) {
			out = doErrorDiffusion(in);

			return out;
		}
		if ("brownian".equals(effectName)) {
			out = doBrownian(in);
			return out;
		}
		if ("histogram".equals(effectName)) {
			out = doHistogram(in);
			return out;
		}
		if ("zerox".equals(effectName)) {
			out = doZerox(in);
			return out;
		}
		if ("texturemix".equals(effectName)) {
			out = doPassTime2(in);
			//out = new EFrame(in.getBuffer());
			return out;
		}
		if ("equalise".equals(effectName)) {
			out = doEqualise(in);
			return out;
		}
		if ("movie".equals(effectName)) {
			out = doMovie(in);
			return out;
		}

		if ("hough".equals(effectName)) {
			out = doHough(in);
			return out;
		}
		if ("ihough".equals(effectName)) {
			out = doHoughInverse(in);
			return out;
		}
		if ("multiplyconst".equals(effectName)) {
			out = doJAIMultiplyConst(in);
			return out;
		}
		if ("divideconst".equals(effectName)) {
			out = doJAIDivideConst(in);
			return out;
		}
		if ("addconst".equals(effectName)) {
			out = doJAIAddConst(in);
			return out;
		}
		if ("subtractconst".equals(effectName)) {
			out = doJAISubtractConst(in);
			return out;
		}

		if ("threshold".equals(effectName)) {
			out = doThreshold(in);
			return out;
		}
		if ("localhistogram".equals(effectName)) {
			out = doLocalHistogram(in);
			return out;
		}
		if ("contrast".equals(effectName)) {
			//??
			out = doContrast(in);
			return out;
		}

		if ("lin".equals(effectName)) {
			out = doLIN(in);
			return out;
		}
		if ("sin".equals(effectName)) {
			out = doSIN(in);
			return out;
		}
		if ("dolps".equals(effectName)) {
			out = doDOLPS(in);
			return out;
		}
		if ("crackdetect".equals(effectName)) {
			out = doCrackDetect(in);
			return out;
		}

		if ("centroid".equals(effectName)) {
			out = doCentroid(in);
			return out;
		}

		if ("grassfire".equals(effectName)) {
			out = doGrassFire(in);
			return out;
		}

		if ("limb".equals(effectName)) {
			out = doLimb(in);
			return out;
		}
		if ("junction".equals(effectName)) {
			out = doJunction(in);
			return out;
		}
		if ("chamfer".equals(effectName)) {
			out = doChamfer(in);
			return out;
		}

		if ("zerocrossing".equals(effectName)) {
			out = doZeroCrossing(in);
			return out;
		}

		if ("clearwhite".equals(effectName)) {
			out = doClearWhite(in);
			return out;
		}

		if ("dilation".equals(effectName)) {
			out = doDilation(in, dilationMatrix);
			return out;
		}

		if ("erosion".equals(effectName)) {
			out = doErosion(in, erosionMatrix);
			return out;
		}

		if ("opening".equals(effectName)) {
			out = doOpening(in);
			return out;
		}

		if ("closing".equals(effectName)) {
			out = doClosing(in);
			return out;
		}

		if ("internalgradient".equals(effectName)) {
			out = doInternalGradient(in);
			return out;
		}

		if ("externalgradient".equals(effectName)) {
			out = doExternalGradient(in);
			return out;
		}

		if ("morphgradient".equals(effectName)) {
			out = doMorphGradient(in);
			return out;
		}

		if ("whitetophat".equals(effectName)) {
			out = doWhiteTopHat(in);
			return out;
		}

		if ("blacktophat".equals(effectName)) {
			out = doBlackTopHat(in);
			return out;
		}

		if ("reconstruct".equals(effectName)) {
			out = doReconstruct(in);
			return out;
		}

		if ("watermark".equals(effectName)) {
			out = doWaterMark(in);
			return out;
		}

		if ("walkline".equals(effectName)) {
			out = doWalkLine(in);
			return out;
		}

		if ("shade".equals(effectName)) {
			out = doShade(in);
			return out;
		}

		if ("fill".equals(effectName)) {
			out = doFill(in);
			return out;
		}

		if ("leader".equals(effectName)) {
			out = doLeader(in);
			return out;
		}

		if ("compose".equals(effectName)) {
			out = doCompose(in);
			return out;
		}

		if ("warppath".equals(effectName)) {
			out = doWarpPath(in);
			return out;
		}

		if ("waterripple".equals(effectName)) {
			out = doWaterRipple(in);
			return out;
		}

		if ("coco".equals(effectName)) {
			out = doCoco(in);
			return out;
		}

		if ("coco2".equals(effectName)) {
			//out = doCoco2(in);
			out = doPassTime3(in);
			return out;
		}

		if ("normalise".equals(effectName)) {
			out = doNormalise(in);
			return out;
		}

		if ("thinning".equals(effectName)) {
			out = doThinning2(in);
			return out;
		}

		if ("band".equals(effectName)) {
			out = doBand(in);
			return out;
		}

		if ("bits".equals(effectName)) {
			out = doBits(in);
			return out;
		}

		if ("warpgen".equals(effectName)) {
			out = doWarpGen(in);
			return out;
		}

		if ("border".equals(effectName)) {
			out = doBinaryBorder(in);
			return out;
		}

		if ("dither".equals(effectName)) {
			out = doDither(in);
			return out;
		}

		if ("reco".equals(effectName)) {
			out = doReconstruct1(in);
			return out;
		}

		if ("crop".equals(effectName)) {
			out = doCrop(in);
			return out;
		}

		if ("laplace".equals(effectName)) {
			out = doLaplace(in);
			return out;
		}

		if ("derive".equals(effectName)) {
			out = doDerive(in);
			return out;
		}

		if ("boundary".equals(effectName)) {
			out = doBoundary(in);
			return out;
		}

		if ("disconnect".equals(effectName)) {
			out = doDisconnect(in);
			return out;
		}

		if ("pointillism".equals(effectName)) {
			out = doPointy(in);
			return out;
		}

		if ("pointy2".equals(effectName)) {
			out = doPointy2(in);
			return out;
		}

		if ("waves".equals(effectName)) {
			out = doWaves(in);
			return out;
		}
		if ("yiq".equals(effectName)) {
			out = doYiq(in);
			return out;
		}
		if ("yiq2".equals(effectName)) {
			out = doYiq2(in);
			return out;
		}

		if ("rgbtohsi".equals(effectName)) {
			out = doRGBToHSI(in);
			return out;
		}

		if ("hsitorgb".equals(effectName)) {
			out = doHSIToRGB(in);
			return out;
		}

		if ("invert".equals(effectName)) {
			out = doInvert(in);
			return out;
		}

		if ("hsi".equals(effectName)) {
			out = doHsi(in);
			return out;
		}

		if ("timeflow".equals(effectName)) {
			out = doTimeFlow(in);
			return out;
		}

		if ("timecorelate".equals(effectName)) {
			out = doTimeCorelate(in);
			return out;
		}
		if ("gaussian".equals(effectName)) {
			out = doGaussian(in);
			return out;
		}
		if ("sobelmax".equals(effectName)) {
			out = doSobelMax(in);
			return out;
		}
		if ("hysteresis".equals(effectName)) {
			out = doHysteresis(in);
			return out;
		}

		if ("marrhill".equals(effectName)) {
			out = doMarrHill(in);
			return out;
		}

		if ("timemix".equals(effectName)) {
			out = doTimeMix(in);
			return out;
		}

		if ("jaitranslate".equals(effectName)) {
			out = doJAITranslate(in);
			return out;
		}

		if ("enhance".equals(effectName)) {
			out = doJAIEnhance(in);
			return out;
		}

		if ("translate".equals(effectName)) {
			out = doTranslate(in);
			return out;
		}

		if ("jaiwarp".equals(effectName)) {
			out = doJAIWarp(in);
			return out;
		}

		System.out.println("No effect found");

		out = new EFrame(in.getBuffer());
		return out;

	}

	private EFrame doComboEffect(EFrame inA, EFrame inB) {
		EFrame out;
		String effectName = effectContext.name;
		if ("add".equals(effectName)) {
			out = doJAIAdd(inA, inB);
			return out;
		}
		if ("dummy".equals(effectName)) {
			out = doDummy(inA, inB);
			return out;
		}
		if ("subtract".equals(effectName)) {
			out = doJAISubtract(inA, inB);
			return out;
		}
		if ("subtractmean".equals(effectName)) {
			out = doJAISubtractM(inA, inB);
			return out;
		}
		if ("multiply".equals(effectName)) {
			out = doJAIMultiply(inA, inB);
			return out;
		}
		if ("max".equals(effectName)) {
			out = doJAIMax(inA, inB);
			return out;
		}
		if ("min".equals(effectName)) {
			out = doJAIMin(inA, inB);
			return out;
		}

		out = new EFrame(inA.getBuffer());
		return out;
	}

	private EFrame doDeltaFrames(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame filterFrame = null;

		prevFrame = null;
		int frameOffset = 0;
		int age = 0;
		int diffr = 0, diffg = 0, diffb = 0;
		int thres = (int) (effectParams[1] * 100.0);
		int step = effectContext.step + 1;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		if (count < step)
			return frameOut;
		//System.out.println("In delta "+getGrowthFactor(1)+",
		// "+getGrowthFactor(10)+", "+
		//		getDecayFactor(1)+", "+getDecayFactor(10)+", "+thres+", "+step);
		double newpixelr = 0, newpixelg = 0, newpixelb = 0;
		for (int p = frameIn.getOffset(); p < frameIn.getLength(); p++) {

			newpixelr = 0;
			newpixelg = 0;
			newpixelb = 0;

			prevFrame = eFrameWork.get(step);

			filterFrame = eFrameOut.get(step - 1);

			if (prevFrame != null) {
				diffr = Math.abs(frameIn.getPixelInt(EFrame.RED, p)
						- prevFrame.getPixelInt(EFrame.RED, p));
				diffg = Math.abs(frameIn.getPixelInt(EFrame.GREEN, p)
						- prevFrame.getPixelInt(EFrame.GREEN, p));
				diffb = Math.abs(frameIn.getPixelInt(EFrame.BLUE, p)
						- prevFrame.getPixelInt(EFrame.BLUE, p));
				if ((effectContext.option1 && (diffr > thres || diffg > thres || diffb > thres))
						|| (!effectContext.option1 && (diffr < thres
								&& diffg < thres && diffb < thres))) {

					age = filterFrame.getPixelAge(p);
					age++;
					frameOut.setPixelAge(p, age);
					newpixelr = newpixelr + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.RED, p));
					newpixelg = newpixelg + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.GREEN, p));
					newpixelb = newpixelb + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.BLUE, p));

				} else {
					frameOut.setPixelAge(p, 0);
					if (!effectContext.option2) {
						newpixelr = newpixelr + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.RED, p));
						newpixelg = newpixelg + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.GREEN, p));
						newpixelb = newpixelb + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.BLUE, p));
					} else {
						newpixelr = newpixelg = newpixelb = 0;
					}

				}

			} else {

				frameOut.setPixelAge(p, 0);
				if (!effectContext.option2) {
					newpixelr = newpixelr + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.RED, p));
					newpixelg = newpixelg + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.GREEN, p));
					newpixelb = newpixelb + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.BLUE, p));
				} else {
					newpixelr = newpixelg = newpixelb = 0;
				}
			}

			count = eFrameWork.getCount();
			int nextAge = frameOut.getPixelAge(p); // ??
			int lastAge = 0;

			EFrame saveFrame = null;
			saveFrame = eFrameWork.get(0);

			for (int offset = 1; offset < count; offset += step) {
				prevFrame = eFrameWork.get(offset);
				filterFrame = eFrameOut.get(offset - 1);
				if (prevFrame == null)
					break;
				lastAge = filterFrame.getPixelAge(p);
				if (effectContext.option6 || lastAge >= nextAge) {
					if (!effectContext.option4 && !effectContext.option5) {
						newpixelr = newpixelr
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.RED, p));
						newpixelg = newpixelg
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.GREEN, p));
						newpixelb = newpixelb
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.BLUE, p));

					} else {
						if (effectContext.option4) {
							newpixelr = newpixelr
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(EFrame.RED,
											p) - saveFrame.getPixel(EFrame.RED,
											p)));
							newpixelg = newpixelg
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(
											EFrame.GREEN, p) - saveFrame
											.getPixel(EFrame.GREEN, p)));
							newpixelb = newpixelb
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(
											EFrame.BLUE, p) - saveFrame
											.getPixel(EFrame.BLUE, p)));
						}
						if (effectContext.option5) {
							newpixelr = newpixelr
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(EFrame.RED,
											p) - saveFrame.getPixel(EFrame.RED,
											p)));
							newpixelg = newpixelg
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(
											EFrame.GREEN, p) - saveFrame
											.getPixel(EFrame.GREEN, p)));
							newpixelb = newpixelb
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(
											EFrame.BLUE, p) - saveFrame
											.getPixel(EFrame.BLUE, p)));
						}
					}

					saveFrame = eFrameIn.get(offset);
					if (effectContext.count > 0)
						break;
				}
				nextAge = lastAge;
			}

			frameOut.setPixel(EFrame.RED, p, newpixelr);
			frameOut.setPixel(EFrame.GREEN, p, newpixelg);
			frameOut.setPixel(EFrame.BLUE, p, newpixelb);

		}

		return frameOut;
	}

	private double getGrowthFactor(int age) {
		if (!effectContext.option3)
			age = age + 1;
		double f = effectParams[2] / effectParams[3]
				+ ((double) age * effectParams[4] / effectParams[5]);
		if (f <= 0 || f > 1.0)
			return 1.0;
		//return 1.0;
		return f;
	}

	private double getDecayFactor(int offset) {
		if (offset == 1)
			return 1.0;
		return (1 / ((effectParams[6] / effectParams[7]) + ((double) offset
				* effectParams[8] / effectParams[9])));

	}

	private EFrame doDitherFrames(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame filterFrame = null;

		prevFrame = null;
		int frameOffset = 0;
		int age = 0;
		int diffr = 0, diffg = 0, diffb = 0;
		int thres = (int) (effectParams[1] * 100.0);
		int step = effectContext.step + 1;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		if (count < step)
			return frameOut;
		//System.out.println("In delta "+getGrowthFactor(1)+",
		// "+getGrowthFactor(10)+", "+
		//		getDecayFactor(1)+", "+getDecayFactor(10)+", "+thres+", "+step);
		double newpixelr = 0, newpixelg = 0, newpixelb = 0;
		for (int p = frameIn.getOffset(); p < frameIn.getLength(); p++) {

			newpixelr = 0;
			newpixelg = 0;
			newpixelb = 0;

			prevFrame = eFrameWork.get(step);

			filterFrame = eFrameOut.get(step - 1);

			if (prevFrame != null) {
				diffr = Math.abs(frameIn.getPixelInt(EFrame.RED, p)
						- prevFrame.getPixelInt(EFrame.RED, p));
				diffg = Math.abs(frameIn.getPixelInt(EFrame.GREEN, p)
						- prevFrame.getPixelInt(EFrame.GREEN, p));
				diffb = Math.abs(frameIn.getPixelInt(EFrame.BLUE, p)
						- prevFrame.getPixelInt(EFrame.BLUE, p));
				if ((effectContext.option1 && (diffr > thres || diffg > thres || diffb > thres))
						|| (!effectContext.option1 && (diffr < thres
								&& diffg < thres && diffb < thres))) {

					age = filterFrame.getPixelAge(p);
					age++;
					frameOut.setPixelAge(p, age);
					newpixelr = newpixelr + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.RED, p));
					newpixelg = newpixelg + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.GREEN, p));
					newpixelb = newpixelb + (getGrowthFactor(age))
							* (frameIn.getPixelDouble(EFrame.BLUE, p));

				} else {
					frameOut.setPixelAge(p, 0);
					if (!effectContext.option2) {
						newpixelr = newpixelr + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.RED, p));
						newpixelg = newpixelg + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.GREEN, p));
						newpixelb = newpixelb + (getGrowthFactor(0))
								* (frameIn.getPixelDouble(EFrame.BLUE, p));
					} else {
						newpixelr = newpixelg = newpixelb = 0;
					}

				}

			} else {

				frameOut.setPixelAge(p, 0);
				if (!effectContext.option2) {
					newpixelr = newpixelr + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.RED, p));
					newpixelg = newpixelg + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.GREEN, p));
					newpixelb = newpixelb + (getGrowthFactor(0))
							* (frameIn.getPixelDouble(EFrame.BLUE, p));
				} else {
					newpixelr = newpixelg = newpixelb = 0;
				}
			}

			count = eFrameWork.getCount();
			int nextAge = frameOut.getPixelAge(p); // ??
			int lastAge = 0;

			EFrame saveFrame = null;
			saveFrame = eFrameWork.get(0);

			for (int offset = 1; offset < count; offset += step) {
				prevFrame = eFrameWork.get(offset);
				filterFrame = eFrameOut.get(offset - 1);
				if (prevFrame == null)
					break;
				lastAge = filterFrame.getPixelAge(p);
				if (effectContext.option6 || lastAge >= nextAge) {
					if (!effectContext.option4 && !effectContext.option5) {
						newpixelr = newpixelr
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.RED, p));
						newpixelg = newpixelg
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.GREEN, p));
						newpixelb = newpixelb
								+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
								* ((double) prevFrame.getPixel(EFrame.BLUE, p));

					} else {
						if (effectContext.option4) {
							newpixelr = newpixelr
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(EFrame.RED,
											p) - saveFrame.getPixel(EFrame.RED,
											p)));
							newpixelg = newpixelg
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(
											EFrame.GREEN, p) - saveFrame
											.getPixel(EFrame.GREEN, p)));
							newpixelb = newpixelb
									+ (getDecayFactor(offset) * getGrowthFactor(lastAge))
									* ((double) (prevFrame.getPixel(
											EFrame.BLUE, p) - saveFrame
											.getPixel(EFrame.BLUE, p)));
						}
						if (effectContext.option5) {
							newpixelr = newpixelr
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(EFrame.RED,
											p) - saveFrame.getPixel(EFrame.RED,
											p)));
							newpixelg = newpixelg
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(
											EFrame.GREEN, p) - saveFrame
											.getPixel(EFrame.GREEN, p)));
							newpixelb = newpixelb
									+ (getDecayFactor(offset))
									* ((double) (prevFrame.getPixel(
											EFrame.BLUE, p) - saveFrame
											.getPixel(EFrame.BLUE, p)));
						}
					}

					saveFrame = eFrameIn.get(offset);
					if (effectContext.count > 0)
						break;
				}
				nextAge = lastAge;
			}

			frameOut.setPixel(EFrame.RED, p, newpixelr);
			frameOut.setPixel(EFrame.GREEN, p, newpixelg);
			frameOut.setPixel(EFrame.BLUE, p, newpixelb);

		}

		return frameOut;
	}


	private EFrame doCell(EFrame frameIn) {

		if (effectContext.mode == 2) {
			return doCell2(frameIn);
		}


		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int w = frameIn.getWidth() - 1;
		int h = frameIn.getHeight() - 1;
		int numBands = frameIn.getPixelStride();

		prevFrame = null;
		int frameOffset = 0;
		int age = 0;
		double newpixelr = 0, newpixelg = 0, newpixelb = 0;
		double ph = 0, ps = 0, pi = 0;
		double lph = 0, lps = 0, lpi = 0;
		double hd = 0, sd = 0, id = 0;

		double stateFactor = effectParams[1] * 255.0;
		int stateNum = effectContext.count;
		int caType = effectContext.step;

		double tThres = effectParams[2] ;
		double t2Thres = effectParams[3] ;


		if (lastStates == null) {

			lastStates = new EJCA[h][w][numBands];

			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					for (int b = 0; b < numBands; b++) {
						lastStates[y][x][b] = new EJCA();
						if (effectContext.option4) {
							double r = Math.random();
							lastStates[y][x][b].value =(r * stateFactor);
							if (r > tThres) lastStates[y][x][b].alive = true;

						}

					}
				}
			}

		}

		EJCA[][][] thisStates = new EJCA[h][w][numBands];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				for (int b = 0; b < numBands; b++) {
					thisStates[y][x][b] = new EJCA();
				}
			}
		}

		ejCell = new EJCell(ejMain, stateFactor, stateNum);

		System.out.println("In Cell 2");
		double value = 0;
		double error = 0;

		if (effectContext.mode == 0) {
			prevFrame = eFrameIn.get(1);
		}
		if (effectContext.mode == 1) {
			prevFrame = eFrameOut.get(2);
		}

		EFrame hFrameLast = null;
		EFrame hFrameWork = null;
		EFrame hFrameThis = null;

		if (effectContext.option5) {

			if (prevFrame != null) hFrameLast = doRGBToHSI(prevFrame);
			hFrameThis = doRGBToHSI(frameIn);
		} else {
			if (prevFrame != null) hFrameLast = new EFrame(prevFrame.getBuffer());
			hFrameThis = new EFrame(frameIn.getBuffer());
		}

		boolean[] hstop = new boolean[(w + 1) * (h + 1)];
		boolean[] sstop = new boolean[(w + 1) * (h + 1)];
		boolean[] istop = new boolean[(w + 1) * (h + 1)];

		double abs1 = 0;
		double base1 = 0;
		double value1 = 0;

		double abs2 = 0;
		double base2 = 0;
		double value2 = 0;
		double lp = 0;
		double tp = 0;
		double stateRange = 0;

		for (int y = 0; y < h; y++) {

			for (int x = 0; x < w; x++) {
				for (int b = 0; b < numBands; b++) {

					if (hFrameLast != null)
						lp = (double) hFrameLast.getPixel(b, x, y);
					tp = (double) hFrameThis.getPixel(b, x, y);
					stateRange = (tp * stateFactor / 255.0);

					abs1 = Math.floor(tp / stateRange);
					base1 = abs1 * stateRange;
					value1 = tp - base1;

					abs2 = Math.floor(lp / stateRange);
					base2 = abs2 * stateRange;
					value2 = lp - base2;

					value = t2Thres * Math.abs(value1 - value2);
					double c = stateRange * tThres;

					if (value > c) {
						lastStates[y][x][b].value = value;
						lastStates[y][x][b].alive = true;

					}
				}
			}
		}

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				if ((effectContext.option1)) {
					ejCell.process(hFrameLast, hFrameThis, lastStates,
							thisStates, x, y, 2, tThres, caType,
							effectContext.option6);
				}

				if ((effectContext.option2)) {

					ejCell.process(hFrameLast, hFrameThis, lastStates,
							thisStates, x, y, 1, tThres, caType,
							effectContext.option6);
				}

				if ((effectContext.option3)) {

					ejCell.process(hFrameLast, hFrameThis, lastStates,
							thisStates, x, y, 0, tThres, caType,
							effectContext.option6);

				}

			}

		}

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				for (int b = 0; b < numBands; b++) {
					lastStates[y][x][b] = thisStates[y][x][b];
				}
			}
		}

		if (effectContext.option5) {
			frameOut = doHSIToRGB(hFrameThis);
		} else {

			frameOut = new EFrame(hFrameThis.getBuffer());
		}
		return frameOut;
	}


	private EFrame doCell2(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int w = frameIn.getWidth() - 1;
		int h = frameIn.getHeight() - 1;
		int numBands = frameIn.getPixelStride();

		prevFrame = null;
		int frameOffset = 0;
		int age = 0;
		double newpixelr = 0, newpixelg = 0, newpixelb = 0;
		double ph = 0, ps = 0, pi = 0;
		double lph = 0, lps = 0, lpi = 0;
		double hd = 0, sd = 0, id = 0;

		int stateRange = (int)(effectParams[1] * 255.0);
		int stateNum = effectContext.count;
		int caType = effectContext.step;

		if (lastStates2 == null) {
			lastStates2 = new EFrame(frameIn.getBuffer());
			if (effectContext.option4) {
				lastStates2.clearBuffer();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						for (int b = 0; b < numBands; b++) {
							lastStates2.setPixel(b, x, y, Math.random() * stateRange);

						}
					}
				}

			} else {
				System.out.println("In Cell 1");

				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						for (int b = 0; b < numBands; b++) {
							double value = lastStates2.getPixelDouble(b, x, y);
							double abs = (int)Math.abs(value/stateRange);
							value = value - abs*stateRange;
							//value = Math.round(value/stateSize);

							if (x==100 && y==100)
								System.out.println("cell val1 "
										+value
										+", "+lastStates2.getPixelDouble(b, x, y)
										+", "+abs

										+", "+stateRange
										+", "+stateNum);

							lastStates2.setPixel(b, x, y,
									value);

						}
					}
				}

			}

		}

		EFrame thisStates = new EFrame(lastStates2.getBuffer());

		EJCell2 ejCell = new EJCell2(ejMain, stateRange, stateNum);

		System.out.println("In Cell 2");
		double value = 0;
		double error = 0;
		double tThres = effectParams[2] ;

		double hThres = 255.0 - effectParams[3] * 255.0;
		double sThres = 255.0 - effectParams[4] * 255.0;
		double iThres = 255.0 - effectParams[5] * 255.0;

		prevFrame = eFrameOut.get(0);
		EFrame workFrame = eFrameIn.get(1);

		if (prevFrame == null)
			return frameIn;

		EFrame hFrameLast = null;
		EFrame hFrameWork = null;
		EFrame hFrameThis = null;

		if (effectContext.option5) {

			hFrameLast = doRGBToHSI(prevFrame);
			hFrameWork = doRGBToHSI(workFrame);
			hFrameThis = doRGBToHSI(frameIn);
		} else {
			hFrameLast = new EFrame(prevFrame.getBuffer());
			hFrameWork = new EFrame(workFrame.getBuffer());
			hFrameThis = new EFrame(frameIn.getBuffer());
		}

		boolean[] hstop = new boolean[(w + 1) * (h + 1)];
		boolean[] sstop = new boolean[(w + 1) * (h + 1)];
		boolean[] istop = new boolean[(w + 1) * (h + 1)];

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				newpixelr = 0;
				newpixelg = 0;
				newpixelb = 0;
				lph = (double) hFrameWork.getPixel(EFrame.RED, x, y);
				lps = (double) hFrameWork.getPixel(EFrame.GREEN, x, y);
				lpi = (double) hFrameWork.getPixel(EFrame.BLUE, x, y);
				ph = (double) hFrameThis.getPixel(EFrame.RED, x, y);
				ps = (double) hFrameThis.getPixel(EFrame.GREEN, x, y);
				pi = (double) hFrameThis.getPixel(EFrame.BLUE, x, y);
				hd = (double) Math.abs(ph - lph);
				sd = (double) Math.abs(ps - lps);
				id = (double) Math.abs(pi - lpi);

				//if ((hd+sd+id) < tThres) {

				if ((effectContext.option1)) {
					ejCell.process(hFrameLast, hFrameThis, lastStates2,
							thisStates, x, y, 2, tThres, caType,effectContext.option6);
				}

				if ((effectContext.option2)) {

					ejCell.process(hFrameLast, hFrameThis, lastStates2,
							thisStates, x, y, 1, tThres, caType,effectContext.option6);
				}

				if ((effectContext.option3)) {

					ejCell.process(hFrameLast, hFrameThis, lastStates2,
							thisStates, x, y, 0, tThres, caType,effectContext.option6);

				}


			}

		}

		lastStates2 = new EFrame(thisStates.getBuffer());

		if (effectContext.option5) {
			frameOut = doHSIToRGB(hFrameThis);
		} else {

			frameOut = new EFrame(hFrameThis.getBuffer());
		}
		return frameOut;
	}

	private EFrame doPassTime(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		float[] mask = cellData;
		if (convolve instanceof float[]) {
			mask = (float[]) convolve;
		}

		double[][] buffer = new double[width][height];
		if (effectContext.option6)
			System.out.println("Pass time cell");

		double tot, cell;
		double k = 4.0;
		double dt = 0.1;
		double qfactor = 50.0;
		double alpha = 0.0;
		double beta = 0.0;
		double gamma = 0.0;
		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double z = 0;
		int sum = 0;
		for (int m = 0; m < mask.length; m++) {
			if ((int) mask[m] != 0) {
				sum += (int) mask[m];
			}
		}

		if (effectContext.option1) {
			alpha = 2 * (effectParams[1] - 0.5) / effectParams[2];
			beta = 2 * (effectParams[3] - 0.5) / effectParams[4];
			gamma = 2 * (effectParams[5] - 0.5) / effectParams[6];
		} else {
			k = 2 * (effectParams[1] - 0.5) / effectParams[2];
			dt = 2 * (effectParams[3] - 0.5) / effectParams[4];
			qfactor = 2 * (effectParams[5] - 0.5) / effectParams[6];
			if (effectContext.mode == 0) {
				IIRBandpassFilterDesign bpfd = new IIRBandpassFilterDesign(
						(int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 1) {
				IIRLowpassFilterDesign bpfd = new IIRLowpassFilterDesign(
						(int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 2) {
				IIRLowpassFilterDesign bpfd = new IIRLowpassFilterDesign(
						(int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			}

		}

		EFrameSet eFrameWork = null;
		if (effectContext.option3) {
			eFrameWork = eFrameOut;
		} else {
			eFrameWork = eFrameIn;
		}
		int count = eFrameWork.getCount();
		double mfactor = effectParams[7] / effectParams[8];

		int step = effectContext.step + 1;

		double maskTotal = 0;
		for (int my = 0; my < maskSize; my++) {
			for (int mx = 0; mx < maskSize; mx++) {
				if (mask[mx + (my * 3)] != 0.0) {
					maskTotal += 255.0 * (double) mask[mx + (my * maskSize)];
				}
			}
		}
		cell = 0.0;
		z = 0;
		int band = 0;
		if (count < 3)
			return frameOut;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//for (band = 0; band < numBands; band++) {

				if (effectContext.option6) {
					tot = 0;

					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							if (mask[mx + (my * 3)] != 0.0) {
								tot += eFrameWork.get(step).getPixelHue(
										x + mx - maskOffset,
										y + my - maskOffset)
										* (double) mask[mx + (my * maskSize)];
							}
						}
					}

					double totThres = (2.0 * 255.0);
					double div = (6.0 * 255.0);
					if (tot > totThres) {
						cell = eFrameWork.get(step).getPixelHue(x, y) - 0.3
								* 255.0 * (tot - totThres) / (div);
					} else {
						cell = eFrameWork.get(step).getPixelHue(x, y) + 0.3
								* 255.0 * (tot / totThres);

					}

					if (cell < 0)
						cell = 0.0;

					if (cell > 255.0)
						cell = 255.0;
				}

				if (!effectContext.option6) {

					if (effectContext.mode == 0) {

						z = 2.0
								* alpha
								* eFrameWork.get(step).getPixelDouble(band, x,
										y);
						z = z
								+ (alpha * eFrameWork.get(0).getPixelDouble(
										band, x, y));
						z = z
								+ (alpha * eFrameWork.get(2 * step)
										.getPixelDouble(band, x, y));
						z = z
								- (beta * eFrameOut.get(step).getPixelDouble(
										band, x, y));
						z = z
								+ (gamma * eFrameOut.get(0).getPixelDouble(
										band, x, y));
					} else if (effectContext.mode == 1) {
						z = 2.0 * alpha
								* eFrameWork.get(0).getPixelDouble(band, x, y);
						z = z
								- (2.0 * alpha * eFrameWork.get(2 * step)
										.getPixelDouble(band, x, y));
						z = z
								- (beta * eFrameOut.get(step).getPixelDouble(
										band, x, y));
						z = z
								+ (gamma * eFrameOut.get(0).getPixelDouble(
										band, x, y));
					} else if (effectContext.mode == 2) {
						z = 2.0
								* alpha
								* eFrameWork.get(step).getPixelDouble(band, x,
										y);
						z = z
								+ (alpha * eFrameWork.get(0).getPixelDouble(
										band, x, y));
						z = z
								+ (alpha * eFrameWork.get(2 * step)
										.getPixelDouble(band, x, y));
						z = z
								- (beta * eFrameOut.get(step).getPixelDouble(
										band, x, y));
						z = z
								+ (gamma * eFrameOut.get(0).getPixelDouble(
										band, x, y));
					}
				} else {

					if (effectContext.mode == 0) {

						z = 2.0 * alpha * cell;
						z = z + (alpha * eFrameWork.get(0).getPixelHue(x, y));
						z = z
								+ (alpha * eFrameWork.get(2 * step)
										.getPixelHue(x, y));
						z = z - (beta * cell);
						z = z + (gamma * eFrameOut.get(0).getPixelHue(x, y));
					} else if (effectContext.mode == 1) {
						z = cell;
					} else if (effectContext.mode == 2) {
						//z = 2.0 * alpha * cell;
						//z = z + (alpha *
						// eFrameWork.get(0).getPixelDouble(band, x, y));
						//z = z + (alpha *
						// eFrameWork.get(2*step).getPixelDouble(band, x, y));
						//z = z - (beta * cell);
						//z = z + (gamma *
						// eFrameOut.get(0).getPixelDouble(band, x, y));
						z = (alpha * eFrameWork.get(0).getPixelHue(x, y))
								+ gamma * cell - beta * cell;
					}
				}

				z = z * mfactor;

				//if (z > 255)
				//	z = 255;
				//if (z < -255)
				//	z = -255;
				//if (effectContext.option2) {
				//	z = z / 2;
				//	z = z + 255 / 2;
				//}
				//if (z < 0) {
				//	if (effectContext.option4) {
				//		z = 255 - z;
				//	} else if (effectContext.option5) {
				//		z = -z;
				//	} else {
				//		z = 0;
				//	}
				//}
				
				buffer[x][y] = z;

				//}
			}
		}
		
		normalise(buffer);
		System.out.println("PASSTIMEM NIOTMALISE");
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
//				frameOut.setPixel(band, x, y, z);
				z = buffer[x][y] ;

				float[] values = new float[3];

				values[1] = (float) eFrameWork.get(0).getPixelSat(x, y);
				values[2] = (float) eFrameWork.get(0).getPixelInten(x, y);
				if (effectContext.option4)
					values[0] = (float)(255.0-z);
				else 
					values[0] = (float) z;

				//frameOut.setPixel(band, x, y, z);
				//frameOut.setPixel(band, x, y, z);

				frameOut.setPixelHSI(x, y, values);
			}
		}
			
		return frameOut;
	}
	
	private EFrame doPassTime2(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		
		double z;
		double k = 4.0;
		double dt = 0.1;
		double qfactor = 50.0;
		double alpha = 0.0;
		double beta = 0.0;
		double gamma = 0.0;
		
		if (effectContext.option1) {
			alpha = 2*(effectParams[1]-0.5)/effectParams[2];
			beta = 2*(effectParams[3]-0.5)/effectParams[4];
			gamma = 2*(effectParams[5]-0.5)/effectParams[6];
		} else {
			k = 2*(effectParams[1]-0.5)/effectParams[2];
			dt = 2*(effectParams[3]-0.5)/effectParams[4];
			qfactor = 2*(effectParams[5]-0.5)/effectParams[6];
			if (effectContext.mode == 0) {
				IIRBandpassFilterDesign bpfd =
				new IIRBandpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 1) {
				IIRLowpassFilterDesign bpfd =
					new IIRLowpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 2) {
				IIRLowpassFilterDesign bpfd =
				new IIRLowpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			}
			
		}
		System.out.println("Before filtering a");
		
		EFrameSet eFrameWork = null;
		if (effectContext.option3) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameIn;
		}
		int count = eFrameWork.getCount();
		System.out.println(
			"Before filtering b " + count + ", " + alpha + ", " + beta + ", " + gamma);
		double mfactor = effectParams[7]/effectParams[8];
		
		int step = effectContext.step+1;
		z = 0;
		if (count < 3)
			return frameOut;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					if (effectContext.mode == 0) {	
						z = 2.0 * alpha * eFrameWork.get(step).getPixelDouble(band, x, y);
						z = z + (alpha * eFrameWork.get(0).getPixelDouble(band, x, y));
						z = z + (alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					} else if (effectContext.mode == 1) {
						z = 2.0 * alpha * eFrameWork.get(0).getPixelDouble(band, x, y);
						z = z - (2.0*alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					} else if (effectContext.mode == 2) {
						z = 2.0 * alpha * eFrameWork.get(step).getPixelDouble(band, x, y);
						z = z + (alpha * eFrameWork.get(0).getPixelDouble(band, x, y));
						z = z + (alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					}
				
					z = z * mfactor;
					
					if (z > 255)
						z = 255;
					if (z < -255)
						z = -255;
					if (effectContext.option2){
						z=z/2;
						z =z+255/2;
					}
					if (z < 0) {
						if(effectContext.option4) {
							z = 255 - z;
						} else if(effectContext.option5) {
							z = -z;
						} else if(effectContext.option6) {
							z = 0;
						} 
					}
					frameOut.setPixel(band, x, y, z);
				}
			}
		}
		return frameOut;
	}


	private void normalise(double[][] buffer) {
		int numBands = 1;
		double[] bandMin = new double[numBands];
		double[] bandMax = new double[numBands];
		double level = 0, max=0, min=10000;
		int x, y, band;
		for (band = 0; band < numBands; band++) {
			bandMax[band] = 0;
			bandMin[band] = 255.0;
		}

		for (x = 0; x < buffer.length; x++) {
			for (y = 0; y < buffer[x].length; y++) {
				level = buffer[x][y];
				if (max < level)
					max = level;
				if (min > level)
					min = level;
			}
		}
		

		for (x = 0; x < buffer.length; x++) {
			for (y = 0; y < buffer[x].length; y++) {
				buffer[x][y] = 255.0 * (buffer[x][y] - min) / (max - min);

			}
		}
		System.out.println("NIOTMALISE "+buffer.length+", "+buffer[0].length);
		
	}

	private void normalise(double[][][] buffer) {
		int numBands = buffer[0][0].length;
		double[] bandMin = new double[numBands];
		double[] bandMax = new double[numBands];
		double level = 0, max = 0, min = 10000;
		int x, y, b, band;
		for (band = 0; band < numBands; band++) {
			bandMax[band] = 0;
			bandMin[band] = 10000.0;
		}

		for (x = 0; x < buffer.length; x++) {
			for (y = 0; y < buffer[x].length; y++) {
				for (b = 0; b < buffer[x][y].length; b++) {
					level = buffer[x][y][b];
					if (bandMax[b] < level)
						bandMax[b] = level;
					if (bandMin[b] > level)
						bandMin[b] = level;
				}
			}
		}

		for (x = 0; x < buffer.length; x++) {
			for (y = 0; y < buffer[x].length; y++) {
				for (b = 0; b < buffer[x][y].length; b++) {

					buffer[x][y][b] = 255.0 * (buffer[x][y][b] - bandMin[b])
							/ (bandMax[b] - bandMin[b]);
				}
			}
		}

	}

	private EFrame doTimeFrames(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		if (effectContext.option6 && !effectContext.option5) {
			return doOozy(frameIn);
		}

		if (effectContext.option5 && !effectContext.option6) {
			return doOozy2(frameIn);
		}

		if (effectContext.option5 && effectContext.option6
				&& !effectContext.option4) {
			return doOozy3(frameIn);
		}

		if (effectContext.option4 && effectContext.option5
				&& effectContext.option6) {
			return doOozy4(frameIn);
		}

		int count = eFrameWork.getCount();
		int step = effectContext.step + 1;
		double thres = effectParams[1] * 255.0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option3) {
						z = frameIn.getPixelDouble(band, x, y);
					} else {
						if (eFrameFilter.get(0) != null) {
							z = eFrameFilter.get(0).getPixelDouble(band, x, y);
						} else {
							z = frameIn.getPixelDouble(band, x, y);

						}
					}

					lastSample = z;
					for (int offset = 1; offset < count; offset += step) {
						newFrame = eFrameWork.get(offset);
						if (newFrame == null)
							break;
						newSample = newFrame.getPixelDouble(band, x, y);
						sampleDelta = newSample - lastSample;
						if (Math.abs(sampleDelta) > thres) {
							sampleDelta = newSample - z;
							if (effectContext.option1) {
								if (sampleDelta < 0) {
									sampleDelta = 255.0 - sampleDelta;
								}
							}
							z += sampleDelta
									/ ((effectParams[4] / effectParams[5]) + (((double) (offset - 1)) * (effectParams[2] / effectParams[3])));
						}
						lastSample = newSample;
					}

					if (z > 255)
						z = 255;
					if (z < -255)
						z = -255;
					if (effectContext.option2) {
						z = z / 2;
						z = z + 255 / 2;
					}
					if (z < 0) {
						if (effectContext.option4) {
							z = 255 - z;
						} else if (effectContext.option5) {
							z = -z;
						} else if (effectContext.option6) {
							z = 0;
						}
					}

					frameOut.setPixel(band, x, y, z);
				}
			}
		}
		return frameOut;
	}

	private EFrame doOozy(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		if (oMatrix == null) {

			oMatrix = new int[(width + 1) * (height + 1)];
		}

		int numR = 0;
		int numL = 0;
		int numU = 0;
		int numD = 0;
		int offset, x1, y1, num;
		double z1, z2, r, g, b;

		int count = eFrameWork.getCount();
		int step = effectContext.step + 1;
		double thres = effectParams[1] * 255.0;
		if (count > 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {

					z = frameIn.getPixelGrey(x, y);
					z1 = eFrameWork.get(0).getPixelGrey(x, y);

					if (effectContext.option1) {

						if (z1 != z) {

							numL = 0;
							for (x1 = x; x1 >= 0; x1--) {
								z2 = frameIn.getPixelGrey(x1, y);
								numL++;
								if (z2 == z1)
									break;
							}

							numR = 0;
							for (x1 = x; x1 < width; x1++) {
								z2 = frameIn.getPixelGrey(x1, y);
								numR++;
								if (z2 == z1)
									break;
							}

							numD = 0;
							for (y1 = y; y1 >= 0; y1--) {
								z2 = frameIn.getPixelGrey(x, y1);
								numD++;
								if (z2 == z1)
									break;
							}

							numU = 0;
							for (y1 = y; y1 < height; y1++) {
								z2 = frameIn.getPixelGrey(x, y1);
								numU++;
								if (z2 == z1)
									break;
							}

							if (numR < numL && numR < numU && numR < numD) {
								num = numR;
							} else if (numL < numU && numL < numD) {
								num = numL;
							} else if (numU < numD) {
								num = numU;
							} else {
								num = numD;
							}

							if (effectContext.option4) {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}

							offset = num - oMatrix[(x + 1) * (y + 1)];
							offset = (int) ((double) offset / (double) step);
							oMatrix[(x + 1) * (y + 1)] = oMatrix[(x + 1)
									* (y + 1)]
									+ offset;

							//frameOut.setPixel(band, x, y, z1);
							r = eFrameWork.get(0).getPixelDouble(EFrame.RED, x,
									y);
							g = eFrameWork.get(0).getPixelDouble(EFrame.GREEN,
									x, y);
							b = eFrameWork.get(0).getPixelDouble(EFrame.BLUE,
									x, y);

							if (effectContext.option2) {
								if (numR > numL && numR > numU && numR > numD) {
									frameOut.setPixel(EFrame.RED, x + offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x + offset,
											y, b);

								} else if (numL > numU && numL > numD) {
									frameOut.setPixel(EFrame.RED, x - offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y, b);

								} else if (numU > numD) {
									frameOut.setPixel(EFrame.RED, x,
											y - offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											- offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											- offset, b);

								} else {
									frameOut.setPixel(EFrame.RED, x,
											y + offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											+ offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											+ offset, b);

								}

							} else if (effectContext.option3) {
								if (numR > numL && numR > numU && numR > numD) {
									frameOut.setPixel(EFrame.RED, x + offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x + offset,
											y, b);
								} else if (numL > numU && numL > numD) {
									frameOut.setPixel(EFrame.RED, x - offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y, b);
								} else if (numD > numU) {
									frameOut.setPixel(EFrame.RED, x,
											y - offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											- offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											- offset, b);
								} else {
									frameOut.setPixel(EFrame.RED, x,
											y + offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											+ offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											+ offset, b);
								}

							} else {

								if (numR < numL && numR < numU && numR < numD) {
									frameOut.setPixel(EFrame.RED, x + offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x + offset,
											y, b);
								} else if (numL < numU && numL < numD) {
									frameOut.setPixel(EFrame.RED, x - offset,
											y, r);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y, g);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y, b);
								} else if (numD < numU) {
									frameOut.setPixel(EFrame.RED, x,
											y - offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											- offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											- offset, b);
								} else {
									frameOut.setPixel(EFrame.RED, x,
											y + offset, r);
									frameOut.setPixel(EFrame.GREEN, x, y
											+ offset, g);
									frameOut.setPixel(EFrame.BLUE, x, y
											+ offset, b);
								}
							}

						} else {
							oMatrix[(x + 1) * (y + 1)] = 0;
						}

					} else {

						for (int band = 0; band < numBands; band++) {
							z = frameIn.getPixelDouble(band, x, y);
							z1 = eFrameWork.get(0).getPixelDouble(band, x, y);
							if (z1 != z) {

								numL = 0;
								for (x1 = x; x1 >= 0; x1--) {
									z2 = frameIn.getPixelDouble(band, x1, y);
									numL++;
									if (z2 == z1)
										break;
								}

								numR = 0;
								for (x1 = x; x1 < width; x1++) {
									z2 = frameIn.getPixelDouble(band, x1, y);
									numR++;
									if (z2 == z1)
										break;
								}

								numD = 0;
								for (y1 = y; y1 >= 0; y1--) {
									z2 = frameIn.getPixelDouble(band, x, y1);
									numD++;
									if (z2 == z1)
										break;
								}

								numU = 0;
								for (y1 = y; y1 < height; y1++) {
									z2 = frameIn.getPixelDouble(band, x, y1);
									numU++;
									if (z2 == z1)
										break;
								}

								if (numR < numL && numR < numU && numR < numD) {
									num = numR;
								} else if (numL < numU && numL < numD) {
									num = numL;
								} else if (numU < numD) {
									num = numU;
								} else {
									num = numD;
								}

								oMatrix[(x + 1) * (y + 1)] = 0;

								offset = num - oMatrix[(x + 1) * (y + 1)];
								offset = (int) ((double) offset / (double) step);
								oMatrix[(x + 1) * (y + 1)] = oMatrix[(x + 1)
										* (y + 1)]
										- offset;

								if (effectContext.option1) {
									if (numR > numL && numR > numU
											&& numR > numD) {
										frameOut.setPixel(band, x + offset, y,
												z1);
									} else if (numL > numU && numL > numD) {
										frameOut.setPixel(band, x - offset, y,
												z1);
									} else if (numU > numD) {
										frameOut.setPixel(band, x, y - offset,
												z1);
									} else {
										frameOut.setPixel(band, x, y + offset,
												z1);
									}

								} else if (effectContext.option2) {
									if (numR > numL && numR > numU
											&& numR > numD) {
										frameOut.setPixel(band, x + offset, y,
												z1);
									} else if (numL > numU && numL > numD) {
										frameOut.setPixel(band, x - offset, y,
												z1);
									} else if (numD > numU) {
										frameOut.setPixel(band, x, y - offset,
												z1);
									} else {
										frameOut.setPixel(band, x, y + offset,
												z1);
									}

								} else {

									if (numR < numL && numR < numU
											&& numR < numD) {
										frameOut.setPixel(band, x + offset, y,
												z1);
									} else if (numL < numU && numL < numD) {
										frameOut.setPixel(band, x - offset, y,
												z1);
									} else if (numD < numU) {
										frameOut.setPixel(band, x, y - offset,
												z1);
									} else {
										frameOut.setPixel(band, x, y + offset,
												z1);
									}
								}

							} else {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doOozy2(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		if (oMatrix == null) {

			oMatrix = new int[(width + 1) * (height + 1)];
		}

		int numR = 0;
		int numL = 0;
		int numU = 0;
		int numD = 0;
		int offset, x1, y1, num;
		double z1 = 0, z2 = 0, r = 0, g = 0, b = 0, r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0;

		int count = eFrameWork.getCount();
		int step = effectContext.step + 1;
		double thres = effectParams[1] * 255.0;
		boolean found;
		if (count > 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {

					r = frameIn.getPixelDouble(EFrame.RED, x, y);
					g = frameIn.getPixelDouble(EFrame.GREEN, x, y);
					b = frameIn.getPixelDouble(EFrame.BLUE, x, y);

					r1 = eFrameWork.get(0).getPixelDouble(EFrame.RED, x, y);
					g1 = eFrameWork.get(0).getPixelDouble(EFrame.GREEN, x, y);
					b1 = eFrameWork.get(0).getPixelDouble(EFrame.BLUE, x, y);

					if (effectContext.option1) {

						if (Math.abs(r - r1) > thres
								|| Math.abs(g - g1) > thres
								|| Math.abs(b - b1) > thres) {

							numL = 0;
							found = false;
							for (x1 = x; x1 >= 0; x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numL++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numL = 999;

							numR = 0;
							found = false;
							for (x1 = x; x1 < width; x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numR++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numR = 999;

							numD = 0;
							found = false;
							for (y1 = y; y1 >= 0; y1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numD = 999;

							numU = 0;
							found = false;
							for (y1 = y; y1 < height; y1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numU = 999;

							if (numR < numL && numR < numU && numR < numD) {
								num = numR;
							} else if (numL < numU && numL < numD) {
								num = numL;
							} else if (numU < numD) {
								num = numU;
							} else {
								num = numD;
							}

							if (effectContext.option4) {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}

							if (num < 999) {
								offset = num - oMatrix[(x + 1) * (y + 1)];
								offset = (int) ((double) offset / (double) step);
								oMatrix[(x + 1) * (y + 1)] = oMatrix[(x + 1)
										* (y + 1)]
										+ offset;

								r = eFrameWork.get(0).getPixelDouble(
										EFrame.RED, x, y);
								g = eFrameWork.get(0).getPixelDouble(
										EFrame.GREEN, x, y);
								b = eFrameWork.get(0).getPixelDouble(
										EFrame.BLUE, x, y);

								if (effectContext.option2) {
									if (numR > numL && numR > numU
											&& numR > numD) {
										frameOut.setPixel(EFrame.RED, x
												+ offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y, b);

									} else if (numL > numU && numL > numD) {
										frameOut.setPixel(EFrame.RED, x
												- offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y, b);

									} else if (numU > numD) {
										frameOut.setPixel(EFrame.RED, x, y
												- offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												- offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												- offset, b);

									} else {
										frameOut.setPixel(EFrame.RED, x, y
												+ offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												+ offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												+ offset, b);

									}

								} else if (effectContext.option3) {
									if (numR > numL && numR > numU
											&& numR > numD) {
										frameOut.setPixel(EFrame.RED, x
												+ offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y, b);
									} else if (numL > numU && numL > numD) {
										frameOut.setPixel(EFrame.RED, x
												- offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y, b);
									} else if (numD > numU) {
										frameOut.setPixel(EFrame.RED, x, y
												- offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												- offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												- offset, b);
									} else {
										frameOut.setPixel(EFrame.RED, x, y
												+ offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												+ offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												+ offset, b);
									}

								} else {

									if (numR < numL && numR < numU
											&& numR < numD) {
										frameOut.setPixel(EFrame.RED, x
												+ offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y, b);
									} else if (numL < numU && numL < numD) {
										frameOut.setPixel(EFrame.RED, x
												- offset, y, r);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y, g);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y, b);
									} else if (numD < numU) {
										frameOut.setPixel(EFrame.RED, x, y
												- offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												- offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												- offset, b);
									} else {
										frameOut.setPixel(EFrame.RED, x, y
												+ offset, r);
										frameOut.setPixel(EFrame.GREEN, x, y
												+ offset, g);
										frameOut.setPixel(EFrame.BLUE, x, y
												+ offset, b);
									}
								}
							}

						} else {
							oMatrix[(x + 1) * (y + 1)] = 0;
						}

					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doOozy3(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		if (oMatrix == null) {
			oMatrix = new int[(width + 1) * (height + 1)];
		}

		int numR = 0;
		int numL = 0;
		int numU = 0;
		int numD = 0;
		int numRU = 0;
		int numLU = 0;
		int numRD = 0;
		int numLD = 0;

		int stateR = 1;
		int stateL = 2;
		int stateU = 3;
		int stateD = 4;
		int stateRU = 5;
		int stateRD = 6;
		int stateLU = 7;
		int stateLD = 8;
		int state = 0;
		int state1 = 0;

		int offset, x1, y1, num, num1;
		double z1 = 0, z2 = 0, r = 0, g = 0, b = 0, r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0;

		int count = eFrameWork.getCount();
		int step = effectContext.step + 1;
		double thres = effectParams[1] * 255.0;
		boolean found;
		if (count > 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {

					r = frameIn.getPixelDouble(EFrame.RED, x, y);
					g = frameIn.getPixelDouble(EFrame.GREEN, x, y);
					b = frameIn.getPixelDouble(EFrame.BLUE, x, y);

					r1 = eFrameWork.get(0).getPixelDouble(EFrame.RED, x, y);
					g1 = eFrameWork.get(0).getPixelDouble(EFrame.GREEN, x, y);
					b1 = eFrameWork.get(0).getPixelDouble(EFrame.BLUE, x, y);

					if (effectContext.option1) {

						if (Math.abs(r - r1) > thres
								|| Math.abs(g - g1) > thres
								|| Math.abs(b - b1) > thres) {

							numL = 0;
							found = false;
							for (x1 = x; x1 >= 0; x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numL++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numL = 999;

							numR = 0;
							found = false;
							for (x1 = x; x1 < width; x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numR++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numR = 999;

							numD = 0;
							found = false;
							for (y1 = y; y1 >= 0; y1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numD = 999;

							numU = 0;
							found = false;
							for (y1 = y; y1 < height; y1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							if (!found)
								numU = 999;

							numRU = 0;
							found = false;
							for (y1 = y, x1 = x; y1 < height || x1 < width; y1++, x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numRU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							numRU = (int) Math
									.sqrt((double) (numRU * numRU + numRU
											* numRU));
							if (!found)
								numRU = 999;

							numRD = 0;
							found = false;
							for (y1 = y, x1 = x; y1 >= 0 || x1 < width; y1--, x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numRD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							numRD = (int) Math
									.sqrt((double) (numRD * numRD + numRD
											* numRD));
							if (!found)
								numRD = 999;

							numLD = 0;
							found = false;
							for (y1 = y, x1 = x; y1 >= 0 || x1 >= 0; y1--, x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numLD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							numLD = (int) Math
									.sqrt((double) (numLD * numLD + numLD
											* numLD));
							if (!found)
								numLD = 999;

							numLU = 0;
							found = false;
							for (y1 = y, x1 = x; y1 < height || x1 >= 0; y1++, x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numLU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}
							numLU = (int) Math
									.sqrt((double) (numLU * numLU + numLU
											* numLU));
							if (!found)
								numLU = 999;

							if (numR < numL && numR < numU && numR < numD) {
								num = numR;
								state = stateR;
							} else if (numL < numU && numL < numD) {
								num = numL;
								state = stateL;

							} else if (numU < numD) {
								num = numU;
								state = stateU;

							} else {
								num = numD;
								state = stateD;
							}

							if (numRU < numLU && numRU < numLD && numRU < numRD) {
								num1 = numRU;
								state1 = stateRU;
							} else if (numLU < numLD && numLU < numRD) {
								num1 = numLU;
								state1 = stateLU;
							} else if (numLD < numRD) {
								num1 = numLD;
								state1 = stateLD;

							} else {
								num1 = numRD;
								state1 = stateRD;

							}

							if (num1 < num) {
								num = num1;
								state = state1;
							}

							if (effectContext.option4) {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}

							if (num < 999) {
								if (oMatrix[(x + 1) * (y + 1)] == 0)
									oMatrix[(x + 1) * (y + 1)] = num;

								if (oMatrix[(x + 1) * (y + 1)] > 0) {

									offset = oMatrix[(x + 1) * (y + 1)];
									offset = (int) ((double) offset / (double) step);
									oMatrix[(x + 1) * (y + 1)] = oMatrix[(x + 1)
											* (y + 1)]
											- offset;

									//r =
									// eFrameWork.get(0).getPixelDouble(EFrame.RED,
									// x, y);
									//g =
									// eFrameWork.get(0).getPixelDouble(EFrame.GREEN,
									// x, y);
									//b =
									// eFrameWork.get(0).getPixelDouble(EFrame.BLUE,
									// x, y);

									if (effectContext.option2) {

										if (numR > numL && numR > numU
												&& numR > numD) {
											num = numR;
											state = stateR;
										} else if (numL > numU && numL > numD) {
											num = numL;
											state = stateL;

										} else if (numU > numD) {
											num = numU;
											state = stateU;

										} else {
											num = numD;
											state = stateD;
										}

										if (numRU > numLU && numRU > numLD
												&& numRU > numRD) {
											num1 = numRU;
											state1 = stateRU;
										} else if (numLU > numLD
												&& numLU > numRD) {
											num1 = numLU;
											state1 = stateLU;
										} else if (numLD > numRD) {
											num1 = numLD;
											state1 = stateLD;

										} else {
											num1 = numRD;
											state1 = stateRD;

										}

										if (num1 > num) {
											num = num1;
											state = state1;
										}

									}

									if (state == stateR) {
										frameOut.setPixel(EFrame.RED, x
												+ offset, y, r1);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y, g1);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y, b1);

									} else if (state == stateL) {
										frameOut.setPixel(EFrame.RED, x
												- offset, y, r1);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y, g1);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y, b1);

									} else if (state == stateU) {
										frameOut.setPixel(EFrame.RED, x, y
												+ offset, r1);
										frameOut.setPixel(EFrame.GREEN, x, y
												+ offset, g1);
										frameOut.setPixel(EFrame.BLUE, x, y
												+ offset, b1);

									} else if (state == stateD) {
										frameOut.setPixel(EFrame.RED, x, y
												- offset, r1);
										frameOut.setPixel(EFrame.GREEN, x, y
												- offset, g1);
										frameOut.setPixel(EFrame.BLUE, x, y
												- offset, b1);

									} else if (state == stateRD) {
										offset = (int) ((double) (offset * offset) / 2.0);
										frameOut.setPixel(EFrame.RED, x
												+ offset, y - offset, r1);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y - offset, g1);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y - offset, b1);

									} else if (state == stateRU) {
										offset = (int) ((double) (offset * offset) / 2.0);
										frameOut.setPixel(EFrame.RED, x
												+ offset, y + offset, r1);
										frameOut.setPixel(EFrame.GREEN, x
												+ offset, y + offset, g1);
										frameOut.setPixel(EFrame.BLUE, x
												+ offset, y + offset, b1);

									} else if (state == stateLD) {
										offset = (int) ((double) (offset * offset) / 2.0);
										frameOut.setPixel(EFrame.RED, x
												- offset, y - offset, r1);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y - offset, g1);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y - offset, b1);

									} else if (state == stateLU) {
										offset = (int) ((double) (offset * offset) / 2.0);
										frameOut.setPixel(EFrame.RED, x
												- offset, y + offset, r1);
										frameOut.setPixel(EFrame.GREEN, x
												- offset, y + offset, g1);
										frameOut.setPixel(EFrame.BLUE, x
												- offset, y + offset, b1);

									}
								} else {
									oMatrix[(x + 1) * (y + 1)] = 0;
								}
							} else {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}

						} else {
							oMatrix[(x + 1) * (y + 1)] = 0;
						}

					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doOozy4(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}

		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}

		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		if (oMatrix == null) {
			oMatrix = new int[(width + 1 + 100) * (height + 1 + 100)];
		}

		for (int oi = 0; oi < oMatrix.length; oi++) {
			oMatrix[oi] = 0;
		}

		int numR = 0;
		int numL = 0;
		int numU = 0;
		int numD = 0;
		int numRU = 0;
		int numLU = 0;
		int numRD = 0;
		int numLD = 0;

		int stateR = 1;
		int stateL = 2;
		int stateU = 3;
		int stateD = 4;
		int stateRU = 5;
		int stateRD = 6;
		int stateLU = 7;
		int stateLD = 8;
		int state = 0;
		int state1 = 0;

		int offset, x1, y1, num, num1;
		double z1 = 0, z2 = 0, r = 0, g = 0, b = 0, r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0;

		int count = eFrameWork.getCount();
		int step = effectContext.step + 1;
		double thres = effectParams[1] * 255.0;
		boolean found;
		if (count > 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					r = frameIn.getPixelDouble(EFrame.RED, x, y);
					g = frameIn.getPixelDouble(EFrame.GREEN, x, y);
					b = frameIn.getPixelDouble(EFrame.BLUE, x, y);

					r1 = eFrameWork.get(0).getPixelDouble(EFrame.RED, x, y);
					g1 = eFrameWork.get(0).getPixelDouble(EFrame.GREEN, x, y);
					b1 = eFrameWork.get(0).getPixelDouble(EFrame.BLUE, x, y);

					if (effectContext.option1) {
						if (Math.abs(r - r1) > thres
								|| Math.abs(g - g1) > thres
								|| Math.abs(b - b1) > thres) {
							numL = 0;
							found = false;
							for (x1 = x; x1 >= 0; x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numL++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (x1 = x; x1 >= 0; x1...

							if (!found)
								numL = 999;

							numR = 0;
							found = false;
							for (x1 = x; x1 < width; x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x1, y);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x1, y);
								numR++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (x1 = x; x1 < width;...

							if (!found)
								numR = 999;

							numD = 0;
							found = false;
							for (y1 = y; y1 >= 0; y1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y; y1 >= 0; y1...

							if (!found)
								numD = 999;

							numU = 0;
							found = false;
							for (y1 = y; y1 < height; y1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x, y1);
								g2 = frameIn
										.getPixelDouble(EFrame.GREEN, x, y1);
								b2 = frameIn.getPixelDouble(EFrame.BLUE, x, y1);
								numU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y; y1 < height...

							if (!found)
								numU = 999;

							numRU = 0;
							found = false;
							for (y1 = y, x1 = x; y1 < height || x1 < width; y1++, x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numRU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y, x1=x; y1 < ...

							numRU = (int) Math
									.sqrt((double) (numRU * numRU + numRU
											* numRU));
							if (!found)
								numRU = 999;

							numRD = 0;
							found = false;
							for (y1 = y, x1 = x; y1 >= 0 || x1 < width; y1--, x1++) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numRD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y, x1=x; y1 >=...

							numRD = (int) Math
									.sqrt((double) (numRD * numRD + numRD
											* numRD));
							if (!found)
								numRD = 999;

							numLD = 0;
							found = false;
							for (y1 = y, x1 = x; y1 >= 0 || x1 >= 0; y1--, x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numLD++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y, x1=x; y1 >=...

							numLD = (int) Math
									.sqrt((double) (numLD * numLD + numLD
											* numLD));
							if (!found)
								numLD = 999;

							numLU = 0;
							found = false;
							for (y1 = y, x1 = x; y1 < height || x1 >= 0; y1++, x1--) {
								r2 = frameIn.getPixelDouble(EFrame.RED, x1, y1);
								g2 = frameIn.getPixelDouble(EFrame.GREEN, x1,
										y1);
								b2 = frameIn
										.getPixelDouble(EFrame.BLUE, x1, y1);
								numLU++;
								found = true;
								if (Math.abs(r2 - r1) <= thres
										&& Math.abs(g2 - g1) <= thres
										&& Math.abs(b2 - b1) <= thres)
									break;
								found = false;
							}//~for (y1 = y, x1=x; y1 < ...

							numLU = (int) Math
									.sqrt((double) (numLU * numLU + numLU
											* numLU));
							if (!found)
								numLU = 999;

							if (numR < numL && numR < numU && numR < numD) {
								num = numR;
								state = stateR;
							}

							else if (numL < numU && numL < numD) {
								num = numL;
								state = stateL;

							}

							else if (numU < numD) {
								num = numU;
								state = stateU;

							}

							else {
								num = numD;
								state = stateD;
							}

							if (numRU < numLU && numRU < numLD && numRU < numRD) {
								num1 = numRU;
								state1 = stateRU;
							}

							else if (numLU < numLD && numLU < numRD) {
								num1 = numLU;
								state1 = stateLU;
							}

							else if (numLD < numRD) {
								num1 = numLD;
								state1 = stateLD;

							}

							else {
								num1 = numRD;
								state1 = stateRD;

							}

							if (num1 < num) {
								num = num1;
								state = state1;
							}

							if (effectContext.option4) {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}

							if (num < 999 && num > 0) {
								offset = num;
								offset = (int) Math.min((double) offset,
										(double) step);

								if (effectContext.option2) {
									if (numR > numL && numR > numU
											&& numR > numD) {
										num = numR;
										state = stateR;
									}

									else if (numL > numU && numL > numD) {
										num = numL;
										state = stateL;

									}

									else if (numU > numD) {
										num = numU;
										state = stateU;

									}

									else {
										num = numD;
										state = stateD;
									}

									if (numRU > numLU && numRU > numLD
											&& numRU > numRD) {
										num1 = numRU;
										state1 = stateRU;
									}

									else if (numLU > numLD && numLU > numRD) {
										num1 = numLU;
										state1 = stateLU;
									}

									else if (numLD > numRD) {
										num1 = numLD;
										state1 = stateLD;

									}

									else {
										num1 = numRD;
										state1 = stateRD;

									}

									if (num1 > num) {
										num = num1;
										state = state1;
									}
								}//~if (effectContext.option...

								if (state == stateR) {
									frameOut.setPixel(EFrame.RED, x + offset,
											y, r1);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y, g1);

									frameOut.setPixel(EFrame.BLUE, x + offset,
											y, b1);

								}

								else if (state == stateL) {
									frameOut.setPixel(EFrame.RED, x - offset,
											y, r1);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y, g1);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y, b1);

								}

								else if (state == stateU) {
									frameOut.setPixel(EFrame.RED, x,
											y + offset, r1);
									frameOut.setPixel(EFrame.GREEN, x, y
											+ offset, g1);
									frameOut.setPixel(EFrame.BLUE, x, y
											+ offset, b1);

								}

								else if (state == stateD) {
									frameOut.setPixel(EFrame.RED, x,
											y - offset, r1);
									frameOut.setPixel(EFrame.GREEN, x, y
											- offset, g1);
									frameOut.setPixel(EFrame.BLUE, x, y
											- offset, b1);

								}

								else if (state == stateRD) {
									offset = (int) ((double) (offset * offset) / 2.0);
									frameOut.setPixel(EFrame.RED, x + offset, y
											- offset, r1);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y - offset, g1);
									frameOut.setPixel(EFrame.BLUE, x + offset,
											y - offset, b1);

								}

								else if (state == stateRU) {
									offset = (int) ((double) (offset * offset) / 2.0);
									frameOut.setPixel(EFrame.RED, x + offset, y
											+ offset, r1);
									frameOut.setPixel(EFrame.GREEN, x + offset,
											y + offset, g1);
									frameOut.setPixel(EFrame.BLUE, x + offset,
											y + offset, b1);

								}

								else if (state == stateLD) {
									offset = (int) ((double) (offset * offset) / 2.0);
									frameOut.setPixel(EFrame.RED, x - offset, y
											- offset, r1);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y - offset, g1);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y - offset, b1);

								}

								else if (state == stateLU) {
									offset = (int) ((double) (offset * offset) / 2.0);
									frameOut.setPixel(EFrame.RED, x - offset, y
											+ offset, r1);
									frameOut.setPixel(EFrame.GREEN, x - offset,
											y + offset, g1);
									frameOut.setPixel(EFrame.BLUE, x - offset,
											y + offset, b1);

								}
							}//~if (num < 999 && num >0)...

							else {
								oMatrix[(x + 1) * (y + 1)] = 0;
							}
						}//~|| Math.abs(b - b1)>thre...

						else {
							oMatrix[(x + 1) * (y + 1)] = 0;
						}
					}//~if(effectContext.option1...
				}//~for (int x = 0; x < widt...
			}//~for (int y = 0; y < heig...
		}//~if (count >1)...

		return frameOut;
	}//~private EFrame doOozy4(E...

	private EFrame doTimeFrameWork(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					//z = frameIn.getPixelDouble(band, x, y);
					z = eFrameWork.get(0).getPixelDouble(band, x, y);

					lastSample = z;
					for (int offset = 1; offset < count; offset += 4) {
						newFrame = eFrameWork.get(offset);
						if (newFrame == null)
							break;
						newSample = newFrame.getPixelDouble(band, x, y);
						sampleDelta = newSample - lastSample;
						if (sampleDelta != 0.0) {
							// ??
							sampleDelta = newSample - z;
							if (sampleDelta > -255.0) {
								//z += sampleDelta/(2 + (offset-1)*0.2);
								//z += sampleDelta/(2 + (offset%4));
								//z += sampleDelta/2;
								z += newSample / (1 + (offset - 1) * 0.2);
							}
						}
						lastSample = newSample;
					}
					frameOut.setPixel(band, x, y, z);
				}
			}
		}
		return frameOut;
	}

	private EFrame doMaxTime(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}
		int step = effectContext.step + 1;
		int count = eFrameWork.getCount();
		EFrame newFrame = null;
		for (int offset = 1; offset < count; offset += step) {
			newFrame = eFrameWork.get(offset);
			if (newFrame == null)
				break;
			if (!effectContext.option1) {
				frameOut = doJAIMax(
						frameOut,
						doJAIMultiplyConst(
								(effectParams[5] / effectParams[6])
										/ (count * effectParams[1]
												/ effectParams[2] + effectParams[3]
												/ effectParams[4]), newFrame));
			} else {
				frameOut = doJAIMax(frameOut, newFrame);
			}

		}
		return frameOut;
	}

	private EFrame doMinTime(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrameSet eFrameWork = null;

		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}
		int step = effectContext.step + 1;
		int count = eFrameWork.getCount();
		EFrame newFrame = null;
		for (int offset = 1; offset < count; offset += step) {
			newFrame = eFrameWork.get(offset);
			if (newFrame == null)
				break;

			if (!effectContext.option1) {
				frameOut = doJAIMin(
						frameOut,
						doJAIMultiplyConst(
								(effectParams[5] / effectParams[6])
										/ (count * effectParams[1]
												/ effectParams[2] + effectParams[3]
												/ effectParams[4]), newFrame));
			} else {
				frameOut = doJAIMin(frameOut, newFrame);
			}
		}
		return frameOut;
	}

	private EFrame doJAIImagine(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();

		Imagine imagine = new Imagine(effectParams[1], effectParams[2],
				effectParams[3], effectParams[4], effectParams[5],
				effectParams[6], effectParams[7], effectParams[8],
				effectParams[9]);
		pb.add(imagine);

		pb.add(ropIn.getWidth());
		pb.add(ropIn.getHeight());
		pb.add((float) 0.0);
		pb.add((float) 0.0);
		pb.add((float) ropIn.getWidth());
		pb.add((float) ropIn.getHeight());
		System.out.println("Imagine width/height: " + ropIn.getWidth() + ", "
				+ ropIn.getHeight());
		RenderedOp ropOut = JAI.create("imagefunction", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJ2DScale(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		BufferedImage biIn = frameIn.getBufferedImage();
		Interpolation interp = new InterpolationBilinear();
		int iw = frameIn.getWidth() * 3;
		int ih = frameIn.getHeight() * 3;
		BufferedImage biOut = new BufferedImage(iw, ih,
				BufferedImage.TYPE_INT_RGB);
		AffineTransform at = new AffineTransform(3.0, 0.0, 0.0, 3.0, 0.0, 0.0);
		/*
		 * RenderingHints rh = new RenderingHints( JAI.KEY_BORDER_EXTENDER,
		 * BorderExtender.createInstance(BorderExtender.BORDER_COPY));
		 * AffineTransformOp aop = new AffineTransformOp(at,
		 * AffineTransformOp.TYPE_BILINEAR);
		 */
		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		rh.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		AffineTransformOp aop = new AffineTransformOp(at, rh);

		aop.filter(biIn, biOut);
		int width = biOut.getWidth();
		int height = biOut.getHeight();
		Dimension size = new Dimension(width, height);
		RGBFormat newFormat = (RGBFormat) frameIn.getFormat();
		float frameRate = newFormat.getFrameRate();
		int flipped = newFormat.getFlipped();
		int endian = newFormat.getEndian();
		int maxDataLength = size.width * size.height * 3;
		int lineStride = size.width * 3;
		newFormat = new RGBFormat(size, maxDataLength, Format.byteArray,
				frameRate, 24, 3, 2, 1, 3, lineStride, flipped, endian);

		Buffer newBuffer = new Buffer();
		newBuffer.copy(frameIn.getBuffer());
		newBuffer.setFormat(newFormat);
		newBuffer.setLength(maxDataLength);
		newBuffer.setOffset(0);
		validateByteArraySize(newBuffer, maxDataLength);
		frameOut = new EFrame(newBuffer);
		frameOut.setBufferedImage(biOut);
		return frameOut;
	}

	private EFrame doJAIScale(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		PlanarImage jaiImage;
		RenderedImage src = null;
		// Create a BufferedImage source directly from JMF data if possible.
		if (frameIn.getBuffer().getFormat() instanceof RGBFormat
				&& (int[].class).isInstance(frameIn.getBuffer().getData())) {
			System.out.println("In JAI Scale 2 - got INT buffer");
			RGBFormat rgbFormat = (RGBFormat) frameIn.getBuffer().getFormat();
			int redMask = rgbFormat.getRedMask();
			int greenMask = rgbFormat.getGreenMask();
			int blueMask = rgbFormat.getBlueMask();
			if (rgbFormat.getBitsPerPixel() <= 32
					&& rgbFormat.getPixelStride() == 1) {
				System.out.println("In JAI Scale 3 - got rgb format");
				Dimension size = rgbFormat.getSize();
				SampleModel sm = new SinglePixelPackedSampleModel(
						DataBuffer.TYPE_INT, size.width, size.height, rgbFormat
								.getLineStride(), new int[] { redMask,
								greenMask, blueMask });
				int[] data = (int[]) frameIn.getBuffer().getData();
				WritableRaster wr = Raster.createWritableRaster(sm,
						new DataBufferInt(data, data.length), new Point(0, 0));
				ColorModel cm = new DirectColorModel(32, redMask, greenMask,
						blueMask);
				src = new BufferedImage(cm, wr, false, null);
			}
		}

		// If an image couldn't be created directly use the AWT image route.
		BufferToImage frameConverter = new BufferToImage((VideoFormat) frameIn
				.getBuffer().getFormat());
		if (src == null) {
			System.out.println("In JAI Scale 4 - creating AWT Image");
			// Convert the Buffer to an AWT Image.
			Image frameImage = frameConverter.createImage(frameIn.getBuffer());

			// Derive a JAI image from the AWT image.
			src = JAI.create("AWTImage", frameImage);
		}

		// Ensure the source has a ComponentSampleModel.
		ParameterBlock pb;
		if (!(src.getSampleModel() instanceof ComponentSampleModel)) {
			System.out.println("In JAI Scale 5 - creating samplemodel");
			SampleModel sampleModel = RasterFactory
					.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
							src.getWidth(), src.getHeight(), 3);
			ImageLayout layout = new ImageLayout();
			layout.setSampleModel(sampleModel);
			//layout.setTileWidth(640).setTileHeight(480);
			RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					layout);
			pb = (new ParameterBlock()).addSource(src);
			src = JAI.create("format", pb, hints);
		}
		// Scale the image by the specified factor.

		float xscale = (float) (effectParams[1] / effectParams[2]);
		float yscale = (float) (effectParams[3] / effectParams[4]);

		pb = (new ParameterBlock()).addSource(src);
		pb.add((float) xscale).add((float) yscale);
		pb.add(0.0F).add(0.0F);
		Interpolation interp = null;
		if (effectContext.mode == 0) {
			interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
		}
		if (effectContext.mode == 1) {
			interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
		}
		if (effectContext.mode == 2) {
			interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
		}
		pb.add(interp);
		BorderExtender extender = BorderExtender
				.createInstance(BorderExtender.BORDER_ZERO);
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				extender);
		jaiImage = JAI.create("Scale", pb, hints);

		BufferedImage biOut = jaiImage.getAsBufferedImage();
		int height = biOut.getHeight();
		int width = biOut.getWidth();
		Dimension size = new Dimension(width, height);
		RGBFormat newFormat = (RGBFormat) frameIn.getBuffer().getFormat();
		float frameRate = newFormat.getFrameRate();
		int flipped = newFormat.getFlipped();
		int endian = newFormat.getEndian();
		int maxDataLength = size.width * size.height * 3;
		int lineStride = size.width * 3;
		newFormat = new RGBFormat(size, maxDataLength, Format.byteArray,
				frameRate, 24, 3, 2, 1, 3, lineStride, flipped, endian);

		Buffer newBuffer = new Buffer();
		newBuffer.copy(frameIn.getBuffer());
		newBuffer.setFormat(newFormat);
		newBuffer.setLength(maxDataLength);
		newBuffer.setOffset(0);
		validateByteArraySize(newBuffer, maxDataLength);
		frameOut = new EFrame(newBuffer);
		frameOut.setBufferedImage(biOut);
		return frameOut;
	}

	private EFrame doJAIInvert(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		RenderedOp ropOut = JAI.create("invert", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doIntervene(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		frameOut.clearBuffer();
		boolean pi = false;
		if (frameIn.isPlanarImage()) {
			ropIn = (RenderedOp) frameIn.dettachPlanarImage();
			pi = true;
		}
		double epa = effectParams[1] / effectParams[2];
		double epb = effectParams[3] / effectParams[4];

		double factor = epa;
		double[] constants = new double[4];
		constants[0] = factor;
		constants[1] = factor;
		constants[2] = factor;
		constants[3] = factor;
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		pb.add(constants);
		RenderedOp ropOut1 = JAI.create("multiplyconst", pb);
		// Create the ParameterBlock specifying the source image.

		pb.addSource(ropOut1);
		factor = epb;
		constants[0] = factor;
		constants[1] = factor;
		constants[2] = factor;
		constants[3] = factor;
		pb.add(constants);
		RenderedOp ropOut = JAI.create("addconst", pb);

		WritableRaster raster1 = (WritableRaster) ropOut.getData();
		int rwidth = raster1.getWidth();
		int rheight = raster1.getHeight();
		int rbands = raster1.getNumBands();
		double epc = effectParams[5] * (double) rwidth;
		double epd = effectParams[6] * (double) rheight;
		double epe = (effectParams[7] * 100.0) + 1;
		double epf = (effectParams[8] * 100.0) + 1;
		double epg = effectParams[9];
		System.out.println("intervene " + rwidth + ", " + rheight + ", "
				+ rbands);

		try {
			for (int x = rwidth - 1; x >= (int) epc; x = x - (int) epe) {
				for (int y = rheight - 1; y >= (int) epd; y = y - (int) epf) {
					for (int b = 0; b < rbands; b++) {
						if (b == 0 && effectContext.option1)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
						if (b == 1 && effectContext.option2)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
						if (b == 2 && effectContext.option3)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
						if (b == 3 && effectContext.option4)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
						if (b == 4 && effectContext.option5)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
						if (b == 5 && effectContext.option6)
							raster1.setSample(x, y, b, epg
									* raster1.getSample(x, y, b));
					}
				}
			}
		} catch (Exception e) {
			System.out.println("intervene exception 1" + e.getMessage());
			e.printStackTrace();

		}

		if (pi) {
			frameOut.attachPlanarImage(ropOut);
		} else {
			frameOut.setRenderedOp(ropOut);
		}
		return frameOut;
	}

	private EFrame doMagnitude(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		boolean pi = false;
		if (frameIn.isPlanarImage()) {
			ropIn = (RenderedOp) frameIn.dettachPlanarImage();
			pi = true;
		}

		int width = ropIn.getWidth();
		int height = ropIn.getHeight();

		// Magnitude requires float/double data type - convert if needed
		int dataType = ropIn.getSampleModel().getDataType();
		if (dataType != java.awt.image.DataBuffer.TYPE_FLOAT &&
			dataType != java.awt.image.DataBuffer.TYPE_DOUBLE) {
			ParameterBlock pbFormat = new ParameterBlock();
			pbFormat.addSource(ropIn);
			pbFormat.add(java.awt.image.DataBuffer.TYPE_FLOAT);
			ropIn = JAI.create("format", pbFormat);
		}

		// Magnitude requires even number of bands (real/imaginary pairs)
		// If odd bands, add a zero-filled band to make it even
		int numBands = ropIn.getSampleModel().getNumBands();
		if (numBands % 2 != 0) {
			// Create a constant float image with zero values for the extra band
			Float[] bandValues = new Float[1];
			bandValues[0] = 0.0f;
			ParameterBlock pbConst = new ParameterBlock();
			pbConst.add((float) width);
			pbConst.add((float) height);
			pbConst.add(bandValues);
			RenderedOp zeroBand = JAI.create("constant", pbConst);

			// Merge original image with zero band
			ParameterBlock pbMerge = new ParameterBlock();
			pbMerge.addSource(ropIn);
			pbMerge.addSource(zeroBand);
			ropIn = JAI.create("bandmerge", pbMerge);
		}

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		RenderedOp ropOut = JAI.create("magnitude", pb, null);
		if (effectContext.option1) {
			int owidth = ropOut.getWidth();
			int oheight = ropOut.getHeight();
			int inWidth = frameIn.getWidth();
			int inHeight = frameIn.getHeight();
			pb.addSource(ropOut);
			pb.add((float) inWidth / (float) owidth);
			pb.add((float) inHeight / (float) oheight);
			pb.add(0.0F).add(0.0F);
			Interpolation interp = Interpolation
					.getInstance(Interpolation.INTERP_BILINEAR);
			pb.add(interp);
			BorderExtender extender = BorderExtender
					.createInstance(BorderExtender.BORDER_ZERO);
			RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
					extender);

			ropOut = JAI.create("Scale", pb, hints);
		}
		frameOut.setRenderedOp(ropOut);

		return frameOut;
	}

	private EFrame doFrameMeanFilter(EFrame frameIn) {

		EFrame frameOut = null;

		EFrameSet eFrameWork = null;

		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		int num = effectContext.count + 1;

		System.out.println("framemean " + num + ", " + count);

		if (count == 0) {
			frameOut = new EFrame(frameIn.getBuffer());
			return frameOut;
		}

		if (count < num) {
			num = count;
		}

		for (int i = 0; i < num; i++) {
			if (frameOut == null) {
				if (eFrameWork.get(i) != null) {
					frameOut = doJAIMultiplyConst(1.0 / (double) num,
							eFrameWork.get(i));
				} else {
					frameOut = doJAIMultiplyConst(1.0 / (double) num, eFrameIn
							.get(i));
				}
			} else {
				if (eFrameWork.get(i) != null) {
					frameOut = doJAIAdd(frameOut, doJAIMultiplyConst(
							1.0 / (double) num, eFrameWork.get(i)));
				} else {
					frameOut = doJAIAdd(frameOut, doJAIMultiplyConst(
							1.0 / (double) num, eFrameIn.get(i)));
				}
			}
		}
		return frameOut;
	}

	private EFrame doZebra(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frame0, frame1;
		EFrameSet eFrameWork = null;
		int step = effectContext.step + 1;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		System.out.println("zebra " + count + ", " + step + ", "
				+ effectParams[1] + ", " + effectParams[2]);
		if (count > step) {
			frame0 = eFrameWork.get(step);
			if (frame0 == null)
				return frameOut;
			frameIn = eFrameWork.get(0);
			if (frameIn == null)
				return frameOut;
			if (effectContext.option6) {
				frameOut = doJAIMin(frame0, frameIn);
			} else {
				frameOut = doJAISubtractM(frameIn, frame0);
			}

			//frame1 = doJAIWarp(doJAISubtract(frameIn, frame0));

			if (effectContext.option4) {
				frameOut = doJAIInvert(doJAISubtractM(frameIn, frame0));
			}
			if (effectContext.option1) {
				frameOut = doJAIAdd(frameOut, frameIn);
			}
			if (effectContext.option2) {
				frameOut = doJAIAdd(frameIn, doJAIMin(frameIn, frameOut));
			}
			if (effectContext.option3) {
				frameOut = doJAISubtractM(frameIn, frameOut);
			}
			if (effectContext.option5) {
				frameOut = doJAIMax(frameOut, frameIn);
			}

		}
		return frameOut;
	}

	private EFrame doFramePassFilter() {
		double k = 4.0;
		double dt = 0.1;
		double qfactor = 50.0;
		IIRBandpassFilterDesign bpfd = new IIRBandpassFilterDesign((int) (k),
				(int) (1.0 / dt), qfactor);
		bpfd.doFilterDesign();
		double alpha = bpfd.getAlpha();
		double beta = bpfd.getBeta();
		double gamma = bpfd.getGamma();
		EFrame frame0, frame0a, frame0b, frame0c, frame1, frame2;
		System.out.println("Before filtering a");
		int count = eFrameIn.getCount();
		System.out.println("Before filtering b " + count + ", " + alpha + ", "
				+ beta + ", " + gamma);
		beta = 0.1;
		alpha = 0.2;
		gamma = 0.1;
		double mfactor = 1.0;
		if (count > 2) {
			System.out.println("Processing filters!");
			frame2 = doJAIMultiplyConst(beta, eFrameOut.get(1));
			frame1 = doJAIMultiplyConst(gamma, eFrameOut.get(0));
			frame0a = doJAIMultiplyConst(2.0 * alpha, eFrameIn.get(1));
			frame0b = doJAIMultiplyConst(alpha, eFrameIn.get(0));
			frame0c = doJAIMultiplyConst(alpha, eFrameIn.get(2));
			frame0 = doJAIAdd(frame0a, frame0b);
			frame0 = doJAIAdd(frame0, frame0c);
			frame1 = doJAIAdd(frame1, frame2);
			frame0 = doJAISubtractM(frame0, frame1);
			nextFrame = doJAIMultiplyConst(mfactor, frame0);
			System.out.println("Processed filters!");
		}
		return nextFrame;
	}

	private EFrame doJAIdft(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		int width = ropIn.getWidth();
		int height = ropIn.getHeight();
		System.out.println("dft :" + width + ", " + height);
		RenderedOp ropIn1 = doJAIScaleRop(ropIn.getAsBufferedImage(),
				(float) 256 / (float) width, (float) 128 / (float) height,
				0.0F, 0.0F);
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.add(DFTDescriptor.SCALING_NONE);
		pb.add(DFTDescriptor.REAL_TO_COMPLEX);
		RenderedOp ropOut = JAI.create("dft", pb);
		frameOut.attachPlanarImage(ropOut);
		return frameOut;
	}

	private EFrame doJAIidft(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());

		RenderedOp ropIn = frameIn.getRenderedOp();
		int width = ropIn.getWidth();
		int height = ropIn.getHeight();

		if (frameIn.isPlanarImage()) {
			ropIn = (RenderedOp) frameIn.dettachPlanarImage();
		}
		int swidth = ropIn.getWidth();
		int sheight = ropIn.getHeight();

		// First apply DFT to convert to complex format
		ParameterBlock pbDft = new ParameterBlock();
		pbDft.addSource(ropIn);
		pbDft.add(DFTDescriptor.SCALING_NONE);
		pbDft.add(DFTDescriptor.REAL_TO_COMPLEX);
		RenderedOp ropDft = JAI.create("dft", pbDft, null);

		// Now apply IDFT on the complex data
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropDft);
		pb.add(DFTDescriptor.SCALING_DIMENSIONS);
		pb.add(DFTDescriptor.COMPLEX_TO_REAL);
		RenderedOp ropOut = JAI.create("idft", pb, null);

		BufferedImage image = ropOut.getAsBufferedImage();
		RenderedOp ropOut1 = doJAIScaleRop(image, (float) 160 / (float) swidth,
				(float) 120 / (float) sheight, 0.0F, 0.0F);
		//frameOut.clearBuffer();
		//frameOut.setImage(image);
		frameOut.setRenderedOp(ropOut1);
		return frameOut;
	}

	private EFrame doJAIdctidct(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		RenderedOp ropOut = JAI.create("dct", pb);
		int width = ropOut.getWidth();
		int height = ropOut.getHeight();

		WritableRaster raster1 = (WritableRaster) ropOut.getData();
		int rwidth = raster1.getWidth();
		int rheight = raster1.getHeight();
		int rbands = raster1.getNumBands();
		double epc = effectParams[5] * (double) rwidth;
		double epd = effectParams[6] * (double) rheight;
		double epe = (effectParams[7] * 100.0) + 1;
		double epf = (effectParams[8] * 100.0) + 1;
		double epg = effectParams[9];
		int lowx = (int) (effectParams[1] * (double) rwidth);
		int highx = (int) (effectParams[2] * (double) rwidth);
		int lowy = (int) (effectParams[3] * (double) rheight);
		int highy = (int) (effectParams[4] * (double) rheight);

		try {
			for (int x = rwidth - 1; x >= (int) epc; x = x - (int) epe) {
				for (int y = rheight - 1; y >= (int) epd; y = y - (int) epf) {
					for (int b = 0; b < rbands; b++) {
						if ((b == 0 && effectContext.option1)
								|| (b == 1 && effectContext.option2)
								|| (b == 2 && effectContext.option3)
								|| (b == 3 && effectContext.option4)
								|| (b == 4 && effectContext.option5)
								|| (b == 5 && effectContext.option6)) {
							if (effectContext.mode == 0) {
								raster1.setSample(x, y, b, epg
										* raster1.getSample(x, y, b));
							} else {
								raster1.setSample(x, y, b, (255.0 - epg
										* raster1.getSample(x, y, b)));
							}
							if (x > lowx && x < highx && y > lowy && y < highy) {
								raster1.setSample(x, y, b, 0);
							}

						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("In dftift exception");

		}

		System.out.println("In dct " + rbands + ", " + rwidth + ", " + rheight);
		pb = new ParameterBlock();
		pb.addSource(ropOut);
		RenderedOp ropOut3 = JAI.create("idct", pb);
		frameOut.clearBuffer();
		frameOut.setRenderedOp(ropOut3);
		return frameOut;

	}

	private EFrame doJAIdftidft(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();

		frameOut.clearBuffer();
		RenderedOp ropIn2 = frameOut.getRenderedOp();

		ParameterBlock pb = new ParameterBlock();
		PlanarImage ropOut = null;
		PlanarImage ropOut2 = null;

		if (effectContext.option6) {
			pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(DFTDescriptor.SCALING_NONE);
			pb.add(DFTDescriptor.REAL_TO_COMPLEX);
			ropOut = JAI.create("dft", pb, null);

			pb = new ParameterBlock();
			pb.addSource(ropIn2);
			pb.add(DFTDescriptor.SCALING_NONE);
			pb.add(DFTDescriptor.REAL_TO_COMPLEX);
			ropOut2 = JAI.create("dft", pb, null);
		} else {
			pb = new ParameterBlock();
			pb.addSource(ropIn2);
			pb.add(DFTDescriptor.SCALING_NONE);
			pb.add(DFTDescriptor.REAL_TO_COMPLEX);
			ropOut = JAI.create("dft", pb, null);

			pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(DFTDescriptor.SCALING_NONE);
			pb.add(DFTDescriptor.REAL_TO_COMPLEX);
			ropOut2 = JAI.create("dft", pb, null);

		}

		pb = new ParameterBlock();
		pb.addSource(ropOut);
		RenderedOp ropOut3 = JAI.create("magnitude", pb, null);

		pb = new ParameterBlock();
		pb.addSource(ropOut2);
		RenderedOp ropOut4 = JAI.create("phase", pb, null);

		pb = new ParameterBlock();
		pb.addSource(ropOut3);
		pb.addSource(ropOut4);
		RenderedOp ropOut5 = JAI.create("polartocomplex", pb, null);

		pb = new ParameterBlock();
		pb.addSource(ropOut5);
		RenderedOp ropOut6 = JAI.create("idft", pb);

		frameOut.setRenderedOp(ropOut6);
		return frameOut;
		/*
		 *
		 * int width = ropOut.getWidth(); int height = ropOut.getHeight();
		 * WritableRaster raster1 = (WritableRaster) ropOut.getData(); int
		 * rwidth = raster1.getWidth(); int rheight = raster1.getHeight(); int
		 * rbands = raster1.getNumBands(); double epc =
		 * effectParams[5]*(double)rwidth; double epd =
		 * effectParams[6]*(double)rheight; double epe =
		 * (effectParams[7]*100.0)+1; double epf = (effectParams[8]*100.0)+1;
		 * double epg = effectParams[9];
		 *
		 * try { for (int x = rwidth-1; x >= (int)epc; x=x-(int)epe) { for (int
		 * y = rheight-1; y >= (int)epd; y=y-(int)epf) { for (int b = 0; b <
		 * rbands; b++) { if ((b==0 && effectContext.option1) ||(b==1 &&
		 * effectContext.option2) ||(b==2 && effectContext.option3) ||(b==3 &&
		 * effectContext.option4) ||(b==4 && effectContext.option5) ||(b==5 &&
		 * effectContext.option6)) { if (effectContext.mode ==0){
		 * raster1.setSample(x, y, b, epg*raster1.getSample(x,y,b)); } else {
		 * raster1.setSample(x, y, b, (255.0-epg*raster1.getSample(x,y,b))); } } } } } }
		 * catch (Exception e) { System.out.println("In dct exception");
		 *  } System.out.println("In dftift "+rbands+", "+rwidth+", "+rheight);
		 * pb = new ParameterBlock(); pb.addSource(ropOut); //RenderedOp ropOut3 =
		 * JAI.create("idft", pb); //PlanarImage ropOut3 = JAI.create("phase",
		 * pb, null); RenderedOp ropOut3 = JAI.create("magnitude", pb, null);
		 * frameOut.clearBuffer(); frameOut.setRenderedOp(ropOut3);
		 * //frameOut.setBufferedImage(biOut);
		 *
		 * return frameOut;
		 */
	}

	private EFrame doJAIdct(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		int width = ropIn.getWidth();
		int height = ropIn.getHeight();
		int[] pixels = null;
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		RenderedOp ropOut = JAI.create("dct", pb);
		frameOut.clearBuffer();
		frameOut.attachPlanarImage(ropOut);
		return frameOut;
	}

	private EFrame doJAIidct(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		int width = ropIn.getWidth();
		int height = ropIn.getHeight();

		System.out.println("idct ropout size 1 " + width + ", " + height);

		if (frameIn.isPlanarImage()) {
			System.out.println("idct get planar image");
			ropIn = (RenderedOp) frameIn.dettachPlanarImage();
		}
		int swidth = ropIn.getWidth();
		int sheight = ropIn.getHeight();

		System.out.println("idct ropout size 2 " + swidth + ", " + sheight);

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		RenderedOp ropOut = JAI.create("idct", pb);
		BufferedImage image = ropOut.getAsBufferedImage();
		image = (BufferedImage) doJAIScale(image, (float) width
				/ (float) swidth, (float) height / (float) sheight, 0.0F, 0.0F);
		frameOut.clearBuffer();
		//frameOut.setImage(image);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	float edgeMatrix[] = { -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
			24.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f };

	float edgeMatrix1[] = { 0.0f, -1.0f, 0.0f, -1.0f, 10.0f, -1.0f, 0.0f,
			-1.0f, 0.0f, };

	float edgeMatrix2[] = { 0.0f, -1.0f, 0.0f, -1.0f, 5.0f, -1.0f, 0.0f, -1.0f,
			0.0f, };

	float[] embossMatrix =
	//{ -5.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 5.0F };
	{ -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F };

	float[] freichen_h_data = { 1.0F, 0.0F, -1.0F, 1.414F, 0.0F, -1.414F, 1.0F,
			0.0F, -1.0F };

	float[] freichen_v_data = { -1.0F, -1.414F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F,
			1.414F, 1.0F };

	float[] prewitt_h_data = { 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F,
			0.0F, -1.0F };

	float[] prewitt_v_data = { -1.0F, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F,
			1.0F, 1.0F };

	float[] roberts_h_data = { 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
			0.0F };

	float[] roberts_v_data = { -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
			0.0F };

	float[][] robertsMatrix = { roberts_h_data, roberts_v_data };

	float[][] freichenMatrix = { freichen_h_data, freichen_v_data };

	float[][] prewittMatrix = { prewitt_h_data, prewitt_v_data };

	float[] normalData = { 1.0F };

	float[] blurData = { 0.0F, 1.0F / 8.0F, 0.0F, 1.0F / 8.0F, 4.0F / 8.0F,
			1.0F / 8.0F, 0.0F, 1.0F / 8.0F, 0.0F };

	float[] blurMoreData = { 1.0F / 14.0F, 2.0F / 14.0F, 1.0F / 14.0F,
			2.0F / 14.0F, 2.0F / 14.0F, 2.0F / 14.0F, 1.0F / 14.0F,
			2.0F / 14.0F, 1.0F / 14.0F };

	float[] sharpenData = { 0.0F, -1.0F / 4.0F, 0.0F, -1.0F / 4.0F,
			8.0F / 4.0F, -1.0F / 4.0F, 0.0F, -1.0F / 4.0F, 0.0F };

	float[] sharpenMoreData = { -1.0F / 4.0F, -1.0F / 4.0F, -1.0F / 4.0F,
			-1.0F / 4.0F, 12.0F / 4.0F, -1.0F / 4.0F, -1.0F / 4.0F,
			-1.0F / 4.0F, -1.0F / 4.0F };

	float[] laplaceMatrix = { 1.0F, -2.0F, 1.0F, -2.0F, 5.0F, -2.0F, 1.0F,
			-2.0F, 1.0F };

	float[] edgeData = { 0.0F, -1.0F, 0.0F, -1.0F, 4.0F, -1.0F, 0.0F, -1.0F,
			0.0F };

	float[] embossData = { -5.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
			5.0F };

	float[] cellData = { 0.5F, 1.0F, 0.5F, 1.0F, 0.0F, 1.0F, 0.5F, 1.0F, 0.5F };

	float[] timeConvolve1 = { -1.0F / 4.0F, -1.0F / 4.0F, -1.0F / 4.0F, 0.0F,
			10.0F / 4.0F, 0.0F, -1.0F / 4.0F, -1.0F / 4.0F, -1.0F / 4.0F };

	float[] timePrewitt = { -1.0F, -1.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 1.0F,
			1.0F };

	float[] timeBlur = { 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F };

	float[] timeMean = { 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F };

	float[] dilationMatrix = { 0.0F, 25.0F, 0.0F, 25.0F, 50.0F, 25.0F, 0.0F,
			25.0F, 0.0F };

	float[] erosionMatrix = { 0.0F, 25.0F, 0.0F, 25.0F, 50.0F, 25.0F, 0.0F,
			25.0F, 0.0F };

	float[] tophatMatrix = { 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F,
			0.0F };

	float[][] openingMatrix = { erosionMatrix, dilationMatrix };

	float[][] closingMatrix = { erosionMatrix, dilationMatrix };

	float[][] morphGradientMatrix = { erosionMatrix, dilationMatrix };

	float[][] WTHMatrix = { tophatMatrix, tophatMatrix };

	float[][] BTHMatrix = { tophatMatrix, tophatMatrix };

	// Invert center band.
	double[][] bcMatrix1 = { { 1.0D, 0.0D, 0.0D, 0.0D },
			{ 0.0D, -1.0D, 0.0D, 255.0D }, { 0.0D, 0.0D, 1.0D, 0.0D }, };

	// Identity.
	double[][] bcMatrix2 = { { 1.0D, 0.0D, 0.0D, 0.0D },
			{ 0.0D, 1.0D, 0.0D, 0.0D }, { 0.0D, 0.0D, 1.0D, 0.0D }, };

	// Luminance stored into red band (3 band).
	double[][] bcMatrixr = { { .114D, 0.587D, 0.299D, 0.0D },
			{ .000D, 0.000D, 0.000D, 0.0D }, { .000D, 0.000D, 0.000D, 0.0D } };

	// Luminance stored into blue band (3 band).
	double[][] bcMatrixg = { { .000D, 0.000D, 0.000D, 0.0D },
			{ .114D, 0.587D, 0.299D, 0.0D }, { .000D, 0.000D, 0.000D, 0.0D } };

	// Luminance stored into green band (3 band).
	double[][] bcMatrixb = { { .000D, 0.000D, 0.000D, 0.0D },
			{ .000D, 0.000D, 0.000D, 0.0D }, { .114D, 0.587D, 0.299D, 0.0D } };

	// Luminance stored into red band (3 band).
	double[][] bcMatrix3 = { { .114D, 0.587D, 0.299D, 0.0D },
			{ .000D, 0.000D, 0.000D, 0.0D }, { .000D, 0.000D, 0.000D, 0.0D } };

	// Luminance (single band output).
	double[][] bcMatrix4 = { { .114D, 0.587D, 0.299D, 0.0D } };

	// Colour to B&W.
	double[][] bcMatrix5 = { { .114D, 0.587D, 0.299D, 0.0D },
			{ .114D, 0.587D, 0.299D, 0.0D }, { .114D, 0.587D, 0.299D, 0.0D } };

	private KernelJAI makeGaussianKernel(int radius) {
		int diameter = 2 * radius + 1;
		float invrsq = 1.0F / (radius * radius);

		float[] gaussianData = new float[diameter];

		float sum = 0.0F;
		for (int i = 0; i < diameter; i++) {
			float d = i - radius;
			float val = (float) Math.exp(-d * d * invrsq);
			gaussianData[i] = val;
			sum += val;
		}

		// Normalize
		float invsum = 1.0F / sum;
		for (int i = 0; i < diameter; i++) {
			gaussianData[i] *= invsum;
		}

		return new KernelJAI(diameter, diameter, radius, radius, gaussianData,
				gaussianData);
	}

	private float[] makeMarrHill(double factor, int size) {

		float[] matrix = new float[size * size];
		int cx = 0, cy = 0, nx = 0, ny = 0;
		cx = (size - 1) / 2;
		cy = (size - 1) / 2;
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				nx = x - cx;
				ny = y - cy;
				matrix[x + y * size] = (float) (1.0F / (float) (factor * factor))
						* (((float) (nx * nx + ny * ny) / (float) (factor * factor)) - 2.0F)
						* (float) Math
								.exp(-1.0F
										* ((float) (nx * nx + ny * ny) / (2.0 * (float) (factor * factor))));
			}
		}
		return matrix;
	}

	private EFrame doJAIBandCombine(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[][] matrix = bcMatrix1;

		if (convolve instanceof double[][]) {
			matrix = (double[][]) convolve;
		}
		pb.add(matrix);
		RenderedOp ropOut = JAI.create("bandcombine", pb, null);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doDummy(EFrame frameIn, EFrame frameIn2) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		return frameOut;

	}

	private EFrame doJAIColorConvert(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		pb.add(cs);
		RenderedOp ropOut = JAI.create("ColorConvert", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIThreshold(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		double[] low, high, map;
		int bands = 3;
		double ht = effectParams[2] * 255.0;
		double lt = effectParams[1] * 255.0;
		double mt = effectParams[3] * 255.0;
		low = new double[bands];
		high = new double[bands];
		map = new double[bands];
		for (int i = 0; i < bands; i++) {
			low[i] = lt;
			high[i] = ht;
			map[i] = mt;
		}
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		pb.add(low);
		pb.add(high);
		pb.add(map);
		RenderedOp ropOut = JAI.create("threshold", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAILaplaceFilter(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		System.out.println(effectContext.convolve);
		float[] matrix = laplaceMatrix;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}
		KernelJAI kernel = new KernelJAI(3, 3, 1, 1, matrix);
		pb.add(kernel);
		RenderedOp ropOut = JAI.create("convolve", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doJAIWarp(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();

		// Create the transform parameter (WarpAffine).
		float m00 =(float) effectParams[1];
		float m10 = (float) effectParams[2];
		float m01 = (float) effectParams[3];
		float m11 = (float) effectParams[4];
		float m02 = (float) effectParams[5];
		float m12 = (float) effectParams[6];
		float m20 = (float) effectParams[7];
		float m21 = (float) effectParams[8];
		float m22 = (float) effectParams[9];

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();


		if (effectContext.option1) {
			m00 = (float) 0.0 - m00;
		}
		if (effectContext.option2) {
			m10 = (float) 0.0 - m10;
		}
		if (effectContext.option3) {
			m01 = (float) 0.0 - m01;
		}
		if (effectContext.option4) {
			m11 = (float) 0.0 - m11;
		}
		if (effectContext.option5) {
			m02 = (float) 0.0 - m02;
		}
		if (effectContext.option6) {
			m12 = (float) 0.0 - m12;
		}

		if (effectContext.mode == 0) {

			AffineTransform transform = new AffineTransform(m00, m10, m01, m11,
					m02, m12);
			Warp warp = new WarpAffine(transform);

			// Create the interpolation parameter.
			Interpolation interp = new InterpolationNearest();

			// Create the ParameterBlock.
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(warp);
			pb.add(interp);

			RenderedOp ropOut = JAI.create("warp", pb);
			frameOut.setRenderedOp(ropOut);
			return frameOut;

		} else if (effectContext.mode == 1) {

			float[] f1 = new float[]{90,153,132,142,138,99,32,138,174,117,137,164};
			float[] f2 = new float[]{96,180,165,139,148,70,24,114,248,97,146,186};
			float[] f3 = new float[12];
			float[] f4 = new float[12];
			float[] f5 = new float[]{107,152,59,99,158,154,103,75,41,159,91,114};
			float[] f6 = new float[]{157,214,48,61,63,187,134,120,291,97,32,125};
			float[] f7 = new float[]{233, 215, 108, 145, 84, 216, 54, 157, 17, 233, 137, 175};
			float[] f8 = new float[]{33, 59, 92, 165, 298, 202, 249, 14, 302, 47, 96, 134};
		
			
			for (int i = 0; i<12;i++){
				
				f3[i] = f7[i];
				f4[i] = f8[i];
				if (i==effectContext.count+1){
					f3[i]*=m00/m10;
					f4[i]*=m01/m11;
				}
				if (i==effectContext.step+1){
					f3[i]*=m02/m12;
					f4[i]*=m20/m21;
				}
				if (i==0){
					f3[i]*=m22;
					
				}
					
			}
				
			Warp warp = WarpPolynomial.createWarp(f3,0,
					f4,0,12,
					1f/width,1f/height,width,height,
					2);

			//Warp warp = new WarpQuadratic(f3,f4);
			System.out.println("Warp quad2");

			// Create the interpolation parameter.
			Interpolation interp = new InterpolationNearest();

			// Create the ParameterBlock.
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(warp);
			pb.add(interp);

			RenderedOp ropOut = JAI.create("warp", pb);
			frameOut.setRenderedOp(ropOut);
			return frameOut;

		} else if (effectContext.mode == 2) {

			PerspectiveTransform transform = PerspectiveTransform
					.getSquareToQuad(m00, m10, m01, m11, m02, m12, m20, m21);
			Warp warp = new WarpPerspective(transform);

			// Create the interpolation parameter.
			Interpolation interp = new InterpolationNearest();

			// Create the ParameterBlock.
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(warp);
			pb.add(interp);

			RenderedOp ropOut = JAI.create("warp", pb);
			frameOut.setRenderedOp(ropOut);
			return frameOut;

		}

		return frameOut;

	}

	private EFrame doMarrHill(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		double factor = effectParams[1] / effectParams[2];
		int size = (int) (effectParams[3] * 100.0);
		System.out.println("Marr Hill");
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		KernelJAI kernel = new KernelJAI(size, size, makeMarrHill(factor, size));
		pb.add(kernel);
		RenderedOp ropOut = JAI.create("convolve", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doGaussian(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		int radius = (int) (effectParams[1] * 255.0);
		KernelJAI kernel = makeGaussianKernel(radius);
		pb.add(kernel);
		RenderedOp ropOut = JAI.create("convolve", pb);
		frameOut.setRenderedOp(ropOut);
		if (effectContext.option1) {
			frameOut = doFillEdge(frameOut);
		}
		if (effectContext.option2) {
			frameOut = doFillEdge2(frameOut);
		}
		return frameOut;

	}

	private EFrame doJAISharpenFilter(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		float[] matrix = sharpenMoreData;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		KernelJAI kernel = new KernelJAI(3, 3, 1, 1, matrix);
		pb.add(kernel);
		RenderedOp ropOut = JAI.create("convolve", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIEmboss(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		float[] matrix = embossData;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		KernelJAI kernel = new KernelJAI(3, 3, 1, 1, matrix);
		pb.add(kernel);
		RenderedOp ropOut = JAI.create("convolve", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIRobertsEdge(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		float[][] matrix = robertsMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}
		pb.addSource(ropIn);
		KernelJAI kern_h = new KernelJAI(3, 3, matrix[0]);
		KernelJAI kern_v = new KernelJAI(3, 3, matrix[1]);
		pb.add(kern_h);
		pb.add(kern_v);
		RenderedOp ropOut = JAI.create("gradientmagnitude", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIFreichenEdge(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		float[][] matrix = freichenMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		KernelJAI kern_h = new KernelJAI(3, 3, matrix[0]);
		KernelJAI kern_v = new KernelJAI(3, 3, matrix[1]);
		pb.add(kern_h);
		pb.add(kern_v);
		RenderedOp ropOut = JAI.create("gradientmagnitude", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIPrewittEdge(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		float[][] matrix = prewittMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		KernelJAI kern_h = new KernelJAI(3, 3, matrix[0]);
		KernelJAI kern_v = new KernelJAI(3, 3, matrix[1]);
		pb.add(kern_h);
		pb.add(kern_v);
		RenderedOp ropOut = JAI.create("gradientmagnitude", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAISobelEdge(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		pb.add(KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL);
		pb.add(KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);
		RenderedOp ropOut = JAI.create("gradientmagnitude", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIMedian(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		int size = (int) (effectParams[1] * 100.0);
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		if (effectContext.count == 0) {
			pb.add(MedianFilterDescriptor.MEDIAN_MASK_SQUARE);
		} else if (effectContext.count == 1) {
			pb.add(MedianFilterDescriptor.MEDIAN_MASK_PLUS);
		} else if (effectContext.count == 2) {
			pb.add(MedianFilterDescriptor.MEDIAN_MASK_X);
		} else {
			pb.add(MedianFilterDescriptor.MEDIAN_MASK_SQUARE_SEPARABLE);
		}
		pb.add(size);
		RenderedOp ropOut = JAI.create("medianfilter", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIMultiplyConst(double factor, EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[] constants = new double[3];
		constants[0] = factor;
		constants[1] = factor;
		constants[2] = factor;
		pb.add(constants);
		RenderedOp ropOut = JAI.create("multiplyconst", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIMultiplyConst(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[] constants = new double[3];
		constants[0] = effectParams[1] / effectParams[2];
		constants[1] = effectParams[3] / effectParams[4];
		constants[2] = effectParams[5] / effectParams[6];
		pb.add(constants);
		RenderedOp ropOut = JAI.create("multiplyconst", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doJAIDivideConst(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[] constants = new double[3];
		constants[0] = effectParams[1] / effectParams[2];
		constants[1] = effectParams[3] / effectParams[4];
		constants[2] = effectParams[5] / effectParams[6];
		pb.add(constants);
		RenderedOp ropOut = JAI.create("divideconst", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doJAIAddConst(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[] constants = new double[3];
		constants[0] = effectParams[1] * 255.0;
		constants[1] = effectParams[2] * 255.0;
		constants[2] = effectParams[3] * 255.0;
		pb.add(constants);
		RenderedOp ropOut = JAI.create("addconst", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doJAISubtractConst(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);
		double[] constants = new double[3];
		constants[0] = effectParams[1] * 255.0;
		constants[1] = effectParams[2] * 255.0;
		constants[2] = effectParams[3] * 255.0;
		pb.add(constants);
		RenderedOp ropOut = JAI.create("subtractconst", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;
	}

	private EFrame doJAIAdd(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		RenderedOp ropIn1 = frameIn1.getRenderedOp();
		RenderedOp ropIn2 = frameIn2.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.addSource(ropIn2);
		System.out.println("In add");
		RenderedOp ropOut = JAI.create("add", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAISubtract(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		RenderedOp ropIn1 = frameIn1.getRenderedOp();
		RenderedOp ropIn2 = frameIn2.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.addSource(ropIn2);
		RenderedOp ropOut = JAI.create("subtract", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doMergeFrame(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = doJAIAdd(
				doJAIMultiplyConst(effectParams[0], frameIn1),
				doJAIMultiplyConst(1.0 - effectParams[0], frameIn2));
		return frameOut;

	}

	private EFrame doJAISubtractM(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = doJAISubtract(frameIn1, frameIn2);
		EFrame frameOut2 = doJAISubtract(frameIn2, frameIn1);
		frameOut = doJAIAdd(frameOut, frameOut2);
		return frameOut;

	}

	private EFrame doJAIMultiply(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		RenderedOp ropIn1 = frameIn1.getRenderedOp();
		RenderedOp ropIn2 = frameIn2.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.addSource(ropIn2);
		RenderedOp ropOut = JAI.create("multiply", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIMax(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		RenderedOp ropIn1 = frameIn1.getRenderedOp();
		RenderedOp ropIn2 = frameIn2.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.addSource(ropIn2);
		RenderedOp ropOut = JAI.create("max", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doJAIMin(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		RenderedOp ropIn1 = frameIn1.getRenderedOp();
		RenderedOp ropIn2 = frameIn2.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn1);
		pb.addSource(ropIn2);
		RenderedOp ropOut = JAI.create("min", pb);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doTimeConvolve(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		float[] mask = timeConvolve1;
		if (convolve instanceof float[]) {
			mask = (float[]) convolve;
		}
		
		EFrameSet eFrameWork = null;

		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		}
		if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		}
		if (effectContext.mode == 2) {
			eFrameWork = eFrameOut;
		}

		int count = eFrameWork.getCount();
		if (count < 3)
			return frameOut;

		EFrame frameIn0 = eFrameWork.get(0);
		EFrame frameIn1 = eFrameWork.get(1);
		EFrame frameIn2 = eFrameWork.get(2);

		if (frameIn1 == null || frameIn2 == null)
			return frameOut;
		int width = frameIn0.getWidth();
		int height = frameIn0.getHeight();
		int numBands = frameIn0.getPixelStride();
		double[][][] buffer = new double[width][height][numBands];
			
		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double z = 0;
		double sum = 0;
		for (int m = 0; m < mask.length; m++) {
			if ((double) mask[m] != 0.0) {
				sum += (double) mask[m];
			}
		}

		int my = 0;
		for (int y = maskOffset; y < height - maskOffset; y++) {
			for (int x = maskOffset; x < width - maskOffset; x++) {
				for (int band = 0; band < numBands; band++) {
					for (int mx = 0; mx < maskSize; mx++) {
						my = 0;
						z = 0;
						if (mask[mx + (my * maskSize)] != 0.0) {
							z += frameIn0.getPixelDouble(band, x + mx
									- maskOffset, y)
									* (double) mask[mx + (my * maskSize)];
							z += frameIn0.getPixelDouble(band, x, y + mx
									- maskOffset)
									* (double) mask[mx + (my * maskSize)];

						}
						my = 1;
						if (mask[mx + (my * maskSize)] != 0.0) {
							z += frameIn1.getPixelDouble(band, x + mx
									- maskOffset, y)
									* (double) mask[mx + (my * maskSize)];
							z += frameIn1.getPixelDouble(band, x, y + mx
									- maskOffset)
									* (double) mask[mx + (my * maskSize)];

						}
						my = 2;
						if (mask[mx + (my * maskSize)] != 0.0) {
							z += frameIn2.getPixelDouble(band, x + mx
									- maskOffset, y)
									* (double) mask[mx + (my * maskSize)];
							z += frameIn2.getPixelDouble(band, x, y + mx
									- maskOffset)
									* (double) mask[mx + (my * maskSize)];

						}

					}
					buffer[x][y][band] = z;
					/*
					if (effectContext.option1) {
						z = z / 2.0;
						if (sum == 0.0)
							z = ((255.0 / 2.0) + (z / 2.0));
					}
					if (sum > 1.0)
						z = z / sum;
					if (sum < 0.0)
						z = 255.0 - (z / sum);
						*/
					//frameOut.setPixel(band, x, y, z);
					
				}
			}
		}
		
		normalise(buffer);
	
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					z = buffer[x][y][band] ;
					frameOut.setPixel(band, x, y, 255-z);
				}
			}
		}
		
		return frameOut;
	}

	private EFrame doConvolve(EFrame frameIn) {

		float[] mask = embossData;
		if (convolve instanceof float[]) {
			mask = (float[]) convolve;
		}
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double z = 0;
		int sum = 0;
		for (int m = 0; m < mask.length; m++) {
			if ((int) mask[m] != 0) {
				sum += (int) mask[m];
			}
		}

		for (int y = maskOffset; y < height - maskOffset; y++) {
			for (int x = maskOffset; x < width - maskOffset; x++) {
				for (int band = 0; band < numBands; band++) {
					z = 0;
					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							if (mask[mx + (my * maskSize)] != 0.0) {
								z += frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset)
										* (double) mask[mx + (my * maskSize)];
							}
						}
					}
					if (effectContext.option1) {
						z = z / 2.0;
						if (sum == 0.0)
							z = ((255.0 / 2.0) + (z / 2.0));
					}

					if (effectContext.option2) {

						if (sum == 0)
							z = ((255.0 / 2.0) + (z / 2.0));
					}
					if (sum > 1.0)
						z = z / sum;
					if (sum < 0)
						z = 255 - (z / sum);
					frameOut.setPixel(band, x, y, z);
				}
			}
		}
		return frameOut;
	}

	private EFrame doErrorDiffusion(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int w = frameIn.getWidth() - 1;
		int h = frameIn.getHeight() - 1;
		int numBands = frameIn.getPixelStride();
		double value = 0;
		double error = 0;
		double thres = effectParams[1] * 255.0;

		for (int y = 1; y < h; y++) {
			for (int x = 1; x < w; x++) {
				for (int band = 0; band < numBands; band++) {
					value = frameIn.getPixelDouble(band, x, y);
					if (value < thres) {
						frameOut.setPixel(band, x, y, 0);
						error = value;
					} else {

						frameOut.setPixel(band, x, y, 255);
						error = value - 255.0;

					}

					value = frameIn.getPixelDouble(band, x + 1, y);
					frameIn.setPixel(band, x + 1, y, clamp(value + 0.4375
							* error));
					value = frameIn.getPixelDouble(band, x - 1, y + 1);
					frameIn.setPixel(band, x - 1, y + 1, clamp(value + 0.1875
							* error));
					value = frameIn.getPixelDouble(band, x, y + 1);
					frameIn.setPixel(band, x, y + 1, clamp(value + 0.3125
							* error));
					value = frameIn.getPixelDouble(band, x + 1, y + 1);
					frameIn.setPixel(band, x + 1, y + 1, clamp(value + 0.0625
							* error));

				}
			}
		}
		return frameOut;
	}

	public static double clamp(double value) {

		return Math.min(Math.max(Math.round(value), 0.0), 255.0);
	}

	private EFrame doBrownian(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int maskSize = (int) (effectParams[1] * 255.0);
		int maskOffset = (maskSize - 1) / 2;
		ArrayList s = null;
		Double dObject = null;
		for (int y = maskOffset; y < height - maskOffset; y++) {
			for (int x = maskOffset; x < width - maskOffset; x++) {
				for (int band = 0; band < numBands; band++) {
					s = new ArrayList();
					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							dObject = new Double(frameIn.getPixelDouble(band, x
									+ mx - maskOffset, y + my - maskOffset));
							s.add(dObject);
						}
					}
					Collections.shuffle(s);
					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							dObject = (Double) s.remove(0);
							frameOut
									.setPixel(band, x, y, dObject.doubleValue());
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doEqualise(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double a, b, c, d, e, f, g, h, i;
		int count;
		double[] sr = null;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					count = 0;
					sr = getSampleRegion33(frameIn, band, x, y);
					if (sr[0] < sr[4])
						count++;
					if (sr[1] < sr[4])
						count++;
					if (sr[2] < sr[4])
						count++;
					if (sr[3] < sr[4])
						count++;
					if (sr[5] < sr[4])
						count++;
					if (sr[6] < sr[4])
						count++;
					if (sr[7] < sr[4])
						count++;
					if (sr[8] < sr[4])
						count++;
					if (effectContext.option1) {
						frameOut.setPixel(band, x, y,
								255.0 - ((double) count) * 255.0 / 8.0);
					} else {
						frameOut.setPixel(band, x, y,
								((double) count) * 255.0 / 8.0);
					}

				}
			}
		}
		return frameOut;

	}

	private EFrame doHistogram(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int shades = (int) (effectParams[1] * 256);
		int[][] histogram = new int[numBands][shades];
		int[][] LUT = new int[numBands][shades];
		int count, x, y, band, i;

		double[] bandMax = new double[numBands];

		for (band = 0; band < numBands; band++) {
			bandMax[band] = 0.0;
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					histogram[band][frameIn.getPixelInt(band, x, y)]++;
				}
			}
		}

		for (i = 1; i < shades; i++) {
			for (band = 0; band < numBands; band++) {
				histogram[band][i] += histogram[band][i - 1];
			}
		}

		int[] pixels = new int[numBands];
		for (band = 0; band < numBands; band++) {
			pixels[band] = histogram[band][shades - 1];
		}

		for (band = 0; band < numBands; band++) {
			if (pixels[band] != 0) {
				for (i = 0; i < shades; i++) {
					LUT[band][i] = (int) ((histogram[band][i] * (shades - 1)) / pixels[band]);
				}
			}
		}

		for (y = 1; y < height - 1; y++) {
			for (x = 1; x < width - 1; x++) {
				for (band = 0; band < numBands; band++) {
					double level = (double) LUT[band][(int) Math
							.floor(((double) shades / 256.0)
									* frameIn.getPixelDouble(band, x, y))];
					if (level > bandMax[band])
						bandMax[band] = level;
				}
			}
		}

		for (y = 1; y < height - 1; y++) {
			for (x = 1; x < width - 1; x++) {
				for (band = 0; band < numBands; band++) {
					frameOut
							.setPixel(
									band,
									x,
									y,
									255.0 * (((double) LUT[band][(int) Math
											.floor(((double) shades / 256.0)
													* frameIn.getPixelDouble(
															band, x, y))]) / bandMax[band]));
				}
			}
		}
		return frameOut;
	}

	private EFrame doZerox(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int shades = 256;
		double a, b, c, d, e, f, g, h, i;
		int count;
		double M = effectParams[1] * 255.0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					count = 0;
					a = frameIn.getPixelDouble(band, x - 1, y - 1);
					b = frameIn.getPixelDouble(band, x, y - 1);
					c = frameIn.getPixelDouble(band, x + 1, y - 1);
					d = frameIn.getPixelDouble(band, x - 1, y);
					e = frameIn.getPixelDouble(band, x, y);
					f = frameIn.getPixelDouble(band, x + 1, y);
					g = frameIn.getPixelDouble(band, x - 1, y + 1);
					h = frameIn.getPixelDouble(band, x, y + 1);
					i = frameIn.getPixelDouble(band, x + 1, y + 1);
					if (((d < M) && (f > M) && (e < f) && (e > d))
							|| ((b < M) && (b > M) && (e < h) && (e > b))
							|| ((d > M) && (f < M) && (e > f) && (e < d))
							|| ((b > M) && (h < M) && (e > h) && (e < b)))
						frameOut.setPixel(band, x, y, 255);
					else
						frameOut.setPixel(band, x, y, 0);

				}
			}
		}
		return frameOut;

	}

	private EFrame doShade(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int a = 0, b = 0, c = 0;
		double re = 0, gr = 0, bl = 0, h, s, i;

		int x = 0, y = 0, band = 0;
		int[] thres = new int[numBands];
		int[] oldShades = new int[numBands];
		int[] newShades = new int[numBands];
		int x1 = (int) (effectParams[4] * 255.0);
		int y1 = (int) (effectParams[5] * 255.0);
		int x2 = (int) (effectParams[7] * 255.0);
		int y2 = (int) (effectParams[8] * 255.0);
		RGBPixel rgb = null;

		for (band = 0; band < numBands; band++) {
			thres[band] = (int) (effectParams[band + 1] * 255.0);

			if (effectContext.option3) {
				oldShades[band] = frameOut.getPixelInt(band, x1, y1);
			} else {
				oldShades[band] = (int) (effectParams[band + 4] * 255.0);
			}

			if (effectContext.option4) {
				newShades[band] = frameOut.getPixelInt(band, x2, y2);
			} else {
				newShades[band] = (int) (effectParams[band + 7] * 255.0);
			}
		}

		for (y = 1; y < height; y++) {
			for (x = 1; x < width; x++) {
				if (effectContext.option2) {
					a = (int) frameOut.getPixel(0, x, y);
					b = (int) frameOut.getPixel(1, x, y);
					c = (int) frameOut.getPixel(2, x, y);
					if (effectContext.option5) {

						re = frameIn.getPixelDouble(EFrame.RED, x, y);
						bl = frameIn.getPixelDouble(EFrame.BLUE, x, y);
						gr = frameIn.getPixelDouble(EFrame.GREEN, x, y);

						h = getHue(re, gr, bl);
						s = getSaturation(re, gr, bl);
						i = getIntensity(re, gr, bl);

						if (Math.abs(h - oldShades[0]) <= thres[0]) {
							if (!effectContext.option6) {

								frameOut.setPixel(0, x, y, newShades[0]);
								frameOut.setPixel(1, x, y, newShades[1]);
								frameOut.setPixel(2, x, y, newShades[2]);
							} else {

								rgb = getRGBFromHSI(newShades[0], s, i);
								re = rgb.r;
								gr = rgb.g;
								bl = rgb.b;

								frameOut.setPixel(0, x, y, bl);
								frameOut.setPixel(1, x, y, gr);
								frameOut.setPixel(2, x, y, re);

							}
						}

					} else {

						if (Math.abs(a - oldShades[0]) <= thres[0]
								&& Math.abs(b - oldShades[1]) <= thres[1]
								&& Math.abs(c - oldShades[2]) <= thres[2]) {
							frameOut.setPixel(0, x, y, newShades[0]);
							frameOut.setPixel(1, x, y, newShades[1]);
							frameOut.setPixel(2, x, y, newShades[2]);
						}
					}

				} else {
					for (band = 0; band < numBands; band++) {
						a = (int) frameOut.getPixel(band, x, y);
						if (effectContext.option1) {
							if (Math.abs(a - oldShades[band]) > thres[band]) {
								frameOut.setPixel(band, x, y, newShades[band]);
							}
						} else {

							if (Math.abs(a - oldShades[band]) <= thres[band]) {
								frameOut.setPixel(band, x, y, newShades[band]);
							}
						}
					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doFill(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameSave = new EFrame(frameIn.getBuffer());
		frameSave.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int a = 0;
		int startValue = 0;
		int margin = 0;
		int x = 0, y = 0, band = 0;
		double[] thres = new double[numBands];
		int[] shades = new int[numBands];

		for (band = 0; band < numBands; band++) {
			thres[band] = effectParams[band + 1] * 255.0;
		}
		int max = (int) (effectParams[4] * 100.0);
		if (effectContext.option4) {
			max *= 10;
		}

		if (effectContext.option5) {
			max *= 10;
		}

		if (effectContext.option6) {
			max *= 10;
		}

		double startx = effectParams[5];
		double starty = effectParams[6];
		for (band = 0; band < numBands; band++) {
			shades[band] = (int) (effectParams[band + 7] * 255.0);
		}
		int count = effectContext.count + 1;
		int fillband = -1;
		boolean start = true;
		boolean fillflag = false;
		for (y = 1; y < height - 1; y++) {
			if (start)
				y = (int) ((double) height * starty);
			for (x = 1; x < width - 1; x++) {
				if (start)
					x = (int) ((double) width * startx);
				if (start)
					startValue = a;
				a = (int) frameIn.getPixelGrey(x, y);
				for (band = 0; band < numBands; band++) {
					if (effectContext.option1) {
						a = (int) frameIn.getPixel(band, x, y);
					}
					if (effectContext.option2) {
						fillband++;
						if (fillband > 2)
							fillband = 0;
					} else {
						fillband = band;
					}

					if (a < (int) thres[band]) {
						fillflag = true;
						if (effectContext.option3) {
							floodfill(frameIn, frameOut, frameSave, band, x, y,
									shades[band], (int) thres[band], fillband,
									0, max);

						} else {
							boundaryfill(frameIn, frameOut, frameSave, band, x,
									y, shades[band], (int) thres[band],
									fillband, 0, max);

						}
					}
				}
				start = false;
				if (fillflag)
					count--;
				if (count <= 0)
					break;
				fillflag = false;
			}
			if (count <= 0)
				break;

		}

		return frameOut;

	}

	private void floodfill(EFrame frameIn, EFrame frame, EFrame frameSave,
			int b, int x, int y, int fill, int old, int fillband, int count,
			int max) {
		try {
			int width = frame.getWidth();
			int height = frame.getHeight();
			java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
			stack.push(new int[]{x, y});
			int fillCount = count;

			while (!stack.isEmpty() && fillCount < max) {
				int[] point = stack.pop();
				int px = point[0];
				int py = point[1];

				if (px < 0 || px >= width || py < 0 || py >= height)
					continue;
				if (frameSave.getPixelInt(b, px, py) == 255)
					continue;

				int level = 0;
				if (effectContext.option1) {
					level = frameIn.getPixelInt(b, px, py);
				} else {
					level = (int) frameIn.getPixelGrey(px, py);
				}

				if (level >= old)
					continue;

				frameSave.setPixel(b, px, py, 255);
				frame.setPixel(fillband, px, py, fill);
				fillCount++;

				stack.push(new int[]{px + 1, py});
				stack.push(new int[]{px - 1, py});
				stack.push(new int[]{px, py + 1});
				stack.push(new int[]{px, py - 1});
			}
		} catch (Throwable t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
		}

	}

	private void boundaryfill(EFrame frameIn, EFrame frame, EFrame frameSave,
			int b, int x, int y, int fill, int boundary, int fillband,
			int count, int max) {
		try {
			int width = frame.getWidth();
			int height = frame.getHeight();
			java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
			stack.push(new int[]{x, y});
			int fillCount = count;

			while (!stack.isEmpty() && fillCount < max) {
				int[] point = stack.pop();
				int px = point[0];
				int py = point[1];

				if (px < 0 || px >= width || py < 0 || py >= height)
					continue;
				if (frameSave.getPixelInt(b, px, py) == fill)
					continue;

				int level = 0;
				if (effectContext.option1) {
					level = frameIn.getPixelInt(b, px, py);
				} else {
					level = (int) frameIn.getPixelGrey(px, py);
				}

				if (level >= boundary)
					continue;

				frameSave.setPixel(b, px, py, fill);
				frame.setPixel(fillband, px, py, fill);
				fillCount++;

				stack.push(new int[]{px + 1, py});
				stack.push(new int[]{px - 1, py});
				stack.push(new int[]{px, py + 1});
				stack.push(new int[]{px, py - 1});
			}
		} catch (Throwable t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
		}

	}

	private EFrame doWalkLine(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());
		frameOut1.clearBuffer();
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int a = 0, b = 0;
		int x = 0, y = 0, band = 0;
		frameOut.clearBuffer();
		ArrayList pathList = new ArrayList();
		GeneralPath path = null;
		frameOut1.clearBuffer();
		BufferedImage bimg = frameOut1.getBufferedImage();
		Graphics2D g2d = bimg.createGraphics();
		double thres = effectParams[4] * 255.0;
		int[] colors = new int[numBands];
		for (band = 0; band < numBands; band++) {
			colors[band] = (int) (effectParams[band + 1] * 255.0);
		}
		double fp1 = effectParams[5] * 255.0;
		double fp2 = effectParams[6];
		int max = (int) (effectParams[7] * 1000.0);
		double lowt = effectParams[8] * 255.0;

		int loopcount = effectContext.count;
		if (loopcount == 0)
			loopcount = 6;

		System.out.println("walk " + thres + ", " + fp1 + ", " + fp2);
		int count = 0;
		int amount = 0;
		int rd, gr, bl;
		Color colorOut = null;
		for (y = 1; y < height - 1; y++) {
			for (x = 1; x < width - 1; x++) {
				//for (band = 0; band < numBands; band++) {
				for (band = 0; band < 1; band++) {

					a = frameIn.getPixelInt(band, x, y);
					b = frameOut.getPixelInt(band, x, y);
					if (a >= (int) thres && b < 255 && count < max) {
						path = new GeneralPath();
						count++;
						if (effectContext.option1) {
							rd = (int) ((double) (count + 100) % 255.0);
							gr = (int) ((double) (count + 140) % 255.0);
							bl = (int) ((double) (count + 160) % 255.0);
							colorOut = new Color(rd, gr, bl);
						} else {
							colorOut = new Color(colors[0], colors[1],
									colors[2]);
						}

						//System.out.println("into follow "+a+", "+b+", "+x+",
						// "+y+", "+band);
						amount = 0;
						if (effectContext.option2) {
							amount = edgeWalk(frameOut, frameIn, band, x, y,
									(int) fp1, (int) thres, (int) fp2, path,
									loopcount);
							if (amount > lowt)
								System.out.println("amount " + amount + ", "
										+ a + ", " + b + ", " + x + ", " + y
										+ ", " + band);

						} else {
							amount = edgeFollow(frameOut, frameIn, x, y,
									(int) fp1, (int) thres, (int) fp2, path,
									loopcount);
						}
						if (amount > lowt) {
							path.closePath();
							g2d.setColor(colorOut);
							g2d.fill(path);
							g2d.draw(path); //?? why need this
						}

					}
				}
			}
		}
		frameOut1.setBufferedImage(bimg);
		return frameOut1;

	}

	private int edgeFollow(EFrame frameOut, EFrame frameIn, int ka, int la,
			int T1, int T, int a, GeneralPath path, int loopcount) {

		//int valTest = frameIn.getPixelInt(EFrame.BLUE, ka, la);
		int diff, mindiff, i, j, cont, l, k, ABS, label, lastx, lasty;
		int valRed, valBlue, valGreen, val, valThis, maxValue;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		boolean contLoop;
		diff = 0;
		mindiff = 300;
		maxValue = 0;
		cont = 0;
		lastx = ka;
		lasty = la;
		boolean first = true;
		int count = 0;
		int kafirst = ka;
		int lafirst = la;
		path.moveTo(ka, la);
		cont = 0;
		for (;;) {
			k = ka;
			l = la;
			frameOut.setPixel(EFrame.RED, ka, la, 255);
			frameOut.setPixel(EFrame.GREEN, ka, la, 255);
			frameOut.setPixel(EFrame.BLUE, ka, la, 255);
			path.lineTo(ka, la);
			valRed = frameIn.getPixelInt(EFrame.RED, ka, la);
			valBlue = frameIn.getPixelInt(EFrame.BLUE, ka, la);
			valGreen = frameIn.getPixelInt(EFrame.GREEN, ka, la);
			System.out.println("IN edge 1 " + ka + ", 2" + la);
			count++;
			val = (int) (((double) (valRed + valBlue + valGreen)) / 3.0);
			frameIn.setPixel(EFrame.RED, ka, la, 0);
			frameIn.setPixel(EFrame.GREEN, ka, la, 0);
			frameIn.setPixel(EFrame.BLUE, ka, la, 0);
			mindiff = 300;
			maxValue = 0;
			contLoop = true;
			cont = 0;
			for (i = -1; i < 2; i++) {
				for (j = -1; j < 2; j++) {
					if (!(i == 0 && j == 0) && k + i >= 0 && k + i < width
							&& l + j >= 0 && l + j < height
							&& !(k + i == lastx && l + j == lasty)) {

						valRed = frameIn.getPixelInt(EFrame.RED, k + i, l + j);
						valBlue = frameIn
								.getPixelInt(EFrame.BLUE, k + i, l + j);
						valGreen = frameIn.getPixelInt(EFrame.GREEN, k + i, l
								+ j);

						valThis = (int) (((double) (valRed + valBlue + valGreen)) / 3.0);
						ABS = Math.abs(val - valThis);

						if (valThis > T && ABS < T1) {
							diff = a * ABS;
							if (diff < mindiff) {
								maxValue = valThis;
								mindiff = diff;
								lastx = ka;
								lasty = la;
								ka = k + i;
								la = l + j;
								valRed = frameIn
										.getPixelInt(EFrame.RED, ka, la);
								valBlue = frameIn.getPixelInt(EFrame.BLUE, ka,
										la);
								valGreen = frameIn.getPixelInt(EFrame.GREEN,
										ka, la);
								System.out.println("IN edge 2 " + ka + ", 2"
										+ la);

								if (valRed == 0 && valBlue == 0
										&& valGreen == 0)
									return (count);
								contLoop = false;
								System.out.println("IN edge 3 " + ka + ", 2"
										+ la);

							} else {
								cont = 1;
							}
						}
					}
					if (!contLoop)
						break;
				}
				if (!contLoop)
					break;
			}
			path.lineTo(ka, la);
			System.out.println("IN edge 4 " + ka + ", 2" + la);

			if (cont == 0 && contLoop)
				break;
			System.out.println("IN edge 5 " + ka + ", 2" + la);

		}
		if (ka != kafirst || la != lafirst)
			path.lineTo(kafirst, lafirst);

		return (count);
	}

	private int edgeWalk(EFrame frameOut, EFrame frameIn, int band, int x,
			int y, int T1, int T, int a, GeneralPath path, int loopcount) {

		int diff, ABS, label, xlast, ylast, xnext = -255, ynext = -255, amount = 0;
		double val, valThis;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		diff = 0;
		path.moveTo(x, y);
		Point p = null;
		boolean cont, end;
		end = false;
		path.moveTo(x, y);
		if (x == 0) {
			xlast = x + 1;
		} else {
			xlast = x - 1;
		}
		if (y == 0) {
			ylast = y + 1;
		} else {
			ylast = y - 1;
		}
		int xo, yo, loopCount = loopcount;
		boolean first = false;
		while (!end) {
			frameOut.setPixel(band, x, y, 255);
			frameIn.setPixel(band, x, y, 0);
			amount++;
			cont = true;
			xnext = xlast;
			ynext = ylast;
			xo = x;
			yo = y;
			val = 0;
			boolean found = false;
			//loopCount = 9;

			while (cont) {
				loopCount--;
				if (loopCount < 0) {
					cont = false;
					end = true;
					break;
				}

				if (!first) {
					p = circulate(xnext, ynext, x, y, width, height, 3, true);
					xnext = (int) p.getX();
					ynext = (int) p.getY();

					if (xnext == xlast && ynext == ylast) {
						cont = false;
						//end = true;
						//System.out.println("break 3");
						break;
					}
				}
				first = false;

				val = frameOut.getPixelDouble(band, xnext, ynext);

				//if (val == (double)255) {
				//	cont = false;
				//	end = true;
				//	//System.out.println("break 3, end A");
				//	break;
				//}

				//System.out.println("got next "+val+", "+xnext+", "+ynext+",
				// "+x+", "+y+", "+xlast+", "+ylast);

				valThis = frameIn.getPixelDouble(band, xnext, ynext);
				//ABS = Math.abs(val - valThis);
				if (valThis > (double) T && val < 255) {
					//System.out.println("Path to "+xnext+", "+ynext+", "+x+",
					// "+y);
					xlast = x;
					ylast = y;
					x = xnext;
					y = ynext;
					cont = false;
					first = false;
					path.lineTo(x, y);
					found = true;
					frameOut.setPixel(band, x, y, 255);
					frameIn.setPixel(band, x, y, 0);
					//System.out.println("break 3, found");
					break;

				} else {
					//if ((xnext == xlast && ynext == ylast)||
					//	(x==xlast && y == ylast)){
					//	cont = false;
					//	end = true;
					//	break;
					//}
				}
			}
			if (end)
				break;
			if (!found) {
				x = xo;
				y = yo;
				if (xlast > xo) {
					xlast = xlast + 1;
				} else if (xlast < xo) {
					xlast = xlast - 1;
				}
				if (ylast > yo) {
					ylast = ylast + 1;
				} else if (ylast < yo) {
					ylast = ylast - 1;
				}
				xnext = xlast;
				ynext = ylast;
				cont = true;
				loopCount = loopcount;
				while (cont) {
					loopCount--;
					if (loopCount < 0) {
						cont = false;
						end = true;
						break;
					}

					p = circulate(xnext, ynext, x, y, width, height, 5, true);
					xnext = (int) p.getX();
					ynext = (int) p.getY();

					if (xnext == xlast && ynext == ylast) {
						cont = false;
						end = true;
						break;
					}

					first = false;

					val = frameOut.getPixelDouble(band, xnext, ynext);

					valThis = frameIn.getPixelDouble(band, xnext, ynext);
					//ABS = Math.abs(val - valThis);
					if (valThis > (double) T && val < 255) {
						xlast = x;
						ylast = y;
						x = xnext;
						y = ynext;
						cont = false;
						first = false;
						path.lineTo(x, y);
						frameOut.setPixel(band, x, y, 255);
						frameIn.setPixel(band, x, y, 0);
						found = true;
						break;

					} else {
						//if ((xnext == xlast && ynext == ylast)||
						//	(x==xlast && y == ylast)){
						//	cont = false;
						//	end = true;
						//	break;
						//}
					}

				}
			}

		}
		return amount;
	}

	private Point circulate(int x, int y, int xo, int yo, int width,
			int height, int size, boolean direction) {

		int xmin, ymin, xmax, ymax;
		xmin = xo - 1;
		ymin = yo - 1;
		xmax = xo + 1;
		ymax = yo + 1;
		if (size == 5) {
			xmin = xo - 2;
			ymin = yo - 2;
			xmax = xo + 2;
			ymax = yo + 2;
		}

		if (y <= ymin) {
			if (x < xmax) {
				x = x + 1;
			} else {
				y = y + 1;
			}
		} else if (y >= ymax) {
			if (x > xmin) {
				x = x - 1;
			} else {
				y = y - 1;
			}
		} else {
			if (x <= xmin) {
				y = y - 1;
			} else {
				y = y + 1;
			}
		}

		return new Point(x, y);
	}

	private EFrame doHough(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		double sine[] = new double[width];
		double cosine[] = new double[width];
		int ht = (int) (effectParams[2] * 255.0);
		int inc = (int) (effectParams[1] * 255.0);
		for (int theta = 0; theta < width; theta++) {
			sine[theta] = Math.sin((theta * Math.PI) / width);
			cosine[theta] = Math.cos((theta * Math.PI) / width);
		}

		int numBands = frameIn.getPixelStride();
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					double cp = frameIn.getPixelDouble(band, x, y);
					if (cp > ht) {
						for (int theta = 0; theta < width; theta++) {
							double r = ((x * cosine[theta]) + (y * sine[theta]));
							r = (double) height / 2 + (r / 3.0);
							int c = theta + (int) (Math.floor(r) * width);

							if ((c > 0) && (c < width * height)) {

								if (frameOut.getPixelDouble(band, c) < ht) {

									frameOut.setPixel(band, c, (frameOut
											.getPixelDouble(band, c) + inc));
								}
							}
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doHoughInverse(EFrame frameIn) {
		EFrame frameIn1 = new EFrame(frameIn.getBuffer());
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int maxx = 0;
		int maxy = 0;
		double r = 0;
		double cons;
		double m;
		double max;
		double thet;
		int ht = (int) (effectParams[2] * 255.0);
		int inc = (int) (effectParams[1] * 255.0);
		int numLines = (int) (effectParams[3] * 100.0);

		int numBands = frameIn1.getPixelStride();
		for (int band = 0; band < numBands; band++) {

			maxx = 0;
			maxy = 0;

			for (int lines = 0; lines < numLines; lines++) {

				max = 0;

				for (int y = 1; y < height - 1; y++) {
					for (int x = 1; x < width - 1; x++) {
						double cp = frameIn1.getPixelDouble(band, x, y);
						if (cp > max) {
							max = cp;
							maxx = x;
							maxy = y;
						}
					}
					frameIn1.setPixel(band, maxx, maxy, 0);
					r = (maxy - ((double) height / 2)) * 3.0;
					thet = (maxx / ((double) width)) * Math.PI;
					if (Math.abs(Math.sin(thet)) < Math.abs(Math.cos(thet))) {
						m = -Math.sin(thet) / Math.cos(thet);
						cons = r / Math.cos(thet);
						for (int y1 = 0; y1 < height; y1++) {
							int x1 = (int) (((double) y1 * m) + cons);
							if (x1 > 0 && x1 < width)
								frameOut.setPixel(band, x1, y1, frameOut
										.getPixelDouble(band, x1, y1)
										+ inc);
						}
					} else {
						m = -Math.cos(thet) / Math.sin(thet);
						cons = r / Math.sin(thet);
						for (int x2 = 0; x2 < width; x2++) {
							int y2 = (int) (((double) x2 * m) + cons);
							if (y2 > 0 && y2 < height)
								frameOut.setPixel(band, x2, y2, frameOut
										.getPixel(x2, y2)
										+ inc);
						}

					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doMovie(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameIn0 = eFrameIn.get(0);
		if (frameIn0 == null) {
			return frameOut;
		}
		int width = frameIn0.getWidth();
		int height = frameIn0.getHeight();
		int numBands = frameIn0.getPixelStride();
		int x, y, band;
		MoviePoint moviePoint;

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					frameOut.setPixel(band, x, y, 0);
				}
			}
		}
		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		int count = eFrameWork.getCount();
		// Movie effect requires at least 2 frames for motion comparison
		if (count < 2) {
			return frameOut;
		}
		EFrame frameIn1 = eFrameWork.get(1);

		if (frameIn1 == null)
			return frameOut;

		BufferedImage biOut = frameOut.getBufferedImage();

		int blockSize = (int) (effectParams[1] * 100.0);
		int stepSize = effectContext.step + 1;
		double scale = effectParams[2] * 255.0;
		double z = 0;
		int sum = 0;
		double dx, dy, dt;
		double maxdx = 0, maxdy = 0, maxdt = 0, mindx = 0, mindy = 0, mindt = 0;
		ArrayList[] list = new ArrayList[numBands];
		for (int i = 0; i < numBands; i++) {
			list[i] = new ArrayList();
		}

		for (y = 0; y < height - blockSize; y += stepSize) {
			for (x = 0; x < width - blockSize; x += stepSize) {
				for (band = 0; band < numBands; band++) {
					dt = 0;
					dx = 0;
					dy = 0;
					for (int my = 0; my < blockSize - 1; my++) {
						for (int mx = 0; mx < blockSize - 1; mx++) {
							dt += Math.abs(0.25 * (frameIn1.getPixelDouble(
									band, x + mx, y + my)
									- frameIn0.getPixelDouble(band, x + mx, y
											+ my)
									+ frameIn1.getPixelDouble(band, x + mx + 1,
											y + my)
									- frameIn0.getPixelDouble(band, x + mx + 1,
											y + my)
									+ frameIn1.getPixelDouble(band, x + mx, y
											+ my + 1)
									- frameIn0.getPixelDouble(band, x + mx, y
											+ my + 1)
									+ frameIn1.getPixelDouble(band, x + mx + 1,
											y + my + 1) - frameIn0
									.getPixelDouble(band, x + mx + 1, y + my
											+ 1)));
							dx += 0.25 * (frameIn1.getPixelDouble(band, x + mx
									+ 1, y + my)
									- frameIn1.getPixelDouble(band, x + mx, y
											+ my)
									+ frameIn1.getPixelDouble(band, x + mx + 1,
											y + my + 1)
									- frameIn1.getPixelDouble(band, x + mx, y
											+ my + 1)
									+ frameIn0.getPixelDouble(band, x + mx + 1,
											y + my)
									- frameIn0.getPixelDouble(band, x + mx, y
											+ my)
									+ frameIn0.getPixelDouble(band, x + mx + 1,
											y + my + 1) - frameIn0
									.getPixelDouble(band, x + mx, y + my + 1));
							dy += 0.25 * (frameIn1.getPixelDouble(band, x + mx,
									y + my + 1)
									- frameIn1.getPixelDouble(band, x + mx, y
											+ my)
									+ frameIn1.getPixelDouble(band, x + mx + 1,
											y + my + 1)
									- frameIn1.getPixelDouble(band, x + mx + 1,
											y + my)
									+ frameIn0.getPixelDouble(band, x + mx, y
											+ my + 1)
									- frameIn0.getPixelDouble(band, x + mx, y
											+ my)
									+ frameIn0.getPixelDouble(band, x + mx + 1,
											y + my + 1) - frameIn0
									.getPixelDouble(band, x + mx + 1, y + my));
						}
					}
					moviePoint = new MoviePoint(x, y, dx, dy, dt);
					if (maxdt < dt)
						maxdt = dt;
					if (maxdx < dx)
						maxdx = dx;
					if (maxdy < dy)
						maxdy = dy;
					if (mindt > dt)
						mindt = dt;
					if (mindx > dx)
						mindx = dx;
					if (mindy > dy)
						mindy = dy;

					list[band].add(moviePoint);
				}
			}
		}
		double dxScale = 255.0 / maxdx;
		double dyScale = 255.0 / maxdy;
		double dtScale = 255.0 / maxdt;

		double sScale = scale / maxdt;
		double k, a, b, s;
		int valueOut;

		if (effectContext.option4) {

			for (band = 0; band < numBands; band++) {
				for (int i = 0; i < list[band].size(); i++) {
					moviePoint = (MoviePoint) list[band].get(i);
					if (effectContext.option3) {
						frameOut.setPixel(band, (moviePoint.x), (moviePoint.y),
								((moviePoint.dt) * dtScale));
					} else {
						k = (moviePoint.dy) / (moviePoint.dx);
						s = (moviePoint.dt) * sScale;
						b = Math.sqrt((s * s) / (k * k + 1));
						a = Math.abs(b * k);
						double aSign = 1.0;
						double bSign = 1.0;
						if (moviePoint.dy < 0)
							aSign = -1.0;
						if (moviePoint.dx < 0)
							bSign = -1.0;
						for (double ox = 0, oy = 0; ox <= b && oy <= a; ox += (b / a), oy += (a / b)) {

							valueOut = 255;
							if (effectContext.option2) {
								valueOut = (int) ((moviePoint.dt) * dtScale);
							}
							if (effectContext.option1) {
								if (bSign < 0 && aSign < 0) {
									frameOut
											.setPixel(
													0,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);
								}
								if (bSign < 0 && aSign > 0) {
									frameOut
											.setPixel(
													1,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);
								}
								if (bSign > 0 && aSign < 0) {
									frameOut
											.setPixel(
													2,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);
								}
								if (bSign > 0 && aSign > 0) {
									frameOut
											.setPixel(
													0,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);
									frameOut
											.setPixel(
													1,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);

									frameOut
											.setPixel(
													2,
													(moviePoint.x + (int) (ox * bSign)),
													(moviePoint.y + (int) (oy * aSign)),
													valueOut);
								}
							} else {
								frameOut.setPixel(band,
										(moviePoint.x + (int) (ox * bSign)),
										(moviePoint.y + (int) (oy * aSign)),
										valueOut);
							}
						}
					}
				}
			}
		} else {
			BufferedImage bimg = frameOut.getBufferedImage();
			Graphics2D g2d = bimg.createGraphics();
			for (band = 0; band < numBands; band++) {
				for (int i = 0; i < list[band].size(); i++) {
					moviePoint = (MoviePoint) list[band].get(i);
					g2d.setColor(EFrame.getColor(band));
					k = (moviePoint.dy) / (moviePoint.dx);
					s = (moviePoint.dt) * sScale;
					b = Math.sqrt((s * s) / (k * k + 1));
					a = b * k;
					//g2d.drawLine(moviePoint.x,moviePoint.y,(int)(moviePoint.x
					// + b), (int)(moviePoint.y + a));
					g2d.drawLine(moviePoint.x, moviePoint.y,
							(int) (moviePoint.x + 1), (int) (moviePoint.y + 1));
				}
			}
			frameOut.setBufferedImage(bimg);
		}
		System.out.println("in movie 4");
		return frameOut;
	}

	class MoviePoint {
		public int x, y;

		public double dx;

		public double dy;

		public double dt;

		public MoviePoint(int x, int y, double dx, double dy, double dt) {
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.dt = dt;
		}
	}

	private EFrame doThreshold(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double highThres = effectParams[2] * 255.0;
		double lowThres = effectParams[1] * 255.0;
		double level, mean;
		double[] sr = null;
		System.out.println("high, low " + highThres + ", " + lowThres);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				level = frameIn.getPixelGrey(x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						level = frameIn.getPixelDouble(band, x, y);
					}

					if (effectContext.option2) {
						if (effectContext.option3) {
							sr = getSampleRegion55(frameIn, band, x, y);
						} else {
							sr = getSampleRegion33(frameIn, band, x, y);
						}
						mean = 0.0;
						for (int i = 0; i < sr.length; i++) {
							mean += sr[i];
						}
						mean = mean / sr.length;
						if (effectContext.option5) {
							lowThres = mean - effectParams[1] * 255.0;
						} else {
							lowThres = effectParams[1] * 255.0 - mean;
						}
					}

					if ((level <= highThres) && (level >= lowThres)) {
						if (effectContext.option4) {
							frameOut.setPixel(band, x, y, 0);
						} else {
							frameOut.setPixel(band, x, y, 255);
						}
					} else {
						if (effectContext.option4) {
							frameOut.setPixel(band, x, y, 255);

						} else {
							frameOut.setPixel(band, x, y, 0);
						}
					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doThreshold(EFrame frameIn, double lowThres, double highThres) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double level, mean;
		double[] sr = null;
		System.out.println("high, low " + highThres + ", " + lowThres);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				level = frameIn.getPixelGrey(x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						level = frameIn.getPixelDouble(band, x, y);
					}

					if ((level <= highThres) && (level >= lowThres)) {
						frameOut.setPixel(band, x, y, 255);
					} else {
						frameOut.setPixel(band, x, y, 0);
					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doLowPass(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double level, mean;
		double[] sr = null;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				level = frameIn.getPixelGrey(x, y);
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
					}
					mean = 0.0;
					for (int i = 0; i < sr.length; i++) {
						mean += sr[i];
					}
					mean = mean / sr.length;
					frameOut.setPixel(band, x, y, mean);
				}
			}
		}
		return frameOut;

	}

	private EFrame doLIN(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double level, mean;
		double[] sr = null;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				level = frameIn.getPixelGrey(x, y);
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
						level = frameIn.getPixelDouble(band, x, y);
					}
					level = Math.max(Math.max(Math.max(Math.max(sr[0], sr[1]),
							Math.max(sr[2], sr[3])), Math.max(Math.max(sr[4],
							sr[5]), Math.max(sr[6], sr[7]))), sr[8]);
					frameOut.setPixel(band, x, y, level);
				}
			}
		}
		return frameOut;

	}

	private EFrame doSIN(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double level, mean;
		double[] sr = null;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				level = frameIn.getPixelGrey(x, y);
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
						level = frameIn.getPixelDouble(band, x, y);
					}
					level = Math.min(Math.min(Math.min(Math.min(sr[0], sr[1]),
							Math.min(sr[2], sr[3])), Math.min(Math.min(sr[4],
							sr[5]), Math.min(sr[6], sr[7]))), sr[8]);
					frameOut.setPixel(band, x, y, level);
				}
			}
		}
		return frameOut;

	}

	private EFrame doLocalHistogram(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int count;
		double[] sr = null;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					count = 0;
					if (effectContext.option1) {
						sr = getSampleRegion55(frameIn, band, x, y);
						if (sr[0] < sr[12])
							count++;
						if (sr[1] < sr[12])
							count++;
						if (sr[2] < sr[12])
							count++;
						if (sr[3] < sr[12])
							count++;
						if (sr[4] < sr[12])
							count++;
						if (sr[5] < sr[12])
							count++;
						if (sr[6] < sr[12])
							count++;
						if (sr[7] < sr[12])
							count++;
						if (sr[8] < sr[12])
							count++;
						if (sr[9] < sr[12])
							count++;
						if (sr[10] < sr[12])
							count++;
						if (sr[11] < sr[12])
							count++;
						if (sr[13] < sr[12])
							count++;
						if (sr[14] < sr[12])
							count++;
						if (sr[15] < sr[12])
							count++;
						if (sr[16] < sr[12])
							count++;
						if (sr[17] < sr[12])
							count++;
						if (sr[18] < sr[12])
							count++;
						if (sr[19] < sr[12])
							count++;
						if (sr[20] < sr[12])
							count++;
						if (sr[21] < sr[12])
							count++;
						if (sr[22] < sr[12])
							count++;
						if (sr[23] < sr[12])
							count++;
						if (sr[24] < sr[12])
							count++;
						frameOut.setPixel(band, x, y, (((double) count) / 24.0)
								* (frameIn.getPixelDouble(band, x, y)));
					} else {
						sr = getSampleRegion33(frameIn, band, x, y);
						if (sr[0] < sr[4])
							count++;
						if (sr[1] < sr[4])
							count++;
						if (sr[2] < sr[4])
							count++;
						if (sr[3] < sr[4])
							count++;
						if (sr[5] < sr[4])
							count++;
						if (sr[6] < sr[4])
							count++;
						if (sr[7] < sr[4])
							count++;
						if (sr[8] < sr[4])
							count++;
						frameOut.setPixel(band, x, y, (((double) count) / 8.0)
								* (frameIn.getPixelDouble(band, x, y)));

					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doSubtract(EFrame frameIn1, EFrame frameIn2) {
		EFrame frameOut = new EFrame(frameIn1.getBuffer());
		int width = frameIn1.getWidth();
		int height = frameIn1.getHeight();
		int numBands = frameIn1.getPixelStride();
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					frameOut.setPixel(band, x, y, Math.abs(frameIn1
							.getPixelDouble(band, x, y)
							- frameIn2.getPixelDouble(band, x, y)));
				}
			}
		}
		return frameOut;

	}

	private EFrame doLimb(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double ht = effectParams[2] * 255.0;
		double lt = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				level = frameIn.getPixelGrey(x, y);
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
						level = frameIn.getPixelDouble(band, x, y);
					}
					if ((sr[4] <= ht) && (sr[3] >= lt) && (sr[6] >= lt)
							&& (sr[7] >= lt) && (sr[8] >= lt) && (sr[5] >= lt))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] >= lt) && (sr[1] >= lt)
							&& (sr[3] >= lt) && (sr[6] >= lt) && (sr[7] >= lt))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[1] >= lt) && (sr[2] >= lt)
							&& (sr[5] >= lt) && (sr[8] >= lt) && (sr[7] >= lt))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] >= lt) && (sr[1] >= lt)
							&& (sr[2] >= lt) && (sr[3] >= lt) && (sr[5] >= lt))
						level = 255.0;
					else
						level = 0.0;
					frameOut.setPixel(band, x, y, level);
				}
			}
		}
		return frameOut;

	}

	private EFrame doJunction(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double ht = effectParams[2] * 255.0;
		double lt = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				level = frameIn.getPixelGrey(x, y);
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
						level = frameIn.getPixelDouble(band, x, y);
					}
					if ((sr[4] <= ht) && (sr[1] <= ht) && (sr[6] <= ht)
							&& (sr[8] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] <= ht) && (sr[5] <= ht)
							&& (sr[6] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[7] <= ht) && (sr[0] <= ht)
							&& (sr[2] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[3] <= ht) && (sr[2] <= ht)
							&& (sr[8] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] <= ht) && (sr[8] <= ht)
							&& (sr[6] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] <= ht) && (sr[2] <= ht)
							&& (sr[6] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[2] <= ht) && (sr[6] <= ht)
							&& (sr[8] <= ht))
						level = 255.0;
					else if ((sr[4] <= ht) && (sr[0] <= ht) && (sr[2] <= ht)
							&& (sr[8] <= ht))
						level = 255.0;
					else
						level = 0.0;
					frameOut.setPixel(band, x, y, level);
				}
			}
		}
		return frameOut;

	}

	private EFrame doZeroCrossing(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
					}
					if (effectContext.option2) {
						for (int i = 0; i < sr.length; i++) {
							sr[i] = sr[i] + 1.0;
						}
					}
					if (((sr[3] < thres) && (sr[5] > thres) && (sr[4] < sr[5]) && (sr[4] > sr[3]))
							|| ((sr[1] < thres) && (sr[7] > thres)
									&& (sr[4] < sr[7]) && (sr[4] > sr[1]))
							|| ((sr[3] > thres) && (sr[5] < thres)
									&& (sr[4] > sr[5]) && (sr[4] < sr[3]))
							|| ((sr[1] > thres) && (sr[7] < thres)
									&& (sr[4] > sr[7]) && (sr[4] < sr[1])))
						frameOut.setPixel(band, x, y, 255);
					else
						frameOut.setPixel(band, x, y, 0);
				}
			}
		}
		return frameOut;

	}

	private EFrame doClearWhite(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		int count = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
					}
					if (sr[4] >= thres) {
						if (effectContext.option2) {
							if ((sr[0] >= thres) || (sr[1] >= thres)
									|| (sr[2] >= thres) || (sr[3] >= thres)
									|| (sr[5] >= thres) || (sr[6] >= thres)
									|| (sr[7] >= thres) || (sr[8] >= thres)) {
								frameOut.setPixel(band, x, y, 255);
							}
						} else {
							count = 0;
							if (sr[0] >= thres)
								count++;
							if (sr[1] >= thres)
								count++;
							if (sr[2] >= thres)
								count++;
							if (sr[3] >= thres)
								count++;
							if (sr[5] >= thres)
								count++;
							if (sr[6] >= thres)
								count++;
							if (sr[7] >= thres)
								count++;
							if (sr[8] >= thres)
								count++;
							if (count > effectContext.count) {
								frameOut.setPixel(band, x, y, 255);
							}
						}
					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doLeader(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		if (effectContext.option1) {
			int x1, y1, x2, y2;
			x1 = (int) (effectParams[1] * (width - 1));
			x2 = (int) (effectParams[3] * (width - 1));
			y1 = (int) (effectParams[2] * (height - 1));
			y2 = (int) (effectParams[4] * (height - 1));

			ejControls.setFilmP(0, (long) frameIn.getPixelDouble(0, x1, y1));
			ejControls.setFilmP(1, (long) frameIn.getPixelDouble(1, x1, y1));
			ejControls.setFilmP(2, (long) frameIn.getPixelDouble(2, x1, y1));
			System.out.println("<<**Colour at (1): "
					+ frameIn.getPixelDouble(EFrame.BLUE, x1, y1) + ", "
					+ frameIn.getPixelDouble(EFrame.GREEN, x1, y1) + ", "
					+ frameIn.getPixelDouble(EFrame.RED, x1, y1));
			System.out.println("<<**Colour at (2): "
					+ frameIn.getPixelDouble(EFrame.BLUE, x2, y2) + ", "
					+ frameIn.getPixelDouble(EFrame.GREEN, x2, y2) + ", "
					+ frameIn.getPixelDouble(EFrame.RED, x2, y2));

			System.out.println("<<**Colour% at (1): " + (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.BLUE, x1, y1) + ", "
					+ (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.GREEN, x1, y1) + ", "
					+ (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.RED, x1, y1));
			System.out.println("<<**Colour% at (2): " + (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.BLUE, x2, y2) + ", "
					+ (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.GREEN, x2, y2) + ", "
					+ (100.0 / 255.0)
					* frameIn.getPixelDouble(EFrame.RED, x2, y2));

			ejControls.setFilmP(3, (long) frameIn.getPixelDouble(0, x2, y2));
			ejControls.setFilmP(4, (long) frameIn.getPixelDouble(1, x2, y2));
			ejControls.setFilmP(5, (long) frameIn.getPixelDouble(2, x2, y2));

			return frameOut;

		} else {

			frameOut.clearBuffer();
			RenderedOp ropIn = frameOut.getRenderedOp();
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			double[] constants = new double[3];
			constants[0] = effectParams[1] * 255.0;
			constants[1] = effectParams[2] * 255.0;
			constants[2] = effectParams[3] * 255.0;

			pb.add(constants);
			RenderedOp ropOut = JAI.create("addconst", pb);
			frameOut.setRenderedOp(ropOut);
			return frameOut;

		}

	}

	private EFrame doChamfer(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());
		EFrame frameOut2 = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		frameOut1.clearBuffer();
		frameOut2.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double ht = effectParams[2] * 255.0;
		double lt = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		int a, b, c, d, z, f, g, h, i;

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					level = frameIn.getPixelDouble(band, x, y);
					if (level >= ht) {
						frameOut1.setPixel(band, x, y, 1);
						frameOut2.setPixel(band, x, y, 1);
					}
				}
			}
		}

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					if (frameOut1.getPixelInt(band, x, y) == 1) {
						a = frameOut1.getPixelInt(band, x - 1, y - 1) + 4;
						b = frameOut1.getPixelInt(band, x, y - 1) + 3;
						c = frameOut1.getPixelInt(band, x + 1, y - 1) + 4;
						d = frameOut1.getPixelInt(band, x - 1, y) + 3;
						z = Math.min(Math.min(a, b), Math.min(c, d));
						frameOut1.setPixel(band, x, y, z);
					}
				}
			}
		}

		for (int y = height - 1; y >= 0; y--) {
			for (int x = width - 1; x >= 0; x--) {
				for (int band = 0; band < numBands; band++) {
					if (frameOut2.getPixelInt(band, x, y) == 1) {
						f = frameOut2.getPixelInt(band, x + 1, y) + 3;
						g = frameOut2.getPixelInt(band, x - 1, y + 1) + 4;
						h = frameOut2.getPixelInt(band, x, y + 1) + 3;
						i = frameOut2.getPixelInt(band, x + 1, y + 1) + 4;
						z = Math.min(Math.min(f, g), Math.min(h, i));
						frameOut2.setPixel(band, x, y, z);
					}
				}
			}
		}

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				for (int band = 0; band < numBands; band++) {
					frameOut.setPixel(band, x, y, Math.min(frameOut1
							.getPixelInt(band, x, y), frameOut2.getPixelInt(
							band, x, y)));

				}
			}
		}

		return frameOut;

	}

	private EFrame doContrast(EFrame frameIn) {

		if (effectContext.option1) {
			return doContrast1(frameIn);
		}
		if (effectContext.option2) {
			return doContrast2(frameIn);
		}

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int count;
		double min, max;
		min = 0.0;
		max = 255.0;
		double level = 0.0;
		double[] sr = null;
		double[] LUT = new double[256];
		double[] bandMax = new double[numBands];
		double[] bandMin = new double[numBands];
		int x, y, band;
		for (band = 0; band < numBands * 2; band += 2) {
			bandMin[band / 2] = effectParams[band + 1];
			bandMax[band / 2] = effectParams[band + 2];
		}

		for (band = 0; band < numBands; band++) {
			min = 255.0;
			max = 0.0;
			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					level = frameIn.getPixelDouble(band, x, y);
					if (level < min)
						min = level;
					if (level > max)
						max = level;
				}
			}

			min = min + (max - min) * (bandMin[band]);
			max = max - (max - min) * (bandMax[band]);
			System.out.println("contrast " + min + ", " + max);
			for (int index = 0; index < (int) min; index++)
				LUT[index] = 0.0;
			for (int index = (int) max; index <= 255; index++)
				LUT[index] = 255.0;
			for (int index = (int) min; index < (int) max; index++)
				LUT[index] = (255.0 * ((double) index - min) / (max - min));

			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					frameOut.setPixel(band, x, y, LUT[frameIn.getPixelInt(band,
							x, y)]);
				}
			}

		}
		return frameOut;
	}

	private EFrame doContrast1(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int count;
		double min, max;
		min = 0.0;
		max = 255.0;
		double level = 0.0;

		double[] sr = null;
		double[] LUT = new double[256];
		double[] bandMax = new double[numBands];
		double[] bandMin = new double[numBands];
		int x, y, band;
		double nPower = effectParams[1];
		double dPower = effectParams[2];
		double power = nPower / dPower;

		for (band = 0; band < numBands; band++) {
			bandMin[band] = 255.0;
			bandMax[band] = 0.0;
		}

		for (band = 0; band < numBands; band++) {
			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					level = frameIn.getPixelDouble(band, x, y);
					level = Math.pow(level, power);
					if (level < bandMin[band])
						bandMin[band] = level;
					if (level > bandMax[band])
						bandMax[band] = level;
				}
			}
		}

		for (band = 0; band < numBands; band++) {
			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					level = frameIn.getPixelDouble(band, x, y);
					level = Math.pow(level, power);
					frameOut.setPixel(band, x, y,
							255.0 * (level / bandMax[band]));

				}
			}
		}

		return frameOut;
	}

	private EFrame doContrast2(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int count;
		double min, max;
		min = 0.0;
		max = 255.0;
		double level = 0.0;

		double[] sr = null;
		double[] LUT = new double[256];
		double[] bandMax = new double[numBands];
		double[] bandMin = new double[numBands];
		int x, y, band;
		double nPower = effectParams[1];
		double dPower = effectParams[2];
		double power = nPower / dPower;

		for (band = 0; band < numBands; band++) {
			bandMin[band] = 255.0;
			bandMax[band] = 0.0;
		}

		for (band = 0; band < numBands; band++) {
			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					level = frameIn.getPixelDouble(band, x, y);
					level = Math.log(level);
					if (level < bandMin[band])
						bandMin[band] = level;
					if (level > bandMax[band])
						bandMax[band] = level;
				}
			}
		}

		for (band = 0; band < numBands; band++) {
			for (y = 1; y < height - 1; y++) {
				for (x = 1; x < width - 1; x++) {
					level = frameIn.getPixelDouble(band, x, y);
					level = Math.log(level);
					frameOut.setPixel(band, x, y,
							255.0 * (level / bandMax[band]));

				}
			}
		}

		return frameOut;
	}

	private EFrame doCentroid(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int[] table = new int[256];
		int[] cenx = new int[256];
		int[] ceny = new int[256];

		int cp, max, index, x_cent, y_cent;
		int cnx = 0, cny = 0;

		for (int i = 0; i <= 255; i++) {
			table[i] = 0;
			cenx[i] = 0;
			ceny[i] = 0;
		}

		for (int band = 0; band < numBands; band++) {
			for (int y = 1; y < height - 1; y++) {
				for (int x = 1; x < width - 1; x++) {
					cp = frameIn.getPixelInt(band, x, y);
					table[cp]++;
					cenx[cp] += x;
					ceny[cp] += y;
				}
			}

			for (int n = 1; n <= 256; n++) {

				max = 0;
				index = 0;
				for (int j = 0; j <= 255; j++) {
					if (table[j] > max) {

						max = table[j];
						index = j;
						cnx = cenx[j];
						cny = ceny[j];
					}
				}
				table[index] = 0;
				if ((max > 0) && (index > 0)) {
					x_cent = cnx / max;
					y_cent = cny / max;
					frameOut.setPixel(band, x_cent, y_cent, 255);
				}
			}
		}
		return frameOut;
	}

	private EFrame doGrassFire(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());
		frameOut1.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		boolean done = false;
		double greyLevel = 0;
		double[] greyLevelb = new double[numBands];

		double thresh = effectParams[1] * 255.0;

		double[] threshb = new double[numBands];
		for (int b = 0; b < numBands; b++) {
			threshb[b] = effectParams[1 + b] * 255.0;
			greyLevelb[b] = 0;
		}

		double[] sr = null;
		double level;

		if (!effectContext.option1) {

			for (int band = 0; band < numBands; band++) {

				greyLevel = 0;
				done = false;
				while (!done && greyLevel < thresh) {

					greyLevel = greyLevel + 1.0;
					done = true;
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							if (!effectContext.option2) {
								sr = getSampleRegion33(frameOut, band, x, y);
							} else {
								sr = getSampleRegion33(frameIn, band, x, y);
							}

							if (sr[4] >= thresh) {
								level = Math.min(sr[8], Math.min(Math.min(Math
										.min(sr[0], sr[1]), Math.min(sr[2],
										sr[3])), Math.min(Math
										.min(sr[4], sr[5]), Math.min(sr[6],
										sr[7]))));

								if ((int) level < (int) greyLevel
										&& level < thresh) {
									frameOut.setPixel(band, x, y, greyLevel);
									frameOut1.setPixel(band, x, y, greyLevel);

									done = false;
								}
							}
						}
					}
				}
			}
		} else {

			for (int band = 0; band < numBands; band++) {

				greyLevelb[band] = 0;
				done = false;
				while (!done && greyLevelb[band] < threshb[band]) {

					greyLevelb[band] = greyLevelb[band] + 1.0;
					done = true;
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							if (!effectContext.option2) {
								sr = getSampleRegion33(frameOut, band, x, y);
							} else {
								sr = getSampleRegion33(frameIn, band, x, y);
							}

							if (sr[4] >= threshb[band]) {
								level = Math.min(sr[8], Math.min(Math.min(Math
										.min(sr[0], sr[1]), Math.min(sr[2],
										sr[3])), Math.min(Math
										.min(sr[4], sr[5]), Math.min(sr[6],
										sr[7]))));
								if ((int) level < (int) greyLevelb[band]
										&& level < threshb[band]) {
									frameOut.setPixel(band, x, y,
											greyLevelb[band]);
									frameOut1.setPixel(band, x, y,
											greyLevelb[band]);
									done = false;
								}
							}
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doCrackDetect(EFrame frameIn) {
		EFrame frameOut = doJAIInvert(doLIN(doLIN(doJAIInvert(doLIN(doLIN(frameIn))))));
		if (effectContext.option3) {
			// note doThreshold uses p1/p2
			if (effectContext.option4) {
				frameOut = doThreshold(doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doSubtract(frameIn,
						frameOut)));
			} else {
				frameOut = doThreshold(doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doJAISubtract(frameIn,
						frameOut)));
			}

		} else {
			if (effectContext.option4) {
				frameOut = doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doSubtract(frameIn,
						frameOut));
			} else {
				frameOut = doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doJAISubtract(frameIn,
						frameOut));

			}
		}

		return frameOut;
	}

	private EFrame doDOLPS(EFrame frameIn) {
		EFrame frameOut1 = doLowPass(doLowPass(doLowPass(frameIn)));
		EFrame frameOut2 = doLowPass(doLowPass(doLowPass(frameOut1)));

		// note doThreshold uses p1/p2
		if (effectContext.option3) {
			if (effectContext.option4) {

				frameOut1 = doThreshold(doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doSubtract(frameOut1,
						frameOut2)));
			} else {
				frameOut1 = doThreshold(doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doJAISubtract(frameOut1,
						frameOut2)));
			}
		} else {
			if (effectContext.option4) {
				frameOut1 = doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doSubtract(frameOut1,
						frameOut2));
			} else {
				frameOut1 = doJAIMultiplyConst(effectParams[3]
						/ (effectParams[4] + 0.001), doJAISubtract(frameOut1,
						frameOut2));
			}
		}
		return frameOut1;
	}

	private EFrame doOpening(EFrame frameIn) {

		float[][] matrix = openingMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		EFrame frameOut = doDilation(doErosion(frameIn, matrix[0]), matrix[1]);
		return frameOut;
	}

	private EFrame doClosing(EFrame frameIn) {

		float[][] matrix = closingMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		EFrame frameOut = doErosion(doDilation(frameIn, matrix[1]), matrix[0]);
		return frameOut;
	}
	


	private EFrame doPassTime3(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		float[] mask = cellData;
		if(convolve instanceof float[]) {
			mask = (float[])convolve;
		}

		if (effectContext.option6) System.out.println("Pass time cell");

		double tot,cell;
		double k = 4.0;
		double dt = 0.1;
		double qfactor = 50.0;
		double alpha = 0.0;
		double beta = 0.0;
		double gamma = 0.0;
		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double z = 0;
		int sum = 0;
		for (int m = 0; m < mask.length; m++) {
			if ((int) mask[m] != 0) {
				sum += (int) mask[m];
			}
		}


		if (effectContext.option1) {
			alpha = 2*(effectParams[1]-0.5)/effectParams[2];
			beta = 2*(effectParams[3]-0.5)/effectParams[4];
			gamma = 2*(effectParams[5]-0.5)/effectParams[6];
		} else {
			k = 2*(effectParams[1]-0.5)/effectParams[2];
			dt = 2*(effectParams[3]-0.5)/effectParams[4];
			qfactor = 2*(effectParams[5]-0.5)/effectParams[6];
			if (effectContext.mode == 0) {
				IIRBandpassFilterDesign bpfd =
				new IIRBandpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 1) {
				IIRLowpassFilterDesign bpfd =
					new IIRLowpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			} else if (effectContext.mode == 2) {
				IIRLowpassFilterDesign bpfd =
				new IIRLowpassFilterDesign((int) (k), (int) (1.0 / dt), qfactor);
				bpfd.doFilterDesign();
				alpha = bpfd.getAlpha();
				beta = bpfd.getBeta();
				gamma = bpfd.getGamma();
			}

		}

		EFrameSet eFrameWork = null;
		if (effectContext.option3) {
			eFrameWork = eFrameOut;
		} else {
			eFrameWork = eFrameIn;
		}
		int count = eFrameWork.getCount();
		double mfactor = effectParams[7]/effectParams[8];

		int step = effectContext.step+1;


		double maskTotal = 0;
		for (int my = 0; my < maskSize; my++) {
			for (int mx = 0; mx < maskSize; mx++) {
				if (mask[mx + (my * 3)] != 0.0) {
					maskTotal += 255.0 * (double) mask[mx
												+ (my * maskSize)];
				}
			}
		}
		cell = 0.0;
		z = 0;
		int band = 0;
		if (count < 3)
			return frameOut;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//for (band = 0; band < numBands; band++) {


					if (effectContext.option6){
					tot = 0;

					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							if (mask[mx + (my * 3)] != 0.0) {
								tot += eFrameWork.get(step).getPixelHue(x + mx - maskOffset, y + my - maskOffset)
									* (double) mask[mx
									+ (my * maskSize)];
							}
						}
					}

					double totThres = (2.0*255.0);
					double div = (6.0*255.0);
					if (tot >totThres){
						cell = eFrameWork.get(step).getPixelHue(x, y)
						- 0.3*255.0*(tot-totThres)/(div);
					} else {
						cell = eFrameWork.get(step).getPixelHue(x, y)
						+ 0.3*255.0*(tot/totThres);

					}

					if (cell <0) cell = 0.0;

					if (cell > 255.0) cell = 255.0;
					}


					if (!effectContext.option6){

					if (effectContext.mode == 0) {

						z = 2.0 * alpha * eFrameWork.get(step).getPixelDouble(band, x, y);
						z = z + (alpha * eFrameWork.get(0).getPixelDouble(band, x, y));
						z = z + (alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					} else if (effectContext.mode == 1) {
						z = 2.0 * alpha * eFrameWork.get(0).getPixelDouble(band, x, y);
						z = z - (2.0*alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					} else if (effectContext.mode == 2) {
						z = 2.0 * alpha * eFrameWork.get(step).getPixelDouble(band, x, y);
						z = z + (alpha * eFrameWork.get(0).getPixelDouble(band, x, y));
						z = z + (alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						z = z - (beta * eFrameOut.get(step).getPixelDouble(band, x, y));
						z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
					}
					} else {

					if (effectContext.mode == 0) {

						z = 2.0 * alpha * cell;
						z = z + (alpha * eFrameWork.get(0).getPixelHue( x, y));
						z = z + (alpha * eFrameWork.get(2*step).getPixelHue( x, y));
						z = z - (beta * cell);
						z = z + (gamma * eFrameOut.get(0).getPixelHue( x, y));
					} else if (effectContext.mode == 1) {
						z=cell;
					} else if (effectContext.mode == 2) {
						//z = 2.0 * alpha * cell;
						//z = z + (alpha * eFrameWork.get(0).getPixelDouble(band, x, y));
						//z = z + (alpha * eFrameWork.get(2*step).getPixelDouble(band, x, y));
						//z = z - (beta * cell);
						//z = z + (gamma * eFrameOut.get(0).getPixelDouble(band, x, y));
						z = (alpha * eFrameWork.get(0).getPixelHue( x, y))
							+ gamma * cell
						    - beta * cell;
					}
					}

					z = z * mfactor;

					if (z > 255)
						z = 255;
					if (z < -255)
						z = -255;
					if (effectContext.option2){
						z=z/2;
						z =z+255/2;
					}
					if (z < 0) {
						if(effectContext.option4) {
							z = 255 - z;
						} else if(effectContext.option5) {
							z = -z;
						} else {
							z = 0;
						}
					}

					//frameOut.setPixel(band, x, y, z);
					float[] values = new float[3];

					values[1] = (float)eFrameWork.get(0).getPixelSat(x, y);
					values[2] = (float)eFrameWork.get(0).getPixelInten(x, y);
					values[0] = (float)z;

					//frameOut.setPixel(band, x, y, z);
					//frameOut.setPixel(band, x, y, z);


					frameOut.setPixelHSI(x, y, values);

				//}
			}
		}
		return frameOut;
	}


	private EFrame doInternalGradient(EFrame frameIn) {

		float[] matrix = erosionMatrix;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}

		EFrame frameOut = doJAISubtract(frameIn, doErosion(frameIn, matrix));
		return frameOut;
	}

	private EFrame doExternalGradient(EFrame frameIn) {

		float[] matrix = dilationMatrix;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}

		EFrame frameOut = doJAISubtract(doDilation(frameIn, matrix), frameIn);
		return frameOut;
	}

	private EFrame doMorphGradient(EFrame frameIn) {

		float[][] matrix = morphGradientMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		EFrame frameOut = doJAISubtract(doDilation(frameIn, matrix[1]),
				doErosion(frameIn, matrix[0]));
		return frameOut;
	}

	private EFrame doBoundary(EFrame frameIn) {

		float[] matrix = erosionMatrix;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}

		EFrame frameOut = doJAISubtract(frameIn, doErosion(frameIn, matrix));
		return frameOut;
	}

	private EFrame doWhiteTopHat(EFrame frameIn) {

		float[][] matrix = WTHMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		EFrame frameOut = null;

		if (effectContext.option5) {
			System.out.println("WTH ");
			frameOut = doDilation(doErosion(frameIn, matrix[0]), matrix[1]);
		} else {
			frameOut = doJAISubtract(frameIn, doDilation(doErosion(frameIn,
					matrix[0]), matrix[1]));
		}
		return frameOut;
	}

	private EFrame doBlackTopHat(EFrame frameIn) {

		float[][] matrix = BTHMatrix;
		if (convolve instanceof float[][]) {
			matrix = (float[][]) convolve;
		}

		EFrame frameOut = doJAISubtract(doErosion(
				doDilation(frameIn, matrix[1]), matrix[0]), frameIn);
		return frameOut;
	}

	private EFrame doReconstruct(EFrame frameIn) {

		float[] matrix = dilationMatrix;
		if (convolve instanceof float[]) {
			matrix = (float[]) convolve;
		}

		double thresl1 = effectParams[5] * 255.0;
		double thresh1 = effectParams[6] * 255.0;
		double thresl2 = effectParams[7] * 255.0;
		double thresh2 = effectParams[8] * 255.0;
		EFrame frameF = doThreshold(frameIn, thresl1, thresh1);
		EFrame frameG = doThreshold(frameIn, thresl2, thresh2);
		EFrame frameOut = doDilation(frameF, matrix);
		frameOut = doJAIMin(frameOut, frameG);
		return frameOut;
	}

	private EFrame doWaterMark(EFrame frameIn) {

		EFrame frameOut = doWhiteTopHat(frameIn);
		frameOut = doReconstruct(frameOut);
		frameOut = doOpening(frameOut);
		frameOut = doClosing(frameOut);
		return frameOut;
	}

	private EFrame doReconstruct1(EFrame frameIn) {

		EFrame frameF = new EFrame(frameIn.getBuffer());

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		double thresxl = effectParams[5];
		double thresyl = effectParams[6];
		double thresxh = effectParams[7];
		double thresyh = effectParams[8];
		double thresm = effectParams[9] * 255.0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					if (effectContext.option5) {
						if (y > thresyl * height && y < thresyh * height
								&& x > thresxl * width && x < thresxh * width) {
							frameF.setPixel(band, x, y, thresm);
						}
					} else {
						if ((y < thresyl * height || y > thresyh * height)
								|| (x < thresxl * width || x > thresxh * width)) {
							frameF.setPixel(band, x, y, thresm);
						}
					}
				}
			}
		}

		frameF = doJAIMin(frameF, frameIn);

		frameF = doDilation(frameF, dilationMatrix);

		frameF = doJAIMin(frameF, frameIn);

		frameF = doDilation(frameF, dilationMatrix);
		frameF = doJAIMin(frameF, frameIn);

		return frameF;
	}

	private EFrame doDilation(EFrame frameIn, float[] mask) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double max;
		double level;
		double thres1 = effectParams[1] * 255.0;
		double thres2 = effectParams[2] * 255.0;

		for (int y = maskOffset; y < height - maskOffset; y++) {
			for (int x = maskOffset; x < width - maskOffset; x++) {
				for (int band = 0; band < numBands; band++) {
					max = 0;
					level = 0;
					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							if (effectContext.option1) {
								if (frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset) >= thres1) {
									level = frameIn.getPixelDouble(band, x + mx
											- maskOffset, y + my - maskOffset);
									if (frameIn.getPixelDouble(band, x + mx
											- maskOffset, y + my - maskOffset) >= thres2) {
										level += (double) mask[mx
												+ (my * maskSize)];
									}
								}
							} else if (effectContext.option2) {
								level = frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset);
								if (frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset) >= thres2) {
									level *= (double) mask[mx + (my * maskSize)];
								}
							} else {
								level = frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset);
								if (frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset) >= thres2) {
									level += (double) mask[mx + (my * maskSize)];
								}
							}
							if (level > max)
								max = level;
						}
					}
					if (max < 0.0)
						max = 0.0;
					if (max > 255.0)
						max = 255.0;
					frameOut.setPixel(band, x, y, max);
				}
			}
		}
		return frameOut;
	}

	private EFrame doErosion(EFrame frameIn, float[] mask) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int maskSize = (int) Math.sqrt(mask.length);
		int maskOffset = (maskSize - 1) / 2;
		double min;
		double level = 0;
		double thres1 = effectParams[3] * 255.0;
		double thres2 = effectParams[4] * 255.0;

		for (int y = maskOffset; y < height - maskOffset; y++) {
			for (int x = maskOffset; x < width - maskOffset; x++) {
				for (int band = 0; band < numBands; band++) {
					min = 255.0;
					level = 0.0;
					for (int my = 0; my < maskSize; my++) {
						for (int mx = 0; mx < maskSize; mx++) {
							if (effectContext.option3) {
								if (frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset) >= thres1) {
									level = frameIn.getPixelDouble(band, x + mx
											- maskOffset, y + my - maskOffset);
									if (frameIn.getPixelDouble(band, x + mx
											- maskOffset, y + my - maskOffset) >= thres2) {
										level -= (double) mask[mx
												+ (my * maskSize)];
									}
								}
							} else if (effectContext.option4) {
								level = frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset)
										/ (double) mask[mx + (my * maskSize)];
							} else {
								level = frameIn.getPixelDouble(band, x + mx
										- maskOffset, y + my - maskOffset)
										- (double) mask[mx + (my * maskSize)];

							}
							if (level < min)
								min = level;
						}
					}

					if (min < 0.0)
						min = 0.0;
					if (min > 255.0)
						min = 255.0;
					frameOut.setPixel(band, x, y, min);
				}
			}
		}
		return frameOut;
	}

	private EFrame doCompose(EFrame frameIn) {
		System.out.println("in Compo");
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameCompo = new EFrame(frameIn.getBuffer());

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		double xoffp = 1.0 - effectParams[1];
		double yoffp = 1.0 - effectParams[2];
		double xscalep = effectParams[3];
		double yscalep = effectParams[4];

		double taoffp = 1.0 - effectParams[5];
		double alphaA = effectParams[6];
		double alphaB = effectParams[7];
		double alphaM = effectParams[8];
		double tboffp = 1.0 - effectParams[9];

		int compoCount = effectContext.depth + 1;
		int compoStep = effectContext.step + 1;
		int compoNumber = effectContext.count;

		Image broll = null;
		Image brolle = null;
		Image arollw = null;

		Image mask = null;
		Time aStartTime = null;
		Time aEndTime = null;
		Time bStartTime = null;
		Time bEndTime = null;
		int bStartFrame = 0;
		int bEndFrame = 0;

		Time mStartTime = null;
		Time mEndTime = null;
		EJEffects compoEffect = null;

		int bwidth = 0;
		int bheight = 0;
		Dimension size = null;
		RGBFormat newFormat = null;
		float frameRate = 0.0F;
		int flipped = 0;
		int endian = 0;
		int maxDataLength = 0;
		int lineStride = 0;
		Buffer newBuffer = null;
		int effectCount = ejSettings.effectList.size();
		int compoMode = effectContext.mode;
		int effectNumber = 1;
		boolean squeeze = false;
		int xnum = 0;
		int ynum = 0;
		if (effectContext.option3) {
			squeeze = true;
			xnum = (int) (100.0 * xscalep);
			ynum = (int) (100.0 * yscalep);
			compoNumber = xnum * ynum;
			xoffp = (double) 1.0 / (double) xnum;
			yoffp = (double) 1.0 / (double) ynum;
			System.out.println("squeeeze: " + xnum + ", " + ynum + ", " + xoffp
					+ ", " + yoffp);
		}

		if (effectContext.option1 && compoNumber > 0 && compoEffectList == null
				&& effectCount > 1) {
			compoEffectList = new ArrayList();
			for (int i = 1; i <= compoNumber; i++) {
				compoEffect = new EJEffects(ejMain);
				effectNumber++;
				if (effectNumber > effectCount)
					effectNumber = 2;
				compoEffect.setEffectNumber(effectNumber);
				compoEffectList.add(compoEffect);
			}
		}

		if (ejSettings.effectCompo.grabberA != null) {
			aStartTime = ejSettings.effectCompo.grabberA.getBeginTime();
			aEndTime = ejSettings.effectCompo.grabberA.getEndTime();
			System.out.println("aroll times " + aStartTime.getNanoseconds()
					+ ", " + aEndTime.getNanoseconds() + ", "
					+ (long) currentTime);
		} else {
			System.out.println("no aroll !!");
			return frameOut;
		}

		long aTimeLength = aEndTime.getNanoseconds()
				- aStartTime.getNanoseconds();
		long aTimeOffset = (long) ((double) aTimeLength * taoffp);
		long compoTime = aTimeOffset + aStartTime.getNanoseconds();

		long bTimeLength = 0;
		long bTimeOffset = 0;
		long bcompoTime = 0;
		long stepTime = 0;

		if (!effectContext.option4) {
			if (compoTime > (long) currentTime) {
				System.out.println("before aroll time offset");
				return frameOut;
			}
		}

		int targetFrame = 0;
		int workFrame = 0;
		if (ejSettings.effectCompo.grabberB != null
				&& ejSettings.effectCompo.grabberB.testMediaFile()) {

			bStartTime = ejSettings.effectCompo.grabberB.getBeginTime();
			bEndTime = ejSettings.effectCompo.grabberB.getEndTime();

			// Validate B-roll times before proceeding
			if (bStartTime == null || bEndTime == null) {
				System.err.println("DoCompose: B-roll start/end time is null, skipping B-roll");
				return frameOut;
			}

			bStartFrame = ejSettings.effectCompo.grabberB.getBeginFrame();
			bEndFrame = ejSettings.effectCompo.grabberB.getEndFrame();

			bTimeLength = bEndTime.getNanoseconds()
					- bStartTime.getNanoseconds();

			// Validate time length
			if (bTimeLength <= 0) {
				System.err.println("DoCompose: B-roll time length is invalid (" + bTimeLength + "), skipping B-roll");
				return frameOut;
			}

			bTimeOffset = (long) ((double) bTimeLength * tboffp);
			bcompoTime = bTimeOffset + bStartTime.getNanoseconds();
			System.out.println("broll :" + bcompoTime + ", " + bTimeOffset
					+ ", " + bStartTime.getNanoseconds());

			stepTime = 0;

			if (effectContext.option4) {
				stepTime = (long) ((double) bTimeLength * taoffp);
				System.out.println("steptime :" + stepTime);

			}
			if (effectContext.option2) {
				// Calculate which B-roll frame to use based on A-roll sequence number
				// Map A-roll frames to B-roll frames
				int brollFrameCount = bEndFrame - bStartFrame + 1;
				if (brollFrameCount > 0) {
					// Use seqNumber to step through B-roll frames
					// compoStep controls how many B-roll frames to advance per A-roll frame
					targetFrame = bStartFrame + ((seqNumber - 1) * compoStep) % brollFrameCount;
				} else {
					targetFrame = bStartFrame;
				}
				if (targetFrame > bEndFrame)
					targetFrame = bEndFrame;
				if (targetFrame < bStartFrame) {
					targetFrame = bStartFrame;
				}
				System.out.println("Broll calc: seqNumber=" + seqNumber +
						", brollFrameCount=" + brollFrameCount +
						", compoStep=" + compoStep +
						", targetFrame=" + targetFrame +
						", bStartFrame=" + bStartFrame +
						", bEndFrame=" + bEndFrame);
				broll = ejSettings.effectCompo.grabberB.grabFrame(targetFrame);
				System.out.println("Broll frames " + targetFrame + ", "
						+ bStartFrame + ", " + bEndFrame + ", " + compoStep
						+ ", " + compoCount + ", " + seqNumber);

				// Check if frame grab failed
				if (broll == null) {
					System.err.println("DoCompose: Failed to grab B-roll frame " + targetFrame);
				}

			} else {
				long nextTime = (long) currentTime + bcompoTime
						+ bStartTime.getNanoseconds();

				//while (nextTime >= bEndTime.getNanoseconds()){
				//	nextTime = nextTime - (bEndTime.getNanoseconds() -
				// bcompoTime);
				//}
				broll = null;
				if (nextTime > bEndTime.getNanoseconds()) {
					nextTime = bEndTime.getNanoseconds();
				}
				// Ensure nextTime is not negative
				if (nextTime < 0) {
					nextTime = 0;
				}

				broll = ejSettings.effectCompo.grabberB.grabImage(nextTime);
				//broll =
				// ejSettings.effectCompo.grabberB.grabImage((long)currentTime-compoTime+bStartTime.getNanoseconds());
				System.out.println("broll time:" + currentTime + ", "
						+ bcompoTime + ", " + bStartTime.getNanoseconds());

				// Check if image grab failed
				if (broll == null) {
					System.err.println("DoCompose: Failed to grab B-roll image at time " + nextTime);
				}

			}
			if (broll != null) {
				// Validate broll dimensions before scaling
				int brollWidth = broll.getWidth(null);
				int brollHeight = broll.getHeight(null);
				if (brollWidth <= 0 || brollHeight <= 0) {
					System.err.println("DoCompose: B-roll has invalid dimensions (" + brollWidth + "x" + brollHeight + "), skipping");
					broll = null;
				} else {
					System.out.println("DoCompose Scale broll " + brollWidth + ", " + brollHeight);
					if (!squeeze) {
						if ((float) (width * xscalep) != (float) brollWidth
								|| (float) (height * yscalep) != (float) brollHeight) {
							broll = doJAIScale(broll, (float) width
									* (float) xscalep
									/ (float) brollWidth, (float) height
									* (float) yscalep
									/ (float) brollHeight, 0.0F, 0.0F);
						}
					} else {
						if ((float) ((effectParams[1]/effectParams[2])*width * xoffp) != brollWidth
								|| (float) (height * (effectParams[1]/effectParams[2])*yoffp) != (float) brollHeight) {
							broll = doJAIScale(broll, (float) width * (float) (effectParams[1]/effectParams[2])*(float) xoffp
									/ (float) brollWidth,
									(float) height * (float) (effectParams[1]/effectParams[2])*(float) yoffp
											/ (float) brollHeight, 0.0F,
									0.0F);
						}
					}

					//frameOut = doWarpCompo(frameOut,broll);

					if (broll != null) {
						System.out.println("DoCompose After Scale broll "
								+ broll.getWidth(null) + ", " + broll.getHeight(null));
					}
				}
			}
		}
		System.out.println("compose test a");

		if (ejSettings.effectCompo.grabberM != null
				&& ejSettings.effectCompo.grabberM.testMediaFile()) {

			mStartTime = ejSettings.effectCompo.grabberM.getBeginTime();
			mEndTime = ejSettings.effectCompo.grabberM.getEndTime();

			long nextTime = (long) currentTime - bcompoTime
					+ mStartTime.getNanoseconds();

			//while (nextTime >= mEndTime.getNanoseconds()){
			//	nextTime = nextTime - (mEndTime.getNanoseconds() - bcompoTime);
			//}

			mask = null;
			if (nextTime > mEndTime.getNanoseconds()) {
				nextTime = mEndTime.getNanoseconds();
			}

			mask = ejSettings.effectCompo.grabberM.grabImage(nextTime);
			if (mask != null) {
				System.out.println("DoCompose Scale mask");
				if (!squeeze) {
					if ((float) (width * xscalep) != (float) mask
							.getWidth(null)
							|| (float) (height * yscalep) != (float) mask
									.getHeight(null)) {

						mask = doJAIScale(mask, (float) width * (float) xscalep
								/ (float) mask.getWidth(null), (float) height
								* (float) yscalep
								/ (float) mask.getHeight(null), 0.0F, 0.0F);
					}
				} else {
					if ((float) (width * xoffp) != (float) mask.getWidth(null)
							|| (float) (height * yoffp) != (float) mask
									.getHeight(null)) {
						mask = doJAIScale(mask, (float) width * (float) xoffp
								/ (float) mask.getWidth(null), (float) height
								* (float) yoffp / (float) mask.getHeight(null),
								0.0F, 0.0F);
					}
				}
			}

		}
		System.out.println("compose test b");

		double[] alphaControls = new double[3];
		alphaControls[0] = alphaA;
		alphaControls[1] = alphaB;
		alphaControls[2] = alphaM;

		System.out.println("DoCompose call mask" + compoNumber);
		int xoffset = 0, yoffset = 0;
		if (compoNumber == 0) {
			xoffset = (int) (xoffp * (double) width);
			yoffset = (int) (yoffp * (double) height);
			// Only composite if broll is available
			if (broll != null) {
				frameOut.composite(broll, mask, alphaControls, compoMode, xoffset,
						yoffset, effectContext.option5, effectContext.option6);
			} else {
				System.out.println("DoCompose: Skipping composite - no B-roll available");
			}
		} else {
			if (effectContext.option6) {
				xoffset = width - (int) Math.ceil((xoffp * (double) width));
			}
			for (int i = 1; i <= compoNumber && yoffset < height; i++) {
				System.out.println("compo offset " + xoffset + ", " + yoffset
						+ ", " + i + ", " + compoNumber);

				// Skip this iteration if broll is null
				if (broll == null) {
					System.out.println("DoCompose: Skipping iteration " + i + " - no B-roll available");
				} else if (compoEffectList != null) {
					System.out.println("compo effct list "
							+ compoEffectList.size());
					newBuffer = frameIn.getBuffer();
					bheight = broll.getHeight(null);
					bwidth = broll.getWidth(null);
					size = new Dimension(bwidth, bheight);
					newFormat = (RGBFormat) frameIn.getBuffer().getFormat();
					frameRate = newFormat.getFrameRate();
					flipped = newFormat.getFlipped();
					endian = newFormat.getEndian();
					maxDataLength = size.width * size.height * 3;
					lineStride = size.width * 3;
					newFormat = new RGBFormat(size, maxDataLength,
							Format.byteArray, frameRate, 24, 3, 2, 1, 3,
							lineStride, flipped, endian);

					newBuffer.setFormat(newFormat);
					newBuffer.setLength(maxDataLength);
					newBuffer.setOffset(0);
					updateOutput(newBuffer, newFormat, maxDataLength, 0);
					validateByteArraySize(newBuffer, maxDataLength);
					frameCompo = new EFrame(newBuffer);
					frameCompo.setImage(broll);
					newBuffer.setData(frameCompo.getData());
					int index = i - 1;
					if (index >= compoEffectList.size())
						index = compoEffectList.size() - 1;
					compoEffect = (EJEffects) compoEffectList.get(index);
					System.out.println("DoCompose: calling processFrame on compoEffect");
					try {
						compoEffect.processFrame(newBuffer);
						System.out.println("DoCompose: processFrame completed");
					} catch (Exception e) {
						System.err.println("DoCompose: processFrame threw exception: " + e.getMessage());
						e.printStackTrace();
					}

					System.out.println("DoCompose: creating BufferToImage");
					BufferToImage btoi = new BufferToImage(
							(VideoFormat) newBuffer.getFormat());
					brolle = btoi.createImage(newBuffer);
					System.out.println("DoCompose: BufferToImage.createImage returned " + (brolle != null ? "image" : "null"));

					// Fallback if BufferToImage fails - use the processed frame directly
					if (brolle == null) {
						System.out.println("DoCompose: BufferToImage failed for processed frame, using EFrame fallback");
						EFrame processedFrame = new EFrame(newBuffer);
						RenderedOp rop = processedFrame.getRenderedOp();
						if (rop != null) {
							brolle = rop.getAsBufferedImage();
							System.out.println("DoCompose: EFrame fallback returned " + (brolle != null ? "image" : "null"));
						}
					}

					if (brolle != null) {
						System.out.println("DoCompose: calling composite with brolle");
						frameOut.composite(brolle, mask, alphaControls, compoMode,
								xoffset, yoffset);
						System.out.println("DoCompose: composite completed");
					} else {
						System.err.println("DoCompose: Failed to create image for composite, skipping");
					}

				} else {
					frameOut.composite(broll, mask, alphaControls, compoMode,
							xoffset, yoffset,effectContext.option5, effectContext.option6);
				}
				if (!effectContext.option6) {
					xoffset = xoffset
							+ (int) Math.ceil((xoffp * (double) width));
					if (xoffset >= width) {
						xoffset = 0;
						yoffset = yoffset
								+ (int) Math.ceil((yoffp * (double) height));
					}
				} else {
					xoffset = xoffset
							- (int) Math.ceil((xoffp * (double) width));
					if (xoffset < 0) {
						xoffset = width
								- (int) Math.ceil((xoffp * (double) width));
						yoffset = yoffset
								+ (int) Math.ceil((yoffp * (double) height));
					}
				}

				if (effectContext.option4) {
					long nextTime = 0;
					if (effectContext.option5) {
						nextTime = (long) currentTime + bcompoTime
								+ (i * stepTime) + bStartTime.getNanoseconds();
						while (nextTime > bEndTime.getNanoseconds()) {
							nextTime = nextTime
									- (bEndTime.getNanoseconds() - bcompoTime);
						}
					} else {
						nextTime = (long) currentTime + bcompoTime
								- (i * stepTime) + bStartTime.getNanoseconds();
						while (nextTime < 0) {
							nextTime = nextTime
									+ ((long) Math.abs(bcompoTime
											- bStartTime.getNanoseconds()));
						}
					}
					broll = ejSettings.effectCompo.grabberB.grabImage(nextTime);
					System.out.println("broll i time:" + i + ", " + nextTime);

					if (broll != null) {
						System.out.println("DoCompose Scale broll "
								+ broll.getWidth(null) + ", "
								+ broll.getHeight(null));
						if (!squeeze) {
							broll = doJAIScale(broll, (float) width
									* (float) xscalep
									/ (float) broll.getWidth(null),
									(float) height * (float) yscalep
											/ (float) broll.getHeight(null),
									0.0F, 0.0F);
						} else {
							broll = doJAIScale(broll, (float) width
									* (float) (effectParams[1]/effectParams[2])*(float) xoffp
									/ (float) broll.getWidth(null),
									(float) height * (float) (effectParams[1]/effectParams[2])*(float) yoffp
											/ (float) broll.getHeight(null),
									0.0F, 0.0F);

						}

					}

				}
			}
		}

		return frameOut;

	}

	private Image doJAIScale(Image imageIn, float scalex, float scaley,
			float offsetx, float offsety) {

		RenderedImage src = JAI.create("AWTImage", imageIn);
		ParameterBlock pb;
		if (!(src.getSampleModel() instanceof ComponentSampleModel)) {
			//System.out.println("In JAI Scale 5 - creating samplemodel");
			SampleModel sampleModel = RasterFactory
					.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
							src.getWidth(), src.getHeight(), 3);
			//System.out.println("In JAI Scale 5 - 2");

			ImageLayout layout = new ImageLayout();
			layout.setSampleModel(sampleModel);
			//layout.setTileWidth(640).setTileHeight(480);
			RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					layout);
			pb = (new ParameterBlock()).addSource(src);
			//System.out.println("In JAI Scale 5 - 3");

			src = JAI.create("format", pb, hints);
		}
		// Scale the image by the specified factor.

		pb = (new ParameterBlock()).addSource(src);
		pb.add((float) scalex);
		pb.add((float) scaley);
		pb.add((float) offsetx);
		pb.add((float) offsety);
		
		Interpolation interp = Interpolation
				.getInstance(Interpolation.INTERP_BICUBIC);
		//Interpolation interp =
		// Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

		pb.add(interp);
		BorderExtender extender = BorderExtender
				.createInstance(BorderExtender.BORDER_ZERO);
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				extender);
		RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				BorderExtender.createInstance(BorderExtender.BORDER_COPY));

		rh.put(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		rh.put(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		
		PlanarImage jaiImage = JAI.create("Scale", pb, hints);
		//System.out.println("In JAI Scale 5 - 5");

		BufferedImage biOut = jaiImage.getAsBufferedImage();
		//System.out.println("In JAI Scale 5 - 6");

		return biOut;
	}

	private RenderedOp doJAIScaleRop(Image imageIn, float scalex, float scaley,
			float offsetx, float offsety) {

		RenderedImage src = JAI.create("AWTImage", imageIn);
		ParameterBlock pb;
		if (!(src.getSampleModel() instanceof ComponentSampleModel)) {
			//System.out.println("In JAI Scale 5 - creating samplemodel");
			SampleModel sampleModel = RasterFactory
					.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
							src.getWidth(), src.getHeight(), 3);
			//System.out.println("In JAI Scale 5 - 2");

			ImageLayout layout = new ImageLayout();
			layout.setSampleModel(sampleModel);
			//layout.setTileWidth(640).setTileHeight(480);
			RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					layout);
			pb = (new ParameterBlock()).addSource(src);
			//System.out.println("In JAI Scale 5 - 3");

			src = JAI.create("format", pb, hints);
		}
		// Scale the image by the specified factor.
		System.out.println("In JAI Scale " + scalex + ", " + scaley + ", "
				+ offsetx + ", " + offsety);

		pb = (new ParameterBlock()).addSource(src);
		pb.add((float) scalex);
		pb.add((float) scaley);
		pb.add((float) offsetx);
		pb.add((float) offsety);
		Interpolation interp = Interpolation
				.getInstance(Interpolation.INTERP_NEAREST);
		//Interpolation interp =
		// Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

		pb.add(interp);
		BorderExtender extender = BorderExtender
				.createInstance(BorderExtender.BORDER_ZERO);
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				extender);
		RenderedOp jaiImage = JAI.create("Scale", pb, hints);
		return jaiImage;
	}

	private Image doJAICrop(Image imageIn, float scalex, float scaley,
			float offsetx, float offsety) {

		RenderedImage src = JAI.create("AWTImage", imageIn);
		ParameterBlock pb;
		if (!(src.getSampleModel() instanceof ComponentSampleModel)) {
			//System.out.println("In JAI Scale 5 - creating samplemodel");
			SampleModel sampleModel = RasterFactory
					.createPixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
							src.getWidth(), src.getHeight(), 3);
			//System.out.println("In JAI Scale 5 - 2");

			ImageLayout layout = new ImageLayout();
			layout.setSampleModel(sampleModel);
			//layout.setTileWidth(640).setTileHeight(480);
			RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					layout);
			pb = (new ParameterBlock()).addSource(src);
			//System.out.println("In JAI Scale 5 - 3");

			src = JAI.create("format", pb, hints);
		}
		// Scale the image by the specified factor.
		RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				BorderExtender.createInstance(BorderExtender.BORDER_COPY));

		rh.put(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		rh.put(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		//rh.put(RenderingHints.KEY_RENDERING,
		// RenderingHints.VALUE_RENDER_QUALITY);

		pb = (new ParameterBlock()).addSource(src);
		pb.add((float) scalex);
		pb.add((float) scaley);
		pb.add((float) offsetx);
		pb.add((float) offsety);
		PlanarImage jaiImage = JAI.create("crop", pb, rh);
		BufferedImage biOut = jaiImage.getAsBufferedImage();
		return biOut;
	}

	private EFrame doWarpPath(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		Image img = frameOut.getImage();
		if (warpPath == null) {
			warpPath = new WarpImagePath();
		}
		Dimension size = new Dimension(width, height);

		warpPath.setParams(effectParams[1], effectParams[2], effectParams[3],
				effectParams[4], effectParams[5], effectParams[6],
				effectParams[7], effectParams[8], effectParams[9]);

		warpPath.setSize(size);

		warpPath.init(img);
		frameOut.clearBuffer();
		BufferedImage bimg = frameOut.getBufferedImage();
		Graphics2D g2d = bimg.createGraphics();
		warpPath.paint(g2d);
		frameOut.setBufferedImage(bimg);
		return frameOut;
	}

	public class WarpMgr {

		public WarpMgr(EFrame frame, double r, double g, double b) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.frame = frame;
		}

		double r, g, b;

		EFrame frame;
	}

	private EFrame doWarpCompo(EFrame frameIn, Image image) {
		EFrame frameWork = new EFrame(frameIn.getBuffer());
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameWarp = new EFrame(frameIn.getBuffer());
		EFrame frameSave = null;
		frameWarp.setImage(image);
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		double level = 0;
		double r = 0, g = 0, b = 0;
		double rl = 0, gl = 0, bl = 0;
		double wp1 = 0, wp2 = 0, wp3 = 0;

		double lastLevel = 0;
		ArrayList warped = new ArrayList();
		WarpMgr warpMgr = null;

		for (int x = 2; x < width - 2; x++) {
			for (int y = 2; y < height - 2; y++) {
				level = frameWarp.getPixelDouble(0, x, y)
						+ frameWarp.getPixelDouble(1, x, y)
						+ frameWarp.getPixelDouble(2, x, y);
				r = frameWarp.getPixelDouble(EFrame.RED, x, y);
				g = frameWarp.getPixelDouble(EFrame.GREEN, x, y);
				b = frameWarp.getPixelDouble(EFrame.BLUE, x, y);

				if ((Math.abs(r - rl) > 10) || (Math.abs(g - gl) > 10)
						|| (Math.abs(b - bl) > 10)) {

					frameSave = null;
					int i = 0;
					for (i = 0; i < warped.size(); i++) {
						warpMgr = (WarpMgr) warped.get(i);
						if ((Math.abs(r - warpMgr.r) < 10)
								&& (Math.abs(g - warpMgr.g) < 10)
								&& (Math.abs(b - warpMgr.b) < 10)) {
							frameSave = warpMgr.frame;
							break;
						}
					}

					if (frameSave == null) {
						if (warped.size() >= 10) {
							frameSave = ((WarpMgr) warped.get(0)).frame;
						}
					}

					if (frameSave == null) {
						lastLevel = level;
						rl = r;
						gl = g;
						bl = b;

						Image img = frameOut.getImage();
						warpPath = new WarpImagePath();
						Dimension size = new Dimension(width, height);
						wp1 = frameWarp.getPixelDouble(EFrame.RED, x, y) / 255.0;
						wp2 = frameWarp.getPixelDouble(EFrame.BLUE, x, y) / 255.0;
						wp3 = frameWarp.getPixelDouble(EFrame.GREEN, x, y) / 255.0;

						warpPath.setParams(wp1 / 3, wp2 / 3, wp3 / 3, wp1 / 2,
								wp2 / 2, wp3 / 2, wp1, wp2, wp3);

						warpPath.setSize(size);

						warpPath.init(img);
						frameWork.clearBuffer();
						BufferedImage bimg = frameIn.getBufferedImage();
						Graphics2D g2d = bimg.createGraphics();
						warpPath.paint(g2d);
						frameWork.setBufferedImage(bimg);
						warpMgr = new WarpMgr(frameWork, r, g, b);
						warped.add(warpMgr);
						System.out.println("Added warp: " + warped.size()
								+ ", " + level + ", " + lastLevel + ", 	" + x
								+ ", " + y);

					} else {

						frameWork = frameSave;
					}

					for (int band = 0; band < numBands; band++) {
						level = frameWork.getPixelDouble(band, x, y);
						frameOut.setPixel(band, x, y, level);
					}
				}
			}
		}

		for (int i1 = 0; i1 < warped.size(); i1++) {
			warped.set(i1, null);
		}
		warped = null;
		System.gc();

		return frameOut;
	}

	private EFrame doWaterRipple(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		Image img = frameOut.getImage();
		if (waterRipple == null) {
			waterRipple = new WaterApplet();
			Dimension size = new Dimension(width, height);
			waterRipple.setSize(size);
			waterRipple.init();
		}
		//waterRipple.setParams(effectParams[1], effectParams[2],
		//		effectParams[3], effectParams[4], effectParams[5],
		//		effectParams[6], effectParams[7], effectParams[8],
		//		effectParams[9], effectContext.mode);

		frameOut.clearBuffer();
		waterRipple.run(img);
		BufferedImage bimg = waterRipple.getBufferedImage();
		frameOut.setBufferedImage(bimg);
		return frameOut;
	}

	private EFrame doCoco(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameWork = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		frameWork.clearBuffer();
		double level, mean;
		double[] sr = null;
		int factor = (int) ((effectParams[1] * 100.0) + 1.0);
		int coc_a, coc_b, coc_c;
		int count = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					coc_a = frameIn.getPixelInt(band, x - 1, y);
					coc_b = frameIn.getPixelInt(band, x, y);
					coc_c = frameIn.getPixelInt(band, x + 1, y);
					frameOut.setPixel(band, coc_b, coc_c, frameOut
							.getPixelDouble(band, coc_b, coc_c)
							+ factor);
					frameOut.setPixel(band, coc_b, coc_a, frameOut
							.getPixelDouble(band, coc_b, coc_a)
							+ factor);
				}
			}
		}
		return frameOut;

	}

	private EFrame doCoco2(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameWork = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		double level, mean;
		int factor = (int) ((effectParams[1] * 100.0) + 1.0);
		double dirs = effectParams[3] / effectParams[4];
		double dist = effectParams[2] * 100.0;
		int coc_a, coc_b, coc_c;
		int xc, yc;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					for (int r = 1; r < dist; r++) {
						for (double theta = 0; theta < 2 * Math.PI; theta += (2 * Math.PI / dirs)) {
							xc = (int) Math.floor(x + r * Math.cos(theta));
							yc = (int) Math.floor(y + r * Math.sin(theta));

							if ((yc >= 0) && (yc < height) && (xc >= 0)
									&& (xc < width)) {
								coc_a = frameIn.getPixelInt(band, x, y);
								coc_b = frameIn.getPixelInt(band, xc, yc);
								frameOut.setPixel(band, coc_a, coc_b, frameOut
										.getPixelDouble(band, coc_a, coc_b)
										+ factor);
							}
						}
					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doNormalise(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double[] thres = new double[numBands];
		double[] bandMax = new double[numBands];
		double[] bandMin = new double[numBands];

		double level = 0;
		int x, y, band;
		for (band = 0; band < numBands; band++) {
			thres[band] = effectParams[band + 1] * 255.0;
			bandMax[band] = 0;
			bandMin[band] = 255.0;
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					level = frameIn.getPixelDouble(band, x, y);
					if (bandMax[band] < level)
						bandMax[band] = level;
					if (bandMin[band] > level)
						bandMin[band] = level;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					level = frameIn.getPixelDouble(band, x, y);
					if (effectContext.option1) {

						frameOut
								.setPixel(
										band,
										x,
										y,
										(Math.log((double) level
												- (double) bandMin[band]) * (double) thres[band])
												/ Math
														.log(((double) bandMax[band] - (double) bandMin[band])));
					} else {
						frameOut
								.setPixel(
										band,
										x,
										y,
										(((double) level - (double) bandMin[band]) * (double) thres[band])
												/ ((double) bandMax[band] - (double) bandMin[band]));

					}
				}
			}
		}
		return frameOut;

	}

	private EFrame doThinning(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		boolean done = false;
		int greyLevel = 0;
		int W = 255;
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level;
		int count = 0;

		int x, y, band;
		frameOut1.clearBuffer();
		boolean test = true;

		for (band = 0; band < numBands; band++) {
			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					sr = getSampleRegion33(frameOut, band, x, y);
					count = 0;
					if (sr[0] >= thres)
						count++;
					if (sr[1] >= thres)
						count++;
					if (sr[2] >= thres)
						count++;
					if (sr[3] >= thres)
						count++;
					if (sr[5] >= thres)
						count++;
					if (sr[6] >= thres)
						count++;
					if (sr[7] >= thres)
						count++;
					if (sr[8] >= thres)
						count++;
					if (count >= 6 || count <= 2) {
						level = frameOut.getPixelDouble(band, x, y);
						if (level >= thres) {

							if (sr[1] < thres && sr[3] < thres && sr[5] < thres
									&& sr[7] < thres) {
								if (test
										|| (sr[0] >= thres && sr[2] < thres
												&& sr[6] < thres
												&& sr[8] < thres && sr[4] < thres)
										|| (sr[0] >= thres && sr[2] < thres
												&& sr[6] < thres && sr[8] < thres)
										|| (sr[2] >= thres && sr[0] < thres
												&& sr[6] < thres && sr[8] < thres)
										|| (sr[6] >= thres && sr[2] < thres
												&& sr[0] < thres && sr[8] < thres)
										|| (sr[8] >= thres && sr[2] < thres
												&& sr[6] < thres && sr[0] < thres)) {
									System.out.println("cleared aa " + band
											+ ", " + x + ", " + y + ", "
											+ count);
									System.out.println(sr[0] + ", " + sr[1]
											+ ", " + sr[2] + ", " + sr[3]
											+ ", " + sr[4] + ", " + sr[5]
											+ ", " + sr[6] + ", " + sr[7]
											+ ", " + sr[8]);

									frameOut1.setPixel(band, x, y, 0);
								}
							}
						}
					}
				}
			}
		}
		for (band = 0; band < numBands; band++) {
			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					level = frameOut1.getPixelDouble(band, x, y);
					if (level >= thres) {
						frameOut.setPixel(band, x, y, 0);
						System.out.println("cleared a " + band + ", " + x
								+ ", " + y);

					}
				}
			}
		}

		frameOut1.clearBuffer();

		for (band = 0; band < numBands; band++) {
			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					sr = getSampleRegion33(frameOut, band, x, y);
					count = 0;
					if (sr[0] >= thres)
						count++;
					if (sr[1] >= thres)
						count++;
					if (sr[2] >= thres)
						count++;
					if (sr[3] >= thres)
						count++;
					if (sr[5] >= thres)
						count++;
					if (sr[6] >= thres)
						count++;
					if (sr[7] >= thres)
						count++;
					if (sr[8] >= thres)
						count++;
					if (count >= 6 || count <= 2) {
						level = frameOut.getPixelDouble(band, x, y);
						if (level >= thres) {
							if (sr[1] < thres && sr[3] < thres && sr[5] < thres
									&& sr[7] < thres) {
								if (test
										|| (sr[0] >= thres && sr[2] < thres
												&& sr[6] < thres
												&& sr[8] < thres && sr[4] < thres)
										|| (sr[0] >= thres && sr[2] < thres
												&& sr[6] < thres && sr[8] < thres)
										|| (sr[2] >= thres && sr[0] < thres
												&& sr[6] < thres && sr[8] < thres)
										|| (sr[6] >= thres && sr[2] < thres
												&& sr[0] < thres && sr[8] < thres)
										|| (sr[8] >= thres && sr[2] < thres
												&& sr[6] < thres && sr[0] < thres)) {
									System.out.println("cleared bb " + band
											+ ", " + x + ", " + y + ", "
											+ count);

									frameOut1.setPixel(band, x, y, thres);
								}
							}
						}
					}
				}
			}
		}
		for (band = 0; band < numBands; band++) {
			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					level = frameOut1.getPixelDouble(band, x, y);
					if (level >= thres) {
						frameOut.setPixel(band, x, y, 0);
						System.out.println("cleared b " + band + ", " + x
								+ ", " + y);
					}
				}
			}
		}

		return frameOut;
	}

	private EFrame doThinning2(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		boolean done = false;
		int greyLevel = 0;
		int W = 255;
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level;
		int count = 0;
		int x, y, band;

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				level = frameOut.getPixelDouble(x, y);
				sr = getSampleRegion33(frameIn, x, y);

				for (band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						level = frameIn.getPixelDouble(band, x, y);
						sr = getSampleRegion33(frameIn, band, x, y);
					}
					if (level >= thres) {
						sr = getSampleRegion33(frameOut, band, x, y);
						count = 0;
						if (sr[0] >= thres)
							count++;
						if (sr[1] >= thres)
							count++;
						if (sr[2] >= thres)
							count++;
						if (sr[3] >= thres)
							count++;
						if (sr[5] >= thres)
							count++;
						if (sr[6] >= thres)
							count++;
						if (sr[7] >= thres)
							count++;
						if (sr[8] >= thres)
							count++;
						if (count > 2 && count < 8) {
							count = 0;
							for (int m = 0; m <= 7; m++) {
								if (sr[m] == 0 && sr[m + 1] >= thres)
									count++;
							}
							if (count == 1) {
								frameOut.setPixel(band, x, y, 0);
							}
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doBand(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double[] start = new double[numBands];
		double[] end = new double[numBands];
		double[] via = new double[numBands];
		Time aStartTime = null;
		Time aEndTime = null;
		double div = effectParams[7];
		double div2 = effectParams[8];
		double thres = (255.0 - (255.0 * effectParams[9]));

		int aStep = effectContext.depth + 1;
		int bStep = effectContext.step + 1;

		int x, y, band;
		for (band = 0; band < numBands; band++) {
			start[band] = effectParams[band + 1] * 255.0;
		}
		for (band = 0; band < numBands; band++) {
			end[band] = effectParams[band + 4] * 255.0;
		}

		double value = 0;
		double avgStartR = 0.0, avgStartG = 0.0, avgStartB = 0.0;
		double avgEndR = 0.0, avgEndG = 0.0, avgEndB = 0.0;

		int numStartR = 0, numStartG = 0, numStartB = 0, numEndR = 0, numEndG = 0, numEndB = 0;

		if (ejSettings.effectCompo.grabberA != null) {
			aStartTime = ejSettings.effectCompo.grabberA.getBeginTime();
			aEndTime = ejSettings.effectCompo.grabberA.getEndTime();

		}

		double level, mean;
		double startValue, endValue, thisValue;
		int count;

		count = seqNumber % (aStep + bStep);

		double[] sr = null;
		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				double rand = Math.random();

				for (band = 0; band < numBands; band++) {

					thisValue = frameIn.getPixelDouble(band, x, y);
					startValue = start[band];
					endValue = end[band];
					if (thres > thisValue) {
						startValue = thisValue;
						endValue = thisValue;
					}

					if (effectContext.option4) {
						if (startValue >= thisValue) {

							startValue = (double) Math.abs(startValue
									- thisValue);
						} else {
							startValue = (double) Math.abs(thisValue
									- (startValue));
						}
					}

					if (effectContext.option5) {
						if (endValue >= thisValue) {

							endValue = (double) Math.abs(endValue - thisValue);
						} else {
							endValue = (double) Math
									.abs(thisValue - (endValue));
						}

					}

					if (effectContext.option6) {
						if (startValue >= endValue) {

							endValue = (double) Math.abs(startValue - endValue);
						} else {
							endValue = (double) Math.abs(endValue
									- (startValue));
						}
					}

					if (effectContext.option1) {

						if (!effectContext.option2) {

							if (count < bStep) {
								if (!effectContext.option3) {

									value = endValue;

									if (band == 0) {
										avgEndR += endValue;
										numEndR++;
									} else if (band == 1) {
										avgEndG += endValue;
										numEndG++;
									} else if (band == 2) {
										avgEndB += endValue;
										numEndB++;
									}

								} else {

									if (rand > (double) bStep
											/ (double) (aStep + bStep)) {
										value = startValue;
										if (band == 0) {
											avgStartR += startValue;
											numStartR++;
										} else if (band == 1) {
											avgStartG += startValue;
											numStartG++;
										} else if (band == 2) {
											avgStartB += startValue;
											numStartB++;
										}

									} else {
										value = endValue;

										if (band == 0) {
											avgEndR += endValue;
											numEndR++;
										} else if (band == 1) {
											avgEndG += endValue;
											numEndG++;
										} else if (band == 2) {
											avgEndB += endValue;
											numEndB++;
										}

									}
								}
							} else {
								value = startValue;
								if (band == 0) {
									avgStartR += startValue;
									numStartR++;
								} else if (band == 1) {
									avgStartG += startValue;
									numStartG++;
								} else if (band == 2) {
									avgStartB += startValue;
									numStartB++;
								}

							}
						} else {
							if (rand > (double) bStep
									/ (double) (aStep + bStep)) {
								value = startValue;

								if (band == 0) {
									avgStartR += startValue;
									numStartR++;
								} else if (band == 1) {
									avgStartG += startValue;
									numStartG++;
								} else if (band == 2) {
									avgStartB += startValue;
									numStartB++;
								}
							} else {
								value = endValue;

								if (band == 0) {
									avgEndR += endValue;
									numEndR++;
								} else if (band == 1) {
									avgEndG += endValue;
									numEndG++;
								} else if (band == 2) {
									avgEndB += endValue;
									numEndB++;
								}

							}

						}

					} else if (effectContext.option2) {

						if (((double) x / (double) (width - 1) < div)
								|| ((double) x / (double) (width - 1) > div2)) {
							value = startValue;

							if (band == 0) {
								avgStartR += startValue;
								numStartR++;
							} else if (band == 1) {
								avgStartG += startValue;
								numStartG++;
							} else if (band == 2) {
								avgStartB += startValue;
								numStartB++;
							}

						} else {
							value = endValue;

							if (band == 0) {
								avgEndR += endValue;
								numEndR++;
							} else if (band == 1) {
								avgEndG += endValue;
								numEndG++;
							} else if (band == 2) {
								avgEndB += endValue;
								numEndB++;
							}

						}

					} else if (effectContext.option3) {

						if (((double) y / (double) (height - 1) < div)
								|| ((double) y / (double) (height - 1) > div2)) {
							value = startValue;

							if (band == 0) {
								avgStartR += startValue;
								numStartR++;
							} else if (band == 1) {
								avgStartG += startValue;
								numStartG++;
							} else if (band == 2) {
								avgStartB += startValue;
								numStartB++;
							}

						} else {
							value = endValue;
							if (band == 0) {
								avgEndR += endValue;
								numEndR++;
							} else if (band == 1) {
								avgEndG += endValue;
								numEndG++;
							} else if (band == 2) {
								avgEndB += endValue;
								numEndB++;
							}

						}

					} else {
						value = startValue
								+ (endValue - startValue)
								* (double) (currentTime - aStartTime
										.getNanoseconds())
								/ (double) (aEndTime.getNanoseconds() - aStartTime
										.getNanoseconds());
						if (band == 0) {
							avgStartR += startValue;
							numStartR++;
						} else if (band == 1) {
							avgStartG += startValue;
							numStartG++;
						} else if (band == 2) {
							avgStartB += startValue;
							numStartB++;
						}
						if (band == 0) {
							avgEndR += endValue;
							numEndR++;
						} else if (band == 1) {
							avgEndG += endValue;
							numEndG++;
						} else if (band == 2) {
							avgEndB += endValue;
							numEndB++;
						}

					}

					frameOut.setPixel(band, x, y, value);
				}
			}
		}

		if (numStartR != 0) {
			avgStartR = avgStartR / (double) numStartR;
		}

		if (numEndR != 0) {
			avgEndR = avgEndR / (double) numEndR;
		}
		if (numStartG != 0) {
			avgStartG = avgStartG / (double) numStartG;
		}

		if (numEndG != 0) {
			avgEndG = avgEndG / (double) numEndG;
		}
		if (numStartB != 0) {
			avgStartB = avgStartB / (double) numStartB;
		}

		if (numEndB != 0) {
			avgEndB = avgEndB / (double) numEndB;
		}

		ejControls.setFilmP(0, (long) avgStartR);
		ejControls.setFilmP(1, (long) avgStartG);
		ejControls.setFilmP(2, (long) avgStartB);

		ejControls.setFilmP(3, (long) avgEndR);
		ejControls.setFilmP(4, (long) avgEndG);
		ejControls.setFilmP(5, (long) avgEndB);

		return frameOut;

	}

	private EFrame doBits(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double[] thres = new double[numBands];
		int x, y, band;
		for (band = 0; band < numBands; band++) {
			thres[band] = effectParams[band + 1] * 8.0;
		}

		int level;
		byte byteLevel;
		double[] sr = null;
		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					level = frameIn.getPixelInt(band, x, y);
					byteLevel = 0;
					if (thres[band] >= 8.0)
						byteLevel = (byte) ((level) & 0xFF);
					else if (thres[band] >= 7.0)
						byteLevel = (byte) ((level) & 0xFE);
					else if (thres[band] >= 6.0)
						byteLevel = (byte) ((level) & 0xFC);
					else if (thres[band] >= 5.0)
						byteLevel = (byte) ((level) & 0xF8);
					else if (thres[band] >= 4.0)
						byteLevel = (byte) ((level) & 0xF0);
					else if (thres[band] >= 3.0)
						byteLevel = (byte) ((level) & 0xE0);
					else if (thres[band] >= 2.0)
						byteLevel = (byte) ((level) & 0xC0);
					else if (thres[band] >= 1.0)
						byteLevel = (byte) ((level) & 0x80);

					frameOut.setPixel(band, x, y, byteLevel);
				}
			}
		}
		return frameOut;

	}

	public int[] warpPoints = { 0, 30, 40, 50, 60, 60, 100, 100, 110, 120, 130,
			255 ,10,20,30};

	private EFrame doWarpGen(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		RenderedImage rimg = frameIn.getRenderedImage();
		System.out.println("Warp GEN1");
		if (warpGen == null) {
			warpGen = new WarpImageGenerator();
		}

		for (int i = 1; i <= 9; i++) {
			warpPoints[i] = (int) (effectParams[i] * 255.0);
		}
		warpPoints[10] = (int) (Math.random()*effectContext.count * 10.0);
		warpPoints[11] = (int) (Math.random()*effectContext.count * 10.0);
		warpPoints[12] = (int) (Math.random()*effectContext.count * 10.0);

		warpGen.setSourceImage(rimg);
		warpGen.setPolyDegree(effectContext.step+1);
		warpGen.setPoints(warpPoints);
		warpGen.generateImage();
		PlanarImage pimg = warpGen.getDestImage();
		if (pimg != null) {
			frameOut.setPlanarImage(pimg);
		} else {
			System.out.println("Null warp image");
		}
		return frameOut;
	}

	private EFrame doBinaryBorder(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level = 0.0;
		int count = 0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				sr = getSampleRegion33(frameIn, x, y);
				for (int band = 0; band < numBands; band++) {
					if (!effectContext.option1) {
						sr = getSampleRegion33(frameIn, band, x, y);
					}
					count = 0;
					if (effectContext.option2) {
						frameOut.setPixel(band, x, y, 0);
					} else {
						frameOut.setPixel(band, x, y, 255);
					}

					if (sr[4] >= thres) {
						if (sr[0] >= thres)
							count++;
						if (sr[1] >= thres)
							count++;
						if (sr[2] >= thres)
							count++;
						if (sr[3] >= thres)
							count++;
						if (sr[5] >= thres)
							count++;
						if (sr[6] >= thres)
							count++;
						if (sr[7] >= thres)
							count++;
						if (sr[8] >= thres)
							count++;
						if (count > effectContext.count) {
							if (effectContext.option2) {
								frameOut.setPixel(band, x, y, 255);
							} else {
								frameOut.setPixel(band, x, y, 0);
							}
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doDither(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(ropIn);

		// Create the table data.
		byte[][] tableData = new byte[3][8];
		for (int i = 0; i < 8; i++) {
			tableData[0][i] = (byte) (i >> 0); // End may be different
			tableData[1][i] = (byte) (i >> 2); // for each band
			tableData[2][i] = (byte) (i >> 3);
		}

		// Create a LookupTableJAI object to be used with the
		// "lookup" operator.
		LookupTableJAI table = new LookupTableJAI(tableData);

		//KernelJAI kern_h = new KernelJAI(3, 3, roberts_h_data);
		//KernelJAI kern_v = new KernelJAI(3, 3, roberts_v_data);

		//pb.add(ColorCube.BYTE_496);
		pb.add(table);

		pb.add(KernelJAI.ERROR_FILTER_FLOYD_STEINBERG);

		System.out.println("do dither");

		RenderedOp ropOut = JAI.create("errordiffusion", pb, null);
		frameOut.setRenderedOp(ropOut);
		return frameOut;

	}

	private EFrame doCrop(EFrame frameIn) {

		if (effectContext.option1) {
			return doCrop1(frameIn);
		}

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		frameOut.clearBuffer();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		if (effectParams[1] == 0)
			effectParams[1] = 0.01;
		if (effectParams[2] == 0)
			effectParams[2] = 0.01;
		if (effectParams[3] == 0)
			effectParams[3] = 0.01;
		if (effectParams[4] == 0)
			effectParams[4] = 0.01;

		float xwidth = (float) (effectParams[1] * width);
		float yheight = (float) (effectParams[2] * height);
		float xoffset = (float) ((width - xwidth) * effectParams[3]);
		float yoffset = (float) ((height - yheight) * effectParams[4]);

		Image image = frameIn.getImage();
		if (image == null) {
			image = frameIn.getBufferedImage();
		}
		if (image == null) {
			return frameIn;
		}
		image = doJAICrop(image, xoffset, yoffset, xwidth, yheight);
		image = doJAIScale(image, (float) width / xwidth, (float) height
				/ yheight, 0.0F, 0.0F);
		//image = doJAITranslate(image, xoffset, yoffset);
		frameOut.clearBuffer();
		frameOut.setImage1(image);
		return frameOut;
	}

	private EFrame doCrop1(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		int xl = 0;
		int yl = 0;
		int xh = 0;
		int yh = 0;
		if (width > 0)
			xl = (int) (effectParams[1] * (width - 1));
		if (height > 0)
			yl = (int) (effectParams[2] * (height - 1));
		if (width > 0)
			xh = (int) ((width - 1) * effectParams[3]);
		if (height > 0)
			yh = (int) ((height - 1) * effectParams[4]);

		double[] back = new double[numBands];
		int x, y, band;
		for (band = 0; band < numBands; band++) {
			back[band] = effectParams[band + 5] * 255.0;
		}

		double factor1 = effectParams[8];
		double factor2 = effectParams[9];

		if (factor2 == 0) {
			factor2 = 0.001;
		}

		double level, mean, backValue = 0.0, thisValue = 0.0;
		double avgBackR = 0.0, avgBackG = 0.0, avgBackB = 0.0;
		double avgThisR = 0.0, avgThisG = 0.0, avgThisB = 0.0;

		int numBackR = 0, numBackG = 0, numBackB = 0, numThisR = 0, numThisG = 0, numThisB = 0;
		double pixels = 0.0, pmax = 0.0, trans = 0.0, pavgr = 0.0, pavgg = 0.0, pavgb = 0.0;
		double r, g, b, h, s, i;
		int pcount = 0;
		double[] sr = null;
		double rmax = 0, gmax = 0, bmax = 0, smax = 0;
		if (effectContext.mode == 0) {

			if (effectContext.option5 || effectContext.option6) {
				for (y = (int) yl; y < yh; y++) {
					for (x = (int) xl; x < xh; x++) {
						pavgr += frameIn.getPixelDouble(EFrame.RED, x, y);
						pavgg += frameIn.getPixelDouble(EFrame.GREEN, x, y);
						pavgb += frameIn.getPixelDouble(EFrame.BLUE, x, y);
						pcount++;
						pixels = frameIn.getPixelDouble(0, x, y)
								+ frameIn.getPixelDouble(1, x, y)
								+ frameIn.getPixelDouble(2, x, y);
						if (pixels > pmax)
							pmax = pixels;
						r = frameIn.getPixelDouble(EFrame.RED, x, y);
						b = frameIn.getPixelDouble(EFrame.BLUE, x, y);
						g = frameIn.getPixelDouble(EFrame.GREEN, x, y);

						h = getHue(r, g, b);
						s = getSaturation(r, g, b);
						i = getIntensity(r, g, b);
						s = s * i;
						if (s > smax) {
							smax = s;
							rmax = r;
							gmax = g;
							bmax = b;

						}

					}
				}
				pavgr = pavgr / pcount;
				pavgg = pavgg / pcount;
				pavgb = pavgb / pcount;

			}

			for (y = 0; y < height; y++) {
				for (x = 0; x < width; x++) {
					for (band = 0; band < numBands; band++) {

						thisValue = frameIn.getPixelDouble(band, x, y);

						if (effectContext.option5) {
							if (effectContext.option6) {
								if (band == EFrame.RED) {
									thisValue = rmax;
								} else if (band == EFrame.GREEN) {
									thisValue = gmax;
								} else if (band == EFrame.BLUE) {
									thisValue = bmax;
								}

							} else {
								if (band == EFrame.RED) {
									thisValue = pavgr;
								} else if (band == EFrame.GREEN) {
									thisValue = pavgg;
								} else if (band == EFrame.BLUE) {
									thisValue = pavgb;
								}
							}
						}

						backValue = back[band];

						if (effectContext.option2) {
							if (backValue >= thisValue) {

								backValue = (double) Math.abs(backValue
										- thisValue);
							} else {
								backValue = (double) Math.abs(thisValue
										- (backValue));
							}

						}

						if (effectContext.option3) {
							if (backValue >= thisValue) {

								thisValue = (double) Math.abs(backValue
										- thisValue);
							} else {
								thisValue = (double) Math.abs(thisValue
										- (backValue));
							}

						}

						if (effectContext.option4) {

							backValue = backValue * (factor1 / factor2);
						}

						if (effectContext.option6 && !effectContext.option5) {
							pixels = frameIn.getPixelDouble(0, x, y)
									+ frameIn.getPixelDouble(1, x, y)
									+ frameIn.getPixelDouble(2, x, y);
							trans = pixels / pmax;
							thisValue = thisValue * trans + backValue
									* (1.0 - trans);
						}

						if (x < xl || x > xh || y < yl || y > yh) {

							frameOut.setPixel(band, x, y, backValue);
							if (band == 0) {
								avgBackR += backValue;
								numBackR++;
							} else if (band == 1) {
								avgBackG += backValue;
								numBackG++;
							} else if (band == 2) {
								avgBackB += backValue;
								numBackB++;
							}

						} else {
							frameOut.setPixel(band, x, y, thisValue);

							if (band == EFrame.RED) {
								avgThisR += thisValue;
								numThisR++;
							} else if (band == EFrame.GREEN) {
								avgThisG += thisValue;
								numThisG++;
							} else if (band == EFrame.BLUE) {
								avgThisB += thisValue;
								numThisB++;
							}

						}
					}
				}
			}
			if (numBackR != 0) {
				avgBackR = avgBackR / (double) numBackR;
			}

			if (numThisR != 0) {
				avgThisR = avgThisR / (double) numThisR;
			}
			if (numBackG != 0) {
				avgBackG = avgBackG / (double) numBackG;
			}

			if (numThisG != 0) {
				avgThisG = avgThisG / (double) numThisG;
			}
			if (numBackB != 0) {
				avgBackB = avgBackB / (double) numBackB;
			}

			if (numThisB != 0) {
				avgThisB = avgThisB / (double) numThisB;
			}

		} else {
			avgBackR = frameIn.getPixelDouble(EFrame.RED, xl, yl);
			avgThisR = frameIn.getPixelDouble(EFrame.RED, xh, yh);
			avgBackG = frameIn.getPixelDouble(EFrame.GREEN, xl, yl);
			avgThisG = frameIn.getPixelDouble(EFrame.GREEN, xh, yh);
			avgBackB = frameIn.getPixelDouble(EFrame.BLUE, xl, yl);
			avgThisB = frameIn.getPixelDouble(EFrame.BLUE, xh, yh);

		}

		ejControls.setFilmP(0, (long) avgBackR);
		ejControls.setFilmP(1, (long) avgBackG);
		ejControls.setFilmP(2, (long) avgBackB);

		ejControls.setFilmP(3, (long) avgThisR);
		ejControls.setFilmP(4, (long) avgThisG);
		ejControls.setFilmP(5, (long) avgThisB);

		return frameOut;
	}

	private EFrame doLaplace(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		int x, y, band;
		double thres1 = effectParams[1] * 255.0;
		double thres2 = effectParams[2] * 255.0;
		double thres1m = effectParams[3] * 10.0;
		double thres2m = effectParams[4] * 10.0;

		double dx, dy, dt, ixx, iyy, ix, iy, ix1, iy1;
		double[] maxi = new double[numBands];
		double[] mini = new double[numBands];

		for (band = 0; band < numBands; band++) {
			maxi[band] = 0;
			mini[band] = 0;
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ixx = frameIn.getPixelDouble(band, x + 1, y) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y);
					iyy = frameIn.getPixelDouble(band, x, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x, y - 1);
					if (maxi[band] < ixx + iyy)
						maxi[band] = ixx + iyy;
					if (mini[band] > ixx + iyy)
						mini[band] = ixx + iyy;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ixx = frameIn.getPixelDouble(band, x + 1, y) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y);
					iyy = frameIn.getPixelDouble(band, x, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x, y - 1);
					frameOut
							.setPixel(
									band,
									x,
									y,
									255.0 * ((ixx + iyy - mini[band]) / (maxi[band] - mini[band])));
				}
			}
		}

		if (!effectContext.option1)
			return frameOut;
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());
		frameOut1.clearBuffer();

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ix = frameOut.getPixelDouble(band, x, y);
					iy = frameOut.getPixelDouble(band, x, y);
					ix1 = frameOut.getPixelDouble(band, x + 1, y);
					iy1 = frameOut.getPixelDouble(band, x, y + 1);
					ix = mini[band] + ix * (maxi[band] - mini[band]) / 255.0;
					iy = mini[band] + iy * (maxi[band] - mini[band]) / 255.0;
					ix1 = mini[band] + ix1 * (maxi[band] - mini[band]) / 255.0;
					iy1 = mini[band] + iy1 * (maxi[band] - mini[band]) / 255.0;

					if ((ix < 0 && ix1 > 0) || (ix > 0 && ix1 < 0)) {
						if (Math.abs(ix1 - ix) > thres1 * thres1m) {
							frameOut1.setPixel(band, x, y, 255);

						}

					} else if ((iy < 0 && iy1 > 0) || (iy > 0 && iy1 < 0)) {
						if (Math.abs(iy1 - iy) > thres2 * thres2m) {
							frameOut1.setPixel(band, x, y, 255);
						}
					}
				}
			}
		}
		return frameOut1;
	}

	private EFrame doDerive(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		int x, y, band;
		double thres1 = effectParams[1] * 255.0;
		double thres2 = effectParams[2] * 255.0;
		double thres1m = effectParams[3] * 10.0;
		double thres2m = effectParams[4] * 10.0;

		double dx, dy, dt, ixx, iyy, ix, iy, ix1, iy1, ixy, level;
		double[] maxi = new double[numBands];
		double[] mini = new double[numBands];

		for (band = 0; band < numBands; band++) {
			maxi[band] = 0;
			mini[band] = 0;
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ixx = frameIn.getPixelDouble(band, x + 1, y) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y);
					iyy = frameIn.getPixelDouble(band, x, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x, y - 1);
					ixy = frameIn.getPixelDouble(band, x + 1, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y - 1);

					ix = frameIn.getPixelDouble(band, x + 1, y)
							- frameIn.getPixelDouble(band, x, y);
					iy = frameIn.getPixelDouble(band, x, y + 1)
							- frameIn.getPixelDouble(band, x, y);

					level = (ix * ix * ixx + 2 * ix * iy * ixy + iy * iy * iyy)
							/ Math.sqrt(ix * ix + iy * iy);

					if (maxi[band] < level)
						maxi[band] = level;
					if (mini[band] > level)
						mini[band] = level;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ixx = frameIn.getPixelDouble(band, x + 1, y) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y);
					iyy = frameIn.getPixelDouble(band, x, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x, y - 1);
					ixy = frameIn.getPixelDouble(band, x + 1, y + 1) - 2
							* (frameIn.getPixelDouble(band, x, y))
							+ frameIn.getPixelDouble(band, x - 1, y - 1);
					ix = frameIn.getPixelDouble(band, x + 1, y)
							- frameIn.getPixelDouble(band, x, y);
					iy = frameIn.getPixelDouble(band, x, y + 1)
							- frameIn.getPixelDouble(band, x, y);

					level = (ix * ix * ixx + 2 * ix * iy * ixy + iy * iy * iyy)
							/ Math.sqrt(ix * ix + iy * iy);

					frameOut
							.setPixel(
									band,
									x,
									y,
									255.0 * ((level - mini[band]) / (maxi[band] - mini[band])));
				}
			}
		}

		if (!effectContext.option1)
			return frameOut;
		EFrame frameOut1 = new EFrame(frameIn.getBuffer());
		frameOut1.clearBuffer();

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					ix = frameOut.getPixelDouble(band, x, y);
					iy = frameOut.getPixelDouble(band, x, y);
					ix1 = frameOut.getPixelDouble(band, x + 1, y);
					iy1 = frameOut.getPixelDouble(band, x, y + 1);
					ix = mini[band] + ix * (maxi[band] - mini[band]) / 255.0;
					iy = mini[band] + iy * (maxi[band] - mini[band]) / 255.0;
					ix1 = mini[band] + ix1 * (maxi[band] - mini[band]) / 255.0;
					iy1 = mini[band] + iy1 * (maxi[band] - mini[band]) / 255.0;

					if ((ix < 0 && ix1 > 0) || (ix > 0 && ix1 < 0)) {
						if (Math.abs(ix1 - ix) > thres1 * thres1m) {
							frameOut1.setPixel(band, x, y, 255);

						}

					} else if ((iy < 0 && iy1 > 0) || (iy > 0 && iy1 < 0)) {
						if (Math.abs(iy1 - iy) > thres2 * thres2m) {
							frameOut1.setPixel(band, x, y, 255);
						}
					}
				}
			}
		}
		return frameOut1;
	}

	private EFrame doFillEdge(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int x, x1, y, y1, band, xsave = 0, ysave = 0;
		double r, g, b;

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					xsave = x;
					for (x1 = 0; x1 < xsave; x1++) {
						frameOut.setPixel(EFrame.RED, x1, y, r);
						frameOut.setPixel(EFrame.GREEN, x1, y, g);
						frameOut.setPixel(EFrame.BLUE, x1, y, b);
					}
					break;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = width - 1; x >= 0; x--) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					xsave = x;
					for (x1 = width - 1; x1 > xsave; x1--) {
						frameOut.setPixel(EFrame.RED, x1, y, r);
						frameOut.setPixel(EFrame.GREEN, x1, y, g);
						frameOut.setPixel(EFrame.BLUE, x1, y, b);
					}
					break;
				}
			}
		}

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					ysave = y;
					for (y1 = 0; y1 < xsave; y1++) {
						frameOut.setPixel(EFrame.RED, x, y1, r);
						frameOut.setPixel(EFrame.GREEN, x, y1, g);
						frameOut.setPixel(EFrame.BLUE, x, y1, b);
					}
					break;
				}
			}
		}

		for (x = 0; x < width; x++) {
			for (y = height - 1; y >= 0; y--) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					ysave = y;
					for (y1 = height - 1; y1 > ysave; y1--) {
						frameOut.setPixel(EFrame.RED, x, y1, r);
						frameOut.setPixel(EFrame.GREEN, x, y1, g);
						frameOut.setPixel(EFrame.BLUE, x, y1, b);
					}
					break;
				}
			}
		}

		return frameOut;

	}

	private EFrame doFillEdge2(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		int x, x1, y, y1, band, xsave = 0, ysave = 0;
		double r, g, b;

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					ysave = y;
					for (y1 = 0; y1 < ysave; y1++) {
						frameOut.setPixel(EFrame.RED, x, y1, r);
						frameOut.setPixel(EFrame.GREEN, x, y1, g);
						frameOut.setPixel(EFrame.BLUE, x, y1, b);
					}
					break;
				}
			}
		}

		for (x = 0; x < width; x++) {
			for (y = height - 1; y >= 0; y--) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					ysave = y;
					for (y1 = height - 1; y1 > ysave; y1--) {
						frameOut.setPixel(EFrame.RED, x, y1, r);
						frameOut.setPixel(EFrame.GREEN, x, y1, g);
						frameOut.setPixel(EFrame.BLUE, x, y1, b);
					}
					break;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					xsave = x;
					for (x1 = 0; x1 < xsave; x1++) {
						frameOut.setPixel(EFrame.RED, x1, y, r);
						frameOut.setPixel(EFrame.GREEN, x1, y, g);
						frameOut.setPixel(EFrame.BLUE, x1, y, b);
					}
					break;
				}
			}
		}

		for (y = 0; y < height; y++) {
			for (x = width - 1; x >= 0; x--) {
				r = frameOut.getPixelDouble(EFrame.RED, x, y);
				g = frameOut.getPixelDouble(EFrame.GREEN, x, y);
				b = frameOut.getPixelDouble(EFrame.BLUE, x, y);
				if (r != 0 || b != 0 || g != 0) {
					xsave = x;
					for (x1 = width - 1; x1 > xsave; x1--) {
						frameOut.setPixel(EFrame.RED, x1, y, r);
						frameOut.setPixel(EFrame.GREEN, x1, y, g);
						frameOut.setPixel(EFrame.BLUE, x1, y, b);
					}
					break;
				}
			}
		}

		return frameOut;

	}

	private EFrame doDisconnect(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double thres = effectParams[1] * 255.0;
		double thres1 = effectParams[2] * 255.0;

		double[] sr = null;
		double level;
		int count = 0;

		int x, y, band;
		if (effectContext.option1) {
			frameOut.clearBuffer();
		}

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				for (band = 0; band < numBands; band++) {
					sr = getSampleRegion33(frameIn, band, x, y);
					count = 0;
					if (sr[4] >= thres
							&& ((sr[0] >= thres && sr[1] < thres1
									&& sr[2] >= thres && sr[3] >= thres
									&& sr[5] >= thres && sr[6] >= thres
									&& sr[7] < thres1 && sr[8] >= thres)
									|| (sr[0] >= thres && sr[1] < thres1
											&& sr[2] < thres1 && sr[3] < thres1
											&& sr[5] >= thres && sr[6] < thres1
											&& sr[7] < thres1 && sr[8] < thres1)
									|| (sr[0] < thres1 && sr[1] < thres1
											&& sr[2] >= thres && sr[3] >= thres
											&& sr[5] < thres1 && sr[6] >= thres
											&& sr[7] < thres1 && sr[8] >= thres) || (sr[0] >= thres
									&& sr[1] >= thres
									&& sr[2] < thres1
									&& sr[3] >= thres
									&& sr[5] < thres1
									&& sr[6] < thres1 && sr[7] < thres1 && sr[8] >= thres))) {
						if (effectContext.option2) {
							frameOut.setPixel(band, x, y, 255);
						} else {
							frameOut.setPixel(band, x, y, 0);
						}
					}
				}
			}
		}
		return frameOut;
	}

	private EFrame doPointy(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		double z = 0;
		int sum = 0;
		ArrayList s = null;
		Double dObject = null;
		double yt = effectParams[1];
		double it = effectParams[2];
		double qt = effectParams[3];
		int limit = (int) (effectParams[4] * 100.0);
		int length = (int) (effectParams[5] * 100.0);
		int[] factors = new int[length];
		for (int i = 0; i < length; i++) {
			factors[i] = (int) (Math.pow(2.0, (double) (length - 1 - i)));
		}
		int[] pixels = new int[factors.length];
		int layer = 0;
		int factor;
		int roValue, boValue, goValue;
		int rValue, bValue, gValue;
		int rnValue, bnValue, gnValue;
		double yoValue, ioValue, qoValue;
		double yValue, iValue, qValue;
		double ynValue, inValue, qnValue;
		double yMax, iMax, qMax;
		double yMin, iMin, qMin;

		yMax = 255.0 * 0.299 + 255.0 * 0.587 + 255.0 * 0.114;
		iMax = 255.0 * 0.596;
		qMax = 255.0 * 0.212 + 255.0 * 0.311;

		yMin = 0.0;
		iMin = -255.0 * 0.275 - 255.0 * 0.321;
		qMin = -255.0 * 0.528;

		int xi, yi;
		ArrayList values = new ArrayList();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				values = new ArrayList();
				xi = x;
				yi = y;
				roValue = frameIn.getPixelInt(EFrame.RED, x, y);
				boValue = frameIn.getPixelInt(EFrame.BLUE, x, y);
				goValue = frameIn.getPixelInt(EFrame.GREEN, x, y);
				yoValue = (double) roValue * 0.299 + (double) goValue * 0.587
						+ (double) boValue * 0.114;
				ioValue = (double) roValue * 0.596 - (double) goValue * 0.275
						- (double) boValue * 0.321;
				qoValue = (double) roValue * 0.212 - (double) goValue * 0.528
						+ (double) boValue * 0.311;

				factor = factors[0];
				for (int f = 0; f < factor; f++) {
					values.add(new PixelPoint(roValue, boValue, goValue));
				}
				for (int i = 1; i < factors.length; i++) {

					factor = factors[i];
					xi = x + i;
					yi = y;

					for (int j = 0; j < i * limit; j++) {

						if (yi <= yi - i) {
							if (xi < xi + i) {
								xi = xi + 1;
							} else {
								yi = yi + 1;
							}
						} else if (yi >= yi + i) {
							if (xi > xi - i) {
								xi = xi - 1;
							} else {
								yi = yi - 1;
							}
						} else {
							if (xi <= xi - i) {
								yi = yi - 1;
							} else {
								yi = yi + 1;
							}
						}

						rValue = frameIn.getPixelInt(EFrame.RED, xi, yi);
						bValue = frameIn.getPixelInt(EFrame.BLUE, xi, yi);
						gValue = frameIn.getPixelInt(EFrame.GREEN, xi, yi);
						yValue = (double) rValue * 0.299 + (double) gValue
								* 0.587 + (double) bValue * 0.114;
						iValue = (double) rValue * 0.596 - (double) gValue
								* 0.275 - (double) bValue * 0.321;
						qValue = (double) rValue * 0.212 - (double) gValue
								* 0.528 + (double) bValue * 0.311;

						double diff = 0;
						if (!(rValue <= 0 && bValue <= 0 && gValue <= 0)
								&& (yValue - yMin) > (yMax - yMin) * yt
								&& (iValue - iMin) > (iMax - iMin) * it) {

							ynValue = yValue;
							inValue = iValue;

							diff = (Math.abs(qValue - qoValue)) / (qMax - qMin);
							if (diff < qt) {
								qnValue = qoValue;
							} else {
								if ((qValue + (qMax - qMin) / 2) > qMax) {
									qnValue = qValue - (qMax - qMin) / 2;
								} else {
									qnValue = qValue + (qMax - qMin) / 2;
								}
							}
							/*
							 * diff =
							 * (double)(Math.abs(rValue-roValue)+Math.abs(bValue-boValue)+Math.abs(gValue-goValue))/(3.0*255.0);
							 * rnValue = (int)((double)(255-rValue)*(diff)+
							 * (double)(rValue)*(1.0-diff)); bnValue =
							 * (int)((double)(255-bValue)*(diff)+
							 * (double)(bValue)*(1.0-diff)); gnValue =
							 * (int)((double)(255-gValue)*(diff)+
							 * (double)(gValue)*(1.0-diff)); if (diff < 0.05) {
							 * rnValue = rValue; bnValue = bValue; gnValue =
							 * gValue;
							 *  } else { rnValue = 255-rValue; bnValue =
							 * 255-bValue; gnValue = 255-gValue; }
							 */
							rnValue = (int) (ynValue + inValue * 0.956 + qnValue * 0.621);
							gnValue = (int) (ynValue - inValue * 0.272 - qnValue * 0.647);
							bnValue = (int) (ynValue - inValue * 1.105 + qnValue * 1.702);

							for (int f = 0; f < factor; f++) {
								values.add(new PixelPoint(rnValue, bnValue,
										gnValue));
							}
						}
					}
					Collections.shuffle(values);

					int index = (int) (values.size() * Math.random());
					if (index >= values.size())
						index = index - 1;
					PixelPoint p = (PixelPoint) values.get(index);

					frameOut.setPixel(EFrame.RED, x, y, p.rValue);
					frameOut.setPixel(EFrame.BLUE, x, y, p.bValue);
					frameOut.setPixel(EFrame.GREEN, x, y, p.gValue);

				}
			}
		}
		return frameOut;
	}

	public class PixelPoint {

		public PixelPoint(int rValue, int bValue, int gValue) {
			this.rValue = rValue;
			this.bValue = bValue;
			this.gValue = gValue;
		}

		int rValue;

		int bValue;

		int gValue;
	}

	private EFrame doPointy2(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		double z = 0;
		int sum = 0;
		ArrayList s = null;
		Double dObject = null;

		double st = effectParams[1] * 255.0;
		double it = effectParams[2] * 255.0;
		double ht = effectParams[3] * 255.0;
		int limit = (int) (effectParams[4] * 100.0);
		int length = (int) (effectParams[5] * 100.0);
		int[] factors = new int[length];
		for (int i = 0; i < length; i++) {
			factors[i] = (int) (Math.pow(2.0, (double) (length - 1 - i)));
		}
		int[] pixels = new int[factors.length];
		int layer = 0;
		int factor;
		int roValue, boValue, goValue;
		int rValue, bValue, gValue;
		int rnValue, bnValue, gnValue;
		double hoValue, soValue, ioValue;
		double hValue, sValue, iValue;
		double hnValue, snValue, inValue;
		RGBPixel rgb = null;

		int xi, yi;
		ArrayList values = new ArrayList();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				values = new ArrayList();
				xi = x;
				yi = y;
				roValue = frameIn.getPixelInt(EFrame.RED, x, y);
				boValue = frameIn.getPixelInt(EFrame.BLUE, x, y);
				goValue = frameIn.getPixelInt(EFrame.GREEN, x, y);
				hoValue = getHue(roValue, goValue, boValue);
				soValue = getSaturation(roValue, goValue, boValue);
				ioValue = getIntensity(roValue, goValue, boValue);

				factor = factors[0];
				for (int f = 0; f < factor; f++) {
					values.add(new PixelPoint(roValue, boValue, goValue));
				}
				for (int i = 1; i < factors.length; i++) {

					factor = factors[i];
					xi = x + i;
					yi = y;

					for (int j = 0; j < i * limit; j++) {

						if (yi <= yi - i) {
							if (xi < xi + i) {
								xi = xi + 1;
							} else {
								yi = yi + 1;
							}
						} else if (yi >= yi + i) {
							if (xi > xi - i) {
								xi = xi - 1;
							} else {
								yi = yi - 1;
							}
						} else {
							if (xi <= xi - i) {
								yi = yi - 1;
							} else {
								yi = yi + 1;
							}
						}

						rValue = frameIn.getPixelInt(EFrame.RED, xi, yi);
						bValue = frameIn.getPixelInt(EFrame.BLUE, xi, yi);
						gValue = frameIn.getPixelInt(EFrame.GREEN, xi, yi);
						hValue = getHue(rValue, gValue, bValue);
						sValue = getSaturation(rValue, gValue, bValue);
						iValue = getIntensity(rValue, gValue, bValue);

						double diff = 0;
						if (!(rValue <= 0 && bValue <= 0 && gValue <= 0)
								&& sValue > st && iValue > it) {

							snValue = sValue;
							inValue = iValue;

							diff = Math.abs(hValue - hoValue);
							if (diff < ht) {
								hnValue = hoValue;
							} else {
								hnValue = 0.5 * ((255.0 - hValue) + (255.0 - hoValue));
							}

							rgb = getRGBFromHSI(hnValue, snValue, inValue);
							rnValue = (int) rgb.r;
							gnValue = (int) rgb.g;
							bnValue = (int) rgb.b;
							for (int f = 0; f < factor; f++) {
								values.add(new PixelPoint(rnValue, bnValue,
										gnValue));
							}
						}
					}
					Collections.shuffle(values);

					int index = (int) (values.size() * Math.random());
					if (index >= values.size())
						index = index - 1;
					PixelPoint p = (PixelPoint) values.get(index);

					frameOut.setPixel(EFrame.RED, x, y, p.rValue);
					frameOut.setPixel(EFrame.BLUE, x, y, p.bValue);
					frameOut.setPixel(EFrame.GREEN, x, y, p.gValue);

				}
			}
		}
		return frameOut;
	}

	private EFrame doWaves(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		return frameOut;
	}

	private EFrame doYiq(EFrame frameIn) {
		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		int rValue, bValue, gValue;
		double yValue, iValue, qValue;
		double yMax, iMax, qMax;
		double yMin, iMin, qMin;

		double yt = 1.0 - effectParams[1];
		double it = 1.0 - effectParams[2];
		double qt = 1.0 - effectParams[3];

		yMax = 255.0 * 0.299 + 255.0 * 0.587 + 255.0 * 0.114;
		iMax = 255.0 * 0.596;
		qMax = 255.0 * 0.212 + 255.0 * 0.311;

		yMin = 0.0;
		iMin = -255.0 * 0.275 - 255.0 * 0.321;
		qMin = -255.0 * 0.528;

		yt = yt * (yMax - yMin);
		it = it * (iMax - iMin);
		qt = qt * (qMax - qMin);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rValue = frameIn.getPixelInt(EFrame.RED, x, y);
				bValue = frameIn.getPixelInt(EFrame.BLUE, x, y);
				gValue = frameIn.getPixelInt(EFrame.GREEN, x, y);

				yValue = (double) rValue * 0.299 + (double) gValue * 0.587
						+ (double) bValue * 0.114;
				iValue = (double) rValue * 0.596 - (double) gValue * 0.275
						- (double) bValue * 0.321;
				qValue = (double) rValue * 0.212 - (double) gValue * 0.528
						+ (double) bValue * 0.311;

				if ((yValue - yMin) >= yt) {
					yValue = yValue - yt;
				} else {
					yValue = yt + yMin;
				}

				if ((iValue - iMin) >= it) {
					iValue = iValue - it;
				} else {
					iValue = it + iMin;
				}

				if ((qValue - qMin) >= qt) {
					qValue = qValue - qt;
				} else {
					qValue = qt + qMin;
				}

				rValue = (int) Math.abs(yValue + iValue * 0.956 + qValue
						* 0.621);
				gValue = (int) Math.abs(yValue - iValue * 0.272 - qValue
						* 0.647);
				bValue = (int) Math.abs(yValue - iValue * 1.105 + qValue
						* 1.702);

				frameOut.setPixel(EFrame.RED, x, y, rValue);
				frameOut.setPixel(EFrame.BLUE, x, y, bValue);
				frameOut.setPixel(EFrame.GREEN, x, y, gValue);
			}
		}
		return frameOut;
	}

	private EFrame doYiq2(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		int rValue, bValue, gValue;
		double yValue, iValue, qValue;
		double yMax, iMax, qMax;
		double yMin, iMin, qMin;

		double yt = effectParams[1];
		double it = effectParams[3];
		double qt = effectParams[5];
		double ytd = effectParams[2] + 0.01;
		double itd = effectParams[4] + 0.01;
		double qtd = effectParams[6] + 0.01;

		yMax = 255.0 * 0.299 + 255.0 * 0.587 + 255.0 * 0.114;
		iMax = 255.0 * 0.596;
		qMax = 255.0 * 0.212 + 255.0 * 0.311;

		yMin = 0.0;
		iMin = -255.0 * 0.275 - 255.0 * 0.321;
		qMin = -255.0 * 0.528;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rValue = frameIn.getPixelInt(EFrame.RED, x, y);
				bValue = frameIn.getPixelInt(EFrame.BLUE, x, y);
				gValue = frameIn.getPixelInt(EFrame.GREEN, x, y);

				yValue = (yt / ytd)
						* ((double) rValue * 0.299 + (double) gValue * 0.587 + (double) bValue * 0.114);
				iValue = (it / itd)
						* ((double) rValue * 0.596 - (double) gValue * 0.275 - (double) bValue * 0.321);
				qValue = (qt / qtd)
						* ((double) rValue * 0.212 - (double) gValue * 0.528 + (double) bValue * 0.311);

				rValue = (int) Math.abs(yValue + iValue * 0.956 + qValue
						* 0.621);
				gValue = (int) Math.abs(yValue - iValue * 0.272 - qValue
						* 0.647);
				bValue = (int) Math.abs(yValue - iValue * 1.105 + qValue
						* 1.702);

				frameOut.setPixel(EFrame.RED, x, y, rValue);
				frameOut.setPixel(EFrame.BLUE, x, y, bValue);
				frameOut.setPixel(EFrame.GREEN, x, y, gValue);
			}
		}
		return frameOut;
	}

	private EFrame doHsi(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		int rValue, bValue, gValue;
		double hValue, sValue, iValue;
		RGBPixel rgb = null;

		double ht = effectParams[1];
		double st = effectParams[3];
		double it = effectParams[5];
		double htd = effectParams[2] + 0.01;
		double std = effectParams[4] + 0.01;
		double itd = effectParams[6] + 0.01;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rValue = frameIn.getPixelInt(EFrame.RED, x, y);
				bValue = frameIn.getPixelInt(EFrame.BLUE, x, y);
				gValue = frameIn.getPixelInt(EFrame.GREEN, x, y);

				hValue = (ht / htd) * getHue(rValue, gValue, bValue);
				sValue = (st / std) * getSaturation(rValue, gValue, bValue);
				iValue = (it / itd) * getIntensity(rValue, gValue, bValue);

				rgb = getRGBFromHSI(hValue, sValue, iValue);
				rValue = (int) rgb.r;
				gValue = (int) rgb.g;
				bValue = (int) rgb.b;

				frameOut.setPixel(EFrame.RED, x, y, rValue);
				frameOut.setPixel(EFrame.BLUE, x, y, bValue);
				frameOut.setPixel(EFrame.GREEN, x, y, gValue);
			}
		}
		return frameOut;
	}

	private EFrame doRGBToHSI(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		double r, g, b, h, s, i;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				r = frameIn.getPixelDouble(EFrame.RED, x, y);
				b = frameIn.getPixelDouble(EFrame.BLUE, x, y);
				g = frameIn.getPixelDouble(EFrame.GREEN, x, y);

				h = getHue(r, g, b);
				s = getSaturation(r, g, b);
				i = getIntensity(r, g, b);

				float[] hsiArray = Color.RGBtoHSB((int) r, (int) g, (int) b,
						null);
				//h = (double)hsiArray[0]*255.0;
				//s = (double)hsiArray[1]*255.0;
				//i = (double)hsiArray[2]*255.0;

				frameOut.setPixel(EFrame.RED, x, y, h);
				frameOut.setPixel(EFrame.BLUE, x, y, s);
				frameOut.setPixel(EFrame.GREEN, x, y, i);
			}
		}
		return frameOut;
	}

	private EFrame doHSIToRGB(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		frameOut.clearBuffer();
		double r, g, b, h, s, i;
		RGBPixel rgb = null;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				h = frameIn.getPixelDouble(EFrame.RED, x, y);
				s = frameIn.getPixelDouble(EFrame.BLUE, x, y);
				i = frameIn.getPixelDouble(EFrame.GREEN, x, y);

				int v = Color.HSBtoRGB((float) (h / 255.0),
						(float) (s / 255.0), (float) (i / 255.0));

				int red = v << 8;
				red = red >>> 24 & 0xFF;

				int green = v << 12;
				green = green >>> 24 & 0xFF;

				int blue = v << 24;
				blue = blue >>> 24 & 0xFF;

				r = (double) red;
				g = (double) green;
				b = (double) blue;

				rgb = getRGBFromHSI(h, s, i);
				r = rgb.r;
				g = rgb.g;
				b = rgb.b;

				frameOut.setPixel(EFrame.RED, x, y, r);
				frameOut.setPixel(EFrame.BLUE, x, y, b);
				frameOut.setPixel(EFrame.GREEN, x, y, g);
			}
		}
		return frameOut;
	}

	private EFrame doInvert(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double rt = effectParams[1];
		double gt = effectParams[2];
		double bt = effectParams[3];
		double r, g, b;
		frameOut.clearBuffer();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				r = frameIn.getPixelDouble(EFrame.RED, x, y);
				b = frameIn.getPixelDouble(EFrame.BLUE, x, y);
				g = frameIn.getPixelDouble(EFrame.GREEN, x, y);

				r = rt * (255.0 - r) + (1.0 - rt) * r;
				g = gt * (255.0 - g) + (1.0 - gt) * g;
				b = bt * (255.0 - b) + (1.0 - bt) * b;

				frameOut.setPixel(EFrame.RED, x, y, r);
				frameOut.setPixel(EFrame.BLUE, x, y, b);
				frameOut.setPixel(EFrame.GREEN, x, y, g);
			}
		}
		return frameOut;
	}

	private double getHue(double r, double g, double b) {

		double intensity, h, s, i, num, den;

		intensity = (r + g + b) / 3;
		if ((r == g) && (r == b)) {
			h = 255;
		} else {
			num = (r - b) + (r - g);
			den = 2 * Math.sqrt((((r - b) * (r - b)) + ((r - g) * (b - g))));
			h = Math.acos(num / den);

			if (h <= 0)
				h = 2 * Math.PI + h;
			if ((g / intensity) > (b / intensity)) {
				h = 2 * Math.PI - h;
			}
			h = (h / (2 * Math.PI)) * 255;
		}

		return h;
	}

	private double getSaturation(double r, double g, double b) {

		double intensity, h, s, i, num, den;

		if ((r == g) && (r == b))
			s = 0;
		else {
			s = (1 - ((3 * Math.min(Math.min(r, g), b)) / (r + g + b))) * 255;
		}
		return s;
	}

	private double getIntensity(double r, double g, double b) {

		double intensity, h, s, i, num, den;

		i = (r + g + b) / 3;

		return i;
	}

	private RGBPixel getRGBFromHSI(double h, double s, double i) {

		double r = 0, g = 0, b = 0;
		RGBPixel rgb = null;

		h = h / 255;
		s = s / 255;
		i = i;
		h = h * 2 * Math.PI;

		if ((h > 0) && (h <= 2 * Math.PI / 3)) {
			b = (1 - s) / 3;
			r = (1 + (s * Math.cos(h) / Math.cos(Math.PI / 3 - h))) / 3;
			g = 1 - (r + b);
		}

		if ((h > 2 * Math.PI / 3) && (h <= 4 * Math.PI / 3)) {
			h = h - (2 * Math.PI / 3);
			r = (1 - s) / 3;
			g = (1 + (s * Math.cos(h) / Math.cos(Math.PI / 3 - h))) / 3;
			b = 1 - (r + g);
		}

		if ((h <= 2 * Math.PI) && (h > 4 * Math.PI / 3)) {

			h = h - (4 * Math.PI / 3);
			g = (1 - s) / 3;
			b = (1 + (s * Math.cos(h) / Math.cos(Math.PI / 3 - h))) / 3;
			r = 1 - (g + b);
		}

		r = r * i * 3;
		g = g * i * 3;
		b = b * i * 3;

		return new RGBPixel(r, g, b);

	}

	private EFrame doTimeFlow(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());

		EFrame frameIn0 = eFrameIn.get(0);
		int width = frameIn0.getWidth();
		int height = frameIn0.getHeight();
		int numBands = frameIn0.getPixelStride();
		int x, y, band;
		MoviePoint moviePoint;

		frameOut.clearBuffer();

		int iters = effectContext.count + 1;
		double factor = effectParams[1] / (effectParams[2] + 0.001);
		double scale = effectParams[3] * 100.0;
		double thres = effectParams[5] * 255.0;
		int blockSize = (int) (effectParams[4] * 100.0);
		int stepSize = effectContext.step + 1;

		if (effectContext.option1) {
			stepSize = 1;
			blockSize = 3;
			iters = 10;
			factor = 10.0;
			scale = 2.0;
		}

		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		EFrame frameIn1 = eFrameWork.get(1);
		if (frameIn1 == null || frameIn0 == null)
			return frameOut;

		BufferedImage biOut = frameOut.getBufferedImage();

		double z = 0;
		int sum = 0;
		double dx, dy, dt, uv, vv, au, av, a, b;
		double maxdx = 0, maxdy = 0, maxdt = 0, mindx = 0, mindy = 0, mindt = 0;
		double[][][] u = new double[numBands][height][width];
		double[][][] v = new double[numBands][height][width];
		double[][][] tu = new double[numBands][height][width];
		double[][][] tv = new double[numBands][height][width];

		System.out.println("in movie 2");

		for (int n = 0; n < iters; n++) {
			for (y = 1; y < height - blockSize; y += stepSize) {
				for (x = 1; x < width - blockSize; x += stepSize) {
					for (band = 0; band < numBands; band++) {
						dt = 0;
						dx = 0;
						dy = 0;
						for (int my = 0; my < blockSize - 1; my++) {
							for (int mx = 0; mx < blockSize - 1; mx++) {
								dt += 0.25 * (frameIn1.getPixelDouble(band, x
										+ mx, y + my)
										- frameIn0.getPixelDouble(band, x + mx,
												y + my)
										+ frameIn1.getPixelDouble(band, x + mx
												+ 1, y + my)
										- frameIn0.getPixelDouble(band, x + mx
												+ 1, y + my)
										+ frameIn1.getPixelDouble(band, x + mx,
												y + my + 1)
										- frameIn0.getPixelDouble(band, x + mx,
												y + my + 1)
										+ frameIn1.getPixelDouble(band, x + mx
												+ 1, y + my + 1) - frameIn0
										.getPixelDouble(band, x + mx + 1, y
												+ my + 1));
								dx += 0.25 * (frameIn1.getPixelDouble(band, x
										+ mx + 1, y + my)
										- frameIn1.getPixelDouble(band, x + mx,
												y + my)
										+ frameIn1.getPixelDouble(band, x + mx
												+ 1, y + my + 1)
										- frameIn1.getPixelDouble(band, x + mx,
												y + my + 1)
										+ frameIn0.getPixelDouble(band, x + mx
												+ 1, y + my)
										- frameIn0.getPixelDouble(band, x + mx,
												y + my)
										+ frameIn0.getPixelDouble(band, x + mx
												+ 1, y + my + 1) - frameIn0
										.getPixelDouble(band, x + mx, y + my
												+ 1));
								dy += 0.25 * (frameIn1.getPixelDouble(band, x
										+ mx, y + my + 1)
										- frameIn1.getPixelDouble(band, x + mx,
												y + my)
										+ frameIn1.getPixelDouble(band, x + mx
												+ 1, y + my + 1)
										- frameIn1.getPixelDouble(band, x + mx
												+ 1, y + my)
										+ frameIn0.getPixelDouble(band, x + mx,
												y + my + 1)
										- frameIn0.getPixelDouble(band, x + mx,
												y + my)
										+ frameIn0.getPixelDouble(band, x + mx
												+ 1, y + my + 1) - frameIn0
										.getPixelDouble(band, x + mx + 1, y
												+ my));
							}
						}

						au = 0.25 * (u[band][y][x - 1] + u[band][y][x + 1]
								+ u[band][y - 1][x] + u[band][y + 1][x]);
						av = 0.25 * (v[band][y][x - 1] + v[band][y][x + 1]
								+ v[band][y - 1][x] + v[band][y + 1][x]);
						a = (dx * au + dy * av + dt);
						b = (1 + factor * (dx * dx + dy * dy));
						tu[band][y][x] = au - (dx * factor * a / b);
						tv[band][y][x] = av - (dy * factor * a / b);
					}
				}
			}
			for (y = 0; y < height - blockSize; y += stepSize) {
				for (x = 0; x < width - blockSize; x += stepSize) {
					for (band = 0; band < numBands; band++) {
						u[band][y][x] = tu[band][y][x];
						v[band][y][x] = tv[band][y][x];

					}
				}
			}
		}

		for (y = 0; y < height - blockSize; y += stepSize) {
			for (x = 0; x < width - blockSize; x += stepSize) {
				for (band = 0; band < numBands; band++) {
					uv = u[band][y][x];
					vv = v[band][y][x];
					if (Math.sqrt(uv * uv + vv * vv) > thres) {
						//frameOut.setPixel(
						//		band,
						//		x,
						//		y,
						//		scale*Math.sqrt(uv*uv+vv*vv));
						double aSign = 1.0;
						double bSign = 1.0;
						if (uv < 0)
							aSign = -1.0;
						if (vv < 0)
							bSign = -1.0;

						for (double ox = 0, oy = 0; ox <= Math.abs(uv * scale)
								&& oy <= Math.abs(vv * scale); ox += Math
								.abs(uv)
								/ Math.abs(vv), oy += Math.abs(vv)
								/ Math.abs(uv)) {

							frameOut.setPixel(band, (x + (int) (ox * aSign)),
									(y + (int) (oy * bSign)), 255.0);
						}

					}
				}
			}
		}

		return frameOut;
	}

	private EFrame doTimeCorelate(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		EFrame frameIn0 = eFrameIn.get(0);
		int width = frameIn0.getWidth();
		int height = frameIn0.getHeight();
		int numBands = frameIn0.getPixelStride();
		int x, y, band;
		MoviePoint moviePoint;

		frameOut.clearBuffer();

		int iters = effectContext.count + 1;

		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		EFrame frameIn1 = eFrameWork.get(1);
		if (frameIn1 == null || frameIn0 == null)
			return frameOut;

		BufferedImage biOut = frameOut.getBufferedImage();

		int blockSize = (int) (effectParams[2] * 100.0);
		int xRange = (int) (effectParams[3] * 100.0);
		int yRange = (int) (effectParams[4] * 100.0);
		double thres = (double) (effectParams[1] * 255.0);
		double scale = (double) (effectParams[5] * 100.0);
		int stepSize = effectContext.step + 1;

		if (effectContext.option1) {
			stepSize = 1;
			blockSize = 8;
			xRange = 8;
			yRange = 8;
			scale = 10.0;
		}

		int xrOffset = (xRange - 1) / 2;
		int yrOffset = (yRange - 1) / 2;
		double sum = 0;
		double avgmin = 0;
		double avg = 0;
		double count = 0;
		int xrMin = 0, yrMin = 0, maxXr = 0, minXr = 0, maxYr = 0, minYr = 0;
		double hv, fv;
		double maxdx = 0, maxdy = 0, maxdt = 0, mindx = 0, mindy = 0, mindt = 0;
		ArrayList[] list = new ArrayList[numBands];
		for (int i = 0; i < numBands; i++) {
			list[i] = new ArrayList();
		}
		System.out.println("in movie 2");

		for (y = yrOffset; y < height - blockSize - yrOffset; y += stepSize) {
			for (x = xrOffset; x < width - blockSize - xrOffset; x += stepSize) {
				for (band = 0; band < numBands; band++) {
					sum = 0;
					avgmin = 255.0;
					avg = 0;
					count = 0;
					for (int my = 0; my < blockSize - 1; my++) {
						for (int mx = 0; mx < blockSize - 1; mx++) {
							hv = frameIn1.getPixelDouble(band, x + mx, y + my);
							fv = frameIn0.getPixelDouble(band, x + mx, y + my);
							sum = sum + fv - hv;
							count++;
						}
					}
					avg = sum / count;
					if (avg > thres) {
						avgmin = 255.0;
						sum = 0;
						count = 0;

						for (int xr = 0; xr < xRange; xr++) {
							for (int yr = 0; yr < yRange; yr++) {
								for (int my = 0; my < blockSize - 1; my++) {
									for (int mx = 0; mx < blockSize - 1; mx++) {
										hv = frameIn1.getPixelDouble(band, x
												+ mx, y + my);
										fv = frameIn0.getPixelDouble(band, x
												+ mx + xr - xrOffset, y + my
												+ yr - yrOffset);
										sum = sum + fv - hv;
										count++;
									}
								}
								avg = sum / count;
								if (avgmin > avg) {
									avgmin = avg;
									xrMin = xr;
									yrMin = yr;
								}
								sum = 0;
								count = 0;
							}
						}

						if (avgmin < thres) {
							moviePoint = new MoviePoint(x, y, xrMin - xrOffset,
									yrMin - yrOffset, 0);
							if (maxXr < xrMin - xrOffset)
								maxXr = xrMin - xrOffset;
							if (maxYr < yrMin - yrOffset)
								maxYr = yrMin - yrOffset;
							if (minXr > xrMin - xrOffset)
								minXr = xrMin - xrOffset;
							if (maxYr > yrMin - yrOffset)
								minYr = yrMin - yrOffset;
							list[band].add(moviePoint);
						}
					}
				}
			}
		}
		double dxScale = 255.0 / maxdx;
		double dyScale = 255.0 / maxdy;
		double dtScale = 255.0 / maxdt;
		System.out.println("in movie 2a");

		double sScale = 20.0 / maxdt;
		double k, a, b, s;

		for (band = 0; band < numBands; band++) {
			for (int i = 0; i < list[band].size(); i++) {
				moviePoint = (MoviePoint) list[band].get(i);

				frameOut.setPixel(0, (moviePoint.x), (moviePoint.y), 255);
				//scale*Math.sqrt(moviePoint.dx*moviePoint.dx +
				// moviePoint.dy*moviePoint.dy));
				double aSign = 1.0;
				double bSign = 1.0;
				if (moviePoint.dy < 0)
					aSign = -1.0;
				if (moviePoint.dx < 0)
					bSign = -1.0;

				for (double ox = 0, oy = 0; ox <= scale
						* Math.abs(moviePoint.dx)
						&& oy <= scale * Math.abs(moviePoint.dy); ox += Math
						.abs(moviePoint.dx / moviePoint.dy), oy += Math
						.abs(moviePoint.dy / moviePoint.dx)) {

					frameOut.setPixel(band, moviePoint.x + (int) (ox * bSign),
							moviePoint.y + (int) (oy * aSign), 255.0);

					/*
					 * if (bSign>0){ frameOut.setPixel( 1,
					 * moviePoint.x+(int)(ox*bSign),
					 * moviePoint.y+(int)(oy*aSign), 255.0); } else {
					 * frameOut.setPixel( 2, moviePoint.x+(int)(ox*bSign),
					 * moviePoint.y+(int)(oy*aSign), 255.0); }
					 */
				}
			}
		}
		return frameOut;
	}

	public class RGBPixel {
		public RGBPixel(double r, double b, double g) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public double r, g, b;
	}

	private EFrame doSobelMax(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());

		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		boolean done = false;
		int greyLevel = 0;
		int W = 255;
		double thres = effectParams[1] * 255.0;
		double[] sr = null;
		double level;
		int count = 0;

		int x, y, band;
		frameOut.clearBuffer();
		boolean test = true;
		int[][][] xmags = new int[numBands][width][height];
		int[][][] ymags = new int[numBands][width][height];
		int[][][] emags = new int[numBands][width][height];

		for (y = 1; y < height - 1; y++) {
			for (x = 1; x < width - 1; x++) {
				for (band = 0; band < numBands; band++) {
					sr = getSampleRegion33(frameIn, band, x, y);
					count = 0;
					xmags[band][x][y] = (int) (sr[8] + 2 * sr[7] + sr[6]
							- sr[2] - 2 * sr[1] - sr[0]);
					ymags[band][x][y] = (int) (sr[2] + 2 * sr[5] + sr[8]
							- sr[0] - 2 * sr[3] - sr[6]);
					emags[band][x][y] = (int) Math.sqrt(xmags[band][x][y]
							* xmags[band][x][y] + ymags[band][x][y]
							* ymags[band][x][y]);
					frameOut.setPixel(band, x, y, emags[band][x][y]);
				}
			}
		}
		int uxs = 0, uys = 0, ux = 0, uy = 0, grad = 0, grad1 = 0, grad2 = 0, cor1 = 0, cor2 = 0, mid1 = 0, mid2 = 0;
		for (y = 1; y < height - 1; y++) {
			for (x = 1; x < width - 1; x++) {
				for (band = 0; band < numBands; band++) {
					uxs = xmags[band][x][y];
					uys = ymags[band][x][y];
					ux = Math.abs(uxs);
					uy = Math.abs(uys);
					if (((uxs >= 0) && (uys >= 0)) || ((uxs < 0) && (uys < 0))) {
						cor1 = emags[band][x + 1][y + 1];
						cor2 = emags[band][x - 1][y - 1];
						if (ux >= uy) {
							mid1 = emags[band][x][y + 1];
							mid2 = emags[band][x][y - 1];
							grad = ux * emags[band][x][y];
							grad1 = uy * cor1 + (ux - uy) * mid1;
							grad2 = uy * cor2 + (ux - uy) * mid2;
						} else {
							mid1 = emags[band][x + 1][y];
							mid2 = emags[band][x - 1][y];
							grad = uy * emags[band][x][y];
							grad1 = ux * cor1 + (uy - ux) * mid1;
							grad2 = ux * cor2 + (uy - ux) * mid2;
						}
					} else {
						cor1 = emags[band][x + 1][y - 1];
						cor2 = emags[band][x - 1][y + 1];
						if (ux >= uy) {
							mid1 = emags[band][x][y - 1];
							mid2 = emags[band][x][y + 1];
							grad = ux * emags[band][x][y];
							grad1 = uy * cor1 + (ux - uy) * mid1;
							grad2 = uy * cor2 + (ux - uy) * mid2;
						} else {
							mid1 = emags[band][x + 1][y];
							mid2 = emags[band][x - 1][y];
							grad = uy * emags[band][x][y];
							grad1 = ux * cor1 + (uy - ux) * mid1;
							grad2 = ux * cor2 + (uy - ux) * mid2;
						}
					}
					frameOut.setPixel(band, x, y, emags[band][x][y]);
					if ((grad > grad1) && (grad >= grad2)) {
						frameOut.setPixel(band, x, y, 0);
					}
				}
			}
		}

		return frameOut;
	}

	private EFrame doHysteresis(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		System.out.println("in hyster");
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();

		int lt = (int) (effectParams[1] * 255.0);
		int ht = (int) (effectParams[2] * 255.0);

		int level;
		int x, y, band;
		frameOut.clearBuffer();

		for (y = 2; y < height - 1; y++) {
			for (x = 2; x < width - 1; x++) {
				for (band = 0; band < numBands; band++) {
					level = frameIn.getPixelInt(band, x, y);
					if ((level >= ht) && (level != 255)) {
						frameOut.setPixel(band, x, y, 255);
						frameOut = connect(band, x, y, frameOut, lt, width,
								height);
					}
				}
			}
		}

		return frameOut;
	}

	private EFrame connect(int band, int x, int y, EFrame frameIn, int lt,
			int width, int height) {

		int x1, y1, band1, level;

		for (y1 = y - 1; y1 < y + 1; y1++) {
			for (x1 = x - 1; x1 < x + 1; x1++) {
				level = frameIn.getPixelInt(band, x1, y1);
				if ((level >= lt)
						&& (level != 255)
						&& ((x1 >= 1) && (x1 <= width - 2) && (y1 >= 1) && (y1 <= height - 2))) {
					frameIn.setPixel(band, x1, y1, 255);
					frameIn = connect(band, x1, y1, frameIn, lt, width, height);
				}
			}
		}
		return frameIn;
	}

	private EFrame doTimeMix(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());

		EFrame frameWork1 = new EFrame(frameIn.getBuffer());
		frameWork1.clearBuffer();
		EFrame newFrame = null;
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		double z, lastSample, newSample, sampleDelta;

		int mixCount = effectContext.count;
		double scale = effectParams[1];
		int step = effectContext.step + 1;
		int skip = (int) (effectParams[1] * 100.0) + 1;

		EFrameSet eFrameWork = null;
		if (effectContext.mode == 0) {
			eFrameWork = eFrameIn;
		} else if (effectContext.mode == 1) {
			eFrameWork = eFrameFilter;
		} else {
			eFrameWork = eFrameOut;
		}
		int frameCount = eFrameWork.getCount();
		int offset;
		double factor1 = 0.0, factor2 = 0.0;
		System.out.println("Time mix factors: " + factor1 + ", " + factor2
				+ ", " + step + ", " + frameCount + ", " + mixCount);

		if (!effectContext.option1) {

			if (frameCount < step)
				return frameOut;

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int band = 0; band < numBands; band++) {
						z = 0;
						for (offset = 0; offset < frameCount
								&& offset < mixCount / 2; offset += step) {
							newFrame = eFrameWork.get(offset);
							if (newFrame == null)
								break;
							factor1 = scale
									* ((double) (offset * 2) / (double) mixCount);
							if (band == 0 && x == 100 && y == 100)
								System.out.println("Time mix factors 1: "
										+ offset + ", " + factor1 + ", " + step
										+ ", " + frameCount + ", " + mixCount);

							newSample = factor1
									* newFrame.getPixelDouble(band, x, y);
							z = newSample + z;
						}
						for (; offset < frameCount && offset < mixCount; offset += step) {
							newFrame = eFrameWork.get(offset);
							if (newFrame == null)
								break;
							factor2 = scale
									* ((double) ((mixCount - offset) * 2.0) / (double) mixCount);
							if (band == 0 && x == 100 && y == 100)
								System.out.println("Time mix factors 2: "
										+ offset + ", " + factor2 + ", " + step
										+ ", " + frameCount + ", " + mixCount);
							newSample = factor2
									* newFrame.getPixelDouble(band, x, y);
							z = newSample + z;
						}
						if (z > 255)
							z = 255;
						if (z < -255)
							z = -255;
						if (effectContext.option2) {
							z = z / 2;
							z = z + 255 / 2;
						}
						if (z < 0) {
							if (effectContext.option4) {
								z = 255 - z;
							} else if (effectContext.option5) {
								z = -z;
							} else if (effectContext.option6) {
								z = 0;
							}
						}
						frameOut.setPixel(band, x, y, z);
					}
				}
			}

		} else {

			skip = (int) (effectParams[3] * 100.0) + 1;
			step = (int) (effectParams[2] * 100.0) + 1;
			mixCount = (int) (effectParams[4] * 100.0) + 1;
			System.out.println("Time mix factors X: " + factor1 + ", "
					+ factor2 + ", " + step + ", " + frameCount + ", "
					+ mixCount);

			Image image = null;
			int workFrame = 0;
			int targetFrame = 0;
			int startFrame = ejSettings.effectCompo.grabberA.getBeginFrame();
			int endFrame = ejSettings.effectCompo.grabberA.getEndFrame();
			frameOut.clearBuffer();
			for (offset = 0; offset < mixCount / 2; offset++) {
				if (skip > 1) {
					workFrame = (int) Math.abs((1 + ((int) Math.abs(seqNumber
							- 1 - offset))
							/ skip));
				} else {
					workFrame = (int) Math.abs((1 + ((int) Math
							.abs(seqNumber - 1))
							/ skip));
				}
				if (step > 1) {
					targetFrame = workFrame + (workFrame - 1) * (step - 1)
							+ startFrame - offset;
				} else {
					targetFrame = workFrame + (workFrame - 1) * (step - 1)
							+ startFrame;
				}

				if (targetFrame > endFrame)
					break;
				if (targetFrame < startFrame)
					break;
				image = ejSettings.effectCompo.grabberA.grabFrame(targetFrame);
				if (image == null)
					break;
				frameWork1.setImage(image);
				factor1 = scale * ((double) (offset * 2) / (double) mixCount);
				System.out.println("Time mix factors A: " + startFrame + ", "
						+ targetFrame + ", " + offset + ", " + factor1 + ", "
						+ step + ", " + frameCount + ", " + mixCount);
				frameWork1 = doJAIMultiplyConst(factor1, frameWork1);
				frameOut = doJAIAdd(frameOut, frameWork1);
			}
			for (; offset < mixCount; offset++) {
				if (skip > 1) {
					workFrame = (int) Math.abs((1 + ((int) Math.abs(seqNumber
							- 1 - offset))
							/ skip));
				} else {
					workFrame = (int) Math.abs((1 + ((int) Math
							.abs(seqNumber - 1))
							/ skip));
				}
				if (step > 1) {
					targetFrame = workFrame + (workFrame - 1) * (step - 1)
							+ startFrame - offset;
				} else {
					targetFrame = workFrame + (workFrame - 1) * (step - 1)
							+ startFrame;
				}

				if (targetFrame > endFrame)
					break;
				if (targetFrame < startFrame)
					break;
				image = ejSettings.effectCompo.grabberA.grabFrame(targetFrame);
				if (image == null)
					break;
				frameWork1.setImage(image);
				factor2 = scale
						* ((double) ((mixCount - offset) * 2.0) / (double) mixCount);
				System.out.println("Time mix factors B: " + startFrame + ", "
						+ targetFrame + ", " + offset + ", " + factor2 + ", "
						+ step + ", " + frameCount + ", " + mixCount);

				frameWork1 = doJAIMultiplyConst(factor2, frameWork1);
				frameOut = doJAIAdd(frameOut, frameWork1);

			}
		}

		return frameOut;
	}

	private EFrame doJAITranslate(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();

		if (effectContext.mode == 0) {
			float xtrans = (float) ((effectParams[1] - effectParams[2]) * ropIn
					.getWidth());
			float ytrans = (float) ((effectParams[3] - effectParams[4]) * ropIn
					.getHeight());
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(xtrans);
			pb.add(ytrans);
			pb.add(new InterpolationNearest());
			RenderedOp ropOut = JAI.create("translate", pb, null);
			frameOut.setRenderedOp(ropOut);
			System.out.println("X/y trans: " + xtrans + ", " + ytrans);
			return frameOut;

		} else if (effectContext.mode == 1) {
			float xOrig = (float) ((effectParams[1]) * ropIn.getWidth());
			float yOrig = (float) ((effectParams[2]) * ropIn.getHeight());
			float angle = (float) ((effectParams[3]) * 360.0);
			float radians = (float) (angle * (Math.PI / 180.0F));
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(ropIn);
			pb.add(xOrig);
			pb.add(yOrig);
			pb.add(6.27F);
			//pb.add(new InterpolationNearest());
			RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
					BorderExtender.createInstance(BorderExtender.BORDER_COPY));

			//rh.put(
			//	RenderingHints.KEY_ANTIALIASING,
			//	RenderingHints.VALUE_ANTIALIAS_ON);
			//rh.put(
			//	RenderingHints.KEY_INTERPOLATION,
			//	RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			//rh.put(RenderingHints.KEY_RENDERING,
			// RenderingHints.VALUE_RENDER_QUALITY);

			RenderedOp ropOut = JAI.create("rotate", pb, rh);

			frameOut.setRenderedOp(ropOut);
			return frameOut;
		}

		return frameOut;
	}

	private EFrame doJAIEnhance(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		if (effectContext.count == 0) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			ContrastFilter f = new ContrastFilter();
			f.setGain(gain);
			f.setBias(bias);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		} else if (effectContext.count == 1) {
			float amount = (float ) effectParams[1] / (float) effectParams[2];
			float factor = 1.0f;
		
			MarbleFilter f = new MarbleFilter();
			factor = f.getAmount();
			if (factor ==0)factor=1.0f;
			f.setAmount(factor*amount);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		} else if (effectContext.count == 2) {
			float amount = (float ) effectParams[1] / (float) effectParams[2];
			float factor = 1.0f;
		
			CellularFilter f = new CellularFilter();
			factor = f.getAmount();
			if (factor ==0)factor=1.0f;
			f.setAmount(factor*amount);
		
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		} else if (effectContext.count == 3) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			CraterFilter f = new CraterFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
		
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 4) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			LifeFilter f = new LifeFilter();
			int op = effectContext.step;
			f.setIterations(op);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 5) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			PlasmaFilter f = new PlasmaFilter();
			int op = effectContext.step;
			f.setUseImageColors(effectContext.option1);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 6) {
			float scale = (float ) effectParams[3] / (float) effectParams[4];
			float amount = (float ) effectParams[1] / (float) effectParams[2];
			float stretch = (float ) effectParams[5] / (float) effectParams[6];
			float turb = (float ) effectParams[7] / (float) effectParams[8];
			float factor = 1.0f;
			int op = effectContext.step;
			
			CrystalizeFilter f = new CrystalizeFilter();
			factor = f.getScale();
			if (factor ==0)factor=1.0f;
			f.setScale(factor*scale);
			factor = f.getAmount();
			if (factor ==0)factor=1.0f;
			f.setAmount(factor*amount);
			factor = f.getStretch();
			if (factor ==0)factor=1.0f;
			f.setStretch(factor*stretch);
			factor = f.getTurbulence();
			if (factor ==0)factor=1.0f;
			f.setTurbulence(factor*turb);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 7) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			DiffusionFilter f = new DiffusionFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 8) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			DilateFilter f = new DilateFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 9) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			DistanceFilter f = new DistanceFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 10) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			DistanceFilter f = new DistanceFilter();
			f.setFactor(gain);
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 11) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			int op = effectContext.step;
			
			DitherFilter f = new DitherFilter();
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 12) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			ErodeFilter f = new ErodeFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 13) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			FBMFilter f = new FBMFilter();
			int op = effectContext.step;
			
			f.setOperation(op);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 14) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			LightFilter f = new LightFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 15) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			OutlineFilter f = new OutlineFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 16) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			PolarFilter f = new PolarFilter();
		
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 17) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			QuiltFilter f = new QuiltFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 18) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			PerspectivFilter f = new PerspectivFilter();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 19) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			RippleFilter f = new RippleFilter ();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 20) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			ShadowFilter f = new ShadowFilter ();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 21) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			SkeletonFilter f = new SkeletonFilter ();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 22) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			SolarizeFilter f = new SolarizeFilter ();
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 23) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			int op = effectContext.step;
			
			SparkleFilter f = new SparkleFilter ();
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 24) {
			float gain = (float) effectParams[1] / (float) effectParams[2];
			float bias = (float) effectParams[3] / (float) effectParams[4];
			int op = effectContext.step;
			TextureFilter f = new TextureFilter ();
			f.setOperation(op);
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 25) {
			float scale = (float ) effectParams[3] / (float) effectParams[4];
			float amount = (float ) effectParams[1] / (float) effectParams[2];
			float stretch = (float ) effectParams[5] / (float) effectParams[6];
			float turb = (float ) effectParams[7] / (float) effectParams[8];
			float factor = 1.0f;
			int op = effectContext.step;
			
			WeaveFilter f = new WeaveFilter();
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 26) {
			float scale = (float ) effectParams[3] / (float) effectParams[4];
			float amount = (float ) effectParams[1] / (float) effectParams[2];
			float stretch = (float ) effectParams[5] / (float) effectParams[6];
			float turb = (float ) effectParams[7] / (float) effectParams[8];
			float factor = 1.0f;
			int op = effectContext.step;
			
			OilFilter f = new OilFilter();
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 27) {
						
			SharpenFilter f = new SharpenFilter();
		
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 28) {
			float rg = (float ) effectParams[1] / (float) effectParams[2];
			float gg = (float ) effectParams[3] / (float) effectParams[4];
			float bg = (float ) effectParams[5] / (float) effectParams[6];
			int op = effectContext.step;
			
			GammaFilter f = new GammaFilter();
			f.setGamma(rg,gg,bg);
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}else if (effectContext.count == 29) {
			float rg = (float ) effectParams[1] / (float) effectParams[2];
			float gg = (float ) effectParams[3] / (float) effectParams[4];
			float bg = (float ) effectParams[5] / (float) effectParams[6];
			int op = effectContext.step;
			
			HSBAdjustFilter f = new HSBAdjustFilter(rg,gg,bg);
			
			ImageProducer ip = new FilteredImageSource(frameOut
					.getBufferedImage().getSource(), f);
			Image im = ejMain.getFrame().createImage(ip);
			frameOut.setImage(im);
		}
		return frameOut;

	}

	private EFrame doTranslate(EFrame frameIn) {

		EFrame frameOut = new EFrame(frameIn.getBuffer());
		RenderedOp ropIn = frameIn.getRenderedOp();
		int width = frameIn.getWidth();
		int height = frameIn.getHeight();
		int numBands = frameIn.getPixelStride();
		float xtrans = (float) ((effectParams[1] - effectParams[2]) * ropIn
				.getWidth());
		float ytrans = (float) ((effectParams[3] - effectParams[4]) * ropIn
				.getHeight());
		frameOut.clearBuffer();
		if (!effectContext.option1) {
			for (int y = 1; y < height - 1; y++) {
				for (int x = 1; x < width - 1; x++) {
					for (int band = 0; band < numBands; band++) {
						frameOut.setPixel(band, x + (int) xtrans, y
								+ (int) ytrans, frameIn.getPixel(band, x, y));
					}
				}
			}
		} else {
			for (int y = 1; y < height - 1; y++) {
				for (int x = 1; x < width - 1; x++) {
					for (int band = 0; band < numBands; band++) {
						frameOut.setPixel(band, x, y, frameIn.getPixel(band, y,
								x));
					}
				}
			}

		}
		return frameOut;
	}

	private double[] getSampleRegion33(EFrame frameIn, int band, int x, int y) {
		double[] sr = new double[9];
		sr[0] = frameIn.getPixelDouble(band, x - 1, y - 1);
		sr[1] = frameIn.getPixelDouble(band, x, y - 1);
		sr[2] = frameIn.getPixelDouble(band, x + 1, y - 1);
		sr[3] = frameIn.getPixelDouble(band, x - 1, y);
		sr[4] = frameIn.getPixelDouble(band, x, y);
		sr[5] = frameIn.getPixelDouble(band, x + 1, y);
		sr[6] = frameIn.getPixelDouble(band, x - 1, y + 1);
		sr[7] = frameIn.getPixelDouble(band, x, y + 1);
		sr[8] = frameIn.getPixelDouble(band, x + 1, y + 1);
		return sr;
	}

	private double[] getSampleRegion55(EFrame frameIn, int band, int x, int y) {
		double[] sr = new double[25];
		sr[0] = frameIn.getPixelDouble(band, x - 2, y - 2);
		sr[1] = frameIn.getPixelDouble(band, x - 1, y - 2);
		sr[2] = frameIn.getPixelDouble(band, x, y - 2);
		sr[3] = frameIn.getPixelDouble(band, x + 1, y - 2);
		sr[4] = frameIn.getPixelDouble(band, x + 2, y - 2);
		sr[5] = frameIn.getPixelDouble(band, x - 2, y - 1);
		sr[6] = frameIn.getPixelDouble(band, x - 1, y - 1);
		sr[7] = frameIn.getPixelDouble(band, x, y - 1);
		sr[8] = frameIn.getPixelDouble(band, x + 1, y - 1);
		sr[9] = frameIn.getPixelDouble(band, x + 2, y - 1);
		sr[10] = frameIn.getPixelDouble(band, x - 2, y);
		sr[11] = frameIn.getPixelDouble(band, x - 1, y);
		sr[12] = frameIn.getPixelDouble(band, x, y);
		sr[13] = frameIn.getPixelDouble(band, x + 1, y);
		sr[14] = frameIn.getPixelDouble(band, x + 2, y);
		sr[15] = frameIn.getPixelDouble(band, x - 2, y + 1);
		sr[16] = frameIn.getPixelDouble(band, x - 1, y + 1);
		sr[17] = frameIn.getPixelDouble(band, x, y + 1);
		sr[18] = frameIn.getPixelDouble(band, x + 1, y + 1);
		sr[19] = frameIn.getPixelDouble(band, x + 2, y + 1);
		sr[20] = frameIn.getPixelDouble(band, x - 2, y + 2);
		sr[21] = frameIn.getPixelDouble(band, x - 1, y + 2);
		sr[22] = frameIn.getPixelDouble(band, x, y + 2);
		sr[23] = frameIn.getPixelDouble(band, x + 1, y + 2);
		sr[24] = frameIn.getPixelDouble(band, x + 2, y + 2);
		return sr;
	}

	private double[] getSampleRegion33(EFrame frameIn, int x, int y) {
		double[] sr = new double[9];
		sr[0] = frameIn.getPixelGrey(x - 1, y - 1);
		sr[1] = frameIn.getPixelGrey(x, y - 1);
		sr[2] = frameIn.getPixelGrey(x + 1, y - 1);
		sr[3] = frameIn.getPixelGrey(x - 1, y);
		sr[4] = frameIn.getPixelGrey(x, y);
		sr[5] = frameIn.getPixelGrey(x + 1, y);
		sr[6] = frameIn.getPixelGrey(x - 1, y + 1);
		sr[7] = frameIn.getPixelGrey(x, y + 1);
		sr[8] = frameIn.getPixelGrey(x + 1, y + 1);
		return sr;
	}

	private double[] getSampleRegion55(EFrame frameIn, int x, int y) {
		double[] sr = new double[25];
		sr[0] = frameIn.getPixelGrey(x - 2, y - 2);
		sr[1] = frameIn.getPixelGrey(x - 1, y - 2);
		sr[2] = frameIn.getPixelGrey(x, y - 2);
		sr[3] = frameIn.getPixelGrey(x + 1, y - 2);
		sr[4] = frameIn.getPixelGrey(x + 2, y - 2);
		sr[5] = frameIn.getPixelGrey(x - 2, y - 1);
		sr[6] = frameIn.getPixelGrey(x - 1, y - 1);
		sr[7] = frameIn.getPixelGrey(x, y - 1);
		sr[8] = frameIn.getPixelGrey(x + 1, y - 1);
		sr[9] = frameIn.getPixelGrey(x + 2, y - 1);
		sr[10] = frameIn.getPixelGrey(x - 2, y);
		sr[11] = frameIn.getPixelGrey(x - 1, y);
		sr[12] = frameIn.getPixelGrey(x, y);
		sr[13] = frameIn.getPixelGrey(x + 1, y);
		sr[14] = frameIn.getPixelGrey(x + 2, y);
		sr[15] = frameIn.getPixelGrey(x - 2, y + 1);
		sr[16] = frameIn.getPixelGrey(x - 1, y + 1);
		sr[17] = frameIn.getPixelGrey(x, y + 1);
		sr[18] = frameIn.getPixelGrey(x + 1, y + 1);
		sr[19] = frameIn.getPixelGrey(x + 2, y + 1);
		sr[20] = frameIn.getPixelGrey(x - 2, y + 2);
		sr[21] = frameIn.getPixelGrey(x - 1, y + 2);
		sr[22] = frameIn.getPixelGrey(x, y + 2);
		sr[23] = frameIn.getPixelGrey(x + 1, y + 2);
		sr[24] = frameIn.getPixelGrey(x + 2, y + 2);
		return sr;
	}

	public class Imagine implements ImageFunction {

		double p1, p2, p3, p4, p5, p6, p7, p8, p9;

		public Imagine(double p1, double p2, double p3, double p4, double p5,
				double p6, double p7, double p8, double p9) {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.p4 = p4;
			this.p5 = p5;
			this.p6 = p6;
			this.p7 = p7;
			this.p8 = p8;
			this.p9 = p9;

		}

		public boolean isComplex() {
			return false;
		}

		public int getNumElements() {
			return 3;
		}

		public void getElements(float startX, float startY, float deltaX,
				float deltaY, int countX, int countY, int element,
				float[] real, float[] imag) {

			System.out.println("imagine " + countX + ", " + countY + ", "
					+ startX + ", " + startY + ", " + real.length);
			if (element == 1) {
				for (int i = seqNumber * 300; i < (countX * countY); i += 3) {
					real[i] = 255;
				}
			}
			if (element == 2) {
				for (int i = ((countX * countY - 1) - seqNumber * 300); i > 0; i -= 2) {
					real[i] = 255;
				}
			}
			int x, y;
			for (int i = 0; i < real.length; i++) {

				y = i / countX;
				x = i % countX;
				real[i] = (float) (Math
						.sqrt((double) ((x - countX / 2) * (x - countX / 2))
								* p1
								/ p2
								+ (double) ((y - countY / 2) * (y - countY / 2))
								* p3 / p4)
						* p5 / p6);

				float factor = (real[i] + 255.0f) / 255.0f;

				if (effectContext.option2) {
					real[i] = real[i] * (factor * (float) element);
				}

				if (effectContext.option3) {
					real[i] = real[i] % 255.0f;
				}

				if (effectContext.option4) {
					if (real[i] < 0)
						real[i] = 0;
					if (real[i] > 255.0f)
						real[i] = 255.0f;
				}

				if (effectContext.option1) {
					real[i] = 255.0f - real[i];
				}
			}

		}

		public void getElements(double startX, double startY, double deltaX,
				double deltaY, int countX, int countY, int element,
				double[] real, double[] imag) {

			if (element == 1) {
				for (int i = seqNumber * 300; i < (countX * countY); i += 3) {
					real[i] = 255;
				}
			}
			if (element == 2) {
				for (int i = ((countX * countY - 1) - seqNumber * 300); i > 0; i -= 2) {
					real[i] = 255;
				}
			}

			int x, y;
			for (int i = 0; i < real.length; i++) {

				y = i / countX;
				x = i % countX;
				real[i] = (float) (Math
						.sqrt((double) ((x - countX) * (x - countX)) * p1 / p2
								+ (double) ((y - countY) * (y - countY)) * p3
								/ p4)
						* p5 / p6);

				double factor = (real[i] + 255.0) / 255.0;

				if (effectContext.option2) {
					real[i] = real[i] * (factor * (double) element);
				}

				if (effectContext.option3) {
					real[i] = real[i] % 255.0;
				}

				if (effectContext.option4) {
					if (real[i] < 0)
						real[i] = 0;
					if (real[i] > 255.0)
						real[i] = 255.0;
				}

				if (effectContext.option1) {
					real[i] = 255.0 - real[i];
				}
			}
		}
	}

}