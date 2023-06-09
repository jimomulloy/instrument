package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import jomu.instrument.Instrument;
import jomu.instrument.InstrumentException;
import jomu.instrument.audio.features.AudioFeatureProcessor;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.Workspace;

public abstract class ProcessorCommon implements ThrowingConsumer<List<NuMessage>, InstrumentException> {

	private static final Logger LOG = Logger.getLogger(ProcessorCommon.class.getName());

	NuCell cell;
	Workspace workspace;
	ParameterManager parameterManager;
	Console console;
	Hearing hearing;
	InstrumentStoreService iss;

	public ProcessorCommon(NuCell cell) {
		super();
		this.cell = cell;
		this.workspace = Instrument.getInstance().getWorkspace();
		this.hearing = Instrument.getInstance().getCoordinator().getHearing();
		this.console = Instrument.getInstance().getConsole();
		this.iss = Instrument.getInstance().getStorage().getInstrumentStoreService();
		this.parameterManager = Instrument.getInstance().getController().getParameterManager();
	}

	@Override
	abstract public void accept(List<NuMessage> messages) throws InstrumentException;

	public final static String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	final static String buildToneMapKey(String tmType, String streamId) {
		return tmType + ":" + streamId;
	}

	final boolean isClosing(String streamId, int sequence) {
		AudioFeatureProcessor afp = hearing.getAudioFeatureProcessor(streamId);
		LOG.finer(">>Isclosing: " + sequence + ", " + this.cell.getCellType() + ", " + afp.isClosed() + ", "
				+ afp.isLastSequence(sequence));
		return (afp == null || (afp.isClosed() && afp.isLastSequence(sequence)));
	}

	final String getMessagesStreamId(List<NuMessage> messages) {
		Optional<String> streamId = messages.stream().findAny().map(message -> message.streamId);
		if (!streamId.isPresent()) {
			throw new InstrumentException("Missing messages in: " + this.cell.getCellType());
		}
		return streamId.get();
	}

	final int getMessagesSequence(List<NuMessage> messages) {
		Optional<Integer> sequence = messages.stream().findAny().map(message -> message.sequence);
		if (!sequence.isPresent()) {
			throw new InstrumentException("Missing messages in: " + this.cell.getCellType());
		}
		return sequence.get();
	}

	final static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	final static float[] convertDoublesToFloat(double[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		float[] output = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = (float) input[i];
		}
		return output;
	}

}
