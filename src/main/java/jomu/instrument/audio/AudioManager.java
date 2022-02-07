package jomu.instrument.audio;

import com.synthbot.jasiohost.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

//import org.quifft.fft.*;

// Simple class to handle audio data from an ASIO compatible Audio Interface.
// Uses JASIOHost (c) M. H. Roth 2010
//Alastair Barber - 2012
public class AudioManager implements AsioDriverListener {
	private HashSet<AsioChannel> inputChannels, outputChannels, activeChannels;
	private int bufferSize;
	private double sampleRate;
	private AsioDriver selectedDriver = null;

	public AudioManager() throws AsioException {
		// Enumerate available ASIO Drivers
		ArrayList<String> driverList = (ArrayList<String>) AsioDriver.getDriverNames();
		for (String dname : driverList) {
			System.out.println("dname: " + dname);
			if (dname.startsWith("Generic")) {
				selectedDriver = AsioDriver.getDriver(dname);
			}
		}
		// Select the first one
		if (selectedDriver == null) {
			selectedDriver = AsioDriver.getDriver(driverList.get(0));
		}
		selectedDriver = AsioDriver.getDriver(driverList.get(0));
		System.out.println(">>driver: " + selectedDriver.toString());
		// Create 2 'Sets' of channels, input and output
		activeChannels = new HashSet<AsioChannel>();
		int inputChannelCount = selectedDriver.getNumChannelsInput();
		int outputChannelCount = selectedDriver.getNumChannelsOutput();
		for (int i = 0; i < inputChannelCount; i++) {
			activeChannels.add(selectedDriver.getChannelInput(i));
			System.out.println(">>input: " + selectedDriver.getChannelInput(i).getChannelName());
		}
		for (int i = 0; i < outputChannelCount; i++) {
			activeChannels.add(selectedDriver.getChannelOutput(i));
			System.out.println(">>output: " + selectedDriver.getChannelOutput(i).getChannelName());
		}

		bufferSize = selectedDriver.getBufferPreferredSize();
		sampleRate = selectedDriver.getSampleRate();
		// Activate these channels and assign this class as the listener
		selectedDriver.addAsioDriverListener(this);
		selectedDriver.createBuffers(activeChannels);
		selectedDriver.start();
	}

	public void shutDown() {
		if (selectedDriver != null) {
			selectedDriver.shutdownAndUnloadDriver();
		}
	}

	public void bufferSwitch(long sampleTime, long samplePosition, Set<AsioChannel> switchActiveChannels) {
		double[] outputLeftArray = new double[bufferSize];
		double[] outputRightArray = new double[bufferSize];
		float[] writeLeftArray = new float[bufferSize];
		float[] writeRightArray = new float[bufferSize];
		Complex[] lc = new Complex[bufferSize];
		Complex[] rc = new Complex[bufferSize];

		boolean sideSwitch = false;
		// System.out.println(">>bufferSwitchC: ");
		for (AsioChannel activeChannel : switchActiveChannels) {
			if (activeChannel.isInput()) {
				for (int i = 0; i < bufferSize; i++) {
					float value = ((float) activeChannel.getByteBuffer().getInt()) / Integer.MAX_VALUE;
					if (sideSwitch) {
						outputLeftArray[i] = value;
						lc[i] = new Complex(value, 0);
					} else {
						outputRightArray[i] = value;
						rc[i] = new Complex(value, 0);
					}
					sideSwitch = !sideSwitch;
				}
			}
		}

		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		System.out.println(">>bufferSwitchD1: ");
		Complex[] ly = fft.transform(outputLeftArray, TransformType.FORWARD);
		System.out.println(">>bufferSwitchD2: ");
		Complex[] lz = fft.transform(ly, TransformType.INVERSE);
		System.out.println(">>bufferSwitchD3: ");

		for (int i = 0; i < lz.length; i++) {
			if (i % 2 == 0) {
				writeLeftArray[i] = (float) lz[i].getReal();
			}
		}

		Complex[] ry = fft.transform(outputRightArray, TransformType.FORWARD);
		Complex[] rz = fft.transform(ry, TransformType.INVERSE);

		for (int i = 0; i < rz.length; i++) {
			if (i % 2 == 0) {
				writeLeftArray[i] = (float) rz[i].getReal();
			}
		}

		System.out.println(">>bufferSwitchD: ");
		// InplaceFFT.fft(x);
		// FFT.show(y, "y = fft(x)");
		// take inverse FFT
		// Complex[] z = FFT.ifft(y);
		// FFT.show(z, "z = ifft(y)");
		// We shall do a separate loop of the channels as there is no guarantee that all
		// the input
		// channels will be returned before the outputs.
		sideSwitch = false;
		for (AsioChannel activeChannel : switchActiveChannels) {
			System.out.println(">>bufferSwitchD: " + activeChannel);
			if (!activeChannel.isInput()) {
				if (sideSwitch) {
					activeChannel.write(writeLeftArray);
				} else {
					activeChannel.write(writeRightArray);
				}
				sideSwitch = !sideSwitch;
			}
		}
	}

	public void bufferSizeChanged(int bufferSize) {
		System.out.println("bufferSizeChanged() callback received.");
	}

	public void latenciesChanged(int inputLatency, int outputLatency) {
		System.out.println("latenciesChanged() callback received.");
	}

	public void resetRequest() {
		/*
		 * This thread will attempt to shut down the ASIO driver. However, it will block
		 * on the AsioDriver object at least until the current method has returned.
		 */
		new Thread() {
			@Override
			public void run() {
				System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
				selectedDriver.returnToState(AsioDriverState.INITIALIZED);
			}
		}.start();
	}

	public void resyncRequest() {
		System.out.println("resyncRequest() callback received.");
	}

	public void sampleRateDidChange(double sampleRate) {
		System.out.println("sampleRateDidChange() callback received.");
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		AudioManager host = new AudioManager();
		waitForEnter(null);
		host.shutDown();
	}

	public static void waitForEnter(String message, Object... args) {
		Scanner scan = new Scanner(System.in);
		System.out.print("Press any key to continue . . . ");
		scan.nextLine();
		System.out.print("CLOSED. . . ");
		scan.close();
	}
}