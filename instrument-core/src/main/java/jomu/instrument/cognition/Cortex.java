package jomu.instrument.cognition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jomu.instrument.InstrumentException;
import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.ProcessorExceptionHandler;
import jomu.instrument.cognition.cell.Weaver;
import jomu.instrument.control.Coordinator;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.perception.Hearing;

@ApplicationScoped
public class Cortex implements Organ, AudioFeatureFrameObserver, ProcessorExceptionHandler<InstrumentException> {

	private static final Logger LOG = Logger.getLogger(Cortex.class.getName());

	@Inject
	Coordinator coordinator;

	@Inject
	Hearing hearing;

	@Inject
	Voice voice;

	@Inject
	ParameterManager parameterManager;

	NuCell sourceAddCell;
	NuCell sourceUpdateCell;
	NuCell audioCQCell;
	NuCell audioCQOriginCell;
	NuCell audioBeatCell;
	NuCell audioOnsetCell;
	NuCell audioPercussionCell;
	NuCell audioHpsCell;
	NuCell audioPitchCell;
	NuCell audioPhaseCell;
	NuCell audioSpectralPeaksCell;
	NuCell audioPreChromaCell;
	NuCell audioPostChromaCell;
	NuCell audioIntegrateCell;
	NuCell audioNotateCell;
	NuCell audioSinkCell;
	NuCell audioTunerPeaksCell;
	NuCell audioYINCell;
	NuCell audioSACFCell;
	NuCell audioMFCCCell;
	NuCell audioCepstrumCell;
	NuCell audioSynthesisCell;

	NuCell[] cells;

	private boolean isReplaying;

	private boolean isRestarting;

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		sourceAddCell.send(audioFeatureFrame.getAudioFeatureProcessor()
				.getStreamId(), audioFeatureFrame.getFrameSequence());
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		if (audioFeatureFrame.getConstantQFeatures()
				.isCommitted()) {
			sourceUpdateCell.send(audioFeatureFrame.getAudioFeatureProcessor()
					.getStreamId(), audioFeatureFrame.getFrameSequence());
		}
	}

	@Override
	public void initialise() {
		boolean cortexShortCircuit = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_CORTEX_SHORT_CIRCUIT_SWITCH);
		List<NuCell> cellList = new ArrayList<>();
		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		cellList.add(sourceAddCell);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		cellList.add(sourceUpdateCell);
		audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		cellList.add(audioCQCell);
		audioCQOriginCell = Generator.createNuCell(CellTypes.AUDIO_CQ_ORIGIN);
		cellList.add(audioCQOriginCell);
		audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		cellList.add(audioBeatCell);
		audioOnsetCell = Generator.createNuCell(CellTypes.AUDIO_ONSET);
		cellList.add(audioOnsetCell);
		audioPercussionCell = Generator.createNuCell(CellTypes.AUDIO_PERCUSSION);
		cellList.add(audioPercussionCell);
		audioHpsCell = Generator.createNuCell(CellTypes.AUDIO_HPS);
		cellList.add(audioHpsCell);
		audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		cellList.add(audioPitchCell);
		audioPhaseCell = Generator.createNuCell(CellTypes.AUDIO_PHASE);
		cellList.add(audioPitchCell);
		audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		cellList.add(audioSpectralPeaksCell);
		audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		cellList.add(audioPreChromaCell);
		audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		cellList.add(audioPostChromaCell);
		audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		cellList.add(audioIntegrateCell);
		audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		cellList.add(audioNotateCell);
		audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		cellList.add(audioSinkCell);
		audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		cellList.add(audioTunerPeaksCell);
		audioYINCell = Generator.createNuCell(CellTypes.AUDIO_YIN);
		cellList.add(audioYINCell);
		audioSACFCell = Generator.createNuCell(CellTypes.AUDIO_SACF);
		cellList.add(audioSACFCell);
		audioMFCCCell = Generator.createNuCell(CellTypes.AUDIO_MFCC);
		cellList.add(audioMFCCCell);
		audioCepstrumCell = Generator.createNuCell(CellTypes.AUDIO_CEPSTRUM);
		cellList.add(audioCepstrumCell);
		audioSynthesisCell = Generator.createNuCell(CellTypes.AUDIO_SYNTHESIS);
		cellList.add(audioSynthesisCell);
		//
		cells = cellList.toArray(new NuCell[cellList.size()]);
		setProcessorExceptionHandlers();

		Weaver.connect(sourceUpdateCell, audioCQCell);
		Weaver.connect(audioCQCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		Weaver.connect(audioNotateCell, audioSynthesisCell);
		Weaver.connect(audioSynthesisCell, audioSinkCell);

		if (!cortexShortCircuit) {
			Weaver.connect(sourceUpdateCell, audioBeatCell);
			Weaver.connect(sourceUpdateCell, audioPercussionCell);
			Weaver.connect(sourceUpdateCell, audioPitchCell);
			Weaver.connect(sourceUpdateCell, audioPhaseCell);
			Weaver.connect(sourceUpdateCell, audioYINCell);
			Weaver.connect(sourceUpdateCell, audioCQOriginCell);
			Weaver.connect(sourceUpdateCell, audioSpectralPeaksCell);
			Weaver.connect(sourceUpdateCell, audioSACFCell);
			Weaver.connect(sourceUpdateCell, audioCepstrumCell);
			Weaver.connect(sourceUpdateCell, audioMFCCCell);
			Weaver.connect(sourceUpdateCell, audioSinkCell);
			//
			Weaver.connect(audioCQCell, audioPreChromaCell);
			Weaver.connect(audioCQOriginCell, audioPreChromaCell);
			Weaver.connect(audioCQCell, audioHpsCell);
			Weaver.connect(audioCQOriginCell, audioHpsCell);
			Weaver.connect(audioCQCell, audioOnsetCell);
			Weaver.connect(audioCQOriginCell, audioOnsetCell);
			Weaver.connect(audioCQCell, audioTunerPeaksCell);
			//
			Weaver.connect(audioHpsCell, audioOnsetCell);
			Weaver.connect(audioHpsCell, audioPreChromaCell);
			//
			Weaver.connect(audioHpsCell, audioIntegrateCell);
			Weaver.connect(audioOnsetCell, audioIntegrateCell);
			Weaver.connect(audioBeatCell, audioIntegrateCell);
			Weaver.connect(audioPercussionCell, audioIntegrateCell);
			Weaver.connect(audioPitchCell, audioIntegrateCell);
			Weaver.connect(audioPhaseCell, audioIntegrateCell);
			Weaver.connect(audioYINCell, audioIntegrateCell);
			Weaver.connect(audioSpectralPeaksCell, audioIntegrateCell);
			Weaver.connect(audioSACFCell, audioIntegrateCell);
			Weaver.connect(audioMFCCCell, audioIntegrateCell);
			Weaver.connect(audioCepstrumCell, audioIntegrateCell);
			Weaver.connect(audioTunerPeaksCell, audioIntegrateCell);
			//
			Weaver.connect(audioPreChromaCell, audioPostChromaCell);
			Weaver.connect(audioPostChromaCell, audioSynthesisCell);
		}
	}

	private void setProcessorExceptionHandlers() {
		for (NuCell cell : cells) {
			cell.setProcessorExceptionHandler(this);
		}
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleException(InstrumentException exception) {
		LOG.severe(">>!!Cortex handleException X");
		if (parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_AI_SEARCH_COUNT) > 0) {
			if (!isRestarting) {
				LOG.severe(">>!!Cortex restarting wait");
				isRestarting = true;
				processException(exception);
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
				LOG.severe(">>!!Cortex restarting runner");
				new Thread(() -> {
					voice.close(hearing.getStreamId());
					for (NuCell cell : cells) {
						cell.reset();
					}
					try {
						hearing.replayAudioStream(hearing.getStreamId(), true);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, ">>Cortex - hearing.replayAudioStream", e);
					}
					isRestarting = false;
				}).start();
			}
		} else {
			coordinator.handleException(exception);
		}
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		for (NuCell cell : cells) {
			cell.stop();
		}
		LOG.severe(">>!!Cortex processException Y");
	}
}
