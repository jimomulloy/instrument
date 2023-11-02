/* EJCell.java */
package comul01.eve;


public class EJCell2 {

	private EJMain ejMain = null;

	private EJSettings ejSettings = null;

	private EJControls ejControls;

	int stateSize = 0;

	int stateNum = 0;

	int stateRange = 0;
	
	int width = 0;
	int height = 0;
	int numBands = 0;
	double z = 0;
	int sum = 0;

	double lp = 0;
	double tp = 0;
	double[] states = null;
	double[] lastp = null;

	/**
	 * initialize the formats
	 */
	public EJCell2(EJMain ejMain) {

		this.ejMain = ejMain;
		ejSettings = ejMain.getEJSettings();
		ejControls = ejMain.getEJControls();
	}

	/**
	 * initialize the formats
	 */
	public EJCell2(EJMain ejMain, int stateRange, int stateNum) {

		this(ejMain);
		this.stateRange = stateRange;
		this.stateNum = stateNum;
		this.stateSize = stateRange/(stateNum-1);
		
		stateRange = stateSize * stateNum;

	}

	public void process(EFrame frameLast, EFrame frameThis,
			EFrame lastStates, EFrame thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		width = frameLast.getWidth();
		height = frameLast.getHeight();
		numBands = frameLast.getPixelStride();
		z = 0;
		sum = 0;

		lp = (double) frameLast.getPixel(b, x, y);
		tp = (double) frameThis.getPixel(b, x, y);
		states = getSampleRegion33(lastStates, b, x, y);
		lastp = getSampleRegion33(frameLast, b, x, y);
		
		thisStates.setPixel(b, x, y, states[4]);

		double value = 0;
				
		if (caType == 0){
			value = processLife(frameLast,  frameThis,
					lastStates,  thisStates, x,y, b,
					t,caType, option);
		} else if (caType == 1){
			value = processFungus(frameLast,  frameThis,
					lastStates,  thisStates, x,y, b,
					t,caType, option);
		} else if (caType == 2){
			value = processPsyche(frameLast,  frameThis,
					lastStates,  thisStates, x,y, b,
					t,caType, option);
		} else if (caType == 3){
			value = processDemon(frameLast,  frameThis,
					lastStates,  thisStates, x,y, b,
					t,caType, option);
		}

		thisStates.setPixel(b, x, y, value);
				
		double newValue = value + tp;

		if (x == 100 && y == 100) {
			System.out.println("EJCell 2 " + newValue + " ," + tp);
		}

		if (newValue > 255)
			newValue = 255;
		frameThis.setPixel(b, x, y, newValue);

	}
	
	
	public double processLife(EFrame frameLast, EFrame frameThis,
			EFrame lastStates, EFrame thisStates, int x, int y, int b,
			double t, int caType, boolean option) {

		
		int total = 0;
		int living = countLive(states,t);
		double value = states[4];

		if (x == 100 && y == 100) {
			System.out.println("EJCell 1 " + tp + " ," + states[4]+ " ," + stateSize
					+ " ," + stateRange
					+ " ," + stateNum +", "+living+", "+t);
		}

		if (states[4] < t*stateSize) {

			if ( living == 2) {

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
		
		double value1 = tp;
		double abs1 = (int)Math.abs(value1/stateRange);
		value1 = value1 - abs1*stateRange;
		double value2 = lp;
		double abs2 = (int)Math.abs(value2/stateRange);
		value2 = value2 - abs2*stateRange;

		if (option){
			value = value - value1 + value2;
			if (value > stateRange)
				value=stateRange;
			if (value < 0)
				value=0;
		}
		return value;
		
	
	}

	
	public double processFungus(EFrame frameLast, EFrame frameThis,
			EFrame lastStates, EFrame thisStates, int x, int y, int b,
			double t, int caType, boolean option) {
	

		int total = 0;
		int living = countLive(states,t);
		double value = 0;

		if (x == 100 && y == 100) {
			System.out.println("EJCell 1 " + tp + " ," + states[4]);
		}

		int white = 0;
		for (int i = 0; i < states.length; i++) {
			if ((states[i] > t) && i != 4)
				white++;
		}

		if (states[4] < t*stateSize) {
			if (white == 3) {
				value = stateRange;
			}
		} else {

			double avg = 0;
			for (int i = 0; i < states.length; i++) {
				if (i != 4)
					avg += states[i];
			}
			avg = avg / 8;
			double diff = Math.abs(avg - states[4]);
			if (diff <t*stateSize)
				value = 0;
			else {

				value = Math.min(avg, states[4]);

			}

		}
		
		if (value >= stateRange) value=0;
		
	
		return value;

	}

	public double processPsyche(EFrame frameLast, EFrame frameThis,
			EFrame lastStates, EFrame thisStates, int x, int y, int b,
			double t, int caType, boolean option) {
	
		double value = 0;

		if (x == 100 && y == 100) {
			System.out.println("EJCell 1 " + tp + " ," + states[4]);
		}

		double avg = 0;
		for (int i = 0; i < states.length; i++) {
			if (i != 4)
				avg += states[i];
		}
		avg = avg / 8;
		value = avg + stateSize;
		
		if (value >= stateRange) value=0;
		
		return value;
		
	}
	
	

	public double processDemon(EFrame frameLast, EFrame frameThis,
			EFrame lastStates, EFrame thisStates, int x, int y, int b,
			double t, int caType, boolean option) {
	
		double value = 0;

		double avg = 0;
		for (int i = 0; i < states.length; i++) {
			if (i != 4 && states[i]>states[4]){
				if ((states[i] - states[4]) > stateSize*t && (states[i] - states[4]) < (stateSize+stateSize*t)){
					value = states[i];
					break;
				}
			}	
		}
	
		if (value >= stateRange) value=0;
		
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

	private double getDeathValue(double total, double state) {

		double value = 0;

		return value;

	}

	private int countLive(double[] s, double t) {

		int count = 0;
		for (int i = 0; i < s.length; i++) {
			if (s[i] > stateSize*t && i != 4)
				count++;
		}

		return count;

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

	private double[] getSampleRegion33(EFrame frameIn, int band, int x, int y) {
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

}