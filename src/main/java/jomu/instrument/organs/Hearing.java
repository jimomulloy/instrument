package jomu.instrument.organs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.beadsproject.beads.analysis.Analyzer;
import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.FeatureSet;
import net.beadsproject.beads.analysis.featureextractors.BasicDataWriter;
import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.Frequency;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.WavePlayer;

public class Hearing {

	private AudioContext ac;
	private Frequency f;
	Glide frequencyGlide;
	float meanFrequency = 400.0F;
	Analyzer analyzer;

	public void initialise() {
		// TODO Auto-generated method stub
		// set up the parent AudioContext object
		JavaSoundAudioIO.printMixerInfo();
		JavaSoundAudioIO jsaIO = new JavaSoundAudioIO();
		jsaIO.selectMixer(5);
		ac = new AudioContext(jsaIO);
		
		List<Class<? extends FeatureExtractor<?,?>>> extractors = new ArrayList<>();
		extractors.add(PowerSpectrum.class);
		
		analyzer = new Analyzer(ac, extractors);

		// get a microphone input unit generator
		UGen microphoneIn = ac.getAudioInput();
		
		analyzer.listenTo(microphoneIn);
		analyzer.updateFrom(ac.out);
		// connect the WavePlayer to the master gain
	}
	
	public void initialise1() {
		// TODO Auto-generated method stub
		// set up the parent AudioContext object
		JavaSoundAudioIO.printMixerInfo();
		JavaSoundAudioIO jsaIO = new JavaSoundAudioIO();
		jsaIO.selectMixer(5);
		ac = new AudioContext(jsaIO);

		// get a microphone input unit generator
		UGen microphoneIn = ac.getAudioInput();

		// connect the WavePlayer to the master gain
		//g.addInput(microphoneIn);

		// set up a master gain object
		//Gain g = new Gain(ac, 2, 0.5F);
		//ac.out.addInput(g);
		ac.out.addInput(microphoneIn);
	}

	public void initialise2() {
		// TODO Auto-generated method stub
		// set up the parent AudioContext object
		JavaSoundAudioIO.printMixerInfo();
		JavaSoundAudioIO jsaIO = new JavaSoundAudioIO();
		jsaIO.selectMixer(5);
		ac = new AudioContext(jsaIO);

		// set up a master gain object
		Gain g = new Gain(ac, 2, 0.5F);
		ac.out.addInput(g);

		// get a microphone input unit generator
		UGen microphoneIn = ac.getAudioInput();
		// set up the WavePlayer and the Glide that will control
		// its frequency
		frequencyGlide = new Glide(ac, 50, 10);
		//WavePlayer wp = new WavePlayer(ac, frequencyGlide, Buffer.SINE);
		// connect the WavePlayer to the master gain
		//g.addInput(wp);
		// In this block of code, we build an analysis chain
		// the ShortFrameSegmenter breaks the audio into short,
		// discrete chunks.
		ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac);
		// connect the microphone input to the ShortFrameSegmenter
		sfs.addInput(microphoneIn);
		// the FFT transforms that into frequency domain data
		FFT fft = new FFT();
		// connect the ShortFramSegmenter object to the FFT
		sfs.addListener(fft);

		// The PowerSpectrum turns the raw FFT output into proper
		// audio data.
		PowerSpectrum ps = new PowerSpectrum();
		// connect the FFT to the PowerSpectrum
		fft.addListener(ps);
		// The Frequency object tries to guess the strongest
		// frequency for the incoming data. This is a tricky
		// calculation, as there are many frequencies in any real
		// world sound. Further, the incoming frequencies are
		// effected by the microphone being used, and the cables
		// and electronics that the signal flows through.
		f = new Frequency(44100.0f);
		// connect the PowerSpectrum to the Frequency object
		ps.addListener(f);
		//
		ac.out.addDependent(sfs);
	}
	
	public void initialise3() {
		// TODO Auto-generated method stub
		// set up the parent AudioContext object
		JavaSoundAudioIO.printMixerInfo();
		JavaSoundAudioIO jsaIO = new JavaSoundAudioIO();
		jsaIO.selectMixer(2);
		ac = new AudioContext(jsaIO);

		// set up a master gain object
		Gain g = new Gain(ac, 2, 0.5F);
		ac.out.addInput(g);

		// get a microphone input unit generator
		UGen microphoneIn = ac.getAudioInput();
		// set up the WavePlayer and the Glide that will control
		// its frequency
		Glide frequencyGlide = new Glide(ac, 50, 10);
		WavePlayer wp = new WavePlayer(ac, frequencyGlide, Buffer.SINE);
		// connect the WavePlayer to the master gain
		g.addInput(wp);
		// In this block of code, we build an analysis chain
		// the ShortFrameSegmenter breaks the audio into short,
		// discrete chunks.
		ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac);
		// connect the microphone input to the ShortFrameSegmenter
		sfs.addInput(microphoneIn);
		// the FFT transforms that into frequency domain data
		FFT fft = new FFT();
		// connect the ShortFramSegmenter object to the FFT
		sfs.addListener(fft);

		// The PowerSpectrum turns the raw FFT output into proper
		// audio data.
		PowerSpectrum ps = new PowerSpectrum();
		// connect the FFT to the PowerSpectrum
		fft.addListener(ps);
		// The Frequency object tries to guess the strongest
		// frequency for the incoming data. This is a tricky
		// calculation, as there are many frequencies in any real
		// world sound. Further, the incoming frequencies are
		// effected by the microphone being used, and the cables
		// and electronics that the signal flows through.
		Frequency f = new Frequency(44100.0f);
		// connect the PowerSpectrum to the Frequency object
		ps.addListener(f);
		//
		FileOutputStream output = null;
		try {
			output = new FileOutputStream("c:\\data\\output-text.txt");
			BasicDataWriter bdw = new BasicDataWriter(output);
			// connect the PowerSpectrum to the Frequency object
			f.addListener(bdw);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// list the frame segmenter as a dependent, so that the
		// AudioContext knows when to update it
		ac.out.addDependent(sfs);
	}

	public void start1() {
		ac.start(); // start processing audio
	}

	public void start() {
		ac.start(); // start processing audio

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				analyse();
			}
		}, 0, 1000);
	}

	// In the draw routine, we will write the current frequency
	// on the screen and set the frequency of our sine wave.
	void draw() {
		// Get the data from the Frequency object. Only run this
		// 1/4 frames so that we don't overload the Glide object
		// with frequency changes.
		if (f.getFeatures() != null) {
			// get the data from the Frequency object
			float inputFrequency = f.getFeatures();
			// Only use frequency data that is under 3000Hz - this
			// will include all the fundamentals of most instruments
			// in other words, data over 3000Hz will usually be
			// erroneous (if we are using microphone input and
			// instrumental/vocal sounds)
			//if (inputFrequency < 3000F) {
				// store a running average
				meanFrequency = (0.4F * inputFrequency) + (0.6F * meanFrequency);
				// set the frequency stored in the Glide object
				frequencyGlide.setValue(meanFrequency);
				System.out.println(">>meanFrequency: " + meanFrequency);
			//}
		}
	}

	// In the draw routine, we will write the current frequency
	// on the screen and set the frequency of our sine wave.
	void analyse() {
		FeatureSet results = analyzer.getResults();
		results.printGlobalFeatures();
	}

}
