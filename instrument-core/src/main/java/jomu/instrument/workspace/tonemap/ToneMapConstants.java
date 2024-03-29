package jomu.instrument.workspace.tonemap;

/**
 * This interface defines a set of Constants used across the whole ToneMap
 * program and is implementeded by most of the classes.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public interface ToneMapConstants {

	public static final int AUDIO_OPEN = 1;

	public static final int BEAT_MODE = 1;
	public static final int CHIRP_MODE = 2;
	public static final int CHORD_MODE = 3;
	public static final int EC_AUDIO_OPEN = 8;

	public static final int EC_AUDIO_OPEN_BADFILE = 12;
	public static final int EC_AUDIO_OPEN_NOFILE = 9;
	public static final int EC_AUDIO_OPEN_NULLFILE = 37;

	public static final int EC_AUDIO_OPEN_READ = 10;
	public static final int EC_AUDIO_PANEL = 13;

	public static final int EC_AUDIO_PLAY = 11;
	public static final int EC_AUDIO_SYSTEM = 7;

	public static final int EC_FRAME_PANEL = 18;
	public static final int EC_MIDI_EVENT = 21;
	public static final int EC_MIDI_OPEN = 6;
	public static final int EC_MIDI_OPEN_CHANNELS = 26;
	public static final int EC_MIDI_OPEN_INSTRUMENTS = 24;

	public static final int EC_MIDI_OPEN_SOUNDBANK = 25;
	public static final int EC_MIDI_PANEL = 14;
	public static final int EC_MIDI_PLAY = 27;
	public static final int EC_MIDI_SAVE_BADFILE = 28;
	public static final int EC_MIDI_SAVE_SYSTEM = 19;

	public static final int EC_MIDI_SAVE_WRITE = 20;
	public static final int EC_MIDI_SYSTEM = 5;
	public static final int EC_MIDI_WRITE_SEQUENCE = 22;
	public static final int EC_PLAYER_PANEL = 17;
	public static final int EC_TONEMAP_OPEN = 29;
	public static final int EC_TONEMAP_OPEN_BADFILE = 31;

	public static final int EC_TONEMAP_OPEN_NOFILE = 30;
	public static final int EC_TONEMAP_PANEL = 16;
	public static final int EC_TONEMAP_SAVE = 32;
	public static final int EC_TONEMAP_SAVE_BADFILE = 34;

	public static final int EC_TONEMAP_SAVE_NOFILE = 33;
	public static final int EC_TONEMAP_SAVE_NOMAP = 35;
	public static final int EC_TUNER_PANEL = 15;
	public final int END = 3;
	public static final int EOM = 3;

	public static final int INIT_BEND_SETTING = 8192;

	public static final int INIT_BPM_SETTING = 120;
	public static final int INIT_HIGH_THRESHHOLD = 50;

	public static final int INIT_INSTRUMENT_SETTING = 1;
	public static final int INIT_LOW_THRESHHOLD = 50;
	public static final int INIT_NOISE_HIGH = 100;
	public static final int INIT_NOISE_LOW = 0;
	public static final int INIT_NOTE_HIGH = 100;
	public static final int INIT_NOTE_LOW = 50;
	public static final int INIT_NOTE_MAX_DURATION = 10000;
	public static final int INIT_NOTE_MIN_DURATION = 100;

	public static final int INIT_NOTE_SUSTAIN = 100;
	public static final int INIT_PAN_SETTING = 50;
	public final static int INIT_PITCH_HIGH = 72;
	public final static int INIT_PITCH_INC = 1;
	public final static int INIT_PITCH_LOW = 24;

	public final static int INIT_PITCH_MAX = 144;
	public final static int INIT_PITCH_MIN = 12;
	public static final int INIT_PITCH_SCALE = 100;
	public static final int INIT_PRESSURE_SETTING = 64;
	public static final int INIT_REVERB_SETTING = 64;

	public static final int INIT_SAMPLE_SIZE = 100;
	public final static int INIT_TIME_END = 60000;

	public final static int INIT_TIME_INC = 100;
	public final static int INIT_TIME_MAX = 60000;
	public final static int INIT_TIME_MIN = 0;
	public static final int INIT_TIME_SCALE = 100;
	public final static int INIT_TIME_START = 0;
	public static final int INIT_VELOCITY_SETTING = 64;
	public static final int INIT_VOLUME_SETTING = 50;

	public static final double MAX_AUDIO_DURATION = 60.0;
	public static final int MAX_BPM_SETTING = 180;

	public static final long MAX_CLIP_LENGTH = 2000000;
	public static final int MAX_SAMPLE_SIZE = 1000;
	public static final int MIN_SAMPLE_SIZE = 10;
	public static final int NONE = -1;
	public static final int NOTE_MODE = 0;
	public final int NOTEOFF = 128;
	public final int NOTEON = 144;
	public final int OFF = 0;
	public final int ON = 1;
	public final int START = 2;
	public final int CONTINUING = 3;
	public final int PENDING = 4;
	public static final int ONE_SECOND = 1000;
	public static final int PAUSED = 2;
	public final static int PITCH_RANGE_MAX = 60;
	public static final int PLAY_MODE_AUDIO = 1;
	public static final int PLAY_MODE_AUDIO_OUT = 4;
	public static final int PLAY_MODE_MAP = 2;
	public static final int PLAY_MODE_MIDI = 3;
	public static final int PLAYING = 1;
	public final int PROGRAM = 192;
	public final int REVERB = 91;
	public static final int SC_MIDI_EMPTY_SEQUENCE = 23;
	public static final int SC_TONEMAP_LOADED = 2;
	public static final int SC_TONEMAP_LOADING = 1;
	public static final int SC_TONEMAP_PROCESSED = 4;
	public static final int SC_TONEMAP_PROCESSING = 3;
	public static final int SC_TONEMAP_READY = 24;
	public static final int ST_ERROR = 2;
	public static final int ST_INFO = 1;
	public static final int STOPPED = 0;
	public final int SUSTAIN = 64;
	public static final int TRANSFORM_MODE_JAVA = 0;
	public static final int TRANSFORM_MODE_JNI = 1;
	public static final int VIEW_MODE_AUDIO = 0;
	public static final int VIEW_MODE_NOTE = 3;
	public static final int VIEW_MODE_POST = 2;
	public static final int VIEW_MODE_PRE = 1;

}