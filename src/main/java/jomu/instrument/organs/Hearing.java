package jomu.instrument.organs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jomu.instrument.audio.TarsosAudioIO;
import jomu.instrument.audio.analysis.Analyzer;
import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.featureextractors.Frequency;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Glide;

public class Hearing {

	private AudioContext ac;
	private Frequency f;
	Glide frequencyGlide;
	float meanFrequency = 400.0F;
	Analyzer analyzer;

	int sampleRate = 44100;
	private TarsosAudioIO tarsosIO;
	TarsosFeatureSource tarsosFeatureSource;
	private PitchFrameProcessor pitchFrameProcessor;
	private PitchFrameCellSource afs;

	public void initialise() {
		// TODO Auto-generated method stub
		// set up the parent AudioContext object
		tarsosIO = new TarsosAudioIO();
		tarsosIO.selectMixer(2);
		File file = new File("D:/audio/tonemap1.wav");
		IOAudioFormat audioFormat = new IOAudioFormat(sampleRate, 16, 1, 1, true, true);
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
		pitchFrameProcessor = new PitchFrameProcessor(analyzer, tarsosFeatureSource);
		pitchFrameProcessor.setMaxFrames(100);
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

	public Glide getFrequencyGlide() {
		return frequencyGlide;
	}

	public float getMeanFrequency() {
		return meanFrequency;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public TarsosAudioIO getTarsosIO() {
		return tarsosIO;
	}

	public TarsosFeatureSource getTarsosFeatureSource() {
		return tarsosFeatureSource;
	}

	public PitchFrameProcessor getPitchFrameProcessor() {
		return pitchFrameProcessor;
	}

	public PitchFrameCellSource getAfs() {
		return afs;
	}

}
