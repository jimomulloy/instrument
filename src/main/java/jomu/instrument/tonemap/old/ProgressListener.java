package jomu.instrument.tonemap.old;

/**
 * This interface is used in the monitoring of progress of long running
 * background threads as used durring Loading and Processing of the Tonemap.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public interface ProgressListener {

	public void setProgress(int progressValue);
}