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

	private String streamId;

	public AudioFeatureProcessor getAudioFeatureProcessor(String streamId) {
		return audioStreams.get(streamId).getAudioFeatureProcessor();
	}

	public void initialise() {
		streamId = UUID.randomUUID().toString();		
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);
		
		audioStream.initialiseFileStream("D:/audio/testsine2noteA.wav");
		
		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().addObserver(Instrument.getInstance().getCoordinator().getCortex());
	}

	public void start() {
		AudioStream audioStream = audioStreams.get(streamId);
		audioStream.start();
	}
	
	private ConcurrentHashMap<String, AudioStream> audioStreams = new ConcurrentHashMap<String, AudioStream>();
	
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
		
		public void start() {
			ac.start();
		}

		public void initialiseFileStream(String fileName) {
			tarsosIO.selectMixer(2);
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
			audioFeatureProcessor = new AudioFeatureProcessor(streamId, analyzer,
					tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(100);

			tarsosIO.getDispatcher().addAudioProcessor(audioFeatureProcessor);

			analyzer.listenTo(microphoneIn);
			analyzer.updateFrom(ac.out);
		}
		
		public void initialiseMicrophoneStream() {
			tarsosIO.selectMixer(2);
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
			audioFeatureProcessor = new AudioFeatureProcessor(streamId, analyzer,
					tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(100);

			tarsosIO.getDispatcher().addAudioProcessor(audioFeatureProcessor);

			analyzer.listenTo(microphoneIn);
			analyzer.updateFrom(ac.out);
		}

		public AudioContext getAc() {
			return ac;
		}

		public AudioFeatureProcessor getAudioFeatureProcessor() {
			return audioFeatureProcessor;
		}

		public TarsosAudioIO getTarsosIO() {
			return tarsosIO;
		}

		public Analyzer getAnalyzer() {
			return analyzer;
		}

		public String getStreamId() {
			return streamId;
		}

		public TarsosFeatureSource getTarsosFeatureSource() {
			return tarsosFeatureSource;
		}

		public int getSampleRate() {
			return sampleRate;
		}

	}

}
