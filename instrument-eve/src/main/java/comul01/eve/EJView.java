	/*
 * %W% %E%
 *
 * Copyright (c) 1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
package comul01.eve;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.control.*;
import javax.media.datasink.*;
// import javax.media.bean.playerbean.*;  // Removed - not available in FMJ and not used
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.Vector;
import java.io.File;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import javax.media.datasink.*;
import javax.media.protocol.*;
import java.io.IOException;
//import jomu.jmf20.media.effect.video.*;
import javax.media.control.MonitorControl;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.media.util.*;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.RenderableOp;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.*;
import java.awt.image.renderable.RenderedImageFactory;
import java.text.*;


public class EJView extends JPanel implements ControllerListener, DataSinkListener, ProgressListener {

	static final int PROCESS_BLANK = 1;
	static final int PROCESS_EFFECT = 2;
	static final int PROCESS_MIX = 3;
	static final int PROCESS_JOIN = 4;
	static final String defaultOutputFile = "file:/C:/java/jmf/minime/test.avi";
  
	FramePositioningControl fpc;
    String [] mediaFiles = {" ", " "};
	VideoCutPanel vcPanel1;
    VideoCutPanel vcPanel2;
    VideoCutPanel vcPanel3;
	VideoCutPanel vcPanel4;
	JFrame pFrame;
    Dimension preferredSize = new Dimension(100, 100);
    JComboBox cbEffect;
    JComboBox cbDuration;
    //JButton buttonExit;
    //JButton buttonGo;
    JProgressBar jProgress;
    String outputFile = new String(defaultOutputFile);
    Image texture;
    DataSink filewriter = null;
	JFrame mainFrame;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	Processor p = null;
	EJMain ejMain;
	EJSettings ejSettings;
	double effectDuration;
	JPanel ejViewPanel;
	JPanel effectPanel;
	int processType; 
	int loopCount=0;
	String mediaFile1 = null;
	String mediaFile2 =	null;
	String mediaFileName1 = null;
	String mediaFileName2 =	null;
	int outNumber =	0;
	
	Time beginTime1 = null;
	Time beginTime2 = null;
	Time endTime1 = null;
	Time endTime2 = null;
	boolean firstPass = true;
	//MediaPlayer mp = null;

	
    public EJView(EJMain ejMain) {

	this.ejMain = ejMain;
	ejViewPanel = new JPanel();
	setLayout(new BorderLayout());
	setOpaque(false);
	ejViewPanel.setLayout(new BorderLayout());
	ejViewPanel.setOpaque(false);
	
	System.out.println("EJViewWOOW");
	vcPanel1 = new VideoCutPanel(mediaFiles, true, mainFrame );
	vcPanel2 = new VideoCutPanel(mediaFiles, true, mainFrame);
	vcPanel3 = new VideoCutPanel(mediaFiles, false, mainFrame);
	vcPanel4 = new VideoCutPanel(mediaFiles, true, mainFrame);
	vcPanel1.setEJMain(ejMain);
	vcPanel2.setEJMain(ejMain);
	vcPanel3.setEJMain(ejMain);
	vcPanel4.setEJMain(ejMain);

	vcPanel3.setFileName(outputFile);

	JScrollPane inAP = new JScrollPane(vcPanel1);
	JScrollPane inBP = new JScrollPane(vcPanel2);
	JScrollPane inCP = new JScrollPane(vcPanel4);
	JScrollPane resultP = new JScrollPane(vcPanel3);

	JPanel panelTop = new JPanel();
	JPanel panelBottom = new JPanel();
	panelTop.add(inAP);
	panelTop.add(resultP);
	panelBottom.add(inBP);

	JSplitPane topSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inAP, resultP);
	topSP.setOneTouchExpandable(true);
	topSP.setDividerLocation(540);
	//Dimension minimumSize = new Dimension(100, 50);
	//viewPane.setMinimumSize(minimumSize);
	//settingsPane.setMinimumSize(minimumSize);
	//splitPane.setPreferredSize(new Dimension(400,200));

	JSplitPane bottomSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inBP, inCP);
	bottomSP.setOneTouchExpandable(true);
	bottomSP.setDividerLocation(540);
	//Dimension minimumSize = new Dimension(100, 50);
	//viewPane.setMinimumSize(minimumSize);
	//settingsPane.setMinimumSize(minimumSize);
	//splitPane.setPreferredSize(new Dimension(400,200));

	JSplitPane viewSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSP, bottomSP);
	viewSP.setOneTouchExpandable(true);
	viewSP.setDividerLocation(540);
	//Dimension minimumSize = new Dimension(100, 50);
	//viewPane.setMinimumSize(minimumSize);
	//settingsPane.setMinimumSize(minimumSize);
	//splitPane.setPreferredSize(new Dimension(400,200));


	ejViewPanel.add(viewSP, BorderLayout.CENTER);

	texture = Toolkit.getDefaultToolkit().getImage("texture.jpg");
    }

    public void paint(Graphics g) {
	if (texture != null) {
	    int width = texture.getWidth(this);
	    int height = texture.getHeight(this);
	    if (width > 0 && height > 0)
		for (int y = 0; y < preferredSize.height + height; y += height) {
		    for (int x = 0; x < preferredSize.width + width; x += width) {
			g.drawImage(texture, x, y, this);
		    }
		}
	}
	super.paint(g);
    }

	public JPanel getPanel() {
		return ejViewPanel;
	}

    public void process(int processType) {
	
	this.processType = processType;
	System.out.println("EJView process "+processType);

	ejSettings = ejMain.getEJSettings();
	
	loopCount = ejSettings.effectTrans.loop;	
	
	vcPanel3.setURL(null);
	String name = vcPanel3.getFileName();
	mediaFile1 = vcPanel1.getMediaFile();
	mediaFile2 = vcPanel2.getMediaFile();
	mediaFileName1 = vcPanel1.getFileName();
	mediaFileName2 = vcPanel2.getFileName();
	
	if (outNumber > 999) outNumber = 0;
	outNumber++;
	NumberFormat nf;
	nf = NumberFormat.getNumberInstance();
	nf.setMaximumFractionDigits(0);
	nf.setMinimumFractionDigits(0);
	nf.setMaximumIntegerDigits(3);
	nf.setMinimumIntegerDigits(3);
	File newFile = null;
	if (mediaFileName1 != null && ejMain.isOutputAuto()) {
		outputFile = mediaFileName1.substring(0, mediaFileName1.length()-4)
			+"-"
			+nf.format(outNumber)+".avi";
		newFile = new File(outputFile); 	
		//vcPanel3.save(newFile);
	} else 
	if (name != null && ejMain.isOutputSave() ) { 
		System.out.println("Saving from: "+name);

		outputFile = new String(name);
		//newFile = new File(outputFile); 	
		//vcPanel3.save(newFile);
		System.out.println("Saving to: "+outputFile);

	} else {
		outputFile = new String(defaultOutputFile);
		newFile = new File(outputFile); 	
		//vcPanel3.save(newFile);

	}
	

	if (processType == PROCESS_BLANK) {
		MediaLocator oml;
		if ((oml = createMediaLocator(outputFile)) == null) {
		   System.err.println("Cannot build media locator from: " + outputFile);
		   System.exit(0);
		}
		RGBToMovie rgbToMovie = new RGBToMovie();
		int bwidth = ejSettings.effectTrans.width;
		int bheight = ejSettings.effectTrans.height;
		int frameRate = ejSettings.effectTrans.frameRate;
		int numFrames = frameRate*ejSettings.effectTrans.duration;
		rgbToMovie.doIt(bwidth, bheight, frameRate, numFrames, oml);
		System.out.println("done rgb to movie");
		vcPanel3.setURL(outputFile);
		return;
	}
	
	beginTime1 = vcPanel1.getBeginTime();
	beginTime2 = vcPanel2.getBeginTime();
	endTime1 = vcPanel1.getEndTime();
	endTime2 = vcPanel2.getEndTime();

	effectDuration = (double)(endTime1.getNanoseconds() - beginTime1.getNanoseconds());
	ejSettings.sortEffects();
	ejSettings.setDuration(effectDuration);

	ejSettings.effectCompo.grabberA = vcPanel1;
	ejSettings.effectCompo.grabberB = vcPanel2;
	ejSettings.effectCompo.grabberM = vcPanel4;

	System.out.println("EJ Times 1: "+beginTime1.getNanoseconds()+", "+endTime1.getNanoseconds());

	long compoDuration = 0;
	long d1 = 0;
	long d2 = 0;
	if (mediaFile2 != null) {
		System.out.println("EJ Test 1");
		

		d1 = endTime1.getNanoseconds() - beginTime1.getNanoseconds();
		d2 = endTime2.getNanoseconds() - beginTime2.getNanoseconds();

		if (ejSettings.effectCompo.duration != 0) {
			System.out.println("EJ Test 2");
	
			if ((ejSettings.effectCompo.duration < d1)
				&& (ejSettings.effectCompo.duration < d2)) {
				compoDuration = ejSettings.effectCompo.duration;
			} else {
				if (d1 > d2) {
					compoDuration = d2;
				} else {
					compoDuration = d1;
				}
			}
		} else {
			System.out.println("EJ Test 3");
	
			if (((endTime1.getNanoseconds() - beginTime1.getNanoseconds()) > ejSettings.effectCompo.start)
				&& ((endTime2.getNanoseconds() - beginTime2.getNanoseconds()) > ejSettings.effectCompo.start)) {
				if ((d1-ejSettings.effectCompo.start) > d2) {
					compoDuration = d2;
				} else {
					compoDuration = (d1-ejSettings.effectCompo.start);
				}
			}
		}

		System.out.println("EJ Times 2: "+beginTime2.getNanoseconds()+", "+endTime2.getNanoseconds());

		System.out.println("EJ Compo Times: "+d1+", "+d2+", "+compoDuration+", "+ejSettings.effectCompo.start+", "+
					ejSettings.effectCompo.duration);

	}

	if (mediaFile1 == null) return;
	vcPanel1.stop();
	vcPanel2.stop();

	if (processType == PROCESS_JOIN) {
		MediaLocator oml, iml1, iml2;
		MediaLocator[] iml;
		if ((iml1 = createMediaLocator(mediaFile1)) == null) {
		   System.err.println("Cannot build media locator from: " + mediaFile1);
		   System.exit(0);
		}
		if ((iml2 = createMediaLocator(mediaFile2)) == null) {
		   System.err.println("Cannot build media locator from: " + mediaFile2);
		   System.exit(0);
		}
		iml = new MediaLocator[2];
		iml[0] = iml1;
		iml[1] = iml2;
		if ((oml = createMediaLocator(outputFile)) == null) {
		   System.err.println("Cannot build media locator from: " + outputFile);
		   System.exit(0);
		}
	
		Concat concat = new Concat();
		concat.doIt(iml, oml);
		System.out.println("done concat");
		vcPanel3.setURL(outputFile);
		return;
	}

	System.out.println("SGDS parms: "+mediaFile1+", "+mediaFile2+", "
		+outputFile+", "+mediaFileName1+", "+mediaFileName2);
	
	
	Vector plugin = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
	String pname = null;
	for (int i = 0; i<plugin.size(); i++) {
		pname = (String)plugin.elementAt(i);
		System.out.println("CODEC :"+pname);
		if (pname.endsWith("Decoder")
			|| pname.equals("com.ibm.media.codec.video.mpeg.MpegVideo")
			|| pname.startsWith("com.sun.media.codec.video.colorspace")
			|| pname.equals("comul01.eve.R210Codec")  // Keep R210 codec for 10-bit video support
			/* pname.equals("com.ibm.media.codec.video.h263.NativeEncoder")*/
			/* pname.equals("com.sun.media.codec.video.jpeg.NativeEncoder")*/) {
			System.out.println("codec enabled");
		} else {
			PlugInManager.removePlugIn(pname, PlugInManager.CODEC);
		}
	}
	try {
		if (mediaFile2 != null && processType == PROCESS_MIX) {
			SuperGlueDataSource sgds = new SuperGlueDataSource(
					ejMain,
				    new String [] { mediaFile1, mediaFile2 },
				    new Time [] { beginTime1, beginTime2 },
				    new Time [] { endTime1, endTime2 },
				    new Time [] { new Time(compoDuration), new Time(compoDuration) },
				    new String[] { "" },
				    new String[0],
				    new Dimension(160, 120));
	    	sgds.connect();
	    	sgds.setProgressListener(this);
	    	p = Manager.createProcessor(sgds);
		} else {
			// Try standard FMJ processor first, fall back to HumbleVideoProcessor
			try {
				p = Manager.createProcessor(new MediaLocator(mediaFile1));
			} catch (NoProcessorException e) {
				System.out.println("FMJ processor failed, trying HumbleVideoProcessor: " + e.getMessage());
				// Use HumbleVideoProcessor for formats not supported by FMJ (e.g., R210)
				try {
					HumbleVideoProcessor hvp = new HumbleVideoProcessor(new MediaLocator(mediaFile1));
					p = hvp;
					System.out.println("HumbleVideoProcessor created for: " + mediaFile1);
				} catch (Exception ex) {
					System.err.println("HumbleVideoProcessor creation also failed: " + ex.getMessage());
					ex.printStackTrace();
					throw ex;
				}
			}
		}

		p.addControllerListener(this);

		// Put the Processor into configured state.
		p.configure();
		System.out.println("EJView before configure");

		if (!waitForState(p.Configured)) {
		    System.err.println("Failed to configure the processor.");
			//buttonGo.setEnabled(true);
			return;
	    }
	
		//p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME));
		p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.MSVIDEO));
	
		// Obtain the track controls. (stole this from EffectTest5 :) !!
			
		TrackControl videoTrack = null;
	 	
		if (!(mediaFile2 != null && processType == PROCESS_MIX)) {

			TrackControl tc[] = p.getTrackControls();

			if (tc == null) {
			    System.out.println("Failed to obtain track controls from the processor.");
			    return;
			}
			
			videoTrack = null;
	 		VideoFormat oldFormat=null, newFormat=null;
			boolean firstVideo = true;
			for (int i = 0; i < tc.length; i++) {
			    if (tc[i].getFormat() instanceof VideoFormat && firstVideo) {
					videoTrack = tc[i];
					firstVideo = false;
					oldFormat =  (VideoFormat)videoTrack.getFormat();
					Dimension newSize = oldFormat.getSize();
					float newRate = 15.0F;
					System.out.println("old format "+oldFormat.getSize()+", "+oldFormat.getFrameRate()+", "+
							oldFormat.getDataType());
					newFormat = (VideoFormat)(new VideoFormat(null, newSize, Format.NOT_SPECIFIED, oldFormat.getDataType(),oldFormat.getFrameRate())).intersects(oldFormat);
				
				}
				if (tc[i].getFormat() instanceof AudioFormat && !ejSettings.effectTrans.audioOn) {
					tc[i].setEnabled(false);
				}
					
			}
			
			if (newFormat != null) {
				Format[] supported;
				Format f;
				boolean fflag = false;
				for (int i = 0; i < tc.length; i++) {
			
					supported = tc[i].getSupportedFormats();
			
					if (supported == null) continue;
			
				    for (int j = 0; j < supported.length &&!fflag; j++ ){
			
						if (newFormat.matches(supported[j]) &&
							(f = newFormat.intersects(supported[j])) != null &&
							tc[i].setFormat(f) != null) {
							fflag = true;
						}
					}
			
				}
				if (videoTrack == null) {
				    System.out.println("The input media does not contain a video track.");
				    return;
				}
		
				System.out.println("Video format: " + videoTrack.getFormat());
				try {
				    Codec codec[] = { new EffectControl(ejMain)};
				    
				    // !!!!!
				    videoTrack.setCodecChain(codec);
			    	System.out.println("!!SET CODEC CHAIN");
			
				} catch (UnsupportedPlugInException e) {
				    System.err.println("The process does not support effects.");
				}
			}
		}
		//p = new MediaPlayer();
		//mp.setPlayer(p);
		
		p.realize();
		if (!waitForState(p.Realized)) {
		    System.err.println("Failed to realize the processor.");
			//buttonGo.setEnabled(true);
			return;
	 	}	
		//p.setTimeBase(null);
		QualityControl qc = null;
		if (videoTrack != null) {
			qc = (QualityControl)videoTrack.getControl("javax.media.control.QualityControl");
		}
		
		if (mediaFile2 == null || processType == PROCESS_EFFECT) {
			p.setTimeBase(null);
			p.setMediaTime(beginTime1);
			p.setStopTime(endTime1);
		}
		
	 	System.out.println("Test minime 3");
		p.prefetch();
		if (!waitForState(p.Prefetched)) {
			System.err.println("Failed to prefetch the processor.");
			return;
		}
		System.out.println("Test minime 4");

		pFrame = new JFrame("MiniME - Mini Media Porcessor");
		pFrame.getContentPane().setLayout(new BorderLayout());
		
		Control controls[] = p.getControls();
		Panel monitorPanel = null;
		Component monitorComp = null;
		Dimension size = null;
		for (int i = 0; i < controls.length; i++) {
			if (controls[i] instanceof MonitorControl) {
			    MonitorControl mc = (MonitorControl)controls[i];
			    monitorComp = mc.getControlComponent();
			    if (monitorPanel == null) {
					monitorPanel = new Panel();
					//monitorPanel.setLayout(new GridLayout(0, 1));
				}
			    if (monitorComp != null) monitorPanel.add(monitorComp);
				mc.setEnabled(true);
			}
		}
		if (monitorPanel != null)
			pFrame.getContentPane().add("Center", monitorPanel);

		// Display the processor's control component.
		Component cc;
	
		if ((cc = p.getControlPanelComponent()) != null) {
			cc.setSize(800,600);
			pFrame.getContentPane().add("South", cc);
		}
			
		if (qc != null) {
			System.out.println("qc quality "+qc.getPreferredQuality());	
			//qc.setQuality(qc.getPreferredQuality());
			pFrame.getContentPane().add("North", qc.getControlComponent());
		}	
		

		System.out.println("Test minime 5: "+beginTime1+", "+endTime1);

		if (mediaFile2 == null || processType == PROCESS_EFFECT) {
			//p.setTimeBase(null);
			//p.setMediaTime(beginTime1);
			//p.setStopTime(endTime1);
		}
		Time mediaTime = p.getMediaTime();
		System.out.println("Test minime 60: "+beginTime1+", "+endTime1+", "+mediaTime.getNanoseconds());
		
		//p.start();
		if (mediaFile2 == null || processType == PROCESS_EFFECT) {
			//mp.setTimeBase(null);
			//mp.setMediaTime(beginTime1);
		}
		/*
		fpc = (FramePositioningControl)p.getControl("javax.media.control.FramePositioningControl");
		if (fpc == null) {
			System.out.println("FPC not supported");
		}
		else {
			int offsetFrame = fpc.mapTimeToFrame(beginTime1);
			fpc.seek(offsetFrame);
			System.out.println("Got FPC");
		}
		*/
		DataSource ds = p.getDataOutput();
		try {
			System.out.println("DS test 1 "+outputFile);

			// Try FMJ DataSink first, fall back to HumbleDataSink
			try {
				filewriter = Manager.createDataSink(ds, new MediaLocator(outputFile));
				System.out.println("DS test 2 - FMJ DataSink created");
			} catch (NoDataSinkException e) {
				System.out.println("FMJ DataSink failed, trying HumbleDataSink: " + e.getMessage());
				filewriter = new HumbleDataSink(ds, new MediaLocator(outputFile));
				System.out.println("DS test 2 - HumbleDataSink created");
			}

		    StreamWriterControl fwc = (StreamWriterControl)
			filewriter.getControl("javax.media.control.StreamWriterControl");
		    filewriter.open();
		} catch (IOException e) {
		    System.out.println("IOException: " + e.getMessage());
			e.printStackTrace();
		    return;
		} catch (SecurityException e) {
		    System.out.println("SecurityException: " + e.getMessage());
		    return;
		}
		System.out.println("Test minime 3");
		filewriter.addDataSinkListener(this);
		try {
		    filewriter.start();
			ds.start();
		    System.out.println("Started filewriter/ds");
		} catch (IOException e) {
		    System.out.println("Error starting file writer");
			return;
		}
		
		System.out.println("Test minime 61: "+beginTime1+", "+endTime1);
		
		pFrame.setSize(800, 600);
		pFrame.addWindowListener( new WindowAdapter() {
			    public void windowClosing(WindowEvent we) {
			    }
		} );
		pFrame.setVisible(true);
		pFrame.pack();
		return;
	} catch (Exception ex) {
	    //buttonGo.setEnabled(true);
	    System.out.println("Exception creating processor: " + ex);
	    ex.printStackTrace();
		return;
	}
    }

    public void updateProgress(long current, long duration) {

		//jProgress.setMaximum((int) (duration / 1000000));
		//jProgress.setValue((int) (current / 1000000));
    }

	/**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(int state) {
	synchronized (waitSync) {
	    try {
		System.out.println("before wait for state");
		while (p.getState() < state && stateTransitionOK)
		    waitSync.wait();
	    } catch (Exception e) {}
	}
	return stateTransitionOK;
    }

    public synchronized void dataSinkUpdate(DataSinkEvent event) {
	if (event instanceof EndOfStreamEvent) {
	    //buttonGo.setEnabled(true);
	    filewriter.close();
	}
	//jProgress.setMaximum(100);
	//jProgress.setValue(100);
	System.out.println("OUTPUT file "+outputFile);
	vcPanel3.setURL(outputFile);
    }

    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

	if (evt instanceof ConfigureCompleteEvent ||
	    evt instanceof RealizeCompleteEvent ||
	    evt instanceof PrefetchCompleteEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = true;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof ResourceUnavailableEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = false;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof EndOfMediaEvent ||
			   evt instanceof StopAtTimeEvent || 
			   evt instanceof StopEvent) {
		if (evt instanceof EndOfMediaEvent)  {
		 	System.out.println("end of media event");
		} else if (evt instanceof StopAtTimeEvent)  {
		 	 System.out.println("Stop at time event");
		} else if (evt instanceof StopEvent)  {
		 	 System.out.println("Stop event");
			}
	
		if (mediaFile2 == null || processType == PROCESS_EFFECT ) {
			loopCount--; 
			if (loopCount < 0) {
				System.out.println("loop count ended "+loopCount+", "+beginTime1.getNanoseconds()+", "+endTime1.getNanoseconds()+", "+evt);
				loopCount = 0;
			} else {
				System.out.println("loop count "+loopCount+", "+ beginTime1.getNanoseconds()+", "+endTime1.getNanoseconds()+", "+p.getState()+evt);
				if (fpc == null) {
					System.out.println("FPC not supported");	
				}	
				else {
					int offsetFrame = fpc.mapTimeToFrame(beginTime1);
					fpc.seek(offsetFrame);
					System.out.println("Got FPC");
				}
			
				p.setMediaTime(beginTime1);
				p.setStopTime(endTime1);
				System.out.println("state "+p.getState());
				System.out.println("Restart processor");
				sleep(1000);
				p.start();
				System.out.println("Restarted");
				return;
			}
		}
	    p.close();
		effectPanel = null;
		pFrame.dispose();
		filewriter.close();
		//jProgress.setMaximum(100);
		//jProgress.setValue(100);
		//vcPanel3.setURL(outputFile);

	} else if (evt instanceof SizeChangeEvent) {
		 System.out.println("Size change event");
	
	} else if (evt instanceof DurationUpdateEvent)  {
	 	System.out.println("Duration update event");
	} else if (evt instanceof StopTimeChangeEvent)  {
	 	 System.out.println("Stop time change event");
	 } else if (evt instanceof MediaTimeSetEvent)  {
	 	 System.out.println("media time set Event");
	 }else if (evt instanceof ControllerClosedEvent)  {
	  	 System.out.println("controller closed Event");
	 }else if (evt instanceof StopEvent)  {
	  	 System.out.println("Stop Event");
	} else if (evt instanceof StartEvent)  {
		//p.setMediaTime(beginTime1);
		 System.out.println("Start Event");
	 } else if (evt instanceof TransitionEvent)  {
	  	 System.out.println("transition Event");
	 }else if (evt instanceof CachingControlEvent)  {
	  	 System.out.println("caching control Event");
	 }else {
	 	System.out.println("Unknown event "+evt);
	}

	}
	
public void sleep(int time) {
	    try {
		Thread.currentThread().sleep(time);
	    } catch (InterruptedException ie) {
	    }
	}
	

  	public static int getSaveType() {
	return 1;
    }

    public Dimension getPreferredSize() {
	return preferredSize;
    }
	
 /**
     * Create a media locator from the given string.
     */
    static MediaLocator createMediaLocator(String url) {

	MediaLocator ml;

	if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
	    return ml;

	if (url.startsWith(File.separator)) {
	    if ((ml = new MediaLocator("file:" + url)) != null)
		return ml;
	} else {
	    String file = "file:" + System.getProperty("user.dir") + File.separator + url;
	    if ((ml = new MediaLocator(file)) != null)
		return ml;
	}

	return null;
    }



}

