package jomu.experimental;

/*File PlotALot07.java
Copyright 2005, R.G.Baldwin
This program is an update to the program named
PlotALot04 for the purpose of plotting six
data channels.  See PlotALot04 for descriptive
comments.  Otherwise, the comments in this
program have not been updated to reflect this
update.
The program was tested using J2SE 5.0 and WinXP.
Requires J2SE 5.0 to support generics.
************************************************/
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class PlotALot07 {
	class Page extends Frame {
		// =========================================//
		// Inner class
		class MyCanvas extends Canvas {
			double[] blackData = new double[samplesPerPage];
			double[] blueData = new double[samplesPerPage];
			double[] cyanData = new double[samplesPerPage];
			double[] greenData = new double[samplesPerPage];
			double[] magentaData = new double[samplesPerPage];
			double[] redData = new double[samplesPerPage];

			// Override the paint method
			@Override
			public void paint(Graphics g) {
				// Draw horizontal axes, one for each
				// trace.
				for (int cnt = 0; cnt < tracesPerPage; cnt++) {
					g.drawLine(0, (cnt + 1) * traceSpacing, this.getWidth(),
							(cnt + 1) * traceSpacing);
				} // end for loop

				// Plot the points if there are any to be
				// plotted.
				if (sampleCounter > 0) {
					for (int cnt = 0; cnt <= sampleCounter; cnt++) {

						// Begin by plotting the values from
						// the blackData array object.
						g.setColor(Color.BLACK);

						// Compute a vertical offset to locate
						// the black data on every third axis
						// on the page.
						int yOffset = ((1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing) - 5 * traceSpacing;
						// Draw an oval centered on the sample
						// value to mark the sample in the
						// plot. It is best if the dimensions
						// of the oval are evenly divisable
						// by 2 for centering purposes.
						// Reverse the sign of the sample
						// value to cause positive sample
						// values to be plotted above the
						// axis.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) blackData[cnt] - ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines. Do not draw a
						// line connecting the last sample in
						// one trace to the first sample in
						// the next trace.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) blackData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) blackData[cnt]);
						} // end if
							// Now plot the data stored in the
							// redData array object.
						g.setColor(Color.RED);
						// Compute a vertical offset to locate
						// the red data on every third axis
						// on the page.
						yOffset = (1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing - 4 * traceSpacing;

						// Draw the ovals as described above.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) redData[cnt] - ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines as described above.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) redData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) redData[cnt]);

						} // end if

						// Now plot the data stored in the
						// blueData array object.
						g.setColor(Color.BLUE);
						// Compute a vertical offset to locate
						// the blue data on every third axis
						// on the page.
						yOffset = (1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing - 3 * traceSpacing;

						// Draw the ovals as described above.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) blueData[cnt] - ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines as described above.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) blueData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) blueData[cnt]);
						} // end if

						// Now plot the data stored in the
						// greenData array object.
						g.setColor(Color.GREEN);
						// Compute a vertical offset to locate
						// the green data on every third axis
						// on the page.
						yOffset = (1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing - 2 * traceSpacing;

						// Draw the ovals as described above.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) greenData[cnt] - ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines as described above.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) greenData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) greenData[cnt]);
						} // end if

						// Now plot the data stored in the
						// magentaData array object.
						g.setColor(Color.MAGENTA);
						// Compute a vertical offset to locate
						// the magenta data on every third axis
						// on the page.
						yOffset = (1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing - traceSpacing;

						// Draw the ovals as described above.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) magentaData[cnt]
										- ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines as described above.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) magentaData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) magentaData[cnt]);
						} // end if

						// Now plot the data stored in the
						// cyanData array object.
						g.setColor(Color.CYAN);
						// Compute a vertical offset to locate
						// the cyan data on every third axis
						// on the page.
						yOffset = (1
								+ cnt * (sampSpacing + 1) / this.getWidth()) * 6
								* traceSpacing;

						// Draw the ovals as described above.
						g.drawOval(
								cnt * (sampSpacing + 1) % this.getWidth()
										- ovalWidth / 2,
								yOffset - (int) cyanData[cnt] - ovalHeight / 2,
								ovalWidth, ovalHeight);

						// Connect the sample values with
						// straight lines as described above.
						if (cnt * (sampSpacing + 1)
								% this.getWidth() >= sampSpacing + 1) {
							g.drawLine(
									(cnt - 1) * (sampSpacing + 1)
											% this.getWidth(),
									yOffset - (int) cyanData[cnt - 1],
									cnt * (sampSpacing + 1) % this.getWidth(),
									yOffset - (int) cyanData[cnt]);
						} // end if

					} // end for loop
				} // end if for sampleCounter > 0
			}// end overridden paint method
		}// end inner class MyCanvas
		MyCanvas canvas;

		int sampleCounter;

		Page(String title) {// constructor
			canvas = new MyCanvas();
			add(canvas);
			setSize(frameWidth, frameHeight);
			setTitle(title + " Page: " + pageCounter);
			setVisible(true);

			// ---------------------------------------//
			// Anonymous inner class to terminate the
			// program when the user clicks the close
			// button on the Frame.
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.exit(0);// terminate program
				}// end windowClosing()
			}// end WindowAdapter
			);// end addWindowListener
				// ---------------------------------------//
		}// end constructor
			// =========================================//

		// This method receives a quadruplet of sample
		// values of type double and stores each of
		// them in a separate array object belonging
		// to the MyCanvas object.
		void putData(double valBlack, double valRed, double valBlue,
				double valGreen, double valMagenta, double valCyan,
				int sampleCounter) {
			canvas.blackData[sampleCounter] = valBlack;
			canvas.redData[sampleCounter] = valRed;
			canvas.blueData[sampleCounter] = valBlue;
			canvas.greenData[sampleCounter] = valGreen;
			canvas.magentaData[sampleCounter] = valMagenta;
			canvas.cyanData[sampleCounter] = valCyan;
			// Save the sample counter in an instance
			// variable to make it available to the
			// overridden paint method. This value is
			// needed by the paint method so it will
			// know how many samples to plot on the
			// final page which probably won't be full.
			this.sampleCounter = sampleCounter;
		}// end putData
	}// end inner class Page

	// This main method is provided so that the
	// class can be run as an application to test
	// itself.
	public static void main(String[] args) {
		// Instantiate a plotting object using the
		// version of the constructor that allows for
		// controlling the plotting parameters.
		PlotALot07 plotObjectA = new PlotALot07("A", 158, 350, 25, 5, 4, 4);

		// Feed quadruplets of data values to the
		// plotting object.
		for (int cnt = 0; cnt < 115; cnt++) {
			// Plot some white random noise. Note that
			// fifteen of the values for each time
			// series are not random. See the opening
			// comments for a discussion of the reasons
			// why.
			double valBlack = (Math.random() - 0.5) * 25;
			double valRed = valBlack;
			double valBlue = valBlack;
			double valGreen = valBlack;
			double valMagenta = valBlack;
			double valCyan = valBlack;
			// Feed quadruplets of values to the plotting
			// object by invoking the feedData method
			// once for each quadruplet of data values.
			if (cnt == 57) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 58) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 59) {
				plotObjectA.feedData(25, 25, 25, 25, 25, 25);
			} else if (cnt == 60) {
				plotObjectA.feedData(-25, -25, -25, -25, -25, -25);
			} else if (cnt == 61) {
				plotObjectA.feedData(25, 25, 25, 25, 25, 25);
			} else if (cnt == 62) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 63) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 26) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 27) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 28) {
				plotObjectA.feedData(20, 20, 20, 20, 20, 20);
			} else if (cnt == 29) {
				plotObjectA.feedData(20, 20, 20, 20, 20, 20);
			} else if (cnt == 30) {
				plotObjectA.feedData(-20, -20, -20, -20, -20, -20);
			} else if (cnt == 31) {
				plotObjectA.feedData(-20, -20, -20, -20, -20, -20);
			} else if (cnt == 32) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else if (cnt == 33) {
				plotObjectA.feedData(0, 0, 0, 0, 0, 0);
			} else {
				plotObjectA.feedData(valBlack, valRed, valBlue, valGreen,
						valMagenta, valCyan);
			} // end else
		} // end for loop
			// Cause the data to be plotted in the default
			// screen location.
		plotObjectA.plotData();
	}// end main
		int frameHeight;
	int frameWidth;
	int ovalHeight;// height of sample marking oval
	int ovalWidth;// width of sample marking oval
	int pageCounter = 0;
	ArrayList<Page> pageLinks = new ArrayList<>();

	int sampleCounter = 0;
	int samplesPerPage;
	int sampSpacing;// pixels between samples
	// -------------------------------------------//
	String title;
	int traceSpacing;// pixels between traces

	int tracesPerPage;

	PlotALot07(String title) {
		// Invoke the other overloaded constructor
		// passing default values for all but the
		// title.
		this(title, 400, 410, 50, 2, 2, 2);
	}// end overloaded constructor
		// -------------------------------------------//

	// There are two overloaded versions of the
	// constructor for this class. This
	// overloaded version accepts several incoming
	// parameters allowing the user to control
	// various aspects of the plotting format. A
	// different overloaded version accepts a title
	// string only and sets all of the plotting
	// parameters to default values.
	PlotALot07(String title, // Frame title
			int frameWidth, // in pixels
			int frameHeight, // in pixels
			int traceSpacing, // in pixels
			int sampSpace, // in pixels per sample
			int ovalWidth, // sample marker width
			int ovalHeight)// sample marker hite
	{// constructor
		// Specify sampSpace as pixels per sample.
		// Should never be less than 1. Convert to
		// pixels between samples for purposes of
		// computation.
		this.title = title;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.traceSpacing = traceSpacing;
		// Convert to pixels between samples.
		this.sampSpacing = sampSpace - 1;
		this.ovalWidth = ovalWidth;
		this.ovalHeight = ovalHeight;
		// The following object is instantiated solely
		// to provide information about the width and
		// height of the canvas. This information is
		// used to compute a variety of other
		// important values.
		Page tempPage = new Page(title);
		int canvasWidth = tempPage.canvas.getWidth();
		int canvasHeight = tempPage.canvas.getHeight();
		// Display information about this plotting
		// object.
		System.out.println("nTitle: " + title);
		System.out.println("Frame width: " + tempPage.getWidth());
		System.out.println("Frame height: " + tempPage.getHeight());
		System.out.println("Page width: " + canvasWidth);
		System.out.println("Page height: " + canvasHeight);
		System.out.println("Trace spacing: " + traceSpacing);
		System.out.println("Sample spacing: " + (sampSpacing + 1));
		if (sampSpacing < 0) {
			System.out.println("Terminating");
			System.exit(0);
		} // end if
			// Get rid of this temporary page.
		tempPage.dispose();
		// Now compute the remaining important values.
		tracesPerPage = (canvasHeight - traceSpacing / 2) / traceSpacing;
		System.out.println("Traces per page: " + tracesPerPage);
		if ((tracesPerPage == 0) || (tracesPerPage % 6 != 0)) {
			System.out.println("Terminating program");
			System.exit(0);
		} // end if
		samplesPerPage = canvasWidth * tracesPerPage / (sampSpacing + 1) / 6;
		System.out.println("Samples per page: " + samplesPerPage);
		// Now instantiate the first usable Page
		// object and store its reference in the
		// list.
		pageLinks.add(new Page(title));
	}// end constructor
		// -------------------------------------------//

	// There are two overloaded versions of the
	// plotData method. One version allows the
	// user to specify the location on the screen
	// where the stack of plotted pages will
	// appear. The other version places the stack
	// in the upper left corner of the screen.

	// Invoke one of the overloaded versions of
	// this method once when all data has been fed
	// to the plotting object in order to rearrange
	// the order of the pages with page 0 at the
	// top of the stack on the screen.

	// Invoke this method once for each quadruplet of
	// data values to be plotted.
	void feedData(double valBlack, double valRed, double valBlue,
			double valGreen, double valMagenta, double valCyan) {
		if ((sampleCounter) == samplesPerPage) {
			// if the page is full, increment the page
			// counter, create a new empty page, and
			// reset the sample counter.
			pageCounter++;
			sampleCounter = 0;
			pageLinks.add(new Page(title));
		} // end if
			// Store the sample values in the MyCanvas
			// object to be used later to paint the
			// screen. Then increment the sample
			// counter. The sample values pass through
			// the page object into the current MyCanvas
			// object.
		pageLinks.get(pageCounter).putData(valBlack, valRed, valBlue, valGreen,
				valMagenta, valCyan, sampleCounter);
		sampleCounter++;
	}// end feedData
		// -------------------------------------------//

	// This overloaded version of the method causes
	// the stack to be located in the upper left
	// corner of the screen by default
	void plotData() {
		plotData(0, 0);// invoke overloaded version
	}// end plotData()
		// -------------------------------------------//
		// Inner class. A PlotALot07 object may
		// have as many Page objects as are required
		// to plot all of the data values. The
		// reference to each Page object is stored
		// in an ArrayList object belonging to the
		// PlotALot07 object.

	// For this overloaded version, specify xCoor
	// and yCoor to control the location of the
	// stack on the screen. Values of 0,0 will
	// place the stack at the upper left corner of
	// the screen. Also see the other overloaded
	// version, which places the stack at the upper
	// left corner of the screen by default.
	void plotData(int xCoor, int yCoor) {
		Page lastPage = pageLinks.get(pageLinks.size() - 1);
		// Delay until last page becomes visible.
		while (!lastPage.isVisible()) {
			// Loop until last page becomes visible
		} // end while loop

		Page tempPage = null;
		// Make all pages invisible
		for (int cnt = 0; cnt < (pageLinks.size()); cnt++) {
			tempPage = pageLinks.get(cnt);
			tempPage.setVisible(false);
		} // end for loop

		// Now make all pages visible in reverse order
		// so that page 0 will be on top of the
		// stack on the screen.
		for (int cnt = pageLinks.size() - 1; cnt >= 0; cnt--) {
			tempPage = pageLinks.get(cnt);
			tempPage.setLocation(xCoor, yCoor);
			tempPage.setVisible(true);
		} // end for loop
	}// end plotData(int xCoor,int yCoor)
		// -------------------------------------------//
}// end class PlotALot07
	// =============================================//