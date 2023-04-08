/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/

package jomu.instrument.audio.features;

import be.tarsos.dsp.beatroot.EventList;

public class OnsetInfo {
	double salience;
	double time;
	EventList onsetList;

	public OnsetInfo(double time, double salience, EventList onsetList) {
		this(time, salience);
		this.onsetList = new EventList(onsetList);
	}

	public OnsetInfo(double time, double salience) {
		super();
		this.time = time;
		this.salience = salience;
	}

	@Override
	public OnsetInfo clone() {
		return new OnsetInfo(time, salience, new EventList(onsetList));
	}

	public double getSalience() {
		return salience;
	}

	public double getTime() {
		return time;
	}

	public EventList getOnsetList() {
		return onsetList;
	}
}
