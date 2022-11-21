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

package jomu.instrument.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import be.tarsos.dsp.util.PitchConverter;
import jomu.instrument.world.tonemap.PitchSet;
import jomu.instrument.world.tonemap.TimeSet;
import jomu.instrument.world.tonemap.ToneMap;
import jomu.instrument.world.tonemap.ToneMapElement;
import jomu.instrument.world.tonemap.ToneTimeFrame;

public class ToneMapView extends JComponent implements ComponentListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3729805747119272534L;

	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;

	private int position;

	private PitchSet currentPitchSet;

	private TimeSet currentTimeSet;

	private double timeAxisEnd;

	private double timeAxisStart;

	private int pitchAxisEnd;

	private int pitchAxisStart;

	private int currentWidth = 0;
	private int currentHeight = 0;

	private int minCents = 0;

	private int maxCents = 1200 * 10;

	public ToneMapView() {
		this.pitchAxisStart = 0;
		this.pitchAxisEnd = 20000;
		this.timeAxisStart = 0;
		this.timeAxisEnd = 20000;
		this.addComponentListener(this);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		position = 0;
		this.currentWidth = getWidth();
		this.currentHeight = getHeight();
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	public void drawToneMap(ToneMap toneMap) {
		if (bufferedImage == null) {
			bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
			bufferedGraphics = bufferedImage.createGraphics();
			this.currentWidth = getWidth();
			this.currentHeight = getHeight();
		}
		ToneTimeFrame ttf = toneMap.getTimeFrame();
		if (ttf != null) {
			TimeSet timeSet = ttf.getTimeSet();
			System.out.println(">>!!draw tm: " + timeSet.getStartTime() + ", " + timeSet.getEndTime());
			PitchSet pitchSet = ttf.getPitchSet();
			updateAxis(timeSet, pitchSet);
			double timeStart = timeSet.getStartTime() * 1000;
			double timeEnd = timeSet.getEndTime() * 1000;
			if (timeStart > timeAxisEnd) {
				timeAxisStart = timeStart;
				timeAxisEnd = timeStart + 20000;
				this.currentWidth = getWidth();
				this.currentHeight = getHeight();
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
			}
			
			if (timeStart == 0) {
				bufferedImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_RGB);
				bufferedGraphics = bufferedImage.createGraphics();
			}
			bufferedGraphics.setColor(Color.black);

			ToneMapElement[] elements = ttf.getElements();
			double lowThreshhold = 0.0;
			double maxAmplitude = -1;
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {

				ToneMapElement toneMapElement = elements[elementIndex];
				Color color = Color.black;
				if (toneMapElement != null) {
					double amplitude = 0.0;
					int width = (int) (timeEnd - timeStart);
					int height = (int) ((100.0/(double)(maxCents - minCents)) * getHeight());
					if (toneMapElement.amplitude > 1.0) {
						amplitude = 100.0 * toneMapElement.amplitude / ttf.getMaxAmplitude();
					}
					int greyValue = (int) (Math.log1p(toneMapElement.amplitude / ttf.getMaxAmplitude())
							/ Math.log1p(1.0000001) * 255);

					if (amplitude > maxAmplitude) {
						maxAmplitude = amplitude;
						color = Color.black;
					}
					if (amplitude <= lowThreshhold) {
						color = Color.black;
					} else {
						greyValue = Math.max(0, greyValue);
						color = new Color(greyValue, greyValue, greyValue);
						System.out.println(">>WIDTH: " + width + ", "+ elementIndex + ", " + pitchSet.getNote(elementIndex)+ ", " + pitchSet.getFreq(elementIndex));
						System.out.println(">>WIDTH minCents: " + minCents + ", "+ maxCents + ", " + getHeight() + ", "+ pitchSet.getNote(elementIndex) * 100);
						System.out.println(">>WIDTH coord: " + getCentsCoordinate(pitchSet.getNote(elementIndex) * 100));
					}

					int centsCoordinate = getCentsCoordinate(pitchSet.getNote(elementIndex) * 100);
					int timeCoordinate = getTimeCoordinate(timeStart);

					bufferedGraphics.setColor(color);
					bufferedGraphics.fillRect(timeCoordinate, centsCoordinate - height, width, height);
					
				}
			}
		}
		drawGrid();	
		repaint();
	}

		
	private void drawGrid() {
		Color gridColor = new Color(50, 50, 50);
	
		for (int i = 0; i < maxCents; i += 100) {
			int centsCoordinate = getCentsCoordinate(i);
			bufferedGraphics.setColor(Color.WHITE);
			bufferedGraphics.drawLine(0, centsCoordinate, 5, centsCoordinate);
			if (i % 1200 == 0) {
				bufferedGraphics.drawString(String.valueOf(i), 10, centsCoordinate);
				bufferedGraphics.setColor(gridColor);
				bufferedGraphics.drawLine(0, centsCoordinate, getWidth() - 1, centsCoordinate);
				System.out.println(">>drawGrid: " + centsCoordinate + ", " + String.valueOf(i));
			}
		}
	    
	    for(int i = 0 ; i <= 20000; i+=1000){
	    	bufferedGraphics.setColor(Color.WHITE);
			int timeCoordinate = getTimeCoordinate(i);
			bufferedGraphics.drawLine(timeCoordinate, getHeight(), timeCoordinate, getHeight() - 5);
			bufferedGraphics.drawString(String.valueOf((int)((timeAxisStart + i) / 1000)), timeCoordinate, getHeight() - 10);
			bufferedGraphics.setColor(gridColor);
			bufferedGraphics.drawLine(timeCoordinate, getHeight(), timeCoordinate, 0);
		}
	}
	
	private int getCentsCoordinate(int cents) {
		return getHeight() - 1 - (int)(((double)(cents - minCents) / (double)maxCents) * getHeight());
	}

	private int getTimeCoordinate(double timeStart) {
		return (int) Math.floor((double) getWidth() * (timeStart / (timeAxisEnd - timeAxisStart)));
	}

	public void paintComponent(final Graphics g) {
		g.drawImage(bufferedImage, 0, 0, null);
	}

	private void updateAxis(TimeSet timeSet, PitchSet pitchSet) {
		this.currentTimeSet = timeSet;
		this.currentPitchSet = pitchSet;
	}

}
