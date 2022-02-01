package jomuddlo.instrument.cell;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Cell<I, O> {

	/** True if the Bead is paused. */
	private boolean paused;

	/** True if the Bead is marked for deletion. */
	private boolean deleted;

	/** A Bead that gets informed when this Bead gets killed. */
	private Cell<I, O> killListener;

	/** The name. */
	private String name;

	/* Unique identification for this CellElement instance. */
	private String ID;

	private final BlockingQueue<I> messageQueue = new LinkedBlockingQueue<I>();

	/**
	 * Generate <code>Cell</code>. and registers the <code>Cell</code> to
	 * <code>ECM<</code>. Every cell is identified by a unique cellID number.
	 */
	public Cell() {
		ID = UUID.randomUUID().toString();
	}

	/**
	 * Returns the cell type. This is just a convenient way to store some property
	 * for the cell. Should not be confused with NeuroMLType.
	 */
	public String getType() {
		return "cellType";
	}

	/**
	 * Sets the cell type. This is just a convenient way to store some property for
	 * the cell. Should not be confused with NeuroMLType.
	 */
	public void setType(String type) {
		// somaElement.setPropertiy("cellType", type);
	}

	public String getID() {
		return this.ID;
	}

	/**
	 * Process some data of type P (specified by the class def). This method must be
	 * overidden by implementing classes.
	 * 
	 * @param data the data.
	 */
	public abstract void process();

	/**
	 * Process some data of type P (specified by the class def). This method must be
	 * overidden by implementing classes.
	 * 
	 * @param data the data.
	 */
	public abstract void send();

	/**
	 * Process some data of type P (specified by the class def). This method must be
	 * overidden by implementing classes.
	 * 
	 * @param data the data.
	 */

	public abstract void receive(I input);

	/**
	 * Send this Bead a message. Typically if another Bead was sending the message,
	 * it would send itself as the argument.
	 * 
	 * @param message the Bead is the message.
	 */

	public final void message(Cell<I, O> message) {
		if (!paused)
			messageReceived(message);
	}

	/**
	 * Responds to an incoming message. Subclasses can override this in order to
	 * handle incoming messages. Typically a Bead would send a message to another
	 * Bead with itself as the arugment.
	 * 
	 * @param message the message
	 */
	protected void messageReceived(Cell<I, O> message) {
		/*
		 * To be subclassed, but not compulsory.
		 */
	}

	/**
	 * Shortcut for pause(false).
	 */
	public void start() {
		paused = false;
	}

	/**
	 * Stops this Bead, and flags it as deleted. This means that the Bead will
	 * automatically be removed from any {@link BeadArray}s. Calling this method for
	 * the first time also causes the killListener to be notified.
	 */
	public void kill() {
		if (!deleted) {
			deleted = true;
			Cell<I, O> killListener = this.killListener;
			if (killListener != null) {
				killListener.message(this);
			}
		}
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
	public void setKillListener(Cell<I, O> killListener) {
		this.killListener = killListener;
	}

	/**
	 * Gets this Bead's kill listener.
	 * 
	 * @return the kill listener.
	 */
	public Cell<I, O> getKillListener() {
		return killListener;
	}

	/**
	 * Determines if this Bead is deleted.
	 * 
	 * @return true if this Bead's state is deleted, false otherwise.
	 */
	public boolean isDeleted() {
		return deleted;
	}

}
