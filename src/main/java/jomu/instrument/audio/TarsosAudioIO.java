package jomu.instrument.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.core.UGen;

public class TarsosAudioIO extends AudioIO {

	/**
	 * JavaSoundRTInput gathers audio from the JavaSound audio input device.
	 */
	private class AudioIOProcessor extends UGen implements AudioProcessor {

		/** Flag to tell whether JavaSound has been initialised. */
		private boolean javaSoundInitialized;

		/** The audio format. */
		private AudioFormat audioFormat;

		private float[] interleavedSamples;
		private byte[] bbuf;
		float[] audioFloatbuffer;
		int buffers_sent;

		/** The input mixer. */
		private Mixer inputMixer;

		private TreeMap<Double, AudioEvent> features = new TreeMap<>();

		/**
		 * Instantiates a new RTInput.
		 * 
		 * @param context     the AudioContext.
		 * @param audioFormat the AudioFormat.
		 */
		AudioIOProcessor(AudioContext context, AudioFormat audioFormat) {
			super(context, 2);
			this.audioFormat = audioFormat;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.olliebown.beads.core.UGen#calculateBuffer()
		 */
		@Override
		public void calculateBuffer() {
			// System.out.println(">>Tarsus calculate");
			for (int i = 0; i < bufferSize; i++) {
				bufOut[0][i] = audioFloatbuffer[i];
				// bufOut[1][i] = audioFloatbuffer[i];
			}
		}

		public void clear() {
			features.clear();
		}

		public TreeMap<Double, AudioEvent> getFeatures() {
			return features;
		}

		public boolean initJSInput() {

			javaSoundInitialized = true;
			interleavedSamples = new float[bufferSize * audioFormat.getChannels()];
			System.out.println(">>bs: " + bufferSize);
			System.out.println(">>af: " + audioFormat);
			System.out.println(">>fs: " + audioFormat.getFrameSize());
			bbuf = new byte[bufferSize * audioFormat.getFrameSize()];

			if (audioFile != null) {
				try {

					GainProcessor gainProcessor = new GainProcessor(1.0);
					AudioPlayer audioPlayer = new AudioPlayer(audioFormat);

					dispatcher = AudioDispatcherFactory.fromFile(audioFile, context.getBufferSize(), 0);

					// dispatcher.skip(startTime);
					dispatcher.addAudioProcessor(this);
					dispatcher.addAudioProcessor(gainProcessor);
					dispatcher.addAudioProcessor(audioPlayer);
				} catch (UnsupportedAudioFileException e) {
					throw new Error(e);
				} catch (IOException e) {
					throw new Error(e);
				} catch (LineUnavailableException e) {
					throw new Error(e);
				}
			} else {
				Mixer.Info[] mixerinfo = AudioSystem.getMixerInfo();
				inputMixer = AudioSystem.getMixer(mixerinfo[5]);
				if (inputMixer != null) {
					System.out.print("JavaSoundAudioIO: Chosen mixer is ");
					System.out.println(inputMixer.getMixerInfo().getName() + ".");
				} else {
					System.out.println("JavaSoundAudioIO: Failed to get mixer.");
					return false;
				}

				float sampleRate = 44100;
				int bufferSize = 1024;
				int overlap = 0;
				final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine targetDataLine = null;
				int inputBufferSize = 1024;
				try {
					targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
					targetDataLine.open(format, inputBufferSize);
				} catch (LineUnavailableException e) {
					System.out.println("no line");
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				System.out.println(
						"CHOSEN INPUT: " + targetDataLine.getLineInfo() + ", buffer size in bytes: " + inputBufferSize);
				targetDataLine.start();
				interleavedSamples = new float[bufferSize * format.getChannels()];
				bbuf = new byte[bufferSize * format.getFrameSize()];

				inputStream = new AudioInputStream(targetDataLine);

				JVMAudioInputStream audioStream = new JVMAudioInputStream(inputStream);

				// create a new dispatcher
				dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
				// add a processor
				dispatcher.addAudioProcessor(this);
			}

			javaSoundInitialized = true;
			return true;
		}

		@Override
		public boolean process(AudioEvent audioEvent) {

			getFeatures().put(audioEvent.getTimeStamp(), audioEvent);

			audioFloatbuffer = audioEvent.getFloatBuffer();

			int bufferSizeInFrames = context.getBufferSize();

			final int outputBufferLength = bufferSizeInFrames * audioFormat.getFrameSize();

			byte[][] output_buffers = new byte[NUM_OUTPUT_BUFFERS][outputBufferLength];
			byte[] current_buffer;
			final int sampleBufferSize = audioFormat.getChannels() * bufferSizeInFrames;
			float[] interleavedOutput = new float[sampleBufferSize];

			if (buffers_sent == 0) {
				for (int i = 0; i < NUM_OUTPUT_BUFFERS; i++) {
					current_buffer = output_buffers[i];
				}
			}

			current_buffer = output_buffers[buffers_sent % NUM_OUTPUT_BUFFERS];
			prepareLineBuffer(audioFormat, current_buffer, interleavedOutput, bufferSizeInFrames, sampleBufferSize);
			// AudioUtils.floatToByte(bbuf, interleavedOutput, audioFormat.isBigEndian());
			sourceDataLine.write(current_buffer, 0, outputBufferLength);
			buffers_sent++;
			return true;
		}

		@Override
		public void processingFinished() {
			if (inputMixer != null) {
				inputMixer.close();
				inputMixer = null;
			}
			// dispatcher.stop();
		}

	}

	/** The default system buffer size. */
	public static final int DEFAULT_SYSTEM_BUFFER_SIZE = 1024;

	/**
	 * Prints information about the current Mixer to System.out.
	 */
	public static void printMixerInfo() {
		Mixer.Info[] mixerinfo = AudioSystem.getMixerInfo();
		for (int i = 0; i < mixerinfo.length; i++) {
			String name = mixerinfo[i].getName();
			if (name.equals(""))
				name = "No name";
			System.out.println((i + 1) + ") " + name + " --- " + mixerinfo[i].getDescription());
			Mixer m = AudioSystem.getMixer(mixerinfo[i]);
			Line.Info[] lineinfo = m.getSourceLineInfo();
			for (int j = 0; j < lineinfo.length; j++) {
				System.out.println("  - " + lineinfo[j].toString());
			}
		}
	}

	/** The source data line. */
	private SourceDataLine sourceDataLine;

	/** The system buffer size in frames. */
	private int systemBufferSizeInFrames;

	/** The number of prepared output buffers ready to go to AudioOutput */
	final int NUM_OUTPUT_BUFFERS = 1;

	/** The output mixer. */
	private Mixer outputMixer;

	private AudioDispatcher dispatcher;

	private Thread audioThread;

	private int threadPriority;

	private AudioIOProcessor inputProcessor;

	private Thread audioInThread;

	private File audioFile;

	private AudioInputStream inputStream;

	public TarsosAudioIO() {
		this(DEFAULT_SYSTEM_BUFFER_SIZE);
	}

	public TarsosAudioIO(int systemBufferSize) {
		this.systemBufferSizeInFrames = systemBufferSize;
		System.out.println("Beads System Buffer size=" + systemBufferSize);
		setThreadPriority(Thread.MAX_PRIORITY);
	}

	/**
	 * Presents a choice of mixers on the commandline.
	 */
	public void chooseMixerCommandLine() {
		System.out.println("Choose a mixer...");
		printMixerInfo();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			selectMixer(Integer.parseInt(br.readLine()) - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clearFeatures() {
		inputProcessor.clear();
	}

	/** Shuts down JavaSound elements, SourceDataLine and Mixer. */
	protected boolean destroy() {
		outputMixer.close();
		outputMixer = null;
		return true;
	}

	@Override
	protected UGen getAudioInput(int[] channels) {
		// final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true,
		// true);
		IOAudioFormat ioAudioFormat = getContext().getAudioFormat();
		AudioFormat audioFormat;
		if (audioFile != null) {
			try {
				// audioFormat = AudioSystem.getAudioFileFormat(audioFile).getFormat();
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
				audioFormat = audioInputStream.getFormat();
				audioInputStream.close();
				System.out.println(">>audioFormat: " + audioFormat);
			} catch (UnsupportedAudioFileException e) {
				throw new Error(e);
			} catch (IOException e) {
				throw new Error(e);
			}
		} else {
			audioFormat = new AudioFormat(ioAudioFormat.sampleRate, ioAudioFormat.bitDepth, ioAudioFormat.inputs,
					ioAudioFormat.signed, ioAudioFormat.bigEndian);
		}
		inputProcessor = new AudioIOProcessor(getContext(), audioFormat);
		inputProcessor.initJSInput();
		return inputProcessor;
	}

	/**
	 * Gets the JavaSound mixer being used by this AudioContext.
	 * 
	 * @return the requested mixer.
	 */
	private void getDefaultMixerIfNotAlreadyChosen() {
		if (outputMixer == null) {
			selectMixer(-1);
		}
	}

	public AudioDispatcher getDispatcher() {
		return dispatcher;
	}

	public TreeMap<Double, AudioEvent> getFeatures() {
		return inputProcessor.getFeatures();
	}

	public AudioInputStream getInputStream() {
		return inputStream;
	}

	/**
	 * Initialises JavaSound.
	 */
	public boolean initJSOutput() {
		IOAudioFormat ioAudioFormat = getContext().getAudioFormat();
		AudioFormat audioFormat = new AudioFormat(ioAudioFormat.sampleRate, ioAudioFormat.bitDepth,
				ioAudioFormat.outputs, ioAudioFormat.signed, ioAudioFormat.bigEndian);
		getDefaultMixerIfNotAlreadyChosen();
		if (outputMixer == null) {
			return false;
		}
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			sourceDataLine = (SourceDataLine) outputMixer.getLine(info);
			if (systemBufferSizeInFrames < 0) {
				sourceDataLine.open(audioFormat);
			} else {
				int sound_output_buffer_size = systemBufferSizeInFrames * audioFormat.getFrameSize() * 2;

				sourceDataLine.open(audioFormat, sound_output_buffer_size);
				System.out.println("Beads Output buffer size=" + sound_output_buffer_size);
			}
		} catch (LineUnavailableException ex) {
			System.out.println(getClass().getName() + " : Error getting line\n");
		}

		// sourceDataLine.start();
		return true;
	}

	/**
	 * Prepares the AudioIO. This method is called by {@link AudioContext}'s
	 * constructor. This has default implementation as it will not be needed by most
	 * implementation.
	 * 
	 * @return true, if successful.
	 */
	protected boolean prepare() {
		// create JavaSound stuff only when needed
		initJSOutput();
		return true;
	}

	/**
	 * Read audio from UGens and copy them into a buffer ready to write to Audio
	 * Line
	 * 
	 * @param audioFormat        The AudioFormat
	 * @param outputBUffer       The buffer that will contain the prepared bytes for
	 *                           the AudioLine
	 * @param interleavedSamples Interleaved samples as floats
	 * @param bufferSizeInFrames The size of interleaved samples in frames
	 * @param sampleBufferSize   The size of our actual sample buffer size
	 */
	private void prepareLineBuffer(AudioFormat audioFormat, byte[] outputBUffer, float[] interleavedSamples,
			int bufferSizeInFrames, int sampleBufferSize) {
		update(); // this propagates update call to context
		for (int i = 0, counter = 0; i < bufferSizeInFrames; ++i) {
			for (int j = 0; j < audioFormat.getChannels(); ++j) {
				interleavedSamples[counter++] = context.out.getValue(j, i);
			}
		}
		AudioUtils.floatToByte(outputBUffer, 0, interleavedSamples, 0, sampleBufferSize, audioFormat.isBigEndian());

	}

	/**
	 * Select a mixer by index.
	 * 
	 * @param i the index of the selected mixer.
	 */
	public void selectMixer(int i) {
		if (i < 0) {
			outputMixer = AudioSystem.getMixer(null);
		} else {
			Mixer.Info[] mixerinfo = AudioSystem.getMixerInfo();

			outputMixer = AudioSystem.getMixer(mixerinfo[i]);
			if (outputMixer != null) {
				System.out.print("JavaSoundAudioIO: Chosen mixer is ");
				System.out.println(outputMixer.getMixerInfo().getName() + ".");
			} else {
				System.out.println("JavaSoundAudioIO: Failed to get mixer.");
			}
		}
	}

	public void setAudioFile(File audioFile) {
		this.audioFile = audioFile;
	}

	/**
	 * Sets the priority of the audio thread. Default priority is
	 * Thread.MAX_PRIORITY.
	 * 
	 * @param priority
	 */
	public void setThreadPriority(int priority) {
		this.threadPriority = priority;
		if (audioThread != null)
			audioThread.setPriority(threadPriority);
		if (audioInThread != null)
			audioInThread.setPriority(threadPriority);
	}

	/** Starts the audio system running. */
	@Override
	protected boolean start() {
		audioThread = new Thread(new Runnable() {
			public void run() {
				// create JavaSound stuff only when needed
				// initJSOutput();
				sourceDataLine.start();
				// create JavaSound stuff only when needed
				dispatcher.run();
			}
		});
		audioThread.setPriority(threadPriority);
		audioThread.start();

		return true;
	}

	/**
	 * Stops the AudioIO. Note this is not usually needed because the more usual way
	 * for the system to stop is simply to check {@link AudioContext#isRunning()} at
	 * each time step.
	 * 
	 * @return true, if successful.
	 */
	protected boolean stop() {
		inputProcessor.processingFinished();
		inputProcessor.kill();
		destroy();
		return true;
	}

}
