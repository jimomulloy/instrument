package jomu.instrument.tonemap;

/**
 * This interface defines the methods used by the Player class to control
 * playback of sound media in both AudioModel and MidiModel classes which
 * implement it.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public interface PlayerInterface {

	public boolean play();

	public void playStop();

	public void playPause();

	public void playResume();

	public void playLoop();

	public double playGetLength();

	public double playGetTime();

	public int playGetState();

	public void playSetSeek(double seekTime);

	public void playSetPlayer(Player player);

}