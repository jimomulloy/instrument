package jomu.instrument.perception;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.InstrumentSession;
import jomu.instrument.store.InstrumentSession.InstrumentSessionState;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.CalibrationMap;

@ApplicationScoped
public class Hearing implements Organ {

	private static final Logger LOG = Logger.getLogger(Hearing.class.getName());

	public static final float AUDIO_HIGHPASS_MAX = 20.0F;

	public static final float AUDIO_LOWPASS_MIN = 12000.0F;

	String streamId;

	ConcurrentHashMap<String, AudioStream> audioStreams = new ConcurrentHashMap<>();

	@Inject
	ParameterManager parameterManager;

	@Inject
	Workspace workspace;

	@Inject
	Cortex cortex;

	@Inject
	Console console;

	@Inject
	Storage storage;

	private BufferedInputStream bs;

	public void closeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStream.close();
		console.getVisor().audioStopped();
		console.getVisor().updateStatusMessage("Ready");
		LOG.severe(">>Closed Audio Stream: " + streamId);
	}

	public void removeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStreams.remove(streamId);
	}

	public AudioFeatureProcessor getAudioFeatureProcessor(String streamId) {
		if (audioStreams.containsKey(streamId)) {
			return audioStreams.get(streamId).getAudioFeatureProcessor();
		} else {
			return null;
		}
	}

	@Override
	public void initialise() {
	}

	@Override
	public void start() {
	}

	public void startAudioFileStream(String fileName) throws Exception {
		if (streamId != null) {
			workspace.getAtlas().removeMapsByStreamId(streamId);
		}
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);
		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
		instrumentSession.setInputAudioFilePath(fileName);
		instrumentSession.setStreamId(streamId);
		instrumentSession.setState(InstrumentSessionState.RUNNING);

		// File file = new File(fileName);
		InputStream stream = storage.getObjectStorage().read(fileName);
		bs = new BufferedInputStream(stream);
		AudioFormat format = AudioSystem.getAudioFileFormat(bs).getFormat();
		LOG.severe(">>Start Audio file: " + fileName + ", streamId: " + streamId + ", " + format);

		try {
			audioStream.calibrateAudioFileStream(bs);
			bs.close();
			stream.close();
		} catch (UnsupportedAudioFileException | IOException ex) {
			LOG.log(Level.SEVERE, "Audio file calibrate error:" + fileName, ex);
			throw new Exception("Audio file calibrate error: " + ex.getMessage());
		}
		stream = storage.getObjectStorage().read(fileName);
		bs = new BufferedInputStream(stream);
		try {
			audioStream.initialiseAudioFileStream(bs);
		} catch (UnsupportedAudioFileException | IOException ex) {
			LOG.log(Level.SEVERE, "Audio file init error:" + fileName, ex);
			throw new Exception("Audio file init error: " + ex.getMessage());
		}

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public void startAudioLineStream(String recordFile) throws LineUnavailableException, IOException {
		if (streamId != null) {
			workspace.getAtlas().removeMapsByStreamId(streamId);
		}
		streamId = UUID.randomUUID().toString();
		LOG.severe(">>Start Audio Stream: " + streamId);
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseMicrophoneStream(recordFile);

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public void stopAudioStream() {
		LOG.severe(">>Stop Audio Stream: " + streamId);
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream != null) {
			audioStream.stop();
			if (audioStream.getAudioFeatureProcessor() != null) {
				audioStream.getAudioFeatureProcessor().removeObserver(cortex);
			}
			closeAudioStream(streamId);
		}
		if (bs != null) {
			try {
				bs.close();
			} catch (IOException e) {
			}
		}
	}

	private class AudioStream {

		private AudioFeatureProcessor audioFeatureProcessor;
		private String streamId;
		private TarsosFeatureSource tarsosFeatureSource;
		private float sampleRate = 44100F;
		private int bufferSize = 1024;
		private int overlap = 0;
		private AudioDispatcher dispatcher;
		private TargetDataLine line;

		public AudioStream(String streamId) {
			this.streamId = streamId;
			sampleRate = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE);
			bufferSize = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW);
		}

		public void close() {
			if (dispatcher != null) {
				dispatcher = null;
			}
		}

		public AudioFeatureProcessor getAudioFeatureProcessor() {
			return audioFeatureProcessor;
		}

		public void calibrateAudioFileStream(BufferedInputStream inputStream)
				throws UnsupportedAudioFileException, IOException {
			final AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);
			TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
			dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
			CalibrationMap calibrationMap = workspace.getAtlas().getCalibrationMap(streamId);
			dispatcher.addAudioProcessor(new AudioProcessor() {

				@Override
				public boolean process(AudioEvent audioEvent) {
					double max = 0;
					float[] values = audioEvent.getFloatBuffer();
					int numSamples = values.length;
					double total = 0;
					for (var cur = 0; cur < numSamples; cur++) {
						total += values[cur] * values[cur];
						if (max < total) {
							max = total;
						}
					}
					double result = Math.sqrt(total / numSamples);
					calibrationMap.put(audioEvent.getTimeStamp(), result);
					return true;
				}

				@Override
				public void processingFinished() {

				}
			});
			LOG.severe(">>Calibrate audio file");
			dispatcher.run();
			LOG.severe(">>Calibrated audio file");
		}

		public void initialiseAudioFileStream(BufferedInputStream inputStream)
				throws UnsupportedAudioFileException, IOException {
			console.getVisor().clearView();
			final AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);
			TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
			dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
			float audioHighPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			float audioLowPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
//			if (audioHighPass > AUDIO_LOWPASS_MIN && audioHighPass < AUDIO_HIGHPASS_MAX) {
//				LOG.finer(">>audioHighPass: " + audioHighPass);
//				dispatcher.addAudioProcessor(new HighPass(audioHighPass, sampleRate));
//			} else {
//				LOG.finer(">>audioHighPass: " + AUDIO_HIGHPASS_MAX);
//				dispatcher.addAudioProcessor(new HighPass(AUDIO_HIGHPASS_MAX, sampleRate));
//			}
//			if (audioLowPass > AUDIO_LOWPASS_MIN && audioLowPass < AUDIO_HIGHPASS_MAX) {
//				LOG.finer(">>audioLowPass: " + audioLowPass);
//				dispatcher.addAudioProcessor(new LowPassSP(audioLowPass, sampleRate));
//			} else {
//				LOG.finer(">>audioLowPass: " + AUDIO_LOWPASS_MIN);
//				dispatcher.addAudioProcessor(new LowPassSP(AUDIO_LOWPASS_MIN, sampleRate));
//			}
			// TODO??
			if (audioLowPass - audioHighPass > 0) {
				dispatcher.addAudioProcessor(new BandPass(audioHighPass, audioLowPass - audioHighPass, sampleRate));
			}
			tarsosFeatureSource = new TarsosFeatureSource(dispatcher);
			tarsosFeatureSource.initialise();
			audioFeatureProcessor = new AudioFeatureProcessor(streamId, tarsosFeatureSource);
			dispatcher.addAudioProcessor(audioFeatureProcessor);

		}

		public void initialiseMicrophoneStream(String recordFile) throws LineUnavailableException, IOException {
			console.getVisor().clearView();
			AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
			// AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
			final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
			line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			final int numberOfSamples = (int) (0.1 * sampleRate);
			line.open(format, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);

			JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
			// create a new dispatcher
			dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

			boolean audioRecord = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_SWITCH);

			if (recordFile != null && audioRecord) {
				AudioFormat recordFormat = new AudioFormat(sampleRate, 16, 1, true, true);
				WaveformWriter writer = new WaveformWriter(recordFormat, recordFile);
				dispatcher.addAudioProcessor(writer);
			}
			float audioHighPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			float audioLowPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
//			if (audioHighPass > AUDIO_LOWPASS_MIN && audioHighPass < AUDIO_HIGHPASS_MAX) {
//			LOG.finer(">>audioHighPass: " + audioHighPass);
//			dispatcher.addAudioProcessor(new HighPass(audioHighPass, sampleRate));
//		} else {
//			LOG.finer(">>audioHighPass: " + AUDIO_HIGHPASS_MAX);
//			dispatcher.addAudioProcessor(new HighPass(AUDIO_HIGHPASS_MAX, sampleRate));
//		}
//		if (audioLowPass > AUDIO_LOWPASS_MIN && audioLowPass < AUDIO_HIGHPASS_MAX) {
//			LOG.finer(">>audioLowPass: " + audioLowPass);
//			dispatcher.addAudioProcessor(new LowPassSP(audioLowPass, sampleRate));
//		} else {
//			LOG.finer(">>audioLowPass: " + AUDIO_LOWPASS_MIN);
//			dispatcher.addAudioProcessor(new LowPassSP(AUDIO_LOWPASS_MIN, sampleRate));
//		}
			// if (audioLowPass - audioHighPass > 0) {
			// dispatcher.addAudioProcessor(new BandPass(audioHighPass, audioLowPass -
			// audioHighPass, sampleRate));
			// }

			tarsosFeatureSource = new TarsosFeatureSource(dispatcher);
			tarsosFeatureSource.initialise();
			audioFeatureProcessor = new AudioFeatureProcessor(streamId, tarsosFeatureSource);

			dispatcher.addAudioProcessor(audioFeatureProcessor);

		}

		public void start() {
			if (dispatcher != null) {
				new Thread(dispatcher, "Audio dispatching").start();
			}
		}

		public void stop() {
			if (dispatcher != null) {
				dispatcher.stop();
			}
			if (line != null) {
				line.drain();
				line.close();
				line = null;
			}
		}

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	public void test() {
		InputStream is = getClass().getClassLoader().getResourceAsStream("test.wav");
		if (is == null) {
			throw new IllegalArgumentException("file not found!");
		}

		if (streamId != null) {
			workspace.getAtlas().removeMapsByStreamId(streamId);
		}
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		try {
			audioStream.initialiseAudioFileStream(new BufferedInputStream(is));
		} catch (UnsupportedAudioFileException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();

	}
}
