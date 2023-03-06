package jomu.instrument.audio;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ResynthInfo;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneMapConstants;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

/**
 * This class defines the Audio Sub System Data Model processing functions for
 * the ToneMap including file reading, Audio data transformation, Playback
 * implementation and control settings management through the AudioPanel class.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ResynthAudioSynthesizer implements ToneMapConstants, AudioSynthesizer {

	public class ResynthProcessor implements AudioProcessor {

		private Double resynthTime;
		private ResynthInfo resynthInfo;

		@Override
		public boolean process(AudioEvent audioEvent) {
			audioEvent.setFloatBuffer(resynthInfo.getSourceBuffer());
			return true;
		}

		@Override
		public void processingFinished() {
			// TODO Auto-generated method stub

		}

		public void setResynthInfo(Double resynthTime, ResynthInfo resynthInfo) {
			this.resynthTime = resynthTime;
			this.resynthInfo = resynthInfo;
		}

	}

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

	private Hearing hearing;

	private int windowSize;

	/**
	 * AudioModel constructor. Test Java Sound Audio System available Instantiate
	 * AudioPanel
	 * 
	 * @param parameterManager
	 */
	public ResynthAudioSynthesizer(ParameterManager parameterManager) {
		this.parameterManager = parameterManager;
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.windowSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SP_WINDOW);
	}

	public int getWindowSize() {
		return windowSize;
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
		AudioStream audioStream = audioStreams.get(streamId);
		AudioQueueMessage audioQueueMessage = new AudioQueueMessage();
		audioStream.bq.add(audioQueueMessage);

		audioStreams.remove(streamId);
	}

	@Override
	public void playFrameSequence(ToneTimeFrame toneTimeFrame, String streamId, int sequence) {
		if (!audioStreams.containsKey(streamId)) {
			audioStreams.put(streamId, new AudioStream(streamId));
		}
		AudioStream audioStream = audioStreams.get(streamId);

		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		AudioFeatureFrame aff = afp.getAudioFeatureFrame(sequence);
		TreeMap<Double, ResynthInfo> features = aff.getResynthFeatures().getFeatures();

		AudioQueueMessage audioQueueMessage = new AudioQueueMessage(toneTimeFrame, features, sequence);

		audioStream.bq.add(audioQueueMessage);

		return;
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

					if (toneTimeFrame == null) {
						this.audioStream.close();
						running = false;
						break;
					}

					if (sampleTime != -1) {
						TimeUnit.MILLISECONDS.sleep((long) (sampleTime * 1000));
					}

					double time = toneTimeFrame.getStartTime();

					for (Entry<Double, ResynthInfo> entry : aqm.features.entrySet()) {
						audioStream.getResynthProcessor().setResynthInfo(entry.getKey(), entry.getValue());
						this.audioStream.getGenerator().process();
					}

				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private class AudioQueueMessage {
		public ToneTimeFrame toneTimeFrame = null;
		public int sequence;
		public TreeMap<Double, ResynthInfo> features;

		public AudioQueueMessage(ToneTimeFrame toneTimeFrame, TreeMap<Double, ResynthInfo> features, int sequence) {
			this.toneTimeFrame = toneTimeFrame;
			this.sequence = sequence;
			this.features = features;
		}

		public AudioQueueMessage() {
			this(null, null, 0);
		}

	}

	private class AudioStream {

		private BlockingQueue<AudioQueueMessage> bq;

		private String streamId;

		private AudioGenerator generator;

		private ResynthProcessor resynthProcessor;

		public AudioStream(String streamId) {
			this.streamId = streamId;
			bq = new LinkedBlockingQueue<>();
			Thread.startVirtualThread(new AudioQueueConsumer(bq, this));
			//new Thread(new AudioQueueConsumer(bq, this)).start();
			resynthProcessor = new ResynthProcessor();
			generator = new AudioGenerator(ResynthAudioSynthesizer.this.getWindowSize(), 0);
			try {
				generator.addAudioProcessor(resynthProcessor);
				generator.addAudioProcessor(new AudioPlayer(new AudioFormat(44100, 16, 1, true, false)));
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public ResynthProcessor getResynthProcessor() {
			return resynthProcessor;
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
			return Objects.equals(streamId, other.streamId);
		}

		public BlockingQueue<AudioQueueMessage> getBq() {
			return bq;
		}

		public String getStreamId() {
			return streamId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(streamId);
			return result;
		}

	}

	@Override
	public AudioInputStream writeStream(ToneMap toneMap, float[] audioOutSamples, SourceDataLine audioOutput) {
		// TODO Auto-generated method stub
		return null;
	}

}
