package jomu.instrument.tonemap;

import java.util.Arrays;

/**
 * This class contains an array of StatusInfo objects that define Status and
 * Error information that is accessed by the ToneMapFrame status report
 * function.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class ToneMapStatus implements ToneMapConstants {

	public static StatusInfo[] SI = { new StatusInfo(SC_TONEMAP_LOADING, ST_INFO, "ToneMap Loading"),
			new StatusInfo(SC_TONEMAP_LOADED, ST_INFO, "ToneMap Loaded"),
			new StatusInfo(SC_TONEMAP_PROCESSING, ST_INFO, "ToneMap Processing"),
			new StatusInfo(SC_TONEMAP_PROCESSED, ST_INFO, "ToneMap Processed"),
			new StatusInfo(EC_MIDI_SYSTEM, ST_ERROR, "MIDI System Startup Error"),
			new StatusInfo(EC_MIDI_OPEN, ST_ERROR, "MIDI System Open Error"),
			new StatusInfo(EC_AUDIO_SYSTEM, ST_ERROR, "Audio System Startup Error"),
			new StatusInfo(EC_AUDIO_OPEN, ST_ERROR, "Audio Open File Error"),
			new StatusInfo(EC_AUDIO_OPEN_NOFILE, ST_ERROR, "Audio Open File Missing"),
			new StatusInfo(EC_AUDIO_OPEN_READ, ST_ERROR, "Audio Open File Read Error"),
			new StatusInfo(EC_AUDIO_PLAY, ST_ERROR, "Audio Play Error"),
			new StatusInfo(EC_AUDIO_OPEN_BADFILE, ST_ERROR, "Audio Open Invalid File"),
			new StatusInfo(EC_AUDIO_PANEL, ST_ERROR, "Audio Panel Java Exception"),
			new StatusInfo(EC_MIDI_PANEL, ST_ERROR, "MIDI Panel Java Exception"),
			new StatusInfo(EC_TUNER_PANEL, ST_ERROR, "Tuner Panel Java Exception"),
			new StatusInfo(EC_TONEMAP_PANEL, ST_ERROR, "Map Panel Java Exception"),
			new StatusInfo(EC_PLAYER_PANEL, ST_ERROR, "Player Panel Java Exception"),
			new StatusInfo(EC_FRAME_PANEL, ST_ERROR, "Tone Map Frame Panel Java Exception"),
			new StatusInfo(EC_MIDI_SAVE_SYSTEM, ST_ERROR, "MIDI Save File System Error"),
			new StatusInfo(EC_MIDI_SAVE_WRITE, ST_ERROR, "MIDI Save File Write Error"),
			new StatusInfo(EC_MIDI_EVENT, ST_ERROR, "MIDI Write Event Error"),
			new StatusInfo(EC_MIDI_WRITE_SEQUENCE, ST_ERROR, "MIDI Write Sequence Error"),
			new StatusInfo(SC_MIDI_EMPTY_SEQUENCE, ST_INFO, "MIDI Note Sequence Empty"),
			new StatusInfo(EC_MIDI_OPEN_INSTRUMENTS, ST_ERROR, "MIDI System Open Error - Missing Instruments"),
			new StatusInfo(EC_MIDI_OPEN_SOUNDBANK, ST_ERROR, "MIDI System Open Error - Missing SoundBank"),
			new StatusInfo(EC_MIDI_OPEN_CHANNELS, ST_ERROR, "MIDI System Open Error - Missing Channels"),
			new StatusInfo(EC_MIDI_PLAY, ST_ERROR, "MIDI Play Error"),
			new StatusInfo(EC_MIDI_SAVE_BADFILE, ST_ERROR, "MIDI File Save Error"),
			new StatusInfo(EC_TONEMAP_OPEN, ST_ERROR, "ToneMap Open File Error"),
			new StatusInfo(EC_TONEMAP_OPEN_NOFILE, ST_ERROR, "ToneMap Open Missing File Error"),
			new StatusInfo(EC_TONEMAP_OPEN_BADFILE, ST_ERROR, "ToneMap Open Invalid File Error"),
			new StatusInfo(EC_TONEMAP_SAVE, ST_ERROR, "ToneMap Save File Error"),
			new StatusInfo(EC_TONEMAP_SAVE_NOFILE, ST_ERROR, "ToneMap Save File Missing"),
			new StatusInfo(EC_TONEMAP_SAVE_BADFILE, ST_ERROR, "ToneMap Save Invalid File Error"),
			new StatusInfo(EC_TONEMAP_SAVE_NOMAP, ST_ERROR, "ToneMap Save Error - No Map Exists"),
			new StatusInfo(SC_TONEMAP_READY, ST_INFO, "ToneMap Ready"),
			new StatusInfo(EC_AUDIO_OPEN_NULLFILE, ST_ERROR, "Audio Open Empty File") };

	static {
		synchronized (SI) {
			Arrays.sort(SI);
		}
	}

	/**
	 * ToneMapStatus constructor comment.
	 */
	public ToneMapStatus() {
		super();
	}

	public static StatusInfo getSI(int statusCode) {
		StatusInfo key = new StatusInfo(statusCode);

		int index = Arrays.binarySearch(SI, key);

		if (SI[index].sc == statusCode)
			return SI[index];
		else
			return null;
	}

}