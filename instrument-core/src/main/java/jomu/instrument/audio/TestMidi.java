package jomu.instrument.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class TestMidi {
	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;
	public static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

	public class Note {
		public int notePitch;
		public long start;
		public long end;
		public int velocity;
	}

	public boolean[][] buildMidiNoteArray(Map<Integer, List<Note>> noteMap, int timeDimension, int pitchDimension,
			long timeFrame) throws Exception {
		boolean[][] midiNoteArray = new boolean[timeDimension - 1][pitchDimension - 1];

		return midiNoteArray;
	}

	public Map<Integer, List<Note>> extractMidiNotes(String midiFileName) throws Exception {

		Map<Integer, List<Note>> noteMap = new HashMap<>();
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(midiFileName).getFile());
		Sequence sequence = MidiSystem.getSequence(file);

		int trackNumber = 0;
		for (Track track : sequence.getTracks()) {
			trackNumber++;
			System.out.println("Track " + trackNumber + ": size = " + track.size());
			System.out.println();
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				System.out.print("@" + event.getTick() + " ");
				MidiMessage message = event.getMessage();
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					System.out.print("Channel: " + sm.getChannel() + " ");
					if (sm.getCommand() == NOTE_ON) {
						int key = sm.getData1();
						int octave = (key / 12) - 1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						addNoteOn(noteMap, event.getTick(), note, velocity);
						System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
					} else if (sm.getCommand() == NOTE_OFF) {
						int key = sm.getData1();
						int octave = (key / 12) - 1;
						int note = key % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						addNoteOff(noteMap, event.getTick(), note);
						System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
					} else {
						System.out.println("Command:" + sm.getCommand());
					}
				} else {
					System.out.println("Other message: " + message.getClass());
				}
			}

			System.out.println();
		}
		return noteMap;
	}

	private void addNoteOff(Map<Integer, List<Note>> noteMap, long tick, int notePitch) {
		Note note = new Note();
		note.notePitch = notePitch;
		List<Note> noteList = new ArrayList<>();
		if (noteMap.containsKey(notePitch)) {
			noteList = noteMap.get(notePitch);
			if (noteList.size() > 0) {
				Note existingNote = noteList.get(noteList.size() - 1);
				existingNote.end = tick;
			}
		}
	}

	private void addNoteOn(Map<Integer, List<Note>> noteMap, long tick, int notePitch, int velocity) {
		Note note = new Note();
		note.notePitch = notePitch;
		note.velocity = velocity;
		note.start = tick;
		note.end = tick;
		List<Note> noteList = new ArrayList<>();
		if (noteMap.containsKey(notePitch)) {
			noteList = noteMap.get(notePitch);
		}
		noteList.add(note);

	}

	public static void main(String[] args) throws Exception {
		TestMidi testMidi = new TestMidi();
		Map<Integer, List<Note>> noteMap = testMidi.extractMidiNotes("instrumentai/c3-1-3sec.mid");
		testMidi.buildMidiNoteArray(noteMap, 10, 10, 100);

	}
}