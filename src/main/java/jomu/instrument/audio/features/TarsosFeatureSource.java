package jomu.instrument.audio.features;

import be.tarsos.dsp.AudioDispatcher;

public class TarsosFeatureSource {

	private BandedPitchDetectorSource bandedPitchDetectorSource;
	private SpectrumSource spectrumSource;
	private ConstantQSource constantQSource;
	private GoertzelSource goertzelSource;
	private OnsetSource onsetSource;
	private BeatSource beatSource;
	private PitchDetectorSource pitchDetectorSource;
	private ScalogramSource scalogramSource;
	private SpectralPeaksSource spectralPeaksSource;
	private SpectrogramSource spectrogramSource;
	private AudioDispatcher dispatcher;

	public TarsosFeatureSource(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public BandedPitchDetectorSource getBandedPitchDetectorSource() {
		return bandedPitchDetectorSource;
	}

	public SpectrumSource getSpectrumSource() {
		return spectrumSource;
	}

	public ConstantQSource getConstantQSource() {
		return constantQSource;
	}

	public AudioDispatcher getDispatcher() {
		return dispatcher;
	}

	public GoertzelSource getGoertzelSource() {
		return goertzelSource;
	}

	public OnsetSource getOnsetSource() {
		return onsetSource;
	}

	public BeatSource getBeatSource() {
		return beatSource;
	}

	public PitchDetectorSource getPitchDetectorSource() {
		return pitchDetectorSource;
	}

	public ScalogramSource getScalogramSource() {
		return scalogramSource;
	}

	public SpectralPeaksSource getSpectralPeaksSource() {
		return spectralPeaksSource;
	}

	public SpectrogramSource getSpectrogramSource() {
		return spectrogramSource;
	}

	public void initialise() {
		constantQSource = new ConstantQSource(dispatcher);
		onsetSource = new OnsetSource(dispatcher);
		beatSource = new BeatSource(dispatcher);
		spectralPeaksSource = new SpectralPeaksSource(dispatcher);
		pitchDetectorSource = new PitchDetectorSource(dispatcher);
		bandedPitchDetectorSource = new BandedPitchDetectorSource(dispatcher);
		spectrumSource = new SpectrumSource(dispatcher);
		spectrogramSource = new SpectrogramSource(dispatcher);
		// audioEventSource = new AudioEventSource(dispatcher);
		goertzelSource = new GoertzelSource(dispatcher);
		scalogramSource = new ScalogramSource(dispatcher);
		constantQSource.initialise();
		onsetSource.initialise();
		beatSource.initialise();
		spectralPeaksSource.initialise();
		pitchDetectorSource.initialise();
		bandedPitchDetectorSource.initialise();
		spectrumSource.initialise();
		spectrogramSource.initialise();
		goertzelSource.initialise();
	}
}
