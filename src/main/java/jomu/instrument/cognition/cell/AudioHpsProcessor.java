package jomu.instrument.cognition.cell;

import java.util.List;

import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.workspace.tonemap.ToneMap;

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
		boolean hpsMedianSwitch = parameterManager
				.getBooleanParameter(InstrumentParameterNames.PERCEPTION_HEARING_HPS_SWITCH_MEDIAN);

		ToneMap hpsHarmonicToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + ".HARMONIC", streamId));
		ToneMap hpsPercussionToneMap = workspace.getAtlas()
				.getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS + ".PERCUSSION", streamId));
		ToneMap hpsToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_HPS, streamId));
		ToneMap cqToneMap = workspace.getAtlas().getToneMap(buildToneMapKey(CellTypes.AUDIO_CQ, streamId));

		hpsToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsHarmonicToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());
		hpsPercussionToneMap.addTimeFrame(cqToneMap.getTimeFrame(sequence).clone());

		hpsHarmonicToneMap.getTimeFrame().hpsHarmonicMedian(cqToneMap, hpsHarmonicToneMap, hpsHarmonicMedian,
				hpsMedianSwitch);
		hpsPercussionToneMap.getTimeFrame().hpsPercussionMedian(hpsPercussionMedian, hpsMedianSwitch);

		hpsToneMap.getTimeFrame().hpsMask(hpsHarmonicToneMap.getTimeFrame(), hpsPercussionToneMap.getTimeFrame(),
				(double) hpsHarmonicWeighting / 100.0, (double) hpsPercussionWeighting / 100.0);

		console.getVisor().updateToneMapView(hpsHarmonicToneMap, this.cell.getCellType().toString() + "_HARMONIC");
		console.getVisor().updateToneMapView(hpsPercussionToneMap, this.cell.getCellType().toString() + "_PERCUSSION");
		console.getVisor().updateToneMapView(hpsToneMap, this.cell.getCellType().toString());

		cell.send(streamId, sequence);
	}
}
