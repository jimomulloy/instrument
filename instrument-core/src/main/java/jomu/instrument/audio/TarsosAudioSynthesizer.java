package jomu.instrument.audio;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import jomu.instrument.Instrument;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.tonemap.NoteListElement;
import jomu.instrument.workspace.tonemap.NoteStatus;
import jomu.instrument.workspace.tonemap.NoteStatusElement;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneMapElement;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class TarsosAudioSynthesizer implements ToneMapConstants, AudioSynthesizer {

	private static final Logger LOG = Logger.getLogger(TarsosAudioSynthesizer.class.getName());

	private static final int MAP_AVERAGE_COUNT = 10;
	private static final int MAP_MAX_COUNT = 10;

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

	private LinkedList<ToneTimeFrame> frameHistory = new LinkedList<>();

	private Hearing hearing;

	private int windowSize;

	/**
	 * AudioModel constructor. Test Java Sound Audio System available Instantiate
	 * AudioPanel
	 * 
	 * @param parameterManager
	 */
	public TarsosAudioSynthesizer(ParameterManager parameterManager) {
		this.parameterManager = parameterManager;
		this.hearing = Instrument.getInstance()
				.getCoordinator()
				.getHearing();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
	}

	public int getWindowSize() {
		return windowSize;
	}

	@Override
	public void clear(String streamId) {
		if (audioStreams.containsKey(streamId)) {
			AudioStream as = audioStreams.get(streamId);
			as.close();
		}
	}

	@Override
	public void close(String streamId) {
		if (!audioStreams.containsKey(streamId)) {
			return;
		}
		AudioStream audioStream = audioStreams.get(streamId);
		AudioQueueMessage audioQueueMessage = new AudioQueueMessage(null, 0);
		audioStream.bq.add(audioQueueMessage);
		audioStreams.remove(streamId);
	}

	@Override
	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		PitchSet pitchSet = toneTimeFrame.getPitchSet();
		if (!audioStreams.containsKey(streamId)) {
			audioStreams.put(streamId, new AudioStream(streamId, pitchSet));
		}
		AudioStream audioStream = audioStreams.get(streamId);
		AudioQueueMessage audioQueueMessage = new AudioQueueMessage(toneTimeFrame, sequence);

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

		AudioFormat outFormat = new AudioFormat(timeSet.getSampleRate(), 16, 1, true, false);

		byte[] outAudioBytes = getOutAudioBytes(audioOutSamples, outFormat);

		ByteArrayInputStream bais = new ByteArrayInputStream(outAudioBytes);
		AudioInputStream outAudioStream = new AudioInputStream(bais, outFormat,
				outAudioBytes.length / outFormat.getFrameSize());

		audioOutput.write(outAudioBytes, 0, outAudioBytes.length);

		// AudioSystem.write(outAudioStream, AudioFileFormat.Type.WAVE, line);
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
		int counter = 0;
		private boolean running = true;
		double lastTime = 0;

		public AudioQueueConsumer(BlockingQueue<AudioQueueMessage> bq, AudioStream audioStream) {
			this.bq = bq;
			this.audioStream = audioStream;
		}

		@Override
		public void run() {
			try {
				double lowVoiceThreshold = parameterManager
						.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
				double highVoiceThreshold = parameterManager
						.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);
				int playDelay = parameterManager.getIntParameter(InstrumentParameterNames.ACTUATION_VOICE_DELAY);
				boolean doneDelay = false;

				while (running) {
					if (audioStream.isClosed()) {
						running = false;
						break;
					}
					AudioQueueMessage aqm = bq.take();
					counter++;

					ToneTimeFrame toneTimeFrame = aqm.toneTimeFrame;

					if (toneTimeFrame == null) {
						running = false;
						break;
					}
					if (audioStream.isClosed()) {
						running = false;
						break;
					}

					if (playDelay > 0 && !doneDelay) {
						LOG.finer(">>MidiStream IN PLAY DELAY: " + System.currentTimeMillis());
						TimeUnit.MILLISECONDS.sleep((long) playDelay);
						LOG.finer(">>MidiStream AFTER PLAY DELAY: " + System.currentTimeMillis());
						doneDelay = true;
					}

					double time = toneTimeFrame.getStartTime();

					if (frameHistory.size() >= MAP_MAX_COUNT) {
						frameHistory.removeLast();
					}
					frameHistory.addFirst(toneTimeFrame);

					TimeSet timeSet = toneTimeFrame.getTimeSet();
					PitchSet pitchSet = toneTimeFrame.getPitchSet();
					NoteStatus noteStatus = toneTimeFrame.getNoteStatus();
					NoteStatusElement noteStatusElement = null;
					ToneMapElement[] ttfElements = toneTimeFrame.getElements();
					if (lastAmps == null) {
						lastAmps = new double[ttfElements.length];
					}

					double maxAmp = -1;
					for (ToneMapElement toneMapElement : ttfElements) {
						if (time > 1.0 && time < 2.0 && toneMapElement.getPitchIndex() == 24) {
							// toneMapElement.amplitude = 1.0;
							// LOG.severe(
							// ">>SET AMP: " + time);
						} else {
							// toneMapElement.amplitude = 0.0;
						}
						double amp = toneMapElement.amplitude;
						if (maxAmp < amp) {
							maxAmp = amp;
						}
						// audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(0.0);
					}

					double totalGain = 0;
					for (ToneMapElement toneMapElement : ttfElements) {
						int note = pitchSet.getNote(toneMapElement.getPitchIndex());
						noteStatusElement = noteStatus.getNoteStatusElement(note);
						double amplitude = getFilteredAmplitude(toneMapElement.getIndex());

						// double amplitude = toneMapElement.amplitude;
						double gain = 0.0F;
						if (amplitude > highVoiceThreshold) {
							gain = 1.0;
						} else if (amplitude <= 0.3) { // lowVoiceThreshold) {
							gain = 0.0;
						} else {
							gain = Math
									.log1p((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold))
									/ Math.log1p(1.0000001);
							gain = Math.max(0, gain);
							gain = ((amplitude - lowVoiceThreshold) / (highVoiceThreshold - lowVoiceThreshold));
						}
						gain = amplitude;
						// if (noteStatusElement.state != OFF) {
						// double newGain = lastAmps[toneMapElement.getIndex()]
						// + (gain - lastAmps[toneMapElement.getIndex()]) / 2.0;
						// if (newGain < 0) {
						// newGain = 0;
						// }
						// totalGain += newGain;
						// lastAmps[toneMapElement.getIndex()] = newGain;
						// audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(newGain);
						totalGain += gain;
						lastAmps[toneMapElement.getIndex()] = gain;
						audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(gain);
						// } else {
						// audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(0.0);
						// lastAmps[toneMapElement.getIndex()] = 0F;
						// }
					}
					// LOG.severe(">>TOTAL gen: " + time + ", " + totalGain);
					// if (totalGain > 1.0) {
					for (ToneMapElement toneMapElement : ttfElements) {
						double gain = audioStream.getSineGenerators()[toneMapElement.getIndex()].getGain();
						if (gain > 0.1) {
							audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(gain / totalGain); // GAIN
						} else {
							audioStream.getSineGenerators()[toneMapElement.getIndex()].setGain(0.0); // GAIN
						}
					}
					// }
					AudioEvent audioEvent = this.audioStream.getGenerator()
							.getAudioEvent();
					int count = 0;
					while (audioEvent.getEndTimeStamp() <= time) {
						// if (audioEvent.getEndTimeStamp() == time) {
						this.audioStream.getGenerator()
								.process();
						count++;
						audioEvent = this.audioStream.getGenerator()
								.getAudioEvent();
					}

				}
			} catch (InterruptedException e) {
				Thread.currentThread()
						.interrupt();
			}
			this.audioStream.close();

		}

		private double getFilteredAmplitude(int index) {
			int count = frameHistory.size();
			double total = 0;
			for (ToneTimeFrame frame : frameHistory) {
				total += frame.getElement(index).amplitude;
			}
			return total / (double) count;
		}

		public void stop() {
			running = false;
		}
	}

	private class AudioQueueConsumer2 implements Runnable {

		private AudioStream audioStream;
		private BlockingQueue<AudioQueueMessage> bq;
		int counter = 0;
		private boolean running = true;
		double lastTime = 0;

		public AudioQueueConsumer2(BlockingQueue<AudioQueueMessage> bq, AudioStream audioStream) {
			this.bq = bq;
			this.audioStream = audioStream;
		}

		@Override
		public void run() {
			try {
				while (running) {
					if (audioStream.isClosed()) {
						running = false;
						break;
					}
					AudioQueueMessage aqm = bq.take();
					counter++;

					ToneTimeFrame toneTimeFrame = aqm.toneTimeFrame;

					if (toneTimeFrame == null) {
						running = false;
						break;
					}
					if (audioStream.isClosed()) {
						running = false;
						break;
					}

					double time = toneTimeFrame.getStartTime();
					if (lastTime > 0) {
						// TimeUnit.MILLISECONDS.sleep((long) (time - lastTime) * 1000);
					}
					lastTime = time;
					// System.out.println(">>TIME: " + time);

					if (frameHistory.size() >= MAP_MAX_COUNT) {
						frameHistory.removeLast();
					}
					frameHistory.addFirst(toneTimeFrame);

					double lowVoiceThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_LOW_THRESHOLD);
					double highVoiceThreshold = parameterManager
							.getDoubleParameter(InstrumentParameterNames.ACTUATION_VOICE_HIGH_THRESHOLD);

					TimeSet timeSet = toneTimeFrame.getTimeSet();
					PitchSet pitchSet = toneTimeFrame.getPitchSet();

					int sampleLength = timeSet.getSampleWindow();
					float[] audioOutSamples = new float[sampleLength];
					for (int i = 0; i < sampleLength; i++) {
						audioOutSamples[i] = 0;
					}

					double frequency;
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
							condition = (toneMapElement.amplitude == -1 || noteListElement == null
									|| noteListElement.underTone);
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
					for (ToneMapElement toneMapElement : ttfElements) {
						lastSample = 0;
						ampAdjust = 0;
						ampFactor = 0;
						power = toneMapElement.amplitude;
						frequency = pitchSet.getFreq(toneMapElement.getIndex());
						noteListElement = toneMapElement.noteListElement;
						if (osc1Switch) {
							condition = (toneMapElement.amplitude == -1 || noteListElement == null
									|| noteListElement.underTone);
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

					for (float sample : audioOutSamples) {
						if (sample > 0) {
							System.out.println(">>SAMPLE: " + sample);
							break;
						}
					}
					AudioFormat outFormat = new AudioFormat(timeSet.getSampleRate(), 16, 1, true, false);

					byte[] outAudioBytes = getOutAudioBytes(audioOutSamples, outFormat);

					ByteArrayInputStream bais = new ByteArrayInputStream(outAudioBytes);
					AudioInputStream outAudioStream = new AudioInputStream(bais, outFormat,
							outAudioBytes.length / outFormat.getFrameSize());

					audioStream.getLine().write(outAudioBytes, 0, outAudioBytes.length);
					// audioOutput.write(outAudioBytes, 0, outAudioBytes.length);

					// AudioSystem.write(outAudioStream, AudioFileFormat.Type.WAVE, line);
					// System.out.println(
					// ">>WRIET: " + time + ", " + outAudioBytes.length + ", " +
					// audioStream.getLine().toString());
					// AudioSystem.write(outAudioStream, AudioFileFormat.Type.WAVE, line);
				}
			} catch (InterruptedException e) {
				Thread.currentThread()
						.interrupt();
			}
			this.audioStream.close();

		}

		private double getFilteredAmplitude(int index) {
			int count = frameHistory.size();
			double total = 0;
			for (ToneTimeFrame frame : frameHistory) {
				total += frame.getElement(index).amplitude;
			}
			return total / (double) count;
		}

		public void stop() {
			running = false;
		}
	}

	private class AudioQueueMessage {
		public ToneTimeFrame toneTimeFrame = null;
		public int sequence;

		public AudioQueueMessage(ToneTimeFrame toneTimeFrame, int sequence) {
			this.toneTimeFrame = toneTimeFrame;
			this.sequence = sequence;
		}

	}

	private class AudioStream {

		private float baseFrequency;

		private BlockingQueue<AudioQueueMessage> bq;

		private int frequencies;

		private SineGenerator[] sineGenerators;

		private String streamId;

		private AudioGenerator generator;

		private boolean closed;

		private AudioQueueConsumer consumer;

		// private AudioQueueConsumer2 consumer2;

		private SourceDataLine line;

		private AudioFormat format;

		public AudioStream(String streamId, PitchSet pitchSet) {
			this.streamId = streamId;
			this.baseFrequency = (float) pitchSet.getFreq(0);
			this.frequencies = pitchSet.getRange();
			bq = new LinkedBlockingQueue<>();
			consumer = new AudioQueueConsumer(bq, this);
			// TODO LOOM Thread.startVirtualThread(consumer);
			Thread aqct = new Thread(consumer,
					"Thread-TarsosAudioSynthesizer-MidiStream-" + streamId + "-" + System.currentTimeMillis());
			aqct.setPriority(Thread.MAX_PRIORITY);
			aqct.start();
			float frequency = baseFrequency;
			format = new AudioFormat(44100, 16, 1, true, false);
			generator = new AudioGenerator(TarsosAudioSynthesizer.this.getWindowSize(), 0);
			sineGenerators = new SineGenerator[frequencies];
			for (int i = 0; i < frequencies; i++) {
				frequency = (float) pitchSet.getFreq(i);
				sineGenerators[i] = new SineGenerator(0.0, frequency);
				generator.addAudioProcessor(sineGenerators[i]);
			}
			try {
				generator.addAudioProcessor(new AudioPlayer(format));
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// final DataLine.Info info = new DataLine.Info(SourceDataLine.class,
			// format, 1024);
			// LOG.info("Opening data line" + info.toString());
			// try {
			// line = (SourceDataLine) AudioSystem.getLine(info);
			// } catch (LineUnavailableException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// try {
			// line.open(format, 2048);
			// } catch (LineUnavailableException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// line.start();
		}

		public SourceDataLine getLine() {
			return line;
		}

		public AudioGenerator getGenerator() {
			return generator;
		}

		public boolean isClosed() {
			return closed;
		}

		public void close() {
			this.generator.stop();
			closed = true;
			consumer.stop();
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
