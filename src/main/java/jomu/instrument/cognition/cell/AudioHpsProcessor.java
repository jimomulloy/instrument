package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

public class AudioHpsProcessor extends ProcessorCommon {

	public AudioHpsProcessor(NuCell cell) {
		super(cell);
	}

	@Override
	public void accept(List<NuMessage> messages) throws Exception {
		String streamId = getMessagesStreamId(messages);
		int sequence = getMessagesSequence(messages);
		System.out.println(">>AudioHpsProcessor accept: " + sequence + ", streamId: " + streamId);
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

		ToneMap hpsHarmonicToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC", streamId));
		ToneMap hpsPercussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION", streamId));
		ToneMap hpsHarmonicMaskedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_HARMONIC_MASK", streamId));
		ToneMap hpsPercussionMaskedToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + "_PERCUSSION_MASK", streamId));
		ToneMap hpsToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS, streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));

		hpsToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsHarmonicToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsPercussionToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsHarmonicMaskedToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsPercussionMaskedToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		System.out.println(">>mHPS MAKE TM: " + sequence);

		hpsPercussionToneMap.getTimeFrame().hpsPercussionMedian(hpsPercussionMedian, hpsMedianSwitch);

		if (sequence > hpsHarmonicMedian / 2) {
			int tmIndex = sequence + 1 - hpsHarmonicMedian / 2;
			ToneTimeFrame hpsHarmonicTimeFrame = hpsHarmonicToneMap.getTimeFrame(tmIndex);
			ToneTimeFrame hpsPercussionTimeFrame = hpsPercussionToneMap.getTimeFrame(tmIndex);
			System.out.println(">>mHPS GET TM: " + tmIndex);
			hpsHarmonicToneMap.getTimeFrame(tmIndex).hpsHarmonicMedian(cqToneMap, sequence, hpsHarmonicMedian,
					hpsMedianSwitch);
			System.out.println(">>mHPS GOT TM: " + (tmIndex));
			hpsToneMap.getTimeFrame(tmIndex).hpsMask(hpsHarmonicTimeFrame, hpsPercussionTimeFrame,
					(double) hpsHarmonicWeighting / 100.0, (double) hpsPercussionWeighting / 100.0);
			hpsHarmonicMaskedToneMap.getTimeFrame(tmIndex).hpsHarmonicMask(hpsHarmonicTimeFrame, hpsPercussionTimeFrame,
					hpsMaskFactor);
			hpsPercussionMaskedToneMap.getTimeFrame(tmIndex).hpsPercussionMask(hpsHarmonicTimeFrame,
					hpsPercussionTimeFrame, hpsMaskFactor);
		}
		console.getVisor().updateToneMapView(hpsHarmonicToneMap, this.cell.getCellType().toString() + "_HARMONIC");
		console.getVisor().updateToneMapView(hpsPercussionToneMap, this.cell.getCellType().toString() + "_PERCUSSION");
		console.getVisor().updateToneMapView(hpsHarmonicMaskedToneMap,
				this.cell.getCellType().toString() + "_HARMONIC_MASK");
		console.getVisor().updateToneMapView(hpsPercussionMaskedToneMap,
				this.cell.getCellType().toString() + "_PERCUSSION_MASK");
		console.getVisor().updateToneMapView(hpsToneMap, this.cell.getCellType().toString());

		cell.send(streamId, sequence);
	}
}
