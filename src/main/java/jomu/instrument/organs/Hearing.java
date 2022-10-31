package jomu.instrument.organs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jomu.instrument.Instrument;
import jomu.instrument.audio.TarsosAudioIO;
import jomu.instrument.audio.analysis.Analyzer;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.core.UGen;

public class Hearing {

	private class AudioStream {

		private AudioContext ac;
		private AudioFeatureProcessor audioFeatureProcessor;
		private TarsosAudioIO tarsosIO;
		private Analyzer analyzer;
		private String streamId;
		private TarsosFeatureSource tarsosFeatureSource;
		private int sampleRate = 44100;

		public AudioStream(String streamId) {
			this.streamId = streamId;
			tarsosIO = new TarsosAudioIO();
		}

		public AudioContext getAc() {
			return ac;
		}

		public Analyzer getAnalyzer() {
			return analyzer;
		}

		public AudioFeatureProcessor getAudioFeatureProcessor() {
			return audioFeatureProcessor;
		}

		public int getSampleRate() {
			return sampleRate;
		}

		public String getStreamId() {
			return streamId;
		}

		public TarsosFeatureSource getTarsosFeatureSource() {
			return tarsosFeatureSource;
		}

		public TarsosAudioIO getTarsosIO() {
			return tarsosIO;
		}

		public void initialiseFileStream(String fileName) {
			Instrument.getInstance().getDruid().getVisor().clearView();
			// tarsosIO.selectMixer(2);
			File file = new File(fileName);
			IOAudioFormat audioFormat = new IOAudioFormat(sampleRate, 16, 1, 1,
					true, true);
			ac = new AudioContext(tarsosIO, 1024, audioFormat);
			// get a microphone input unit generator
			tarsosIO.setAudioFile(file);
			UGen microphoneIn = ac.getAudioInput();

			// Oscilloscope oscilloscope = new
			// Oscilloscope(Instrument.getInstance().getDruid().getOscilloscopeHandler());
			// tarsosIO.getDispatcher().addAudioProcessor(oscilloscope);

			tarsosFeatureSource = new TarsosFeatureSource(tarsosIO);
			tarsosFeatureSource.initialise();

			List<Class<? extends FeatureExtractor<?, ?>>> extractors = new ArrayList<>();
			// extractors.add(MelSpectrum.class);
			extractors.add(SpectralPeaks.class);

			analyzer = new Analyzer(ac, extractors);
			audioFeatureProcessor = new AudioFeatureProcessor(streamId,
					analyzer, tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(100);

			tarsosIO.getDispatcher().addAudioProcessor(audioFeatureProcessor);

			analyzer.listenTo(microphoneIn);
			analyzer.updateFrom(ac.out);
		}

		public void initialiseMicrophoneStream(int mixer) {
			tarsosIO.selectMixer(mixer);
			IOAudioFormat audioFormat = new IOAudioFormat(sampleRate, 16, 1, 1,
					true, true);
			ac = new AudioContext(tarsosIO, 1024, audioFormat);
			// get a microphone input unit generator
			UGen microphoneIn = ac.getAudioInput();

			// Oscilloscope oscilloscope = new
			// Oscilloscope(Instrument.getInstance().getDruid().getOscilloscopeHandler());
			// tarsosIO.getDispatcher().addAudioProcessor(oscilloscope);

			tarsosFeatureSource = new TarsosFeatureSource(tarsosIO);
			tarsosFeatureSource.initialise();

			List<Class<? extends FeatureExtractor<?, ?>>> extractors = new ArrayList<>();
			// extractors.add(MelSpectrum.class);
			extractors.add(SpectralPeaks.class);

			analyzer = new Analyzer(ac, extractors);
			audioFeatureProcessor = new AudioFeatureProcessor(streamId,
					analyzer, tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(100);

			tarsosIO.getDispatcher().addAudioProcessor(audioFeatureProcessor);

			analyzer.listenTo(microphoneIn);
			analyzer.updateFrom(ac.out);
		}

		public void start() {
			ac.start();
		}

		public void close() {
			ac.stop();
		}

	}

	private String streamId;

	private ConcurrentHashMap<String, AudioStream> audioStreams = new ConcurrentHashMap<>();

	public AudioFeatureProcessor getAudioFeatureProcessor(String streamId) {
		return audioStreams.get(streamId).getAudioFeatureProcessor();
	}

	public void initialise() {
	}

	public void start() {
	}

	public void startAudioFileStream(String fileName) {
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		System.out.println(">>!!hearing initialise: " + streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseFileStream(fileName);

		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().addObserver(
				Instrument.getInstance().getCoordinator().getCortex());
		audioStream.start();
	}

	public void startAudioLineStream(int mixer) {
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		System.out.println(">>!!hearing initialise: " + streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseMicrophoneStream(mixer);

		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().addObserver(
				Instrument.getInstance().getCoordinator().getCortex());
		audioStream.start();
	}

	public void closeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStream.close();
		audioStreams.remove(streamId);
	}
}
