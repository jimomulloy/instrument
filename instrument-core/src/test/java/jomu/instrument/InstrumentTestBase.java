package jomu.instrument;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.cognition.cell.ProcessorCommon;
import jomu.instrument.workspace.Atlas;
import jomu.instrument.workspace.Workspace;
import jomu.instrument.workspace.tonemap.PitchSet;
import jomu.instrument.workspace.tonemap.TimeSet;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@EnableWeld
public class InstrumentTestBase {

	@WeldSetup
	WeldInitiator weld = WeldInitiator.performDefaultDiscovery();

	@Inject
	protected Instrument instrument;

	@Inject
	protected Atlas atlas;

	@Inject
	protected Workspace workspace;

	@BeforeEach
	public void init() {
		System.out.println(">>Weld test");
		Instrument.setInstance(instrument);
		mockInstrument(instrument);
		instrument.initialise();
		instrument.start();
	}

	@AfterEach
	public void close() {
		instrument.stop();
	}

	protected void mockInstrument(Instrument instrument) {
	}

	protected ToneMap initToneMap(CellTypes cellType, String streamId) {
		ToneMap toneMap = workspace.getAtlas().getToneMap(ProcessorCommon.buildToneMapKey(cellType, streamId));
		double timeStart = 0;
		double timeEnd = 100;
		TimeSet timeSet = new TimeSet(timeStart, timeEnd, 44125, timeEnd - timeStart);
		PitchSet pitchSet = new PitchSet();
		ToneTimeFrame ttf = new ToneTimeFrame(toneMap, timeSet, pitchSet);
		toneMap.addTimeFrame(ttf);
		ttf.reset();
		return toneMap;
	}
}
