package jomu.instrument.cognition.cell;

import java.util.Objects;
import java.util.UUID;

import net.beadsproject.beads.core.BeadArray;

public abstract class Cell {

	private CellTypes cellType;

	/** True if the Bead is marked for deletion. */
	private boolean deleted;

	/* Unique identification for this CellElement instance. */
	private String ID;

	/** A Bead that gets informed when this Bead gets killed. */
	private Cell killListener;

	/** True if the Bead is paused. */
	private boolean paused;

	/**
	 * Generate <code>Cell</code>. and registers the <code>Cell</code> to
	 * <code>ECM<</code>. Every cell is identified by a unique cellID number.
	 */
	public Cell(CellTypes cellType) {
		ID = UUID.randomUUID().toString();
		this.cellType = cellType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;
		Cell other = (Cell) obj;
		return Objects.equals(ID, other.ID);
	}

	public CellTypes getCellType() {
		return cellType;
	}

	public String getID() {
		return this.ID;
	}

	/**
	 * Gets this Bead's kill listener.
	 *
	 * @return the kill listener.
	 */
	public Cell getKillListener() {
		return killListener;
	}

	/**
	 * Returns the cell type. This is just a convenient way to store some property
	 * for the cell. Should not be confused with NeuroMLType.
	 */
	public String getType() {
		return "cellType";
	}

	@Override
	public int hashCode() {
		return Objects.hash(ID);
	}

	/**
	 * Determines if this Bead is deleted.
	 *
	 * @return true if this Bead's state is deleted, false otherwise.
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Checks if this Bead is paused.
	 *
	 * @return true if paused
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Stops this Bead, and flags it as deleted. This means that the Bead will
	 * automatically be removed from any {@link BeadArray}s. Calling this method for
	 * the first time also causes the killListener to be notified.
	 */
	public void kill() {
		if (!deleted) {
			deleted = true;
			Cell killListener = this.killListener;
			if (killListener != null) {
				killListener.message(this);
			}
		}
	}

	/**
	 * Send this Bead a message. Typically if another Bead was sending the message,
	 * it would send itself as the argument.
	 *
	 * @param message the Bead is the message.
	 */

	public final void message(Cell message) {
		if (!paused)
			messageReceived(message);
	}

	/**
	 * Toggle the paused state of the Bead.
	 *
	 * @param paused true to pause Bead.
	 */
	public void pause(boolean paused) {
		this.paused = paused;
	}

	/**
	 * Sets this Bead's kill listener. The kill listener will receive a message
	 * containing this Bead as an argument when this Bead is killed.
	 *
	 * @param killListener the new kill listener.
	 */
	public void setKillListener(Cell killListener) {
		this.killListener = killListener;
	}

	/**
	 * Sets the cell type. This is just a convenient way to store some property for
	 * the cell. Should not be confused with NeuroMLType.
	 */
	public void setType(String type) {
		// somaElement.setPropertiy("cellType", type);
	}

	/**
	 * Shortcut for pause(false).
	 */
	public void start() {
		paused = false;
	}

	@Override
	public String toString() {
		return "Cell [ID=" + ID + ", cellType=" + cellType + "]";
	}

	/**
	 * Responds to an incoming message. Subclasses can override this in order to
	 * handle incoming messages. Typically a Bead would send a message to another
	 * Bead with itself as the arugment.
	 *
	 * @param message the message
	 */
	protected void messageReceived(Cell message) {
		/*
		 * To be subclassed, but not compulsory.
		 */
	}

	public static enum CellTypes {
		AUDIO_CQ, AUDIO_CQ_MICRO_TONE, AUDIO_TUNER_PEAKS, AUDIO_BEAT, AUDIO_ONSET, AUDIO_INTEGRATE, AUDIO_NOTATE,
		AUDIO_PITCH, AUDIO_HPS, AUDIO_SPECTRAL_PEAKS, AUDIO_SINK, AUDIO_PRE_CHROMA, AUDIO_POST_CHROMA, JUNCTION,
		PASS_THROUGH, SINK, SOURCE

	}

}
