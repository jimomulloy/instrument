package jomu.instrument.tonemap;

import java.io.Serializable;

/**
 * This class encapsulates a set of data objects that define the internal Tone
 * "Map" This includes a collection of ToneMapElements organised within a
 * "Matrix" of 2 dimensions Time and Pitch coordinates as defined in the
 * TimeSest and PitchSet objects. This class provides methods for accessing the
 * Map as a whole and also provides an inner class for iterating through
 * elements of the Map Matrix.
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */
public class ToneMapMatrix implements Serializable {

	public ToneMapMatrix(int matrixSize, TimeSet timeSet, PitchSet pitchSet) {

		this.matrixSize = matrixSize;
		this.timeSet = timeSet;
		this.pitchSet = pitchSet;
		timeRange = timeSet.getRange();
		pitchRange = pitchSet.getRange();
		matrix = new ToneMapElement[matrixSize];

	}

	public void setMaxPower(double maxFTPower) {
		this.maxFTPower = maxFTPower;
		System.out.println(">>Set max power: " + this.maxFTPower);
		this.maxAmplitude = FTPowerToAmp(maxFTPower);
		System.out.println(">>Set max ampl: " + maxAmplitude);
	}

	public void reset() {

		maxAmplitude = 0;
		minAmplitude = 0;
		maxFTPower = 0;
		minFTPower = 0;
		avgAmplitude = 0;
		avgFTPower = 0;
		long count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				count++;
				avgFTPower += matrix[i].preFTPower;

				if (maxFTPower < matrix[i].preFTPower)
					maxFTPower = matrix[i].preFTPower;
				if ((minFTPower == 0) || (minFTPower > matrix[i].preFTPower))
					minFTPower = matrix[i].preFTPower;
			}
		}

		avgFTPower = avgFTPower / count;

		count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				matrix[i].noteState = 0;
				matrix[i].postFTPower = matrix[i].preFTPower;
				matrix[i].noteListElement = null;
				matrix[i].preAmplitude = FTPowerToAmp(matrix[i].preFTPower);
				matrix[i].postAmplitude = matrix[i].preAmplitude;
				avgAmplitude += matrix[i].preAmplitude;
				if (matrix[i].preAmplitude != -1) {
					avgAmplitude += matrix[i].preAmplitude;
					count++;
					if (maxAmplitude < matrix[i].preAmplitude)
						maxAmplitude = matrix[i].preAmplitude;
					if ((minAmplitude == 0) || (minAmplitude > matrix[i].preAmplitude))
						minAmplitude = matrix[i].preAmplitude;
				}
			}
		}
		avgAmplitude = avgAmplitude / count;
	}

	public void update() {

		maxAmplitude = 0;
		minAmplitude = 0;
		maxFTPower = 0;
		minFTPower = 0;
		avgAmplitude = 0;
		avgFTPower = 0;
		long count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				count++;
				avgFTPower += matrix[i].postFTPower;
				if (maxFTPower < matrix[i].postFTPower)
					maxFTPower = matrix[i].postFTPower;
				if ((minFTPower == 0) || (minFTPower > matrix[i].postFTPower))
					minFTPower = matrix[i].postFTPower;
			}
		}

		avgFTPower = avgFTPower / count;

		count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				matrix[i].postAmplitude = FTPowerToAmp(matrix[i].postFTPower);
				avgAmplitude += matrix[i].postAmplitude;
				if (matrix[i].preAmplitude != -1) {
					avgAmplitude += matrix[i].postAmplitude;
					count++;
					if (maxAmplitude < matrix[i].postAmplitude)
						maxAmplitude = matrix[i].postAmplitude;
					if ((minAmplitude == 0) || (minAmplitude > matrix[i].postAmplitude))
						minAmplitude = matrix[i].postAmplitude;
				}
			}
		}
		avgAmplitude = avgAmplitude / count;
	}

	public void resetOld() {

		maxAmplitude = 0;
		minAmplitude = 0;
		maxFTPower = 0;
		minFTPower = 0;
		avgAmplitude = 0;
		avgFTPower = 0;
		long count = 0;
		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				if (matrix[i].preAmplitude != -1) {

					count++;
					avgAmplitude += matrix[i].preAmplitude;
					avgFTPower += matrix[i].preFTPower;

					if (maxAmplitude < matrix[i].preAmplitude)
						maxAmplitude = matrix[i].preAmplitude;
					if ((minAmplitude == 0) || (minAmplitude > matrix[i].preAmplitude))
						minAmplitude = matrix[i].preAmplitude;
					if (maxFTPower < matrix[i].preFTPower)
						maxFTPower = matrix[i].preFTPower;
					if ((minFTPower == 0) || (minFTPower > matrix[i].preFTPower))
						minFTPower = matrix[i].preFTPower;
				}

			}
		}

		avgAmplitude = avgAmplitude / count;
		avgFTPower = avgFTPower / count;

		for (int i = 0; i < (matrixSize - 1); i++) {
			if (matrix[i] != null) {
				if (matrix[i].preAmplitude != -1) {
					matrix[i].postAmplitude = matrix[i].preAmplitude;
					matrix[i].postFTPower = matrix[i].preFTPower;
					matrix[i].noteState = 0;
					matrix[i].noteListElement = null;
				}

			}
		}
	}

	public void setAmpType(boolean ampType) {
		this.ampType = ampType;
	}

	public void setHighThres(int highThres) {
		this.highThres = highThres;
	}

	public void setLowThres(int lowThres) {
		this.lowThres = lowThres;
	}

	public double FTPowerToAmp(double FTPower) {
		double amplitude = 0.0;
		if (FTPower <= 0.0)
			return 0.0;
		if (ampType == LOGAMP) {
			if (minFTPower < maxFTPower / (double) lowThres * 10.0)
				minFTPower = maxFTPower / (double) lowThres * 10.0; // ??
			double logMinFTPower = Math.abs(Math.log(minFTPower / maxFTPower));
			// amplitude = (logMinFTPower - Math.abs(Math.log(FTPower / maxFTPower))) /
			// logMinFTPower;
			amplitude = (20 * Math.log(1 + Math.abs(FTPower)) / Math.log(10));
			if (amplitude < 0)
				amplitude = 0.0;// ??
		} else {
			if ((getMaxFTPower() - getMinFTPower()) <= 0) {
				amplitude = 0.0;
			} else {
				double minpow = getMinFTPower() + (getMaxFTPower() - getMinFTPower()) * ((double) lowThres / 100.0);
				double maxpow = getMaxFTPower()
						- (getMaxFTPower() - getMinFTPower()) * ((double) (100 - highThres) / 100.0);
				if (FTPower > maxpow) {
					amplitude = 1.0;
				} else if (FTPower < minpow) {
					amplitude = 0.0;
				} else {
					amplitude = Math.sqrt(FTPower - minpow) / Math.sqrt(maxpow - minpow);
				}
			}
		}

		return amplitude;
	}

	public double getMaxAmplitude() {
		return maxAmplitude;
	}

	public double getMinAmplitude() {
		return minAmplitude;
	}

	public double getAvgAmplitude() {
		return avgAmplitude;
	}

	public double getMaxFTPower() {
		return maxFTPower;
	}

	public double getMinFTPower() {
		return minFTPower;
	}

	public double getAvgFTPower() {
		return avgFTPower;
	}

	public TimeSet getTimeSet() {
		return timeSet;
	}

	public PitchSet getPitchSet() {
		return pitchSet;
	}

	public int getTimeRange() {
		return timeRange;
	}

	public int getPitchRange() {
		return pitchRange;
	}

	public int getMatrixSize() {
		return matrixSize;
	}

	public Iterator newIterator() {
		return new Iterator();
	}

	public class Iterator {

		public Iterator() {
			index = 0;
			timeIndex = 0;
			pitchIndex = 0;
		}

		public ToneMapElement getElement() {
			return matrix[index];
		}

		public void setElement(ToneMapElement toneMapElement) {
			matrix[index] = toneMapElement;
		}

		public void newElement(double amplitude, double FTPower) {
			toneMapElement = new ToneMapElement(amplitude, FTPower, index, timeIndex, pitchIndex);

			matrix[index] = toneMapElement;
		}

		public boolean nextTime() {
			if (timeIndex < (timeRange - 1)) {
				timeIndex++;
				index++;
				return true;
			} else
				return false;
		}

		public boolean nextPitch() {
			if (pitchIndex < (pitchRange - 1)) {
				pitchIndex++;
				index = index + timeRange;
				return true;
			} else
				return false;
		}

		public boolean next() {
			if (index < (matrixSize - 1)) {
				index++;
				if (timeIndex < (timeRange)) {
					timeIndex++;
				} else {
					timeIndex = 0;
					pitchIndex++;
				}
				return true;
			} else
				return false;

		}

		public boolean isNextTime() {
			return (timeIndex < (timeRange - 1));
		}

		public boolean isNextPitch() {
			return (pitchIndex < (pitchRange - 1));
		}

		public boolean isNext() {
			return (index < (matrixSize - 1));
		}

		public boolean isLastTime() {
			return (timeIndex == (timeRange - 1));
		}

		public boolean isLastPitch() {
			return (pitchIndex == (pitchRange - 1));
		}

		public boolean isLast() {
			return (index == (matrixSize - 1));
		}

		public ToneMapElement readElementAt(double time, double pitch) {
			return element;
		}

		public boolean prevTime() {
			if (timeIndex > 0) {
				timeIndex--;
				index--;
				return true;
			} else
				return false;

		}

		public boolean prevPitch() {
			if (pitchIndex > 0) {
				pitchIndex--;
				index = index - timeRange;
				return true;
			} else
				return false;

		}

		public boolean prev() {
			if (index > 0) {
				index--;
				if (timeIndex > 0) {
					timeIndex--;
				} else {
					timeIndex = timeRange - 1;
					pitchIndex--;
				}
				return true;
			} else
				return false;
		}

		public void lastTime() {
			index = timeRange - timeIndex + index;
			timeIndex = timeRange - 1;
		}

		public void lastPitch() {
			index = timeRange * (pitchRange - pitchIndex - 2) + timeIndex;
			pitchIndex = pitchRange - 1; // ??
		}

		public void last() {
			index = (timeRange * pitchRange) - 1;
			timeIndex = timeRange - 1;
			pitchIndex = pitchRange - 1;
		}

		public void firstTime() {
			index = index - timeIndex;
			timeIndex = 0;
		}

		public void firstPitch() {
			index = timeIndex;
			pitchIndex = 0;
		}

		public void first() {
			timeIndex = 0;
			pitchIndex = 0;
			index = 0;
		}

		public int getTimeIndex() {
			return timeIndex;
		}

		public int getPitchIndex() {
			return pitchIndex;
		}

		public int getIndex() {
			return index;
		}

		public void setTimeIndex(int index) {
			this.index = this.index + (index - timeIndex);
			timeIndex = index;
		}

		public void setPitchIndex(int index) {
			this.index = this.index + (timeRange * (index - pitchIndex));
			pitchIndex = index;
		}

		public void setIndex(int index) {
			this.index = index;
			pitchIndex = index / timeRange;
			timeIndex = index % timeRange;

		}

		private int timeIndex;
		private int pitchIndex;
		private int index;

	} // End Iterator

	private ToneMapElement[] matrix;
	private ToneMapElement toneMapElement;
	private TimeSet timeSet;
	private PitchSet pitchSet;
	private int timeRange;
	private int pitchRange;
	private double minAmplitude;
	private double maxAmplitude;
	private double minFTPower;
	private double maxFTPower;
	private double avgAmplitude;
	private double avgFTPower;
	private int matrixSize;
	private ToneMapElement element;
	private int lowThres;
	private int highThres;
	private boolean ampType;
	public static final boolean POWERAMP = false;
	public static final boolean LOGAMP = true;
} // End ToneMapMatrix