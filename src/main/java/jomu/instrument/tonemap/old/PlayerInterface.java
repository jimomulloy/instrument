package jomu.instrument.tonemap.old;

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

	public double playGetLength();

	public int playGetState();

	public double playGetTime();

	public void playLoop();

	public void playPause();

	public void playResume();

	public void playSetPlayer(Player player);

	public void playSetSeek(double seekTime);

	public void playStop();

}