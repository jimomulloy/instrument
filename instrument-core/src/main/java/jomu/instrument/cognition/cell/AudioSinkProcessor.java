package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentException;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioSinkProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioSinkProcessor.class.getName());

	public AudioSinkProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioSinkProcessor accept: " + sequence + ", streamId: " + streamId);

		int persistenceMode = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_TONEMAP_PERSISTENCE_MODE);
		boolean pausePlay = parameterManager
				.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_PAUSE_PLAY_SWITCH);
		boolean saveToneMap = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_STREAM_SAVE_SWITCH);
		int sinkSweepRange = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_SINK_SWEEP_RANGE);

		Voice voice = Instrument.getInstance().getCoordinator().getVoice();
		ToneMap synthesisToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));

		console.getVisor().updateSpectrumView(cqToneMap.getTimeFrame(),
				parameterManager.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_DEFAULT_WINDOW));
		ToneTimeFrame synthesisFrame = null;

		int tmIndex = sequence - sinkSweepRange;
		if (tmIndex > 0) {
			synthesisFrame = synthesisToneMap.getTimeFrame(tmIndex);
			if (synthesisFrame != null) {
				voice.send(synthesisFrame, streamId, tmIndex, pausePlay);
			}
			if (persistenceMode < 3) {
				commitMaps(streamId, tmIndex, persistenceMode);
			}
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				synthesisFrame = synthesisToneMap.getTimeFrame(i);
				if (synthesisFrame != null) {
					voice.send(synthesisFrame, streamId, i, pausePlay);
				}
				if (persistenceMode < 3) {
					commitMaps(streamId, i, persistenceMode);
				}
			}
			LOG.severe(">>AudioSinkProcessor last seq: " + sequence);
			final ToneMap synthToneMap = this.workspace.getAtlas()
					.getToneMap(ToneMap.buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId));
			if (synthToneMap != null && saveToneMap) {
				this.workspace.getAtlas().saveToneMap(synthToneMap);
			}

			new Thread(() -> {
				voice.close(streamId);
				if (synthToneMap != null && this.parameterManager
						.getBooleanParameter(InstrumentParameterNames.ACTUATION_VOICE_LOOP_SAVE)) {
					voice.startStreamPlayer(streamId, synthToneMap);
				} else if (parameterManager
						.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_COUNT) > 0) {
					try {
						hearing.replayAudioStream(streamId);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, ">>AudioSinkProcessor hearing.replayAudioStream", e);
					}
				}
			}).start();
			// console.getVisor().updateViewThresholds();
			LOG.severe(">>AudioSinkProcessor CLOSE: " + saveToneMap);
		}
	}

	private void commitMaps(String streamId, int sequence, int persistenceMode) {
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_BEAT, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CEPSTRUM, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CEPSTRUM, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_MICRO_TONE, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_MICRO_TONE, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_HPS, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_HARMONIC_MASK", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_HARMONIC_MASK", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas()
				.hasToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_PERCUSSION_MASK", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_PERCUSSION_MASK", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_PERCUSSION", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_PERCUSSION", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_HARMONIC", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS.name() + "_HARMONIC", streamId))
					.commit(sequence);
		}
		// if
		// (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE,
		// streamId))) {
		// workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE,
		// streamId)).commit(sequence);
		// }
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE.name() + "_PEAKS", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE.name() + "_PEAKS", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas()
				.hasToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE.name() + "_SPECTRAL", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_INTEGRATE.name() + "_SPECTRAL", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_MFCC, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_MFCC, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_ONSET.name() + "_SMOOTHED", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_PERCUSSION, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PERCUSSION, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PITCH, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_POST_CHROMA, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_PRE_CHROMA, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_SACF, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SACF, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SPECTRAL_PEAKS, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_TUNER_PEAKS, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_YIN, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_YIN, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE, streamId)).commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.name() + "_PEAKS", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.name() + "_PEAKS", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.name() + "_SPECTRAL", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_NOTATE.name() + "_SPECTRAL", streamId))
					.commit(sequence);
		}
		// if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
		// streamId))) {
		// workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ,
		// streamId)).commit(sequence);
		// }
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ.name() + "_ENVELOPE", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ.name() + "_ENVELOPE", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ.name() + "_WHITENER", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ.name() + "_WHITENER", streamId))
					.commit(sequence);
		}
		if (workspace.getAtlas()
				.hasToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN.name() + "_WHITENER", streamId))) {
			workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN.name() + "_WHITENER", streamId))
					.commit(sequence);
		}
		if (persistenceMode < 2) {
			if (workspace.getAtlas().hasToneMap(buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId))) {
				workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_SYNTHESIS, streamId)).commit(sequence);
			}
		}
	}
}
