package jomu.instrument.cognition;

import javax.enterprise.context.ApplicationScoped;

import org.springframework.stereotype.Component;

import jomu.instrument.Instrument;
import jomu.instrument.Organ;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureFrameObserver;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.Generator;
import jomu.instrument.cognition.cell.NuCell;
import jomu.instrument.cognition.cell.Weaver;
import jomu.instrument.control.ParameterManager;

@ApplicationScoped
@Component
public class Cortex implements Organ, AudioFeatureFrameObserver {

	private NuCell sourceAddCell;
	private NuCell sourceUpdateCell;
	private ParameterManager parameterManager;

	@Override
	public void audioFeatureFrameAdded(AudioFeatureFrame audioFeatureFrame) {
		sourceAddCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
				audioFeatureFrame.getFrameSequence());
	}

	@Override
	public void audioFeatureFrameChanged(AudioFeatureFrame audioFeatureFrame) {
		if (audioFeatureFrame.getConstantQFeatures().isCommitted()) {
			sourceUpdateCell.send(audioFeatureFrame.getAudioFeatureProcessor().getStreamId(),
					audioFeatureFrame.getFrameSequence());
		}
	}

	@Override
	public void initialise() {
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
		sourceAddCell = Generator.createNuCell(CellTypes.SOURCE);
		sourceUpdateCell = Generator.createNuCell(CellTypes.SOURCE);
		NuCell audioCQCell = Generator.createNuCell(CellTypes.AUDIO_CQ);
		NuCell audioBeatCell = Generator.createNuCell(CellTypes.AUDIO_BEAT);
		NuCell audioPitchCell = Generator.createNuCell(CellTypes.AUDIO_PITCH);
		NuCell audioSpectralPeaksCell = Generator.createNuCell(CellTypes.AUDIO_SPECTRAL_PEAKS);
		NuCell audioPreChromaCell = Generator.createNuCell(CellTypes.AUDIO_PRE_CHROMA);
		NuCell audioPostChromaCell = Generator.createNuCell(CellTypes.AUDIO_POST_CHROMA);
		NuCell audioIntegrateCell = Generator.createNuCell(CellTypes.AUDIO_INTEGRATE);
		NuCell audioNotateCell = Generator.createNuCell(CellTypes.AUDIO_NOTATE);
		NuCell audioSinkCell = Generator.createNuCell(CellTypes.AUDIO_SINK);
		NuCell audioTunerPeaksCell = Generator.createNuCell(CellTypes.AUDIO_TUNER_PEAKS);
		Weaver.connect(audioCQCell, audioTunerPeaksCell);
		Weaver.connect(audioCQCell, audioPreChromaCell);
		Weaver.connect(audioCQCell, audioIntegrateCell);
		Weaver.connect(audioPreChromaCell, audioPostChromaCell);
		// Weaver.connect(audioSpectralPeaksCell, audioIntegrateCell);
		// Weaver.connect(audioSpectrumCell, audioIntegrateCell);
		Weaver.connect(audioTunerPeaksCell, audioIntegrateCell);
		Weaver.connect(audioPostChromaCell, audioIntegrateCell);
		Weaver.connect(audioIntegrateCell, audioNotateCell);
		Weaver.connect(audioNotateCell, audioSinkCell);
		Weaver.connect(audioBeatCell, audioSinkCell);
		Weaver.connect(audioPitchCell, audioSinkCell);
		Weaver.connect(audioSpectralPeaksCell, audioSinkCell);
		Weaver.connect(sourceUpdateCell, audioCQCell);
		Weaver.connect(sourceUpdateCell, audioBeatCell);
		Weaver.connect(sourceUpdateCell, audioPitchCell);
		Weaver.connect(sourceUpdateCell, audioSpectralPeaksCell);
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
