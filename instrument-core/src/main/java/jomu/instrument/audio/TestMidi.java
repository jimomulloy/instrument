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
		@Override
		public String toString() {
			return "Note [notePitch=" + notePitch + ", start=" + start + ", end=" + end + ", velocity=" + velocity
					+ "]";
		}

		public int notePitch;
		public long start;
		public long end;
		public int velocity;
	}

	public boolean[][] buildMidiNoteArray(Map<Integer, List<Note>> noteMap, int timeDimension, int pitchDimension,
			long timeFrame) {
		boolean[][] midiNoteArray = new boolean[timeDimension - 1][pitchDimension - 1];
		for (int p = 0; p < pitchDimension; p++) {
			if (noteMap.containsKey(p)) {
				System.out.println("buildMidiNoteArray P: " + p);
				List<Note> noteList = noteMap.get(p);
				for (Note note : noteList) {
					int tStart = (int) ((double) note.start / (double) timeFrame);
					int tEnd = (int) ((double) note.end / (double) timeFrame);
					for (int t = tStart; t <= tEnd; t++) {
						midiNoteArray[t][p] = true;
						System.out.println("buildMidiNoteArray set TRUE: " + t + ", " + p);
					}
				}
			}
		}
		System.out.println("buildMidiNoteArray " + midiNoteArray.length + ", " + midiNoteArray[0].length);
		return midiNoteArray;
	}

	public int scoreMidiNoteArray(boolean[][] sourceMidiNoteArray, boolean[][] targetMidiNoteArray) {
		int pitchDimension = sourceMidiNoteArray[0].length > targetMidiNoteArray[0].length
				? targetMidiNoteArray[0].length - 1
				: sourceMidiNoteArray[0].length - 1;
		int timeDimension = sourceMidiNoteArray.length > targetMidiNoteArray.length ? targetMidiNoteArray.length - 1
				: sourceMidiNoteArray.length - 1;
		int score = 0;
		for (int p = 0; p < pitchDimension; p++) {
			for (int t = 0; t < timeDimension; t++) {
				if (sourceMidiNoteArray[t][p] != targetMidiNoteArray[t][p]) {
					score -= 1;
				} else if (sourceMidiNoteArray[t][p] && targetMidiNoteArray[t][p]) {
					score += 2;
				}
			}
		}

		return score;
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
						int notePitch = sm.getData1();
						int octave = (notePitch / 12) - 1;
						int note = notePitch % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						addNoteOn(noteMap, event.getTick(), notePitch, velocity);
						System.out.println(
								" Note on, " + noteName + octave + " pitch=" + notePitch + " velocity: " + velocity);
					} else if (sm.getCommand() == NOTE_OFF) {
						int notePitch = sm.getData1();
						int octave = (notePitch / 12) - 1;
						int note = notePitch % 12;
						String noteName = NOTE_NAMES[note];
						int velocity = sm.getData2();
						addNoteOff(noteMap, event.getTick(), notePitch);
						System.out.println(
								" Note off, " + noteName + octave + " pitch=" + notePitch + " velocity: " + velocity);
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
		} else {
			noteMap.put(notePitch, noteList);
		}
		noteList.add(note);

	}

	public static void main(String[] args) throws Exception {
		TestMidi testMidi = new TestMidi();
		System.out.println("Source");
		Map<Integer, List<Note>> noteMap = testMidi.extractMidiNotes("instrumentai/c3egchord-1-3sec.mid");
		boolean[][] source = testMidi.buildMidiNoteArray(noteMap, 50, 100, 100);
		System.out.println("Target");
		noteMap = testMidi.extractMidiNotes("instrumentai/c3-1-3sec.mid");
		boolean[][] target = testMidi.buildMidiNoteArray(noteMap, 50, 100, 100);
		int score = testMidi.scoreMidiNoteArray(source, target);
		System.out.println("Score: " + score);
	}
}