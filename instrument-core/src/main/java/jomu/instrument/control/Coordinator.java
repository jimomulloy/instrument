package jomu.instrument.control;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import jomu.instrument.InstrumentException;
import jomu.instrument.InstrumentExceptionHandler;
import jomu.instrument.Organ;
import jomu.instrument.actuation.Voice;
import jomu.instrument.cognition.Cortex;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.store.InstrumentSession.InstrumentSessionState;
import jomu.instrument.workspace.InstrumentSessionManager;
import jomu.instrument.workspace.Workspace;

@ApplicationScoped
public class Coordinator implements Organ, InstrumentExceptionHandler {

	private static final Logger LOG = Logger.getLogger(Coordinator.class.getName());

	@Inject
	Cortex cortex;

	@Inject
	Hearing hearing;

	@Inject
	Voice voice;

	@Inject
	Console console;

	@Inject
	Coordinator coordinator;

	@Inject
	Controller controller;

	@Inject
	Workspace workspace;

	@Inject
	InstrumentSessionManager instrumentSessionManager;

	public Cortex getCortex() {
		return cortex;
	}

	public Hearing getHearing() {
		return hearing;
	}

	public Voice getVoice() {
		return voice;
	}

	@Override
	public void initialise() {
		cortex.initialise();
		hearing.initialise();
		voice.initialise();
	}

	@Override
	public void start() {
		cortex.start();
		hearing.start();
		voice.start();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleException(InstrumentException exception) {
		LOG.log(Level.SEVERE, "CortexExceptionHandler handling exception: " + exception.getMessage(), exception);
		workspace.processException(exception);
		cortex.processException(exception);
		hearing.processException(exception);
		console.processException(exception);
		instrumentSessionManager.getCurrentSession().setState(InstrumentSessionState.FAILED);
		if (controller.isCountDownLatch()) {
			controller.getCountDownLatch().countDown();
		}
	}

	@Override
	public void processException(InstrumentException exception) throws InstrumentException {
		// TODO Auto-generated method stub

	}

}
