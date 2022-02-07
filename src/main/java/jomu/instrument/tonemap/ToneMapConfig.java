package jomu.instrument.tonemap;

import java.io.*;
public class ToneMapConfig implements Serializable {
/**
 * ToneMapConfig constructor comment.
 */
public ToneMapConfig() {
	super();
}

public double timeStart;
public double timeEnd;
public int pitchLow;
public int pitchHigh;
public int tFactor;
public int pFactor;
public int resolution;
public double sampleTimeSize;
public File audioFile;
public int noteSustain;
public int noteMinDuration;
public int noteMaxDuration;
public int normalizeSetting;
public int noteLow;
public int noteHigh;
public int processMode;
public int underToneSetting;
public int harmonic1Setting;
public int harmonic2Setting;
public int harmonic3Setting;
public int harmonic4Setting;
public int formantLowSetting;
public int formantMiddleSetting;
public int formantHighSetting;
public int formantFactor;
public boolean harmonicSwitch;
public boolean formantSwitch;
public boolean undertoneSwitch;
public boolean peakSwitch;
public boolean normalizeSwitch;
public int lowThreshhold;
public int highThreshhold;



}