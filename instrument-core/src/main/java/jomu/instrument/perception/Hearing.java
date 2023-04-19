package jomu.instrument.perception;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.control.Coordinator;
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

	@Inject
	Coordinator coordinator;

	BufferedInputStream bs;

	public void closeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStream.close();
		console.getVisor().audioStopped();
		console.getVisor().updateStatusMessage("Ready");
		if (streamId != null) {
			// workspace.getAtlas().removeMapsByStreamId(streamId);
			LOG.finer(">>Clear MAPS in Audio Stream: " + streamId);
		}
		LOG.finer(">>Closed Audio Stream: " + streamId);
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

	static void showLineInfoFormats(final Line.Info lineInfo) {
		if (lineInfo instanceof DataLine.Info) {
			final DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;

			Arrays.stream(dataLineInfo.getFormats()).forEach(format -> LOG.finer("    " + format.toString()));
		}
	}

	static void showAudioMixerInfo() {
		try {
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				LOG.finer((i + 1) + ". " + mixers[i].getName() + " --> " + mixers[i].getDescription());

				Line.Info[] sourceLines = AudioSystem.getMixer(mixers[i]).getSourceLineInfo();
				LOG.finer("\tSource Lines:");
				for (int j = 0; j < sourceLines.length; j++) {
					LOG.finer("\t" + (j + 1) + ". " + sourceLines[j].toString());
					showLineInfoFormats(sourceLines[j]);
				}
				LOG.finer("\n");

				Line.Info[] targetLines = AudioSystem.getMixer(mixers[i]).getTargetLineInfo();
				LOG.finer("\tTarget Lines:");
				for (int j = 0; j < targetLines.length; j++) {
					LOG.finer("\t" + (j + 1) + ". " + targetLines[j].toString());
					showLineInfoFormats(targetLines[j]);
				}
				LOG.finer("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void startAudioFileStream(String inputFileName) throws Exception {
		String fileName = inputFileName;
		if (streamId != null) {
			workspace.getAtlas().removeMapsByStreamId(streamId);
		}
		System.gc();
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory();
		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.//
		// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		// Get amount of free memory within the heap in bytes. This size will increase
		// // after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		LOG.severe(">>heapSize: " + heapSize + ", heapMaxSize: " + heapMaxSize + ", heapFreeSize: " + heapFreeSize);

		showAudioMixerInfo();

		LOG.severe(">>Start Audio file isFileTypeSupported(AudioFileFormat.Type.WAVE): "
				+ AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE));
		if (!AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE)) {
			throw new Exception("Audio system WAV file not supported");
		}

		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
		instrumentSession.setInputAudioFilePath(fileName);
		instrumentSession.setStreamId(streamId);
		instrumentSession.setState(InstrumentSessionState.RUNNING);

		InputStream stream = null;
		if (fileName.endsWith(".mp3") || fileName.endsWith(".ogg")) {
			String wavFilePath = convertToWav(fileName);
			LOG.severe(">>MP3/OGG file converted: " + wavFilePath);
			stream = new FileInputStream(wavFilePath);
		} else {
			stream = storage.getObjectStorage().read(fileName);
		}
		bs = new BufferedInputStream(stream);

		AudioFormat format = AudioSystem.getAudioFileFormat(bs).getFormat();
		LOG.severe(">>Start Audio file: " + fileName + ", streamId: " + streamId + ", " + format.getEncoding() + ", "
				+ format);
		if (!format.getEncoding().toString().startsWith("PCM")) {
			bs.close();
			stream.close();
			String wavFilePath = convertToWav(fileName);
			LOG.severe(">>MP3/OGG file converted: " + wavFilePath);
			stream = new FileInputStream(wavFilePath);
			bs = new BufferedInputStream(stream);
		}

		if (format.getSampleRate() != audioStream.getSampleRate()) {
			audioStream.setSampleRate(format.getSampleRate());
			LOG.finer(">>Start Audio file set sample rate: " + audioStream.getSampleRate());
		}

		LOG.severe(">>Start Audio file processing buffer size: " + audioStream.getBufferSize() + ", sampelRate: "
				+ audioStream.getSampleRate());
		try {
			audioStream.calibrateAudioFileStream(bs);
			bs.close();
			stream.close();
		} catch (UnsupportedAudioFileException | IOException ex) {
			LOG.log(Level.SEVERE, "Audio file calibrate error:" + fileName, ex);
			throw new Exception("Audio file calibrate error: " + ex.getMessage());
		}

		if (fileName.endsWith(".mp3") || fileName.endsWith(".ogg")) {
			String wavFilePath = convertToWav(fileName);
			LOG.severe(">>MP3/OGG file converted: " + wavFilePath);
			stream = new FileInputStream(wavFilePath);
		} else {
			stream = storage.getObjectStorage().read(fileName);
		}
		bs = new BufferedInputStream(stream);

		if (!format.getEncoding().toString().startsWith("PCM")) {
			bs.close();
			stream.close();
			String wavFilePath = convertToWav(fileName);
			LOG.severe(">>MP3/OGG file converted: " + wavFilePath);
			stream = new FileInputStream(wavFilePath);
			bs = new BufferedInputStream(stream);
		}
		try {
			audioStream.processAudioFileStream(bs);
		} catch (UnsupportedAudioFileException | IOException ex) {
			LOG.log(Level.SEVERE, "Audio file init error:" + fileName, ex);
			throw new Exception("Audio file init error: " + ex.getMessage());
		}

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public static void skipFromBeginning(AudioInputStream audioStream, double secondsToSkip)
			throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		AudioFormat format = audioStream.getFormat();

		// find out how many bytes you have to skip, this depends on bytes per frame
		// (a.k.a. frameSize)
		long bytesToSkip = (long) (format.getFrameSize() * format.getFrameRate() * secondsToSkip);

		// now skip until the correct number of bytes have been skipped
		long justSkipped = 0;
		while (bytesToSkip > 0 && (justSkipped = audioStream.skip(bytesToSkip)) > 0) {
			bytesToSkip -= justSkipped;
		}
	}

	/**
	 * Invoke this function to convert to a playable file.
	 */
	public String convertToWav(String fileName) throws UnsupportedAudioFileException, IOException {
		// open stream
		AudioInputStream stream = AudioSystem.getAudioInputStream(storage.getObjectStorage().read(fileName));
		AudioFormat sourceFormat = stream.getFormat();
		// create audio format object for the desired stream/audio format
		// this is *not* the same as the file format (wav)
		AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
		// create stream that delivers the desired format
		AudioInputStream converted = AudioSystem.getAudioInputStream(convertFormat, stream);
		// write stream into a file with file format wav
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = fileName.lastIndexOf(System.getProperty("file.separator")) != -1
				? fileName.lastIndexOf(System.getProperty("file.separator")) + 1
				: 0;
		String wavFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + ".wav";
		String wavFilePath = folder + System.getProperty("file.separator") + wavFileName;
		File wavFile = new File(wavFilePath);
		AudioSystem.write(converted, AudioFileFormat.Type.WAVE, wavFile);
		return wavFilePath;
	}

	public void startAudioLineStream(String recordFile) throws LineUnavailableException, IOException {
		if (streamId != null) {
			workspace.getAtlas().removeMapsByStreamId(streamId);
		}
		System.gc();
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory();
		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.//
		// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		// Get amount of free memory within the heap in bytes. This size will increase
		// // after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		LOG.finer(">>heapSize: " + heapSize + ", heapMaxSize: " + heapMaxSize + ", heapFreeSize: " + heapFreeSize);

		streamId = UUID.randomUUID().toString();
		LOG.finer(">>Start Audio Stream: " + streamId);
		AudioStream audioStream = new AudioStream(streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.processMicrophoneStream(recordFile);

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
			// coordinator.getVoice().clear(streamId);
		}
		if (bs != null) {
			try {
				bs.close();
				LOG.finer(">>Close Audio Stream: " + streamId);
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Exception closig " + streamId, e);
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
		private boolean isFile = true;

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

		public float getSampleRate() {
			return sampleRate;
		}

		public int getBufferSize() {
			return bufferSize;
		}

		public void setSampleRate(float sampleRate) {
			this.sampleRate = sampleRate;
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE,
					Float.toString(sampleRate));

		}

		public void setBufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW,
					Integer.toString(bufferSize));
		}

		public AudioFeatureProcessor getAudioFeatureProcessor() {
			return audioFeatureProcessor;
		}

		private void calibrateAudioFileStream(BufferedInputStream inputStream)
				throws UnsupportedAudioFileException, IOException, LineUnavailableException {
			final AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);

			double audioOffset = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET);
			if (audioOffset > 0) {
				skipFromBeginning(stream, audioOffset / 1000.0);
			}

			TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
			LOG.severe(">>calibarteAudioFileStream: " + bufferSize + ", " + overlap + ", " + audioStream.getFormat());
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

			ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize);
			BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
			detector.setHandler(handler);

			dispatcher.addAudioProcessor(detector);
			dispatcher.run();
			LOG.finer(">>Calibrate audio file");
			dispatcher.run();
			LOG.finer(">>Calibrated audio file");
			handler.trackBeats(calibrationMap);

		}

		private void processAudioFileStream(BufferedInputStream inputStream)
				throws UnsupportedAudioFileException, IOException, LineUnavailableException {
			console.getVisor().clearView();
			this.setIsFile(true);
			final AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);
			double audioOffset = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_OFFSET);
			if (audioOffset > 0) {
				skipFromBeginning(stream, audioOffset / 1000.0);
			}
			LOG.severe(">>processAudioFileStream skip from secs: " + audioOffset / 1000.0);
			TarsosDSPAudioInputStream audioStream = new JVMAudioInputStream(stream);
			LOG.severe(">>processAudioFileStream: " + bufferSize + ", " + overlap + ", " + audioStream.getFormat());
			dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
			float audioHighPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			float audioLowPass = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
			if (audioLowPass - audioHighPass > 0) {
				dispatcher.addAudioProcessor(new BandPass(audioHighPass, audioLowPass - audioHighPass, sampleRate));
			}
			tarsosFeatureSource = new TarsosFeatureSource(dispatcher);
			tarsosFeatureSource.initialise();
			audioFeatureProcessor = new AudioFeatureProcessor(streamId, tarsosFeatureSource);
			dispatcher.addAudioProcessor(audioFeatureProcessor);

		}

		private void processMicrophoneStream(String recordFile) throws LineUnavailableException, IOException {
			console.getVisor().clearView();
			this.setIsFile(false);
			Info[] mixerInfo = AudioSystem.getMixerInfo();
			for (Info info : mixerInfo) {
				LOG.severe(">>processMicrophoneStream: " + info.getDescription());
				Mixer m = AudioSystem.getMixer(info);
				LOG.severe(">>processMicrophoneStream mixer: " + m.getMixerInfo().toString());
				Line[] sl = m.getSourceLines();
				for (Line l : sl) {
					LOG.severe(">>processMicrophoneStream source line: " + l.getLineInfo().toString());
				}
				Line[] tl = m.getTargetLines();
				for (Line l : tl) {
					LOG.severe(">>processMicrophoneStream target line: " + l.getLineInfo().toString());
				}
				LOG.severe(">>processMicrophoneStream: " + info.getDescription());
			}
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
			if (audioLowPass - audioHighPass > 0) {
				dispatcher.addAudioProcessor(new BandPass(audioHighPass, audioLowPass - audioHighPass, sampleRate));
			}

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

		public boolean isFile() {
			return isFile;
		}

		public void setIsFile(boolean isFile) {
			this.isFile = isFile;
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
			audioStream.processAudioFileStream(new BufferedInputStream(is));
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();

	}
}
