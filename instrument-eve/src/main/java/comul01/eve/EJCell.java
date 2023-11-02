/* EJCell.java */
package comul01.eve;

import java.awt.*;
import java.util.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.media.util.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.util.*;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.operator.*;
import javax.media.jai.RenderedOp;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.WarpPerspective;
import java.awt.RenderingHints;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderableOp;
import javax.media.jai.*;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.awt.color.*;

public class EJCell {

	private EJMain ejMain = null;

	private EJSettings ejSettings = null;

	private EJControls ejControls;

	double stateSize = 0;

	int stateNum = 0;

	int stateRange = 0;

	double stateFactor = 0;

	int width = 0;

	int height = 0;

	int numBands = 0;

	double z = 0;

	int sum = 0;

	double lp = 0;

	double tp = 0;

	double[] states = null;

	EJCA[] cas = null;

	double[] lastp = null;

	/**
	 * initialize the formats
	 */
	public EJCell(EJMain ejMain) {

		this.ejMain = ejMain;
		ejSettings = ejMain.getEJSettings();
		ejControls = ejMain.getEJControls();
	}

	/**
	 * initialize the formats
	 */
	public EJCell(EJMain ejMain, double stateFactor, int stateNum) {

		this(ejMain);
		this.stateFactor = stateFactor;
		this.stateNum = stateNum;
		this.stateSize = (double) stateRange / (double) (stateNum - 1);

	}

	public void process(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		width = frameThis.getWidth();
		height = frameThis.getHeight();
		numBands = frameThis.getPixelStride();
		z = 0;
		sum = 0;
		lp = 0;
		tp = 0;

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		stateRange = (int) (tp * stateFactor / 255.0);
		stateSize = (double) stateRange / (double) (stateNum - 1);

		states = getSampleRegion33(lastStates, b, x, y);
		cas = getCARegion33(lastStates, b, x, y);

		//lastp = getSampleRegion33(frameLast, b, x, y);

		thisStates[y][x][b].value = states[4];

		double value = 0;

		if (caType == 0) {
			value = processLife(frameLast, frameThis, lastStates, thisStates,
					x, y, b, t, caType, option);
		} else if (caType == 1) {
			value = processFungus(frameLast, frameThis, lastStates, thisStates,
					x, y, b, t, caType, option);
		} else if (caType == 2) {
			value = processPsyche(frameLast, frameThis, lastStates, thisStates,
					x, y, b, t, caType, option);
		} else if (caType == 3) {
			value = processDemon(frameLast, frameThis, lastStates, thisStates,
					x, y, b, t, caType, option);
		}

		else if (caType == 4) {
			value = processLife2(frameLast, frameThis, lastStates, thisStates,
					x, y, b, t, caType, option);
		}

		else if (caType == 5) {
			value = processXor(frameLast, frameThis, lastStates, thisStates, x,
					y, b, t, caType, option);
			
		} else if (caType == 6) {
			value = processMax(frameLast, frameThis, lastStates, thisStates, x,
					y, b, t, caType, option);
		} else if (caType == 7) {
			value = processLife3(frameLast, frameThis, lastStates, thisStates, x,
					y, b, t, caType, option);
		} else if (caType == 8) {
			value = processSpread(frameLast, frameThis, lastStates, thisStates, x,
					y, b, t, caType, option);
		} else if (caType == 9) {
			value = processBrain(frameLast, frameThis, lastStates, thisStates, x,
					y, b, t, caType, option);
		}

		if (x == 100 && y == 100) {
			System.out.println("EJCell state values X " + ", " + value);
		}

		
		if (x == 100 && y == 100) {
			System.out.println("EJCell state values Y " + ", " + value);
		}

		double abs1 = Math.floor(tp / stateRange);
		double base1 = abs1 * stateRange;
		double value1 = tp - base1;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value2 = lp - base2;

		if (x == 100 && y == 100) {
			System.out.println("EJCell state values 1 " + ", " + stateRange
					+ ", " + value + ", " + tp + ", " + abs1 + ", " + base1
					+ ", " + value1 + lp + ", " + abs2 + ", " + base2 + ", "
					+ value2);
		}

		value = value + value1 - value2;

		if (value < 0)
			value = -value;
		if (value > stateRange)
			value = stateRange;

		if (x == 100 && y == 100) {
			System.out.println("EJCell state values 2 " + ", " + value + ", "
					+ tp + ", " + abs1 + ", " + base1 + ", " + value1 + lp
					+ ", " + abs2 + ", " + base2 + ", " + value2);
		}

		thisStates[y][x][b].value = value;

		double newValue = value + base1;
		if (option) newValue = value + tp;

		if (x == 100 && y == 100) {
			System.out.println("EJCell new value 2 " + newValue + " ," + tp);
		}

		if (newValue >= 255)
			newValue = 500-newValue;
		
		if (x == 100 && y == 100) {
			System.out.println("EJCell new value 3 " + newValue + " ," + tp);
		}
		
		frameThis.setPixel(b, x, y, newValue);

	}

	public double processLife2(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		stateRange = (int) (tp * stateFactor / 255.0);
		stateNum = 23;
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countLive(cas);

		double[] values = getValues(stateSize);

		if (x == 100 && y == 100) {
			System.out.println("EJCell life2 " + tp + " ," + cas[4].value
					+ " ," + cas[4].state + " ," + stateSize + " ,"
					+ stateRange + " ," + stateNum + ", " + living + ", " + t
					+ ", " + states[0]+", "+values[0]
					  +", "+values[1]
					+", "+values[2]
								 +", "+values[3]);
		}
		
		

		if (!cas[4].alive) {

			if (living == 2) {
				
				total = totalState(cas);
				value = getBirthValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;
				thisStates[y][x][b].age = 0;
				

			}
			
			/*int age = cas[4].age;
			age++;
			double r = Math.random();
			if (r > t) {
				if (!option){
					thisStates[y][x][b].alive = true;
					thisStates[y][x][b].age = 0;
				} else {
					if (age > stateNum){
						thisStates[y][x][b].alive = true;
						thisStates[y][x][b].age = 0;
			
					}
				}
		
			}
			*/	

		} else {

			if (living == 2 || living == 3) {

				total = totalState(cas);
				value = getSurviveValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 4a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;
				thisStates[y][x][b].age = 0;
				

			} else {

				total = totalState(cas);
				value = getDeathValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 5a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = false;
				thisStates[y][x][b].age = 0;
				

			}

		}

		return value;

	}


	public double processLife3(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		stateRange = (int) (tp * stateFactor / 255.0);
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countLive(cas);

		double[] values = getValues(stateSize);

		if (x == 100 && y == 100) {
			System.out.println("EJCell life2 " + tp + " ," + cas[4].value
					+ " ," + cas[4].state + " ," + stateSize + " ,"
					+ stateRange + " ," + stateNum + ", " + living + ", " + t
					+ ", " + states[0]);
		}

		if (!cas[4].alive) {

			if (living == 2) {

				total = totalState(cas);
				value = stateRange;
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;
				thisStates[y][x][b].age = 0;
				

			}

			/*int age = cas[4].age;
			age++;
			double r = Math.random();
			if (r > t) {
				if (!option){
					thisStates[y][x][b].alive = true;
					thisStates[y][x][b].age = 0;
				} else {
					if (age > stateNum){
						thisStates[y][x][b].alive = true;
						thisStates[y][x][b].age = 0;
			
					}
				}
		
			}
			*/

		} else {

			if (living == 2 || living == 3) {

				total = totalState(cas);
				value = stateRange/2;
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 4a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;
				thisStates[y][x][b].age = 0;
				

			} else {

				total = totalState(cas);
				value = 0;
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 5a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = false;
				thisStates[y][x][b].age = 0;
				

			}

		}

		return value;

	}
	

	public double processBrain(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {


		stateRange = (int) (tp * stateFactor / 255.0);
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countLive(cas);

		double[] values = getValues(stateSize);

		if (x == 100 && y == 100) {
			System.out.println("EJCell life2 " + tp + " ," + cas[4].value
					+ " ," + cas[4].state + " ," + stateSize + " ,"
					+ stateRange + " ," + stateNum + ", " + living + ", " + t
					+ ", " + states[0]);
		}

		if (!cas[4].alive ) {
			
			if (cas[4].age == 0) {

				if (living == 2) {

					total = totalState(cas);
					value = stateRange;
					if (x == 100 && y == 100) {
						System.out.println("EJCell 3a " + total + ", " + value);
					}
					thisStates[y][x][b].alive = true;

				}
			} else {
			
				int age = cas[4].age;
				age++;
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3b " + age);
				}
				if (age >= stateNum) {
					thisStates[y][x][b].age = 0;
					value = 0;
				} else {
					thisStates[y][x][b].age = age;
					value = getBrainDeath(total, values, age);
				}			
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3c " + age);
				}
				
			}

		} else {

			if (living == 2 || living == 3) {

				total = totalState(cas);
				value = stateRange/2;
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 4a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;

			} else {

				total = totalState(cas);
				int age = 0;
				age++;
				thisStates[y][x][b].age = age;
				value = getBrainDeath(total, values, age);
				
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 5a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = false;

			}

		}

		return value;

	}
	
	public double processXor(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		stateRange = (int) (tp * stateFactor / 255.0);
		stateNum = 23;
		
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countXor(cas);

		double[] values = getValues(stateSize);

		if (x == 100 && y == 100) {
			System.out.println("EJCell life2 " + tp + " ," + cas[4].value
					+ " ," + cas[4].state + " ," + stateSize + " ,"
					+ stateRange + " ," + stateNum + ", " + living + ", " + t
					+ ", " + states[0]);
		}

		if (!cas[4].alive) {

			if (living == 1) {

				total = totalState(states);
				value = getBirthValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;

			}

		} else {

			if (living == 1) {

				total = totalState(states);
				value = getSurviveValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 4a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = true;

			} else {

				total = totalState(states);
				value = getDeathValue(total, values);
				if (x == 100 && y == 100) {
					System.out.println("EJCell2 5a " + total + ", " + value);
				}
				thisStates[y][x][b].alive = false;

			}

		}

		return value;

	}

	public double processLife(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		int total = 0;
		int living = countLive(states, t);
		
		//double value = states[4];

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		if (x == 100 && y == 100) {
			System.out.println("EJCell 1 " + tp + " ," + states[4] + " ,"
					+ stateSize + " ," + stateRange + " ," + stateNum + ", "
					+ living + ", " + t);
		}

		if (states[4] < t * stateSize) {

			if (living == 2) {

				total = totalState(states);
				value = getBirthValue(total, lp, tp, t);
				if (x == 100 && y == 100) {
					System.out.println("EJCell 3 " + total);
				}

			}

		} else {

			if (living == 2 || living == 3) {

				total = totalState(states);
				value = getSurviveValue(total, states[4]);
				if (x == 100 && y == 100) {
					System.out.println("EJCell 4 " + total);
				}

			} else {

				total = totalState(states);
				value = getDeathValue(total, states[4]);
				if (x == 100 && y == 100) {
					System.out.println("EJCell 5 " + total);
				}

			}

		}

		return value;

	}

	public double processFungus(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		stateRange = (int) (tp * stateFactor / 255.0);
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countLive(cas);

		double[] values = getValues(stateSize);

		if (x == 100 && y == 100) {
			System.out.println("EJCell 1 " + tp + " ," + states[4]);
		}

		if (!cas[4].alive) {
			if (living == 3) {
				value = stateRange;
				thisStates[y][x][b].alive = true;

			}
		} else {

			double avg = 0;
			for (int i = 0; i < states.length; i++) {
				if (i != 4)
					avg += states[i];
			}
			avg = avg / 8;
			double diff = Math.abs(avg - states[4]);
			if (diff < t * stateSize) {
				thisStates[y][x][b].alive = false;
				value = 0;
			} else {
				value = Math.min(avg, states[4]);
			}

		}

		if (value >= stateRange)
			value = stateRange;

		return value;

	}

	public double processPsyche(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		stateRange = (int) (tp * stateFactor / 255.0);
		stateSize = (double) stateRange / (double) (stateNum - 1);

		if (frameLast != null)
			lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);

		int total = 0;

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;

		int living = countLive(cas);

		double avg = 0;
		for (int i = 0; i < states.length; i++) {
			if (i != 4)
				avg += states[i];
		}
		avg = avg / 8;
		value = avg + stateSize;

		if (value >= stateRange)
			value = 0;

		return value;

	}

	public double processDemon(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;


		double avg = 0;
		for (int i = 0; i < states.length; i++) {
			if (i != 4 && states[i] > states[4]) {
				if ((states[i] - states[4]) > stateSize * t
						&& (states[i] - states[4]) < (stateSize + stateSize * t)) {
					value = states[i];
					break;
				}
			}
		}

		if (value >= stateRange)
			value = 0;

		return value;

	}
	

	public double processMax(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		
		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;


		double avg = 0;
		for (int i = 0; i < states.length; i++) {
			if (i != 4 && states[i] > states[4]) {
				if ((states[i] - states[4]) > stateSize * t
						&& (states[i] - states[4]) < (stateSize + stateSize * t)) {
					value = states[i];
					break;
				}
			}
		}

		if (value >= stateRange)
			value = 0;

		return value;

	}


	public double processSpread(EFrame frameLast, EFrame frameThis,
			EJCA[][][] lastStates, EJCA[][][] thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		double abs2 = Math.floor(lp / stateRange);
		double base2 = abs2 * stateRange;
		double value = lp - base2;


		//double[] s = getSampleRegion33(lastStates, b, x, y);
		double sMax = 0;
		double v = states[4]*Math.random();
		double l = states[4]*Math.random();
		if (Math.random() > t){
			int i = (int)(Math.random() * (double)(states.length-1));
			value = states[i];
		}

		return value;

	}

	private double getBirthValue(int total, double[] values) {

		double value = 0;

		if (total == 2) {

			value = values[2];

		} else if (total == 8) {
			value = values[8];

		} else if (total == 5) {
			value = values[5];

		} else {
			value = values[0];

			//System.out.println("Cell 1 "+ lp+" ,"+ t +", "+total);
		}

		return value;

	}
	
	
	private double getBirthValue(double total, double lp, double tp, double t) {

		double value = 0;

		if (total == 2) {

			value = stateSize;

		} else if (total == 8) {
			value = stateSize;

		} else if (total == 5) {
			value = stateSize;

		} else {
			value = stateSize;

			//System.out.println("Cell 1 "+ lp+" ,"+ t +", "+total);
		}

		return value;

	}

	private double getSurviveValue(double total, double state) {

		double value = 0;

		if (total == 3) {

			value = state + stateSize;

		} else if (total == 12) {
			value = state + stateSize;

		} else if (total == 9) {
			value = state + stateSize;

		} else if (total == 6) {
			value = state + stateSize;

		} else if (total == 2) {

			value = state + stateSize;

		} else if (total == 8) {
			value = state + stateSize;

		} else if (total == 5) {
			value = state + stateSize;

		} else {
			value = state + stateSize;

		}

		if (value < 0)
			value = 0;
		if (value > stateRange)
			value = stateRange;

		return value;

	}

	private double getSurviveValue(int total, double[] values) {

		double value = 0;

		if (total == 2) {

			value = values[20];

		} else if (total == 8) {
			value = values[21];

		} else if (total == 5) {
			value = values[22];

		} else if (total == 3) {

			value = values[3];

		} else if (total == 12) {
			value = values[12];

		} else if (total == 9) {
			value = values[9];

		} else if (total == 6) {
			value = values[6];

		} else {
			value = values[0];

		}

		if (value < 0)
			value = 0;
		if (value > stateRange)
			value = stateRange;

		return value;

	}

	private double getDeathValue(double total, double state) {

		double value = 0;

		return value;

	}

	private double getDeathValue(int total, double[] values) {

		double value = 0;
		if (total >= values.length ) {
			System.out.println("EJCELL DEATH I "
					+total
					+", "+values.length);
				}
		value = values[total];

		if (value < 0)
			value = 0;
		if (value > stateRange)
			value = stateRange;

		return value;

	}
	
	private double getBrainDeath(int total, double[] values, int age) {

		double value = 0;
		int i = stateNum-1-age;
		if (i >= values.length || i<0) {
			System.out.println("EJCELL BRAIN I "
					+i
					+", "+values.length
					+", "+stateNum
					+", "+age);
		}
		value = values[i];

		return value;

	}


	private int countLive(double[] s, double t) {

		int count = 0;
		for (int i = 0; i < s.length; i++) {
			if (s[i] > stateSize * t && i != 4)
				count++;
		}

		return count;

	}

	private double[] getValues(double stateSize) {

		double[] values = new double[stateNum];
		double value = 0;

		for (int i = 0; i < values.length; i++) {
			values[i] = value;
			value += stateSize;
		}

		return values;

	}

	private int countLive(EJCA[] s) {

		int count = 0;
		for (int i = 0; i < s.length; i++) {
			if (s[i].alive && i != 4)
				count++;
		}

		return count;

	}

	private int countXor(EJCA[] s) {

		int count = 0;
		if (s[1].alive)
			count++;
		if (s[3].alive)
			count++;
		if (s[5].alive)
			count++;
		if (s[7].alive)
			count++;
		//if (s[4].alive) count++;

		return count % 2;

	}

	private boolean reBirth(double[] s, double t) {

		boolean flag = false;
		double a = 0;
		for (int i = 0; i < s.length; i++) {
			if (i != 4)
				a += s[i];
		}
		if (a > stateSize)
			flag = true;
		return flag;

	}

	private int totalState(double[] s) {

		int total = 0;
		for (int i = 0; i < s.length; i++) {
			if (!(s[i] < stateSize) && i != 4) {

				if (i == 0 || i == 2 || i == 6 || i == 8) {
					total = total + 1;
				} else {
					total = total + 4;
				}

			}
		}

		return total;

	}

	private int totalState(EJCA[] cas) {

		int total = 0;
		for (int i = 0; i < cas.length; i++) {
			if ((cas[i].alive) && i != 4) {

				if (i == 0 || i == 2 || i == 6 || i == 8) {
					total = total + 1;
				} else {
					total = total + 4;
				}

			}
		}

		return total;

	}

	private double[] getSampleFrameRegion33(EFrame frameIn, int band, int x,
			int y) {
		double[] sr = new double[9];
		sr[0] = frameIn.getPixelDouble(band, x - 1, y - 1);
		sr[1] = frameIn.getPixelDouble(band, x, y - 1);
		sr[2] = frameIn.getPixelDouble(band, x + 1, y - 1);
		sr[3] = frameIn.getPixelDouble(band, x - 1, y);
		sr[4] = frameIn.getPixelDouble(band, x, y);
		sr[5] = frameIn.getPixelDouble(band, x + 1, y);
		sr[6] = frameIn.getPixelDouble(band, x - 1, y + 1);
		sr[7] = frameIn.getPixelDouble(band, x, y + 1);
		sr[8] = frameIn.getPixelDouble(band, x + 1, y + 1);
		return sr;
	}

	private double[] getSampleRegion33(EJCA[][][] frameIn, int band, int x,
			int y) {

		double[] sr = new double[9];
		if (y > 0 && x > 0)
			sr[0] = frameIn[y - 1][x - 1][band].value;
		if (y > 0)
			sr[1] = frameIn[y - 1][x][band].value;
		if (y > 0 && x < frameIn[0].length - 1)
			sr[2] = frameIn[y - 1][x + 1][band].value;
		if (x > 0)
			sr[3] = frameIn[y][x - 1][band].value;
		sr[4] = frameIn[y][x][band].value;
		if (x < frameIn[0].length - 1)
			sr[5] = frameIn[y][x + 1][band].value;
		if (x > 0 && y < frameIn.length - 1)
			sr[6] = frameIn[y + 1][x - 1][band].value;
		if (y < frameIn.length - 1)
			sr[7] = frameIn[y + 1][x][band].value;
		if (y < frameIn.length - 1 && x < frameIn[0].length - 1)
			sr[8] = frameIn[y + 1][x + 1][band].value;
		return sr;
	}

	private EJCA[] getCARegion33(EJCA[][][] frameIn, int band, int x, int y) {

		EJCA[] sr = new EJCA[9];
		for (int i = 0; i < 9; i++) {
			sr[i] = new EJCA();
		}
		if (y > 0 && x > 0)
			sr[0] = frameIn[y - 1][x - 1][band];
		if (y > 0)
			sr[1] = frameIn[y - 1][x][band];
		if (y > 0 && x < frameIn[0].length - 1)
			sr[2] = frameIn[y - 1][x + 1][band];
		if (x > 0)
			sr[3] = frameIn[y][x - 1][band];
		sr[4] = frameIn[y][x][band];
		if (x < frameIn[0].length - 1)
			sr[5] = frameIn[y][x + 1][band];
		if (x > 0 && y < frameIn.length - 1)
			sr[6] = frameIn[y + 1][x - 1][band];
		if (y < frameIn.length - 1)
			sr[7] = frameIn[y + 1][x][band];
		if (y < frameIn.length - 1 && x < frameIn[0].length - 1)
			sr[8] = frameIn[y + 1][x + 1][band];
		return sr;
	}

}