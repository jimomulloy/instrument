package jomu.instrument;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import jomu.instrument.control.Controller;
import jomu.instrument.control.Coordinator;
import jomu.instrument.control.InstrumentParameterNames;
import jomu.instrument.monitor.Console;
import jomu.instrument.store.Storage;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
@Default
public class Instrument implements Organ, InstrumentFactory {

	private static final Logger LOG = Logger.getLogger(Instrument.class.getName());

	static Instrument instrument;

	@Inject
	Coordinator coordinator;

	@Inject
	Controller controller;

	@Inject
	Console console;

	@Inject
	Storage storage;

	@Inject
	Workspace workspace;

	public Controller getController() {
		return controller;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public Console getConsole() { 
		return console;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public Storage getStorage() {
		return storage;
	}

	public void initialise() {
		LOG.finer(">>Initialise INSTRUMENT"); 
		controller.initialise();
		storage.initialise();
		workspace.initialise();
		console.initialise();
		coordinator.initialise();
		LOG.finer(">>Initialised INSTRUMENT");
	}

	public void start() {
		LOG.finer(">>Start INSTRUMENT");
		controller.start();
		storage.start();
		workspace.start();
		console.start();
		coordinator.start();
		LOG.finer(">>Started INSTRUMENT");
	}
	
	public void test() {
		LOG.severe(">>Test INSTRUMENT");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_TRACK_WRITE_SWITCH,
				"false");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_INSTRUMENT_VOICE_1,
				"true");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY,
				"true");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_MIDI_PLAY,
				"false");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE,
				"true");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE,
				"true");
		controller.getParameterManager().setParameter(InstrumentParameterNames.ACTUATION_VOICE_SILENT_WRITE,
				"true");
		//TODO update UI here
		//TODO update UI here)
		CountDownLatch countDownLatch = new CountDownLatch(1);
		controller.setCountDownLatch(countDownLatch);
		coordinator.getHearing().test();
		try {
			countDownLatch.await(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		controller.clearCountDownLatch();
		LOG.severe(">>Tested INSTRUMENT");
	}

	@Override
	public void stop() {
		LOG.finer(">>Stop INSTRUMENT");

	}

	public static Instrument getInstance() {
		if (instrument == null) {
			instrument = new Instrument();
		}
		return instrument;
	}

	public static void setInstance(Instrument injectedInstrument) {
		instrument = injectedInstrument;
	}

}
