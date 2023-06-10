package jomu.instrument.actuation;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.inject.Inject;
import jomu.instrument.Instrument;
import jomu.instrument.InstrumentTestBase;
import jomu.instrument.audio.MidiSynthesizer;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.workspace.tonemap.ToneMap;
import jomu.instrument.workspace.tonemap.ToneTimeFrame;

@ExtendWith(MockitoExtension.class)
class VoiceTest extends InstrumentTestBase {

	@Inject
	protected Voice voice;

	@Mock
	MidiSynthesizer midiSynthesizer;

	@Test
	void testAudioFileStreamProcess() {

		String streamId = "stream1";
		ToneMap toneMap = initToneMap(CellTypes.AUDIO_CQ, streamId);
		int sequence = 1;

		try {
			doNothing().when(midiSynthesizer).playFrameSequence(any(ToneTimeFrame.class), anyString(), anyInt());
			voice.send(toneMap.getFirstTimeFrame(), streamId, sequence, false);
			verify(midiSynthesizer).playFrameSequence(any(ToneTimeFrame.class), anyString(), anyInt());
		} catch (Exception e) {
			fail("Invalid Exception thrown", e);
		}
	}

	protected void mockInstrument(Instrument instrument) {
		voice.setMidiSynthesizer(midiSynthesizer);
		when(midiSynthesizer.open()).thenReturn(true);
	}
}
