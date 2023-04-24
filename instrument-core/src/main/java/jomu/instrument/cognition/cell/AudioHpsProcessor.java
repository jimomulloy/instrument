package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.logging.Logger;

import jomu.instrument.InstrumentException;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioHpsProcessor extends ProcessorCommon {

	private static final Logger LOG = Logger.getLogger(AudioHpsProcessor.class.getName());

	public AudioHpsProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws InstrumentException {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		LOG.finer(">>AudioHpsProcessor accept: " + sequence + ", streamId: " + streamId);
		int hpsHarmonicMedian = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_MEDIAN);
		int hpsPercussionMedian = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_MEDIAN);
		int hpsHarmonicWeighting = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_HARMONIC_WEIGHTING);
		int hpsPercussionWeighting = parameterManager
				.getIntParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_PERCUSSION_WEIGHTING);
		double hpsMaskFactor = parameterManager
				.getDoubleParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_MASK_FACTOR);
		boolean hpsMedianSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_SWITCH_MEDIAN);
		boolean hpsCQOriginSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_CQ_ORIGIN_SWITCH);

		ToneMap hpsToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(this.cell.getCellType(), streamId));

		ToneMap hpsHarmonicToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC", streamId));
		ToneMap hpsPercussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION", streamId));
		ToneMap hpsHarmonicMaskedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC_MASK", streamId));
		ToneMap hpsPercussionMaskedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION_MASK", streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));
		if (hpsCQOriginSwitch) {
			cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ_ORIGIN, streamId));
		}
		hpsToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsHarmonicToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsPercussionToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsHarmonicMaskedToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsPercussionMaskedToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());

		hpsPercussionToneMap.getTimeFrame().hpsPercussionMedian(hpsPercussionMedian, hpsMedianSwitch);

		int tmIndex = sequence - hpsHarmonicMedian / 2;
		if (tmIndex > 0) {
			ToneTimeFrame hpsHarmonicTimeFrame = hpsHarmonicToneMap.getTimeFrame(tmIndex);
			ToneTimeFrame hpsPercussionTimeFrame = hpsPercussionToneMap.getTimeFrame(tmIndex);
			hpsHarmonicToneMap.getTimeFrame(tmIndex).hpsHarmonicMedian(cqToneMap, sequence, hpsHarmonicMedian,
					hpsMedianSwitch);
			hpsToneMap.getTimeFrame(tmIndex).hpsMask(hpsHarmonicTimeFrame, hpsPercussionTimeFrame,
					(double) hpsHarmonicWeighting / 100.0, (double) hpsPercussionWeighting / 100.0);
			hpsHarmonicMaskedToneMap.getTimeFrame(tmIndex).hpsHarmonicMask(hpsHarmonicTimeFrame, hpsPercussionTimeFrame,
					hpsMaskFactor);
			hpsPercussionMaskedToneMap.getTimeFrame(tmIndex).hpsPercussionMask(hpsHarmonicTimeFrame,
					hpsPercussionTimeFrame, hpsMaskFactor);
			console.getVisor().updateToneMapView(hpsHarmonicToneMap, hpsHarmonicToneMap.getTimeFrame(tmIndex),
					this.cell.getCellType().toString() + "_HARMONIC");
			console.getVisor().updateToneMapView(hpsHarmonicMaskedToneMap,
					hpsHarmonicMaskedToneMap.getTimeFrame(tmIndex),
					this.cell.getCellType().toString() + "_HARMONIC_MASK");
			console.getVisor().updateToneMapView(hpsPercussionMaskedToneMap,
					hpsPercussionMaskedToneMap.getTimeFrame(tmIndex),
					this.cell.getCellType().toString() + "_PERCUSSION_MASK");
			console.getVisor().updateToneMapView(hpsToneMap, hpsToneMap.getTimeFrame(tmIndex),
					this.cell.getCellType().toString());
			cell.send(streamId, tmIndex);
		}

		if (isClosing(streamId, sequence)) {
			if (tmIndex < 0) {
				tmIndex = 0;
			}
			for (int i = tmIndex + 1; i <= sequence; i++) {
				ToneTimeFrame hpsHarmonicTimeFrame = hpsHarmonicToneMap.getTimeFrame(i);
				if (hpsHarmonicTimeFrame != null) {
					ToneTimeFrame hpsPercussionTimeFrame = hpsPercussionToneMap.getTimeFrame(i);
					hpsHarmonicToneMap.getTimeFrame(i).hpsHarmonicMedian(cqToneMap, sequence, hpsHarmonicMedian,
							hpsMedianSwitch);
					hpsToneMap.getTimeFrame(i).hpsMask(hpsHarmonicTimeFrame, hpsPercussionTimeFrame,
							(double) hpsHarmonicWeighting / 100.0, (double) hpsPercussionWeighting / 100.0);
					hpsHarmonicMaskedToneMap.getTimeFrame(i).hpsHarmonicMask(hpsHarmonicTimeFrame,
							hpsPercussionTimeFrame, hpsMaskFactor);
					hpsPercussionMaskedToneMap.getTimeFrame(i).hpsPercussionMask(hpsHarmonicTimeFrame,
							hpsPercussionTimeFrame, hpsMaskFactor);
					console.getVisor().updateToneMapView(hpsHarmonicToneMap, hpsHarmonicToneMap.getTimeFrame(i),
							this.cell.getCellType().toString() + "_HARMONIC");
					console.getVisor().updateToneMapView(hpsHarmonicMaskedToneMap,
							hpsHarmonicMaskedToneMap.getTimeFrame(i),
							this.cell.getCellType().toString() + "_HARMONIC_MASK");
					console.getVisor().updateToneMapView(hpsPercussionMaskedToneMap,
							hpsPercussionMaskedToneMap.getTimeFrame(i),
							this.cell.getCellType().toString() + "_PERCUSSION_MASK");
					console.getVisor().updateToneMapView(hpsToneMap, hpsToneMap.getTimeFrame(i),
							this.cell.getCellType().toString());
				}
				cell.send(streamId, i);
			}
		}
		//
		console.getVisor().updateToneMapView(hpsHarmonicToneMap, this.cell.getCellType().toString() + "_HARMONIC");
		console.getVisor().updateToneMapView(hpsPercussionToneMap, this.cell.getCellType().toString() + "_PERCUSSION");
		console.getVisor().updateToneMapView(hpsHarmonicMaskedToneMap,
				this.cell.getCellType().toString() + "_HARMONIC_MASK");
		console.getVisor().updateToneMapView(hpsPercussionMaskedToneMap,
				this.cell.getCellType().toString() + "_PERCUSSION_MASK");
		console.getVisor().updateToneMapView(hpsToneMap, this.cell.getCellType().toString());

	}
}
