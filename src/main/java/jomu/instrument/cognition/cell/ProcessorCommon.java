package jomu.instrument.cognition.cell;

import java.util.List;
import java.util.Optional;

import jomu.instrument.Instrument;
import jomu.instrument.cognition.cell.Cell.CellTypes;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.monitor.Console;
import jomu.instrument.perception.Hearing;
import jomu.instrument.store.InstrumentStoreService;
import jomu.instrument.workspace.Workspace;

public abstract class ProcessorCommon implements ThrowingConsumer<List<NuMessage>, Exception> {

	static final int C4_NOTE = 36;
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
	abstract public void accept(List<NuMessage> messages) throws Exception;

	static String buildToneMapKey(CellTypes cellType, String streamId) {
		return cellType + ":" + streamId;
	}

	static String buildToneMapKey(String tmType, String streamId) {
		return tmType + ":" + streamId;
	}

	String getMessagesStreamId(List<NuMessage> messages) throws Exception {
		Optional<String> streamId = messages.stream().findAny().map(message -> message.streamId);
		if (!streamId.isPresent()) {
			// TODO exception handling in API
			throw new Exception("Missing messages in: " + this.cell.getCellType());
		}
		return streamId.get();
	}

	int getMessagesSequence(List<NuMessage> messages) throws Exception {
		Optional<Integer> sequence = messages.stream().findAny().map(message -> message.sequence);
		if (!sequence.isPresent()) {
			// TODO exception handling in API
			throw new Exception("Missing messages in: " + this.cell.getCellType());
		}
		return sequence.get();
	}

	static double[] convertFloatsToDoubles(float[] input) {
		if (input == null) {
			return null; // Or throw an exception - your choice
		}
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

	static float[] convertDoublesToFloat(double[] input) {
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
