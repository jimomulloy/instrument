package jomu.instrument.perception;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.workspace.Workspace;

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

	public void closeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStream.close();
		console.getVisor().audioStopped();
		console.getVisor().updateStatusMessage("Ready");
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

	public void startAudioFileStream(String fileName)
			throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		if (streamId != null) {
			workspace.getAtlas().removeToneMapsByStreamId(streamId);
		}
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseAudioFileStream(fileName);

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public void startAudioLineStream(String recordFile) throws LineUnavailableException, IOException {
		if (streamId != null) {
			workspace.getAtlas().removeToneMapsByStreamId(streamId);
		}
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseMicrophoneStream(recordFile);

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public void stopAudioStream() {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream != null) {
			audioStream.stop();
			if (audioStream.getAudioFeatureProcessor() != null) {
				audioStream.getAudioFeatureProcessor().removeObserver(cortex);
			}
			closeAudioStream(streamId);
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

		public void initialiseAudioFileStream(String fileName)
				throws UnsupportedAudioFileException, IOException, LineUnavailableException {
			console.getVisor().clearView();
			// tarsosIO.selectMixer(2);
			File file = new File(fileName);
			AudioFormat format = AudioSystem.getAudioFileFormat(file).getFormat();
			LOG.finer(">>Open Audio file:  " + fileName + ", format: " + format);
			// UniversalAudioInputStream ais = new UniversalAudioInputStream(new
			// FileInputStream(file),
			// new TarsosDSPAudioFormat((int)sampleRate, 16, 1, true, false));
			dispatcher = AudioDispatcherFactory.fromFile(file, bufferSize, overlap);
			// AudioFormat format = AudioSystem.getAudioFileFormat(file).getFormat();
			float audioHighPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			float audioLowPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
//			if (audioHighPass > AUDIO_LOWPASS_MIN && audioHighPass < AUDIO_HIGHPASS_MAX) {
//				LOG.severe(">>audioHighPass: " + audioHighPass);
//				dispatcher.addAudioProcessor(new HighPass(audioHighPass, sampleRate));
//			} else {
//				LOG.severe(">>audioHighPass: " + AUDIO_HIGHPASS_MAX);
//				dispatcher.addAudioProcessor(new HighPass(AUDIO_HIGHPASS_MAX, sampleRate));
//			}
//			if (audioLowPass > AUDIO_LOWPASS_MIN && audioLowPass < AUDIO_HIGHPASS_MAX) {
//				LOG.severe(">>audioLowPass: " + audioLowPass);
//				dispatcher.addAudioProcessor(new LowPassSP(audioLowPass, sampleRate));
//			} else {
//				LOG.severe(">>audioLowPass: " + AUDIO_LOWPASS_MIN);
//				dispatcher.addAudioProcessor(new LowPassSP(AUDIO_LOWPASS_MIN, sampleRate));
//			}
			// if (audioLowPass - audioHighPass > 0) {
			// dispatcher.addAudioProcessor(new BandPass(audioHighPass, audioLowPass -
			// audioHighPass, sampleRate));
			// }
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
			TargetDataLine line;
			// line = (TargetDataLine) mixer.getLine(dataLineInfo);
			line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			// final int numberOfSamples = bufferSize;
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
//			LOG.severe(">>audioHighPass: " + audioHighPass);
//			dispatcher.addAudioProcessor(new HighPass(audioHighPass, sampleRate));
//		} else {
//			LOG.severe(">>audioHighPass: " + AUDIO_HIGHPASS_MAX);
//			dispatcher.addAudioProcessor(new HighPass(AUDIO_HIGHPASS_MAX, sampleRate));
//		}
//		if (audioLowPass > AUDIO_LOWPASS_MIN && audioLowPass < AUDIO_HIGHPASS_MAX) {
//			LOG.severe(">>audioLowPass: " + audioLowPass);
//			dispatcher.addAudioProcessor(new LowPassSP(audioLowPass, sampleRate));
//		} else {
//			LOG.severe(">>audioLowPass: " + AUDIO_LOWPASS_MIN);
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
		}

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}
