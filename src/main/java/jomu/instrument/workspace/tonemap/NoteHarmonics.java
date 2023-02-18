package jomu.instrument.workspace.tonemap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class NoteHarmonics {

	@Override
	public int hashCode() {
		return Objects.hash(noteHarmonicsMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NoteHarmonics other = (NoteHarmonics) obj;
		return Objects.equals(noteHarmonicsMap, other.noteHarmonicsMap);
	}

	ConcurrentSkipListMap<Integer, Integer> noteHarmonicsMap = new ConcurrentSkipListMap<>();

	@Override
	public NoteHarmonics clone() {
		NoteHarmonics copy = new NoteHarmonics();
		return copy;
	}

	public void addNoteHarmonic(int rootNote, int harmonic) {
		noteHarmonicsMap.put(rootNote, harmonic);
	}

	public Map<Integer, Integer> getNoteHarmonics() {
		Map<Integer, Integer> noteHarmonics = new ConcurrentHashMap<>();
		for (Entry<Integer, Integer> nhm : noteHarmonicsMap.entrySet()) {
			noteHarmonics.put(nhm.getKey(), nhm.getValue());
		}
		return noteHarmonics;
	}
}
