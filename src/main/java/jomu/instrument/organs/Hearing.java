package jomu.instrument.organs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import jomu.instrument.audio.TarsosAudioIO;
import jomu.instrument.audio.analysis.Analyzer;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.featureextractors.Frequency;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Glide;

public class Hearing {

	private AudioContext ac;
	private AudioFeatureProcessor audioFeatureProcessor;
	private Frequency f;
	private TarsosAudioIO tarsosIO;
	Analyzer analyzer;
	Glide frequencyGlide;

	float meanFrequency = 400.0F;
	int sampleRate = 44100;
	String streamId;
	TarsosFeatureSource tarsosFeatureSource;

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public AudioFeatureProcessor getAudioFeatureProcessor() {
		return audioFeatureProcessor;
	}

	public Glide getFrequencyGlide() {
		return frequencyGlide;
	}

	public float getMeanFrequency() {
		return meanFrequency;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public TarsosFeatureSource getTarsosFeatureSource() {
		return tarsosFeatureSource;
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public void initialise() {
		this.streamId = UUID.randomUUID().toString();
		tarsosIO = new TarsosAudioIO();
		tarsosIO.selectMixer(2);
		File file = new File("D:/audio/testsine2noteA.wav");
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

	public void start() {
		ac.start(); // start processing audio

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// analyse();
			}
		}, 0, 1000);
	}

}
