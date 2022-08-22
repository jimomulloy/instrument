package org.quifft.fft;

/**
 * Class representing a complex number and its operations
 * 
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public class Complex {
	// converts an array of double values to an array of Complex values (imaginary
	// component always 0)
	static Complex[] convertIntToComplex(int[] d) {
		Complex[] c = new Complex[d.length];
		for (int i = 0; i < c.length; i++) {
			c[i] = new Complex(d[i], 0.00);
		}
		return c;
	}
	private final double re; // the real part

	private final double im; // the imaginary part

	// create a new object with the given real and imaginary parts
	public Complex(double real, double imag) {
		re = real;
		im = imag;
	}

	// return abs/modulus/magnitude
	public double abs() {
		return Math.hypot(re, im);
	}

	// return a new Complex object whose value is (this - b)
	public Complex minus(Complex b) {
		Complex a = this;
		double real = a.re - b.re;
		double imag = a.im - b.im;
		return new Complex(real, imag);
	}

	// return a new Complex object whose value is (this + b)
	public Complex plus(Complex b) {
		Complex a = this; // invoking object
		double real = a.re + b.re;
		double imag = a.im + b.im;
		return new Complex(real, imag);
	}

	// return a new Complex object whose value is (this * b)
	public Complex times(Complex b) {
		Complex a = this;
		double real = a.re * b.re - a.im * b.im;
		double imag = a.re * b.im + a.im * b.re;
		return new Complex(real, imag);
	}
}