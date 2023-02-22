package jomu.instrument.audio;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

/**
 * This class defines the Audio Sub System Data Model processing functions for
 * the ToneMap including file reading, Audio data transformation, Playback
 * implementation and control settings management through the AudioPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class TarsosAudioSynthesizer implements ToneMapConstants, AudioSynthesizer {

	public int gainSetting = INIT_VOLUME_SETTING;

	public boolean logSwitch = false;

	public int osc1Setting = 0;
	public boolean osc1Switch = false;
	public int osc2Setting = 0;
	public boolean osc2Switch = false;
	public int oscType = Oscillator.SINEWAVE;
	public int panSetting = INIT_PAN_SETTING;
	public int pFactor = 100;
	public int pitchHigh = INIT_PITCH_HIGH;
	public int pitchLow = INIT_PITCH_LOW;
	public int pOffset = 0;
	public int powerHigh = 100;
	public int powerLow = 0;
	public int resolution = 1;
	public int reverbRSetting = 0;
	public int reverbSSetting = 0;
	public double sampleTimeSize = INIT_SAMPLE_SIZE;
	public int t1Setting = 50;
	public boolean t1Switch = false;
	public int t2Setting = 50;
	public boolean t2Switch = false;
	public int t3Setting = 50;
	public boolean t3Switch = false;
	public int t4Setting = 50;

	public boolean t4Switch = false;
	public int tFactor = 50;
	public double timeEnd = INIT_TIME_END;
	public double timeStart = INIT_TIME_START;
	public int transformMode = TRANSFORM_MODE_JAVA;
	private Map<String, AudioStream> audioStreams = new ConcurrentHashMap<>();
	private double[] lastAmps;
	private ParameterManager parameterManager;

	/**
	 * AudioModel constructor. Test Java Sound Audio System available Instantiate
	 * AudioPanel
	 * 
	 * @param parameterManager
	 */
	public TarsosAudioSynthesizer(ParameterManager parameterManager) {
		this.parameterManager = parameterManager;
	}

	/**
	 * Clear current AudioModel objects after Reset
	 */

	@Override
	public void clear() {
	}

	@Override
	public void close(String streamId) {
		if (!audioStreams.containsKey(streamId)) {
			return;
		}
		System.out.println(">>!!! Audio audioStreams.close: " + streamId);
		AudioStream audioStream = audioStreams.get(streamId);
		AudioQueueMessage audioQueueMessage = new AudioQueueMessage();
		audioStream.bq.add(audioQueueMessage);
		System.out.println(">>!!! Audio audioStreams.remove: " + streamId);

		audioStreams.remove(streamId);
	}

	@Override
	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		System.out.println(">>!!! Audio audioStreams play: " + streamId);
		if (!audioStreams.containsKey(streamId)) {
			audioStreams.put(streamId, new AudioStream(streamId, pitchSet));
			System.out.println(">>!!! Audio audioStreams create: " + streamId);
		}
		AudioStream audioStream = audioStreams.get(streamId);
		AudioQueueMessage audioQueueMessage = new AudioQueueMessage(toneTimeFrame);

		audioStream.bq.add(audioQueueMessage);

		return;
	}

	/**
	 * Create audio output stream from ToneMap data
	 *
	 * @param audioOutSamples
	 * @param audioOutput
	 * @return
	 */
	@Override
	public AudioInputStream writeStream(ToneMap toneMap, float[] audioOutSamples, SourceDataLine audioOutput) {

		ToneTimeFrame toneTimeFrame = toneMap.getTimeFrame();

		TimeSet timeSet = toneTimeFrame.getTimeSet();
		PitchSet pitchSet = toneTimeFrame.getPitchSet();

		int sampleLength = timeSet.getSampleWindow();
		for (int i = 0; i < sampleLength; i++) {
			audioOutSamples[i] = 0;
		}
		System.out.println("Write stream samples: " + sampleLength);

		double time, frequency;
		double maxSumAmp = 0;
		double minSumAmp = 0;
		double sumAmp = 0;
		int numPitches = 0;
		boolean condition = false;

		ToneMapElement[] ttfElements = toneTimeFrame.getElements();
		sumAmp = 0;
		numPitches = 0;

		time = timeSet.getStartTime();
		NoteListElement noteListElement;
		for (ToneMapElement toneMapElement : ttfElements) {
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			noteListElement = toneMapElement.noteListElement;
			if (osc1Switch) {
				condition = (toneMapElement.amplitude == -1 || noteListElement == null || noteListElement.underTone);
			} else {
				condition = (toneMapElement.amplitude == -1);
			}
			if (!condition) {
				sumAmp += toneMapElement.amplitude;
			}
			numPitches++;

		}
		if (maxSumAmp < sumAmp)
			maxSumAmp = sumAmp;
		if (minSumAmp > sumAmp)
			minSumAmp = sumAmp;

		if (lastAmps == null) {
			lastAmps = new double[numPitches];
		}

		System.out.println("min/max sums: " + maxSumAmp + ", " + minSumAmp);
		Oscillator[] oscillators = new Oscillator[numPitches];

		int i, iStart, iEnd;
		sumAmp = 0;
		i = 0;
		float sampleRate = timeSet.getSampleRate();
		for (ToneMapElement toneMapElement : ttfElements) {
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			oscillators[i] = new Oscillator(oscType, (int) frequency, (int) sampleRate, 1);
			i++;
		}
		System.out.println("created oscs: " + i);
		iStart = 0;
		iEnd = 0;
		i = 0;

		for (int j = 0; j < numPitches; j++) {
			lastAmps[j] = 0;
		}

		double lastSample = 0;
		double ampFactor = 0;
		double ampAdjust = 0;
		iStart = iEnd;
		if (iStart > (int) (time * sampleRate))
			iStart = (int) (time * sampleRate);
		iEnd = iStart + (int) (timeSet.getSampleTimeSize() * sampleRate);
		double power;
		System.out.println("istart/end: " + time + ", " + iStart + ", " + iEnd);
		for (ToneMapElement toneMapElement : ttfElements) {
			lastSample = 0;
			ampAdjust = 0;
			ampFactor = 0;
			power = toneMapElement.amplitude;
			frequency = pitchSet.getFreq(toneMapElement.getIndex());
			noteListElement = toneMapElement.noteListElement;
			if (osc1Switch) {
				condition = (toneMapElement.amplitude == -1 || noteListElement == null || noteListElement.underTone);
			} else {
				// condition = (toneMapElement.amplitude == -1);
				condition = (toneMapElement.amplitude < 0.2);
			}
			if (condition) {
				power = 0;
			}
			if ((toneMapElement.getIndex() > 20 && toneMapElement.getIndex() < 40)
					&& (power != 0 || lastAmps[toneMapElement.getIndex()] != 0)) {
				for (i = iStart; i < iEnd; i++) {
					ampFactor = (double) (i - iStart) / (double) (iEnd - iStart);
					ampAdjust = lastAmps[toneMapElement.getIndex()]
							+ ampFactor * (power - lastAmps[toneMapElement.getIndex()]);
					oscillators[toneMapElement.getIndex()].setAmplitudeAdj(ampAdjust / (1000 * maxSumAmp));
					oscillators[toneMapElement.getIndex()].setAmplitudeAdj(1.0);
					lastSample = oscillators[toneMapElement.getIndex()].getSample();
					audioOutSamples[i] += lastSample / 100.0;
				}
			}
			if (ampAdjust == 0)
				oscillators[toneMapElement.getIndex()].reset();
			lastAmps[toneMapElement.getIndex()] = ampAdjust;
		}

		System.out.println("getout audio bytes");

		AudioFormat outFormat = new AudioFormat(timeSet.getSampleRate(), 16, 1, true, false);

		byte[] outAudioBytes = getOutAudioBytes(audioOutSamples, outFormat);

		ByteArrayInputStream bais = new ByteArrayInputStream(outAudioBytes);
		AudioInputStream outAudioStream = new AudioInputStream(bais, outFormat,
				outAudioBytes.length / outFormat.getFrameSize());
		System.out.println("made new out audio stream");

		audioOutput.write(outAudioBytes, 0, outAudioBytes.length);
		return outAudioStream;
	}

	// convert audioBytes sampled audio data into standard format in audioData
	// array.
	private byte[] getOutAudioBytes(float[] outAudioData, AudioFormat format) {

		byte[] outAudioBytes = new byte[outAudioData.length * 2];
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
		return outAudioBytes;
	}

	private class AudioQueueConsumer implements Runnable {

		private AudioStream audioStream;
		private BlockingQueue<AudioQueueMessage> bq;
		double sampleTime = -1;
		int counter = 0;

		public AudioQueueConsumer(BlockingQueue<AudioQueueMessage> bq, AudioStream audioStream) {
			this.bq = bq;
			this.audioStream = audioStream;
		}

		@Override
		public void run() {
			try {
				boolean running = true;
				while (running) {
					AudioQueueMessage aqm = bq.take();
					counter++;

					ToneTimeFrame toneTimeFrame = aqm.toneTimeFrame;
					System.out.println(">>!!! Audio QueueConsumer take: " + this.audioStream.getStreamId() + ", "
							+ counter + ", " + toneTimeFrame);

					if (toneTimeFrame == null) {
						this.audioStream.close();
						running = false;
						break;
					}

					if (sampleTime != -1) {
						TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
					}

					TimeSet timeSet = toneTimeFrame.getTimeSet();
					PitchSet pitchSet = toneTimeFrame.getPitchSet();
					NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
					NoteStatusElement noteStatusElement = null;
					ToneMapElement[] ttfElements = toneTimeFrame.getElements();
					double time = toneTimeFrame.getStartTime();
					if (lastAmps == null) {
						lastAmps = new double[ttfElements.length];
					}

					double maxAmp = -1;
					for (ToneMapElement toneMapElement : ttfElements) {
						double amp = toneMapElement.amplitude;
						if (maxAmp < amp) {
							maxAmp = amp;
						}
					}

					for (ToneMapElement toneMapElement : ttfElements) {
						int note = pitchSet.getNote(toneMapElement.getPitchIndex());
						noteStatusElement = noteStatus.getNoteStatusElement(note);
						double amp = toneMapElement.amplitude;

						if (noteStatusElement.state != OFF) {
							double power = amp / maxAmp;
							System.out.println(">>SET GAIN: " + toneMapElement.getIndex() + ", "
									+ +audioStream.getSineGenerators()[toneMapElement.getIndex()].getFrequency());
							audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(power);
							lastAmps[toneMapElement.getIndex()] = power;
						} else {
							audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(0.0);
							lastAmps[toneMapElement.getIndex()] = 0F;
						}
					}
					AudioEvent audioEvent = this.audioStream.getGenerator().getAudioEvent();
					while (audioEvent.getEndTimeStamp() < time) {
						System.out.println(">>Process audio event: " + time + ", " + audioEvent.getTimeStamp());
						this.audioStream.getGenerator().process();
						audioEvent = this.audioStream.getGenerator().getAudioEvent();
					}

				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private class AudioQueueMessage implements Delayed {
		public ToneTimeFrame toneTimeFrame = null;
		private long startTime;

		public AudioQueueMessage(ToneTimeFrame toneTimeFrame, long delayInMilliseconds) {
			this.toneTimeFrame = toneTimeFrame;
			this.startTime = System.currentTimeMillis() + delayInMilliseconds;
		}

		public AudioQueueMessage(ToneTimeFrame toneTimeFrame) {
			this(toneTimeFrame, parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		public AudioQueueMessage() {
			this(null, parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY));
		}

		@Override
		public int compareTo(Delayed o) {
			return (int) (this.startTime - ((AudioQueueMessage) o).startTime);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long diff = startTime - System.currentTimeMillis();
			return unit.convert(diff, TimeUnit.MILLISECONDS);
		}
	}

	private class AudioStream {

		private float baseFrequency;

		private DelayQueue<AudioQueueMessage> bq;

		private int frequencies;

		private SineGenerator[] sineGenerators;

		private String streamId;

		private AudioGenerator generator;

		public AudioStream(String streamId, PitchSet pitchSet) {
			this.streamId = streamId;
			this.baseFrequency = (float) pitchSet.getFreq(0);
			this.frequencies = pitchSet.getRange();
			bq = new DelayQueue<>(); // LinkedBlockingQueue<>();
			Thread.startVirtualThread(new AudioQueueConsumer(bq, this));

			float frequency = baseFrequency;

			generator = new AudioGenerator(1024, 0);
			sineGenerators = new SineGenerator[frequencies];
			for (int i = 0; i < frequencies; i++) {
				frequency = (float) pitchSet.getFreq(i);
				sineGenerators[i] = new SineGenerator(0.0, frequency);
				generator.addAudioProcessor(sineGenerators[i]);
			}
			System.out.println(">>!!! Audio added freqs: " + frequencies);
			try {
				generator.addAudioProcessor(new AudioPlayer(new AudioFormat(44100, 16, 1, true, false)));
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public AudioGenerator getGenerator() {
			return generator;
		}

		public void close() {
			this.generator.stop();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			AudioStream other = (AudioStream) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(streamId, other.streamId);
		}

		public float getBaseFrequency() {
			return baseFrequency;
		}

		public BlockingQueue<AudioQueueMessage> getBq() {
			return bq;
		}

		public int getFrequencies() {
			return frequencies;
		}

		public SineGenerator[] getSineGenerators() {
			return sineGenerators;
		}

		public String getStreamId() {
			return streamId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(streamId);
			return result;
		}

		private AudioSynthesizer getEnclosingInstance() {
			return TarsosAudioSynthesizer.this;
		}

	}

}
