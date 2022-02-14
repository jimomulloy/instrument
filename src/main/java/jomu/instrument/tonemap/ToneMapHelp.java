package jomu.instrument.tonemap;

/**
 * This class contains the message displayed on the ToneMap Help Panel.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

class ToneMapHelp {

	public static final String message = "ToneMap Help Panel.\n" + "\n"
			+ "The User Interface is composed of a Main Frame of generic displays and controls which also contains 4 tabbed panes of Sub system controls including:\n"
			+ "\n"
			+ "1. MAP - Display of current ToneMap data. Controls to adjust scale, position and attributes displayed.\n"
			+ "LOAD button - Requests Load of MAP from currently open Audio file. This is a batch process that will\n"
			+ "take several seconds depending on extent of file data and power of user's computer. A Progress bar monitors this.\n"
			+ "PROCESS button - Enabled after LOAD complete. Requests processing of MAP data trough filtering and conversion as configured with TUNER and MIDI controls.\n"
			+ "\n" + "2. AUDIO - OPEN button to open new sampled audio data file - reads upto 60secs.\n"
			+ "Controls to adjust scale and resolution parameters of MAP LOAD transformation process.\n" + "\n"
			+ "3. MIDI - SAVE button to save current MIDI sequencer data to file.\n"
			+ "Controls to adjust parameters for MAP PROCESS conversion to MIDI data.\n" + "\n"
			+ "4. TUNER - Controls to adjust parameters that configure the MAP PROCESS filtering and conversion function.\n"
			+ "\n" + "Main Frame display and controls include:\n" + "\n"
			+ "Menu bar (line 2) - File option includes options to OPEN and SAVE '.tom' ToneMAP data file and option to QUIT. Help option as here.\n"
			+ "Tool Bar (line 3) - RESET button resets ToneMap to its initial state from any point.\n"
			+ "PLAYBACK contols to play current audio or midi media data.\n"
			+ "Status message (bottom line). Current status or error message.\n";

}