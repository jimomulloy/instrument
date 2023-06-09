package jomu.instrument.cognition.cell;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentTestBase;
import jomu.instrument.audio.features.AudioFeatureFrame;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.audio.features.ConstantQFeatures;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.perception.Hearing;
import jomu.instrument.workspace.tonemap.ToneMap;

@ExtendWith(MockitoExtension.class)
class AudioCQProcessorTest extends InstrumentTestBase {

	@Mock
	private Hearing hearing;

	@Mock
	private AudioFeatureProcessor afp;

	@Mock
	private AudioFeatureFrame aff;

	@Mock
	private NuCell nuCell;

	@Mock
	private ConstantQFeatures cqf;

	@Test
	void testAccept() {

		String streamId = "stream1";
		ToneMap toneMap = initToneMap(CellTypes.AUDIO_CQ, streamId);

		when(hearing.getAudioFeatureProcessor(streamId)).thenReturn(afp);
		when(afp.getAudioFeatureFrame(1)).thenReturn(aff);
		when(aff.getConstantQFeatures()).thenReturn(cqf);
		doNothing().when(cqf).buildToneMapFrame(toneMap);
		when(nuCell.getCellType()).thenReturn(CellTypes.AUDIO_CQ);
		AudioCQProcessor processor = new AudioCQProcessor(nuCell);

		List<NuMessage> messages = new ArrayList<>();
		messages.add(new NuMessage(nuCell, streamId, 1));

		processor.accept(messages);

		verify(nuCell).send(streamId, 1);
	}

	protected void mockInstrument(Instrument instrument) {
		instrument.getCoordinator().setHearing(hearing);
	}

}