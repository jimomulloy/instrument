package jomu.instrument.organs;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import jomu.instrument.Instrument;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.TarsosFeatureSource;
import net.beadsproject.beads.core.AudioContext;

public class Hearing implements Organ {

	private String streamId;

	private ConcurrentHashMap<String, AudioStream> audioStreams = new ConcurrentHashMap<>();

	public void closeAudioStream(String streamId) {
		AudioStream audioStream = audioStreams.get(streamId);
		if (audioStream == null) {
			return;
		}
		audioStream.close();
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
			throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		System.out.println(">>!!hearing initialise: " + streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseAudioFileStream(fileName);

		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().addObserver(
				Instrument.getInstance().getCoordinator().getCortex());
		audioStream.start();
	}

	public void startAudioLineStream() throws LineUnavailableException {
		streamId = UUID.randomUUID().toString();
		AudioStream audioStream = new AudioStream(streamId);
		System.out.println(">>!!hearing initialise: " + streamId);
		audioStreams.put(streamId, audioStream);

		audioStream.initialiseMicrophoneStream();

		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().addObserver(
				Instrument.getInstance().getCoordinator().getCortex());
		audioStream.start();
	}

	public void stopAudioLineStream() throws LineUnavailableException {
		AudioStream audioStream = audioStreams.get(streamId);
		audioStream.stop();

		Instrument.getInstance().getCoordinator().getCortex();
		audioStream.getAudioFeatureProcessor().removeObserver(
				Instrument.getInstance().getCoordinator().getCortex());

		closeAudioStream(streamId);

	}

	private class AudioStream {

		private AudioContext ac;
		private AudioFeatureProcessor audioFeatureProcessor;
		private String streamId;
		private TarsosFeatureSource tarsosFeatureSource;
		private int sampleRate = 44100;
		private int bufferSize = 1024 * 4;
		private int overlap = 768 * 4;
		private AudioDispatcher dispatcher;

		public AudioStream(String streamId) {
			this.streamId = streamId;
		}

		public void close() {
			if (ac != null) {
				ac.stop();
				ac = null;
			} else if (dispatcher != null) {
				// dispatcher.stop();
				dispatcher = null;
			}
		}

		public AudioContext getAc() {
			return ac;
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

		public void initialiseAudioFileStream(String fileName)
				throws UnsupportedAudioFileException, IOException,
				LineUnavailableException {
			Instrument.getInstance().getDruid().getVisor().clearView();
			// tarsosIO.selectMixer(2);
			File file = new File(fileName);
			dispatcher = AudioDispatcherFactory.fromFile(file, bufferSize,
					overlap);
			AudioFormat format = AudioSystem.getAudioFileFormat(file)
					.getFormat();
			// dispatcher.addAudioProcessor(new AudioPlayer(format));

			// Oscilloscope oscilloscope = new
			// Oscilloscope(Instrument.getInstance().getDruid().getOscilloscopeHandler());
			// tarsosIO.getDispatcher().addAudioProcessor(oscilloscope);

			tarsosFeatureSource = new TarsosFeatureSource(dispatcher);
			tarsosFeatureSource.initialise();
			audioFeatureProcessor = new AudioFeatureProcessor(streamId,
					tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(10000); // TODO!!

			dispatcher.addAudioProcessor(audioFeatureProcessor);

		}

		public void initialiseMicrophoneStream()
				throws LineUnavailableException {
			AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
			final DataLine.Info dataLineInfo = new DataLine.Info(
					TargetDataLine.class, format);
			TargetDataLine line;
			// line = (TargetDataLine) mixer.getLine(dataLineInfo);
			line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			final int numberOfSamples = bufferSize;
			line.open(format, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);

			JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
			// create a new dispatcher
			dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);
			tarsosFeatureSource = new TarsosFeatureSource(dispatcher);
			tarsosFeatureSource.initialise();
			audioFeatureProcessor = new AudioFeatureProcessor(streamId,
					tarsosFeatureSource);
			audioFeatureProcessor.setMaxFrames(10000); // TODO!!

			dispatcher.addAudioProcessor(audioFeatureProcessor);

		}

		public void start() {
			if (ac != null) {
				ac.start();
			} else if (dispatcher != null) {
				new Thread(dispatcher, "Audio dispatching").start();
			}
		}

		public void stop() {
			if (ac != null) {
				ac.stop();
			} else if (dispatcher != null) {
				dispatcher.stop();
			}
		}

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
