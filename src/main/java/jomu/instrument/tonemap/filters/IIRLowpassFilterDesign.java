// IIR Low Pass Shelf Filter Design Class
// Written by: Craig A. Lindley
// Last Update: 06/28/99

package jomu.instrument.tonemap.filters;

// IIR Low Pass Shelf Filter Design Class
// Written by: Craig A. Lindley
// Last Update: 06/28/99

// The IIR lowpass filter designed here has unity gain 

public class IIRLowpassFilterDesign extends IIRFilterDesignBase {

	// Entry point for testing
	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("\nIIR Lowpass Filter Design Program");
			System.out.println("\nUsage:");
			System.out.println("\tIIRLowpassFilterDesign " + "cutoffFrequency sampleRate dampingFactor");

			System.exit(1);
		}
		// Parse the command line arguments
		int cutoffFrequency = new Integer(args[0]).intValue();
		int sampleRate = new Integer(args[1]).intValue();
		double dampingFactor = new Double(args[2]).doubleValue();

		// Instantiate highpass filter designer
		IIRLowpassFilterDesign d = new IIRLowpassFilterDesign(cutoffFrequency, sampleRate, dampingFactor);
		// Do the design
		d.doFilterDesign();

		// Print out the coefficients
		d.printCoefficients();
	}

	public IIRLowpassFilterDesign(int cutoffFrequency, int sampleRate, double dampingFactor) {

		super(cutoffFrequency, sampleRate, dampingFactor);
	}

	// Do the design of the filter. Filter response controlled by
	// just three coefficients: alpha, beta and gamma.
	public void doFilterDesign() {

		// Get radians per sample at cutoff frequency
		double thetaZero = getThetaZero();

		double theSin = parameter / (2.0 * Math.sin(thetaZero));

		// Beta relates gain to cutoff freq
		beta = 0.5 * ((1.0 - theSin) / (1.0 + theSin));

		// Final filter coefficient
		gamma = (0.5 + beta) * Math.cos(thetaZero);

		// For unity gain
		alpha = (0.5 + beta - gamma) / 4.0;
	}
}