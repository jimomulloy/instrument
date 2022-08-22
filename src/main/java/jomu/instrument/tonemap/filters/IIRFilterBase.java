// IIR Filter Base Class
// Written by: Craig A. Lindley
// Last Update: 09/03/98

package jomu.instrument.tonemap.filters;

// IIR Filter Base Class
// Written by: Craig A. Lindley
// Last Update: 09/03/98

// Base class for all IIR filters.

public abstract class IIRFilterBase {

	protected static final int HISTORYSIZE = 3;

	// Private class data
	protected double alpha;

	protected double beta;

	protected double gamma;

	protected double amplitudeAdj;

	protected double[] inArray;

	protected double[] outArray;

	protected int iIndex;

	protected int jIndex;

	protected int kIndex;
	// IIRFilterBase class constructor
	// alpha, beta and gamma are precalculated filter coefficients
	// that are passed into this filter element.
	public IIRFilterBase(double alpha, double beta, double gamma) {

		// Save incoming
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;

		amplitudeAdj = 1.0;

		// Allocate history buffers
		inArray = new double[HISTORYSIZE];
		outArray = new double[HISTORYSIZE];
	}
	// Filter coefficients can also be extracted by passing in
	// design object.
	public IIRFilterBase(IIRFilterDesignBase fdb) {
		this(fdb.getAlpha(), fdb.getBeta(), fdb.getGamma());
	}
	// Abstract method that runs the filter algorithm
	public abstract void doFilter(short[] inBuffer, double[] outBuffer, int length);
	public void setAlpha(double alpha) {

		this.alpha = alpha;
	}
	// Set the amplitude adjustment to be applied to filtered data
	// Values typically range from -.25 to +4.0 or -12 to +12 db.
	public void setAmplitudeAdj(double amplitudeAdj) {

		this.amplitudeAdj = amplitudeAdj;
	}
	public void setBeta(double beta) {

		this.beta = beta;
	}
	public void setGamma(double gamma) {

		this.gamma = gamma;
	}
	public void updateFilterCoefficients(IIRFilterDesignBase fdb) {

		this.alpha = fdb.getAlpha();
		this.beta = fdb.getBeta();
		this.gamma = fdb.getGamma();
	}
}