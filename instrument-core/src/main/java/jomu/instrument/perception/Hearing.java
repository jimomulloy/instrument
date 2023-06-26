package jomu.instrument.perception;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.signal.Smooth;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.BitDepthProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.MultichannelToMono;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.resample.RateTransposer;
import be.tarsos.dsp.wavelet.HaarWaveletCoder;
import be.tarsos.dsp.wavelet.HaarWaveletDecoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.audio.PidProcessor;
import jomu.instrument.audio.TarsosAudioDispatcherFactory;
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
	Voice voice;

	@Inject
	Cortex cortex;

	@Inject
	Console console;

	@Inject
	Storage storage;

	@Inject
	Coordinator coordinator;

	BufferedInputStream bs;
	InputStream is;

	private AudioDispatcher audioPlayerDispatcher;

	private TarsosAudioDispatcherFactory audioDispatcherFactory = new TarsosAudioDispatcherFactory();

	public void setAudioDispatcherFactory(TarsosAudioDispatcherFactory audioDispatcherFactory) {
		this.audioDispatcherFactory = audioDispatcherFactory;
	}

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
		audioStreams = new ConcurrentHashMap<>();
		bs = null;
		is = null;
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
		LOG.severe(">>HEARING startAudioFileStream: " + inputFileName);
		if (getStreamId() != null) {
			LOG.severe(">>HEARING startAudioFileStream clear old stream: " + getStreamId());
			workspace.getAtlas().removeMapsByStreamId(getStreamId());
			voice.reset();
			System.gc();
			LOG.severe(">>HEARING startAudioFileStream cleared old stream: " + getStreamId());
		}

		workspace.getAtlas().clear();

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
				+ AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE) + ", for file: " + inputFileName);
		if (!AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE)) {
			throw new Exception("Audio system WAV file not supported");
		}

		streamId = UUID.randomUUID().toString();
		LOG.severe(">>HEARING startAudioFileStream new stream: " + getStreamId());

		AudioStream audioStream = new AudioStream(getStreamId());
		audioStreams.put(getStreamId(), audioStream);

		InstrumentSession instrumentSession = workspace.getInstrumentSessionManager().getCurrentSession();
		instrumentSession.setInputAudioFilePath(fileName);
		instrumentSession.setStreamId(getStreamId());
		instrumentSession.setState(InstrumentSessionState.RUNNING);

		is = null;
		boolean isResample = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RESAMPLE);

		if (fileName.toLowerCase().endsWith(".mp3") || fileName.toLowerCase().endsWith(".ogg")) {
			String wavFilePath = convertToWav(fileName);
			if (isResample) {
				String resampleFilePath = resample(wavFilePath);
				is = new FileInputStream(resampleFilePath);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE,
						resampleFilePath);
			} else {
				is = new FileInputStream(wavFilePath);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE,
						wavFilePath);
			}
		} else {
			String cacheFilePath = cacheFile(fileName);
			if (isResample) {
				String resampleFilePath = resample(cacheFilePath);
				is = new FileInputStream(resampleFilePath);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE,
						resampleFilePath);
			} else {
				is = new FileInputStream(cacheFilePath);
				parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE,
						cacheFilePath);
			}
		}

		String filePath = parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE);

		double audioTimeStretch = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_TIME_STRETCH);
		if (audioTimeStretch != 0 && audioTimeStretch != 1.0) {
			filePath = timeStretch(filePath, audioTimeStretch);
			is = new FileInputStream(filePath);
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE, filePath);
		}

		double audioPitchShift = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PITCH_SHIFT);
		if (audioPitchShift != 0) {
			filePath = pitchShift(filePath, audioPitchShift);
			is = new FileInputStream(filePath);
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE, filePath);
		}

		if (parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_BEFORE) > 0
				|| parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_AFTER) > 0) {
			String padFilePath = padAudio(filePath);
			is = new FileInputStream(padFilePath);
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE, padFilePath);
		}

		if (parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_BEFORE) > 0
				|| parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_AFTER) > 0) {
			String padFilePath = padAudio(filePath);
			is = new FileInputStream(padFilePath);
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE, padFilePath);
		}

		filePath = parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE);
		bs = new BufferedInputStream(is);

		AudioFormat format = AudioSystem.getAudioFileFormat(bs).getFormat();
		LOG.severe(">>Start Audio file: " + fileName + ", path: " + filePath + ", streamId: " + getStreamId() + ", "
				+ format);
		if (!format.getEncoding().toString().startsWith("PCM")) {
			bs.close();
			is.close();
			String wavFilePath = convertToWav(filePath);
			is = new FileInputStream(wavFilePath);
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE, wavFilePath);
			bs = new BufferedInputStream(is);
			throw new InstrumentException("Why am I here!!");
		}

		if (format.getSampleRate() != audioStream.getSampleRate()) {
			audioStream.setSampleRate((int) format.getSampleRate());
			LOG.severe(">>Start Audio file set sample rate: " + audioStream.getSampleRate());
		}
		try {
			audioStream.calibrateAudioFileStream(bs);
			bs.close();
			is.close();
		} catch (UnsupportedAudioFileException | IOException ex) {
			LOG.log(Level.SEVERE, "Audio file calibrate error:" + fileName, ex);
			throw new Exception("Audio file calibrate error: " + ex.getMessage());
		}

		filePath = parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE);

		LOG.severe(">>Start Audio file path: " + filePath);

		is = new FileInputStream(filePath);
		bs = new BufferedInputStream(is);

		try {
			audioStream.setAudioFileName(
					parameterManager.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_INPUT_FILE));
			audioStream.processAudioFileStream(bs, format);
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
	public String padAudio(String fileName) throws UnsupportedAudioFileException, IOException {
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}
		String appendedFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + "_padded_" + ".wav";
		String appendedFilePath = folder + System.getProperty("file.separator") + appendedFileName;
		File appendedFile = new File(appendedFilePath);

		int padBefore = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_BEFORE);
		int padAfter = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_PAD_AFTER);

		AudioInputStream sourceFileStream = AudioSystem.getAudioInputStream(new File(fileName));

		while (padBefore > 0) {
			InputStream ssis = getClass().getClassLoader().getResourceAsStream("secondSilence.wav");
			AudioInputStream secondSilenceStream = AudioSystem.getAudioInputStream(ssis);

			AudioInputStream appendedFileStream = new AudioInputStream(
					new SequenceInputStream(secondSilenceStream, sourceFileStream), sourceFileStream.getFormat(),
					secondSilenceStream.getFrameLength() + sourceFileStream.getFrameLength());
			AudioSystem.write(appendedFileStream, AudioFileFormat.Type.WAVE, appendedFile);
			ssis.close();
			secondSilenceStream.close();
			sourceFileStream.close();
			sourceFileStream = AudioSystem.getAudioInputStream(appendedFile);
			padBefore--;
		}

		while (padAfter > 0) {
			InputStream ssis = getClass().getClassLoader().getResourceAsStream("secondSilence.wav");
			AudioInputStream secondSilenceStream = AudioSystem.getAudioInputStream(ssis);

			AudioInputStream appendedFileStream = new AudioInputStream(
					new SequenceInputStream(sourceFileStream, secondSilenceStream), sourceFileStream.getFormat(),
					secondSilenceStream.getFrameLength() + sourceFileStream.getFrameLength());
			AudioSystem.write(appendedFileStream, AudioFileFormat.Type.WAVE, appendedFile);
			ssis.close();
			secondSilenceStream.close();
			sourceFileStream.close();
			sourceFileStream = AudioSystem.getAudioInputStream(appendedFile);
			padAfter--;
		}

		sourceFileStream.close();

		return appendedFilePath;
	}

	/**
	 * Invoke this function to convert to a playable file.
	 */
	public String convertToWav(String fileName) throws UnsupportedAudioFileException, IOException {
		BufferedInputStream bis = new BufferedInputStream(storage.getObjectStorage().read(fileName));
		AudioInputStream stream = AudioSystem.getAudioInputStream(bis);

		AudioFormat sourceFormat = stream.getFormat();
		// create audio format object for the desired stream/audio format
		// this is *not* the same as the file format (wav)
		AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16,
				sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
		// AudioFormat convertFormat = new AudioFormat(sourceFormat.getSampleRate(), 16,
		// 1, true, true);

		// create stream that delivers the desired format
		AudioInputStream converted = AudioSystem.getAudioInputStream(convertFormat, stream);
		// write stream into a file with file format wav
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}
		String wavFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + "converted_" + ".wav";
		String wavFilePath = folder + System.getProperty("file.separator") + wavFileName;
		File wavFile = new File(wavFilePath);
		AudioSystem.write(converted, AudioFileFormat.Type.WAVE, wavFile);
		bis.close();
		stream.close();
		return wavFilePath;
	}

	public String timeStretch(String fileName, double audioTimeStretch)
			throws UnsupportedAudioFileException, IOException {
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}
		String tsFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + "_stretched_" + ".wav";
		String tsFilePath = folder + System.getProperty("file.separator") + tsFileName;

		File inputFile = new File(fileName);
		AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();
		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(
				WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(audioTimeStretch, format.getSampleRate()));
		WaveformWriter writer = new WaveformWriter(format, tsFilePath);
		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(inputFile, wsola.getInputBufferSize(),
				wsola.getOverlap());
		wsola.setDispatcher(dispatcher);
		dispatcher.addAudioProcessor(wsola);
		dispatcher.addAudioProcessor(writer);
		dispatcher.run();
		return tsFilePath;
	}

	public String pitchShift(String fileName, double audioPitchShift)
			throws UnsupportedAudioFileException, IOException {
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}
		String psFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + "_shifted_" + ".wav";
		String psFilePath = folder + System.getProperty("file.separator") + psFileName;

		File inputFile = new File(fileName);
		AudioFormat format = AudioSystem.getAudioFileFormat(inputFile).getFormat();

		double sampleRate = format.getSampleRate();
		double factor = 1 / Math.pow(Math.E, audioPitchShift * Math.log(2) / 1200 / Math.log(Math.E));
		RateTransposer rateTransposer = new RateTransposer(factor);
		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(
				Parameters.musicDefaults(factor, sampleRate));

		WaveformWriter writer = new WaveformWriter(format, psFilePath);

		AudioDispatcher dispatcher;
		if (format.getChannels() != 1) {
			dispatcher = AudioDispatcherFactory.fromFile(inputFile, wsola.getInputBufferSize() * format.getChannels(),
					wsola.getOverlap() * format.getChannels());
			dispatcher.addAudioProcessor(new MultichannelToMono(format.getChannels(), true));
		} else {
			dispatcher = AudioDispatcherFactory.fromFile(inputFile, wsola.getInputBufferSize(), wsola.getOverlap());
		}
		wsola.setDispatcher(dispatcher);
		dispatcher.addAudioProcessor(wsola);
		dispatcher.addAudioProcessor(rateTransposer);
		dispatcher.addAudioProcessor(writer);
		dispatcher.run();

		return psFilePath;
	}

	public String cacheFile(String fileName) throws UnsupportedAudioFileException, IOException {
		LOG.severe(">>cacheFile: " + fileName);
		BufferedInputStream bis = new BufferedInputStream(storage.getObjectStorage().read(fileName));
		AudioInputStream stream = AudioSystem.getAudioInputStream(bis);

		LOG.severe(">>cacheFile 2: " + fileName);

		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}

		String cacheFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + ".wav";
		String cacheFilePath = folder + System.getProperty("file.separator") + "cache_" + cacheFileName;
		File cacheFile = new File(cacheFilePath);
		AudioSystem.write(stream, AudioFileFormat.Type.WAVE, cacheFile);
		bis.close();
		stream.close();
		return cacheFilePath;
	}

	public String resample(String fileName) throws UnsupportedAudioFileException, IOException {
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));
		AudioFormat sourceFormat = audioInputStream.getFormat();
		AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 192000,
				sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), sourceFormat.getFrameSize(),
				sourceFormat.getFrameRate(), sourceFormat.isBigEndian());

		AudioInputStream inputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);

		// write stream into a file with file format wav
		String baseDir = storage.getObjectStorage().getBasePath();
		String folder = Paths
				.get(baseDir,
						parameterManager
								.getParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RECORD_DIRECTORY))
				.toString();
		int startIndex = 0;
		if (fileName.lastIndexOf("/") != -1) {
			startIndex = fileName.lastIndexOf("/") + 1;
		} else if (fileName.lastIndexOf("\\") != -1) {
			startIndex = fileName.lastIndexOf("\\") + 1;
		}
		String rsFileName = fileName.substring(startIndex, fileName.lastIndexOf(".")) + ".wav";
		String rsFilePath = folder + System.getProperty("file.separator") + "resampled_" + rsFileName;
		File file = new File(rsFilePath);
		AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, file);

		return rsFilePath;
	}

	public void startAudioLineStream(String recordFile) throws LineUnavailableException, IOException {
		if (getStreamId() != null) {
			LOG.severe(">>HEARING startAudioLineStream clear old stream: " + getStreamId());
			workspace.getAtlas().removeMapsByStreamId(getStreamId());
			voice.reset();
			System.gc();
			LOG.severe(">>HEARING startAudioLineStream cleared old stream: " + getStreamId());
		}
		workspace.getAtlas().clear();

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
		LOG.finer(">>Start Audio Stream: " + getStreamId());
		AudioStream audioStream = new AudioStream(getStreamId());
		audioStreams.put(getStreamId(), audioStream);

		audioStream.processMicrophoneStream(recordFile);

		audioStream.getAudioFeatureProcessor().addObserver(cortex);
		audioStream.getAudioFeatureProcessor().addObserver(console.getVisor());
		audioStream.start();
	}

	public void stopAudioStream() {
		LOG.severe(">>Stop Audio Stream: " + getStreamId());
		AudioStream audioStream = audioStreams.get(getStreamId());
		if (audioStream != null) {
			audioStream.stop();
			if (audioStream.getAudioFeatureProcessor() != null) {
				audioStream.getAudioFeatureProcessor().removeObserver(cortex);
			}
			closeAudioStream(getStreamId());
			// coordinator.getVoice().clear(streamId);
		}
		if (bs != null) {
			try {
				bs.close();
				is.close();
				LOG.finer(">>Close Audio Stream: " + getStreamId());
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Exception closig " + getStreamId(), e);
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
		private String audioFileName;

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

		public String getAudioFileName() {
			return audioFileName;
		}

		public void setAudioFileName(String audioFileName) {
			this.audioFileName = audioFileName;
		}

		public int getBufferSize() {
			return bufferSize;
		}

		public void setSampleRate(int sampleRate) {
			this.sampleRate = sampleRate;
			parameterManager.setParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_SAMPLE_RATE,
					Integer.toString(sampleRate));
			console.getVisor().updateParameters();

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

			dispatcher = audioDispatcherFactory.getAudioDispatcher(audioStream, bufferSize, overlap);
			int audioHighPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			int audioLowPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
			if (audioHighPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add High pass: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.highPassFilter(convertFloatsToDoubles(values), order, audioHighPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});
			}
			if (audioLowPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add Low pass: " + audioLowPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.lowPassFilter(convertFloatsToDoubles(values), order, audioLowPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			int smoothFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR);
			if (smoothFactor > 0) {
				LOG.severe(">>Hearing add Smooth: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						String mode = "triangular";
						Smooth s1 = new Smooth(convertFloatsToDoubles(values), smoothFactor, mode);
						double[] result = s1.smoothSignal();
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			float pidPFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR);
			float pidDFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR);
			float pidIFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR);

			if (pidPFactor > 0 || pidDFactor > 0 || pidIFactor > 0) {
				dispatcher.addAudioProcessor(new PidProcessor(pidPFactor, pidDFactor, pidIFactor));
			}

			int range = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE);

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

					double startTimeMS = audioEvent.getTimeStamp() * 1000;
					if (startTimeMS > range) {
						dispatcher.stop();
						return false;
					}
					return true;
				}

				@Override
				public void processingFinished() {

				}
			});
			double threshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_THRESHOLD);
			double onsetInterval = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_INTERVAL);
			double onsetSilenceThreshold = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_ONSET_SILENCE_THRESHOLD);

			ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, threshold, onsetInterval,
					onsetSilenceThreshold);
			BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
			detector.setHandler(handler);

			dispatcher.addAudioProcessor(detector);
			dispatcher.run();
			LOG.finer(">>Calibrate audio file");
			dispatcher.run();
			LOG.finer(">>Calibrated audio file");
			handler.trackBeats(calibrationMap);

		}

		private void processAudioFileStream(BufferedInputStream inputStream, AudioFormat format)
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
			dispatcher = audioDispatcherFactory.getAudioDispatcher(audioStream, bufferSize, overlap);

			int audioHighPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			int audioLowPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
			if (audioHighPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add High pass: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.highPassFilter(convertFloatsToDoubles(values), order, audioHighPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});
			}
			if (audioLowPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add Low pass: " + audioLowPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.lowPassFilter(convertFloatsToDoubles(values), order, audioLowPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			double gainCompressFactor = parameterManager
					.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_GAIN_COMPRESS_FACTOR);

			if (gainCompressFactor > 0) {
				HaarWaveletCoder coder = new HaarWaveletCoder();
				HaarWaveletDecoder decoder = new HaarWaveletDecoder();
				GainProcessor gain = new GainProcessor(gainCompressFactor);
				BitDepthProcessor bithDeptProcessor = new BitDepthProcessor();
				bithDeptProcessor.setBitDepth(format.getSampleSizeInBits());
				dispatcher.addAudioProcessor(gain);
			}

			int smoothFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR);
			if (smoothFactor > 0) {
				LOG.severe(">>Hearing add Smooth: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						String mode = "triangular";
						Smooth s1 = new Smooth(convertFloatsToDoubles(values), smoothFactor, mode);
						double[] result = s1.smoothSignal();
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			boolean pidSwitch = parameterManager
					.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_SWITCH);
			float pidPFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR);
			float pidDFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR);
			float pidIFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR);

			if (pidSwitch && (pidPFactor != 0 || pidDFactor != 0 || pidIFactor != 0)) {
				dispatcher.addAudioProcessor(new PidProcessor(pidPFactor, pidDFactor, pidIFactor));
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
				LOG.severe(">>processMicrophoneStream writer: " + recordFile);
			}

			int audioHighPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_HIGHPASS);
			int audioLowPass = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_LOWPASS);
			if (audioHighPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add High pass: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.highPassFilter(convertFloatsToDoubles(values), order, audioHighPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});
			}
			if (audioLowPass > 0) {
				int order = 1; // order of the filter
				Butterworth flt = new Butterworth(sampleRate); // signal is of type double[]

				LOG.severe(">>Hearing add Low pass: " + audioLowPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						double[] result = flt.lowPassFilter(convertFloatsToDoubles(values), order, audioLowPass);
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			int smoothFactor = parameterManager
					.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_SMOOTH_FACTOR);
			if (smoothFactor > 0) {
				LOG.severe(">>Hearing add Smooth: " + audioHighPass);

				dispatcher.addAudioProcessor(new AudioProcessor() {

					@Override
					public boolean process(AudioEvent audioEvent) {
						float[] values = audioEvent.getFloatBuffer();
						String mode = "triangular";
						Smooth s1 = new Smooth(convertFloatsToDoubles(values), smoothFactor, mode);
						double[] result = s1.smoothSignal();
						audioEvent.setFloatBuffer(convertDoublesToFloat(result));
						return true;
					}

					@Override
					public void processingFinished() {

					}
				});

			}

			float pidPFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_P_FACTOR);
			float pidDFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_D_FACTOR);
			float pidIFactor = parameterManager
					.getFloatParameter(InstrumentParameterNames.PERCEPTION_HEARING_PID_I_FACTOR);

			if (pidPFactor > 0 || pidDFactor > 0 || pidIFactor > 0) {
				dispatcher.addAudioProcessor(new PidProcessor(pidPFactor, pidDFactor, pidIFactor));
			}

			int range = parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AUDIO_RANGE);

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

					double startTimeMS = audioEvent.getTimeStamp() * 1000;
					if (startTimeMS > range) {
						dispatcher.stop();
						return false;
					}
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
		stopAudioPlayer();
		stopAudioStream();
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		stopAudioStream();
	}

	final static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	final static float[] convertDoublesToFloat(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
	}

	public String getStreamId() {
		return streamId;
	}

	public boolean startAudioPlayer() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		if (streamId == null) {
			return false;
		}
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null || audioStream.getAudioFileName() == null) {
			return false;
		}
		stopAudioPlayer();
		File file = new File(audioStream.getAudioFileName());
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
		AudioFormat format = fileFormat.getFormat();

		GainProcessor gainProcessor = new GainProcessor(1.0);
		AudioPlayer audioPlayer = new AudioPlayer(format);

		audioPlayerDispatcher = AudioDispatcherFactory.fromFile(file, audioStream.getBufferSize(), 0);
		audioPlayerDispatcher.addAudioProcessor(gainProcessor);
		audioPlayerDispatcher.addAudioProcessor(audioPlayer);

		Thread t = new Thread(audioPlayerDispatcher, "Audio Player Thread");
		t.start();
		return true;
	}

	public void stopAudioPlayer() {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null || audioStream.getAudioFileName() == null || audioPlayerDispatcher == null
				|| audioPlayerDispatcher.isStopped()) {
			return;
		}
		audioPlayerDispatcher.stop();
	}

}
