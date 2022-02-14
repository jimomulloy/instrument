package jomu.instrument.tonemap;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JPanel;

/**
 * This class defines the Audio Sub System Data Model processing functions for
 * the ToneMap including file reading, Audio data transformation, Playback
 * implementation and control settings management through the AudioPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class AudioModel implements PlayerInterface, ToneMapConstants {

	// Inner class to handle Audio playback line listener
	private class ClipListener implements LineListener {
		public void update(LineEvent event) {

			if (event.getType() == LineEvent.Type.STOP && playState != PAUSED) {
				audioEOM = true;
				if (playState != STOPPED) {
					clip.stop();
					playState = EOM;
				}

			}
		}
	}

	public double timeStart = (double) INIT_TIME_START;
	public double timeEnd = (double) INIT_TIME_END;
	public double sampleTimeSize = (double) INIT_SAMPLE_SIZE;
	public int reverbSSetting = 0;
	public int reverbRSetting = 0;
	public int osc1Setting = 0;
	public int osc2Setting = 0;
	public int t1Setting = 50;
	public int t2Setting = 50;
	public int t3Setting = 50;
	public int t4Setting = 50;
	public int panSetting = INIT_PAN_SETTING;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	public int gainSetting = INIT_VOLUME_SETTING;
	public int resolution = 1;
	public int tFactor = 50;
	public int pFactor = 100;
	public int pOffset = 0;
	public int transformMode = TRANSFORM_MODE_JAVA;
	public int powerHigh = 100;
	public int oscType = Oscillator.SINEWAVE;

	public int powerLow = 0;
	public boolean logSwitch = false;
	public boolean osc1Switch = false;
	public boolean osc2Switch = false;
	public boolean t1Switch = false;
	public boolean t2Switch = false;
	public boolean t3Switch = false;
	public boolean t4Switch = false;

	private int playState = STOPPED;

	public ToneMapFrame toneMapFrame;
	private Player player;
	private AudioPanel audioPanel;
	private ToneMapMatrix toneMapMatrix;
	private ToneMapElement element;
	private int matrixLength;

	private double amplitude;

	private int index;
	private NoteSequence noteSequence;
	private NoteSequenceElement noteSequenceElement;
	private NoteList noteList;
	private NoteListElement noteListElement;
	private ToneMapMatrix.Iterator mapIterator;
	private ToneMapElement toneMapElement;

	private ToneMap toneMap;
	private TimeSet timeSet;
	private PitchSet pitchSet;

	private double sampleRate;
	private int numChannels;
	private double sampleBitSize;

	private String errStr;

	private double duration, seconds;

	private AudioInputStream audioInputStream;
	private AudioFormat format = null;
	private AudioInputStream outAudioStream;
	private AudioFormat outFormat = null;

	private AudioFormat playFormat = null;
	private int[] audioData = null;
	private byte[] audioBytes = null;
	private byte[] outAudioBytes = null;

	private int nlengthInSamples;
	private double[] audioFTPower = null;
	private double[] pitchFreqSet;

	private int timeRange;
	private int pitchRange;

	private double transTime;

	private String fileName = "untitled";
	private File file;

	Wavelet wavelet = new Wavelet();

	private Clip clip;
	private boolean audioEOM;

	/**
	 * AudioModel constructor. Test Java Sound Audio System available Instantiate
	 * AudioPanel
	 */
	public AudioModel(ToneMapFrame toneMapFrame) {

		this.toneMapFrame = toneMapFrame;

		try {

			if (AudioSystem.getMixer(null) == null) {
				toneMapFrame.reportStatus(EC_AUDIO_SYSTEM);
				return;
			}
		} catch (Exception ex) {
			toneMapFrame.reportStatus(EC_AUDIO_SYSTEM);
			return;
		}

		audioPanel = new AudioPanel(this);
		if (toneMapFrame.getJNIStatus())
			audioPanel.jniB.setEnabled(true);

	}

	/**
	 * Get configuration parameters
	 */
	public void getConfig(ToneMapConfig config) {
		config.timeStart = this.timeStart;
		config.timeEnd = this.timeEnd;
		config.pitchLow = this.pitchLow;
		config.pitchHigh = this.pitchHigh;
		config.sampleTimeSize = this.sampleTimeSize;
		config.resolution = this.resolution;
		config.tFactor = this.tFactor;
		config.pFactor = this.pFactor;
		config.audioFile = this.file;
	}

	/**
	 * Set configuration parameters
	 */
	public void setConfig(ToneMapConfig config) {
		audioPanel.timeControl.setTimeStart(config.timeStart);
		audioPanel.timeControl.setTimeEnd(config.timeEnd);
		audioPanel.timeControl.setTimeMax((int) (config.timeEnd - config.timeStart));
		audioPanel.pitchControl.setPitchLow(config.pitchLow);
		audioPanel.pitchControl.setPitchHigh(config.pitchHigh);
		// audioPanel.pitchControl.setPitchRange(config.pitchLow, config.pitchHigh);
		audioPanel.sampleSizeSlider.setValue((int) config.sampleTimeSize);
		audioPanel.resolutionSlider.setValue(config.resolution);
		audioPanel.tFactorSlider.setValue(config.tFactor);
		audioPanel.pFactorSlider.setValue(config.pFactor);
		load(config.audioFile);
		audioPanel.fileNameField.setText(config.audioFile.getParent() + "\\" + getFileName());
		audioPanel.durationField.setText(String.valueOf(getDuration()));
		audioPanel.sampleRateField.setText(String.valueOf(getSampleRate()));
		audioPanel.bitSizeField.setText(String.valueOf(getSampleBitSize()));
		audioPanel.channelsField.setText(String.valueOf(getNumChannels()));

	}

	/**
	 * Clear current AudioModel objects after Reset
	 */

	public void clear() {
		playStop();
		clip = null;
		audioBytes = null;
		audioData = null;
		audioFTPower = null;
		file = null;
		fileName = "";
		audioPanel.fileNameField.setText("");
		audioPanel.durationField.setText("");
		audioPanel.sampleRateField.setText("");
		audioPanel.bitSizeField.setText("");
		audioPanel.channelsField.setText("");
		audioPanel.timeControl.setTimeMax(INIT_TIME_MAX);
	}

	/**
	 * Open File and load audio data.
	 */
	public boolean openFile() {
		return audioPanel.openFile();
	}

	public double[] getAudioFTPower() {
		return audioFTPower;
	}

	public double getDuration() {
		return duration;
	}

	public double getEndTime() {
		return timeEnd;
	}

	public File getFile() {
		return file;
	}

	public String getFileName() {
		return fileName;
	}

	public int getHighPitch() {
		return pitchHigh;
	}

	public int getLowPitch() {
		return pitchLow;
	}

	public int getNumChannels() {
		return numChannels;
	}

	public JPanel getPanel() {
		return audioPanel;
	}

	public double getSampleBitSize() {
		return sampleBitSize;
	}

	public double getSampleRate() {
		return sampleRate;
	}

	public double getSampleTimeSize() {
		return sampleTimeSize;
	}

	public double getStartTime() {
		return timeStart;
	}

	/**
	 * Load Audio file data into audioData Array.
	 */
	public boolean load(File file) {

		this.file = file;

		if (file != null && file.isFile()) {
			try {
				errStr = null;
				// connect inputStream to Audio file
				audioInputStream = AudioSystem.getAudioInputStream(file);
				fileName = file.getName();
				// get Audio file format
				format = audioInputStream.getFormat();
				playFormat = null;

			} catch (Exception ex) {
				toneMapFrame.reportStatus(EC_AUDIO_OPEN);
				this.file = null;
				fileName = null;
				return false;
			}
		} else {
			this.file = null;
			fileName = null;
			toneMapFrame.reportStatus(EC_AUDIO_OPEN_NOFILE);
			return false;
		}

		// extract information from audio file
		numChannels = format.getChannels();
		sampleRate = (double) format.getSampleRate();
		sampleBitSize = format.getSampleSizeInBits();
		long frameLength = audioInputStream.getFrameLength();
		// exit if null file
		if (frameLength == 0) {
			toneMapFrame.reportStatus(EC_AUDIO_OPEN_NULLFILE);
			this.file = null;
			fileName = null;
			return false;
		}
		long milliseconds = (long) ((frameLength * 1000) / audioInputStream.getFormat().getFrameRate());
		double audioFileDuration = milliseconds / 1000.0;

		if (audioFileDuration > MAX_AUDIO_DURATION)
			duration = MAX_AUDIO_DURATION;
		else
			duration = audioFileDuration;

		frameLength = (int) Math.floor((duration / audioFileDuration) * (double) frameLength);

		// extract sampled data from audio file into audioBytes array
		try {
			audioBytes = new byte[(int) frameLength * format.getFrameSize()];
			audioInputStream.mark(audioBytes.length);
			audioInputStream.read(audioBytes);
		} catch (Exception ex) {
			this.file = null;
			fileName = null;
			toneMapFrame.reportStatus(EC_AUDIO_OPEN_READ);
			return false;
		}

		// convert audioBytes to standard format in audioData array
		getAudioData();

		// merge 2 channel stereo data into one channel
		if (numChannels == 2)
			stereoToMono();

		return true;
	}

	// convert audioBytes sampled audio data into standard format in audioData
	// array.
	private void getOutAudioBytes(double[] outAudioData) {

		if (format.getSampleSizeInBits() == 16) {
			outAudioBytes = new byte[outAudioData.length * 2];
			if (format.isBigEndian()) {
				for (int i = 0; i < outAudioData.length; i++) {
					// First byte is MSB (high order)
					int MSB = 255 & (((int) (outAudioData[i])) >> 8);
					outAudioBytes[2 * i] = (byte) MSB;
					// Second byte is LSB (low order)
					int LSB = 255 & ((int) (outAudioData[i]));
					outAudioBytes[2 * i + 1] = (byte) LSB;
				}
			} else {
				for (int i = 0; i < outAudioData.length; i++) {
					// First byte is MSB (high order)
					int MSB = 255 & (((int) (outAudioData[i])) >> 8);
					outAudioBytes[2 * i + 1] = (byte) MSB;
					// Second byte is LSB (low order)
					int LSB = 255 & ((int) (outAudioData[i]));
					outAudioBytes[2 * i] = (byte) LSB;
				}
			}
		} else {
		}
	}

	// merge 2 channel stereo data into one
	private void stereoToMono() {
		int j = 0;
		for (int i = 0; i < audioData.length; i += 2) {
			audioData[j] = (audioData[i] + audioData[i + 1]) / 2;
			j++;
		}
	}

	private void getAudioData() {

		if (format.getSampleSizeInBits() == 16) {
			nlengthInSamples = audioBytes.length / 2;
			audioData = new int[nlengthInSamples];
			if (format.isBigEndian()) {
				for (int i = 0; i < nlengthInSamples; i++) {
					// First byte is MSB (high order)
					int MSB = (int) audioBytes[2 * i];
					// Second byte is LSB (low order)
					int LSB = (int) audioBytes[2 * i + 1];
					audioData[i] = MSB << 8 | (255 & LSB);
				}
			} else {
				for (int i = 0; i < nlengthInSamples; i++) {
					// First byte is LSB (low order)
					int LSB = (int) audioBytes[2 * i];
					// Second byte is MSB (high order)
					int MSB = (int) audioBytes[2 * i + 1];
					audioData[i] = MSB << 8 | (255 & LSB);
				}
			}
		} else {
			if (format.getSampleSizeInBits() == 8) {
				nlengthInSamples = audioBytes.length;
				audioData = new int[nlengthInSamples];
				if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
					for (int i = 0; i < audioBytes.length; i++) {
						audioData[i] = audioBytes[i];
					}
				} else {
					for (int i = 0; i < audioBytes.length; i++) {
						audioData[i] = audioBytes[i] - 128;
					}
				}
			}
		}
	}

	public boolean play() {

		try {
			if (playState != STOPPED)
				playStop();

			if (audioBytes == null)
				return false;

			if (playFormat == null) {

				playFormat = format;
				if (playFormat == null)
					return false;

				/**
				 * Java Sound can't yet open the device for ALAW/ULAW playback, convert
				 * ALAW/ULAW to PCM
				 */
				if ((format.getEncoding() == AudioFormat.Encoding.ULAW)
						|| (format.getEncoding() == AudioFormat.Encoding.ALAW)) {

					playFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(),
							format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2,
							format.getFrameRate(), true);
					audioInputStream.reset();
					audioInputStream.mark(audioBytes.length);
					AudioInputStream stream = AudioSystem.getAudioInputStream(playFormat, audioInputStream);
					audioBytes = new byte[(int) stream.getFrameLength() * playFormat.getFrameSize()];
					stream.mark(audioBytes.length);
					stream.read(audioBytes);

				}

			}

			DataLine.Info info = new DataLine.Info(Clip.class, playFormat);

			clip = (Clip) AudioSystem.getLine(info);
			clip.addLineListener(new ClipListener());
			System.out.println("audio play 1");
			long clipStart = (long) (audioBytes.length * getStartTime() / (getDuration() * 1000.0));
			long clipEnd = (long) (audioBytes.length * getEndTime() / (getDuration() * 1000.0));
			if ((clipEnd - clipStart) > MAX_CLIP_LENGTH)
				clipEnd = clipStart + MAX_CLIP_LENGTH;
			byte[] clipBytes = new byte[(int) (clipEnd - clipStart)];
			System.out.println("audio play 2 " + clipBytes.length);

			System.arraycopy(audioBytes, (int) clipStart, clipBytes, 0, clipBytes.length);
			System.out.println("audio play 3 " + clipEnd + ", " + clipStart);

			clip.open(playFormat, clipBytes, 0, clipBytes.length);
			System.out.println("audio play 4");

			double playStartTime = (player.getSeekTime() / 100) * (playGetLength());
			setGain();
			setPan();
			clip.setMicrosecondPosition((long) playStartTime);
			clip.start();
			playState = PLAYING;
			return true;

		} catch (Exception ex) {
			ex.printStackTrace();
			playState = STOPPED;
			clip = null;
			toneMapFrame.reportStatus(EC_AUDIO_PLAY);
			return false;
		}
	}

	public boolean playOut() {

		try {
			if (playState != STOPPED)
				playStop();

			if (outAudioBytes == null)
				return false;

			if (playFormat == null) {

				playFormat = outFormat;
				if (playFormat == null)
					return false;

				/**
				 * Java Sound can't yet open the device for ALAW/ULAW playback, convert
				 * ALAW/ULAW to PCM
				 */
				if ((outFormat.getEncoding() == AudioFormat.Encoding.ULAW)
						|| (outFormat.getEncoding() == AudioFormat.Encoding.ALAW)) {

					playFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, outFormat.getSampleRate(),
							outFormat.getSampleSizeInBits() * 2, outFormat.getChannels(), outFormat.getFrameSize() * 2,
							outFormat.getFrameRate(), true);
					outAudioStream.reset();
					outAudioStream.mark(outAudioBytes.length);
					AudioInputStream stream = AudioSystem.getAudioInputStream(playFormat, outAudioStream);
					outAudioBytes = new byte[(int) stream.getFrameLength() * playFormat.getFrameSize()];
					stream.mark(outAudioBytes.length);
					stream.read(outAudioBytes);

				}

			}

			DataLine.Info info = new DataLine.Info(Clip.class, playFormat);

			clip = (Clip) AudioSystem.getLine(info);
			clip.addLineListener(new ClipListener());
			System.out.println("audio out play 1");
			long clipStart = (long) (outAudioBytes.length * getStartTime() / (getDuration() * 1000.0));
			long clipEnd = (long) (outAudioBytes.length * getEndTime() / (getDuration() * 1000.0));
			if ((clipEnd - clipStart) > MAX_CLIP_LENGTH)
				clipEnd = clipStart + MAX_CLIP_LENGTH;
			byte[] clipBytes = new byte[(int) (clipEnd - clipStart)];
			System.out.println("audio play 2 " + clipBytes.length);

			System.arraycopy(outAudioBytes, (int) clipStart, clipBytes, 0, clipBytes.length);
			System.out.println("audio play 3 " + clipEnd + ", " + clipStart);

			clip.open(playFormat, clipBytes, 0, clipBytes.length);
			System.out.println("audio play 4");

			double playStartTime = (player.getSeekTime() / 100) * (playGetLength());
			setGain();
			setPan();
			clip.setMicrosecondPosition((long) playStartTime);
			clip.start();
			playState = PLAYING;
			return true;

		} catch (Exception ex) {
			ex.printStackTrace();
			playState = STOPPED;
			clip = null;
			toneMapFrame.reportStatus(EC_AUDIO_PLAY);
			return false;
		}
	}

	public void setGain() {
		if (clip != null) {
			try {
				double value = (double) gainSetting / 100.0;
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float dB = (float) (Math.log(value == 0.0 ? 0.0001 : value) / Math.log(10.0) * 20.0);
				gainControl.setValue(dB);
			} catch (Exception ex) {
				return;
			}
		}
	}

	public void setPan() {
		if (clip != null) {
			try {
				FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
				panControl.setValue((float) panSetting / 100.0f);
			} catch (Exception ex) {
				return;
			}
		}
	}

	public void setReverbReturn() {
		if (clip != null) {
			try {
				FloatControl reverbControl = (FloatControl) clip.getControl(FloatControl.Type.REVERB_RETURN);
				reverbControl.setValue((float) reverbRSetting / 100.0f);
			} catch (Exception ex) {
				return;
			}
		}
	}

	public void setReverbSend() {
		if (clip != null) {
			try {
				FloatControl reverbControl = (FloatControl) clip.getControl(FloatControl.Type.REVERB_SEND);
				reverbControl.setValue((float) reverbSSetting / 100.0f);
			} catch (Exception ex) {
				return;
			}
		}
	}

	public double playGetLength() {
		if (clip != null) {
			return clip.getMicrosecondLength() * 1000000.0;
		}
		return 0.0;
	}

	public int playGetState() {

		return playState;

	}

	public double playGetTime() {

		if (clip != null) {
			return ((double) clip.getMicrosecondPosition()) / 1000.0;
		}
		return 0;
	}

	public void playLoop() {

		if (clip != null) {

			double playStartTime = (player.getSeekTime() / 100) * (playGetLength());
			clip.setMicrosecondPosition((long) playStartTime);
			if (playState != PLAYING) {
				clip.start();
				playState = PLAYING;
			}

		}
	}

	public void playPause() {

		if (clip != null) {
			if (playState == PLAYING) {

				clip.stop();
				playState = PAUSED;

			}
		}
	}

	public void playResume() {

		if (clip != null) {
			if (playState == PAUSED) {

				clip.start();
				playState = PLAYING;

			}
		}
	}

	public void playSetPlayer(Player player) {

		this.player = player;
	}

	public void playSetSeek(double seekTime) {
		if (clip != null && playState == PLAYING) {
			clip.setMicrosecondPosition((long) (seekTime * 1000.0));
		}
	}

	public void playStop() {

		if (clip != null) {
			if (playState == PLAYING || playState == PAUSED) {

				clip.stop();
				clip.close();
				playState = STOPPED;
			}
		}

	}

	public void setTime(TimeSet timeSet) {

		audioPanel.timeControl.setTimeMax((int) (timeSet.getEndTime() - timeSet.getStartTime()));

	}

	public void setPitch(PitchSet pitchSet) {
		audioPanel.pitchControl.setPitchRange((int) (pitchSet.getLowNote()), (int) (pitchSet.getHighNote()));

	}

	/**
	 * Transform audioData from raw sampled form in audioData into Frequency/Time
	 * domain defined by data contained in audioFTPower object.
	 */
	public boolean transform(ProgressListener progressListener) {

		toneMap = toneMapFrame.getToneMap();
		timeSet = toneMap.getTimeSet();
		pitchSet = toneMap.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();

		pitchFreqSet = pitchSet.getFreqSet();
		audioFTPower = new double[timeRange * (pitchRange + 1)];

		int startSample = timeSet.getStartSample();
		int endSample = timeSet.getEndSample();
		int sampleLength = (int) Math.floor((endSample - startSample) / ((double) resolution));
		double[] audioSamples = new double[sampleLength];

		double min, max;
		min = 0;
		max = 0;
		for (int i = 0; i < sampleLength; i++) {
			audioSamples[i] = (double) audioData[startSample + i * resolution];
			if (min > audioSamples[i])
				min = audioSamples[i];
			if (max < audioSamples[i])
				max = audioSamples[i];
		}
		System.out.println("Audio Samples min/max: " + min + ", " + max);

		int sampleIndexSize = (int) Math.floor((double) timeSet.getSampleIndexSize() / (double) resolution);

		double dt = (double) resolution / sampleRate;
		if (transformMode == TRANSFORM_MODE_JAVA) {
			wavelet.convert(audioFTPower, audioSamples, pitchFreqSet, dt, sampleIndexSize, sampleLength, pitchRange,
					progressListener, (double) pFactor, (double) tFactor, (double) pOffset, (double) t1Setting,
					(double) t2Setting, (double) t3Setting, (double) t3Setting, t1Switch, t2Switch, t3Switch, t4Switch);
		} else {

			WaveletJNI waveletJNI = new WaveletJNI();

			waveletJNI.waveletConvert(audioFTPower, audioSamples, pitchFreqSet, dt, (double) pFactor, (double) tFactor,
					sampleIndexSize, sampleLength, pitchRange, progressListener);
		}

		return true;
	}

	/**
	 * Create audio output stream from ToneMap data
	 */
	public boolean writeStream(NoteList noteList) {

		toneMap = toneMapFrame.getToneMap();
		timeSet = toneMap.getTimeSet();
		pitchSet = toneMap.getPitchSet();
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();
		toneMapMatrix = toneMap.getMatrix();

		pitchFreqSet = pitchSet.getFreqSet();

		int startSample = timeSet.getStartSample();
		int endSample = timeSet.getEndSample();
		int sampleLength = (int) Math.floor(endSample - startSample);
		double[] audioOutSamples = new double[sampleLength];
		for (int i = 0; i < sampleLength; i++) {
			audioOutSamples[i] = 0;
		}
		System.out.println("Write sream samples: " + sampleLength);

		long frameLength = 0;
		int x, y, xa1, xa2, ya1, ya2;
		int note;
		double time, frequency;
		double maxSumPower = 0;
		double minSumPower = 0;
		double maxSumAmp = 0;
		double minSumAmp = 0;
		double sumAmp = 0;
		double sumPower = 0;
		int numPitches = 0;
		boolean condition = false;
		if (toneMapMatrix != null) {

			mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();

			do {
				sumAmp = 0;
				sumPower = 0;
				numPitches = 0;

				mapIterator.firstPitch();
				x = mapIterator.getTimeIndex();
				y = mapIterator.getPitchIndex();
				time = timeSet.getTime(x);
				frequency = pitchSet.getFreq(y);
				note = pitchSet.getNote(y);
				System.out.println("pitch/time: " + y + ", " + x + ", " + note + ", " + frequency + ", " + time);
				do {
					toneMapElement = mapIterator.getElement();
					if (toneMapElement != null) {
						amplitude = toneMapElement.postAmplitude;
						x = mapIterator.getTimeIndex();
						y = mapIterator.getPitchIndex();
						time = timeSet.getTime(x);
						frequency = pitchSet.getFreq(y);
						note = pitchSet.getNote(y);
						noteListElement = toneMapElement.noteListElement;
						if (osc1Switch) {
							condition = (toneMapElement.preAmplitude == -1 || noteListElement == null
									|| noteListElement.underTone);
						} else {
							condition = (toneMapElement.preAmplitude == -1);
						}
						if (!condition) {
							sumPower = sumPower + toneMapElement.preFTPower;
							sumAmp = sumAmp + toneMapElement.postAmplitude;
						}
						numPitches++;

					}
				} while (mapIterator.nextPitch());
				if (maxSumPower < sumPower)
					maxSumPower = sumPower;
				if (minSumPower > sumPower)
					minSumPower = sumPower;
				if (maxSumAmp < sumAmp)
					maxSumAmp = sumAmp;
				if (minSumAmp > sumAmp)
					minSumAmp = sumAmp;

			} while (mapIterator.nextTime());
		}
		System.out.println("min/max sums: " + maxSumPower + ", " + minSumPower + ", " + maxSumAmp + ", " + minSumAmp);
		Oscillator[] oscillators = new Oscillator[numPitches];
		if (toneMapMatrix != null) {

			mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();
			mapIterator.firstPitch();
			sumAmp = 0;
			sumPower = 0;
			int i = 0;
			do {
				x = mapIterator.getTimeIndex();
				y = mapIterator.getPitchIndex();
				time = timeSet.getTime(x);
				frequency = pitchSet.getFreq(y);
				note = pitchSet.getNote(y);
				oscillators[i] = new Oscillator(oscType, (int) frequency, (int) sampleRate, 1);
				i++;
			} while (mapIterator.nextPitch());
		}
		System.out.println("min/max sums: " + maxSumPower + ", " + minSumPower + ", " + maxSumAmp + ", " + minSumAmp);
		int i, iStart, iEnd;
		iStart = 0;
		iEnd = 0;
		i = 0;
		double maxPower = 0;
		double[] lastAmps = new double[numPitches];
		for (int j = 0; j < numPitches; j++) {
			lastAmps[j] = 0;
		}
		if (toneMapMatrix != null) {

			mapIterator = toneMapMatrix.newIterator();
			mapIterator.firstTime();
			double lastSample = 0;
			double ampFactor = 0;
			double ampAdjust = 0;
			do {
				mapIterator.firstPitch();
				x = mapIterator.getTimeIndex();
				y = mapIterator.getPitchIndex();
				time = timeSet.getTime(x);
				frequency = pitchSet.getFreq(y);
				note = pitchSet.getNote(y);
				System.out.println("pitch/time: " + y + ", " + x + ", " + note + ", " + frequency + ", " + time);
				iStart = iEnd;
				if (iStart > (int) (time * sampleRate / 1000.0))
					iStart = (int) (time * sampleRate / 1000.0);
				iEnd = iStart + (int) (timeSet.getSampleTimeSize() * sampleRate / 1000.0);
				double power;
				System.out.println("istart/end: " + iStart + ", " + iEnd);

				do {
					lastSample = 0;
					ampAdjust = 0;
					ampFactor = 0;
					toneMapElement = mapIterator.getElement();
					if (toneMapElement != null) {
						amplitude = toneMapElement.postAmplitude;
						power = toneMapElement.preFTPower;
						x = mapIterator.getTimeIndex();
						y = mapIterator.getPitchIndex();
						time = timeSet.getTime(x);
						frequency = pitchSet.getFreq(y);
						note = pitchSet.getNote(y);
						noteListElement = toneMapElement.noteListElement;
						if (osc1Switch) {
							condition = (toneMapElement.preAmplitude == -1 || noteListElement == null
									|| noteListElement.underTone);
						} else {
							condition = (toneMapElement.preAmplitude == -1);
						}
						if (condition) {
							power = 0;
						}
						if (power != 0 || lastAmps[y] != 0) {
							// System.out.println("testing : "+power+", "+lastAmps[y]+", "+frequency+",
							// "+time/1000.0+", "+sampleRate+", "+timeSet.getSampleTimeSize()/1000.0);
							for (i = iStart; i < iEnd; i++) {
								ampFactor = (double) (i - iStart) / (double) (iEnd - iStart);
								ampAdjust = lastAmps[y] + ampFactor * (power - lastAmps[y]);
								oscillators[y].setAmplitudeAdj(ampAdjust / (double) maxSumPower);
								lastSample = oscillators[y].getSample();
								audioOutSamples[i] += lastSample;
							}
						}
					}
					if (ampAdjust == 0)
						oscillators[y].reset();
					lastAmps[y] = ampAdjust;
				} while (mapIterator.nextPitch());
				System.out.println("maxPower " + maxPower);

			} while (mapIterator.nextTime());
		}
		System.out.println("getout audio bytes");
		getOutAudioBytes(audioOutSamples);
		outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(),
				format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2,
				format.getFrameRate(), true);

		ByteArrayInputStream bais = new ByteArrayInputStream(outAudioBytes);
		outAudioStream = new AudioInputStream(bais, outFormat, outAudioBytes.length / outFormat.getFrameSize());
		System.out.println("made new out audio stream");

		return true;
	}
}
