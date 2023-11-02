/*
 * @(#)WarpImage.java	1.6  98/12/03
 *
 * Copyright 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package comul01.eve;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JApplet;
import javax.swing.JFrame;


/**
 * The WarpImage class warps an image on a CubicCurve2D flattened path.
 */
public class WarpImagePath extends JApplet implements Runnable {

    private static int iw, ih, iw2, ih2;
    private static Image img;
    private static final int FORWARD = 0;
    private static final int BACK = 1;
	private double p1,p2,p3,p4,p5,p6,p7,p8,p9;

    // the points of the curve
    private Point2D pts[];

    // initializes direction of movement forward, or left-to-right 
    private int direction = FORWARD;
    private int pNum;
    private int x, y;
    private Thread thread;
    private BufferedImage bimg;

    public void init(Image img) {
		this.img = img;
        iw = img.getWidth(this);
        ih = img.getHeight(this);
        iw2 = iw/2;
        ih2 = ih/2;
    }

	 public void setParams(double p1,
	 						double p2,
							double p3,
							double p4,
							double p5,
							double p6,
							double p7,
							double p8,
							double p9) {

		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;
		this.p5 = p5;
		this.p6 = p6;
		this.p7 = p7;
		this.p8 = p8;
		//setPNum((int)(p9*100.0));

	}

    public void init() {
        setBackground(Color.white);
        img = getToolkit().getImage(WarpImage.class.getResource("surfing.gif"));
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        } catch (Exception e) {}
        iw = img.getWidth(this);
        ih = img.getHeight(this);
        iw2 = iw/2;
        ih2 = ih/2;
    }


    public void reset(int w, int h) {
        pNum = 0;
        direction = FORWARD;

        // initializes the cubic curve
        CubicCurve2D cc = new CubicCurve2D.Float(
                        w*(float)p1, h*(float)p2, w*(float)p3,(float)p4, w*(float)p5,h*(float)p6,w*(float)p7,h*(float)p8);

        // creates an iterator to define the boundary of the flattened curve
        PathIterator pi = cc.getPathIterator(null, 0.1);
        Point2D tmp[] = new Point2D[200];
        int i = 0;

        // while pi is iterating the curve, adds points to tmp array
        while ( !pi.isDone() ) {
            float[] coords = new float[6];
            switch ( pi.currentSegment(coords) ) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                        tmp[i] = new Point2D.Float(coords[0], coords[1]);
            }
            i++;
            pi.next();
        }
        pts = new Point2D[i];

        // copies points from tmp to pts
        System.arraycopy(tmp,0,pts,0,i);
    }

	public void setPNum(int pNum) {
		this.pNum = pNum;
	}
	
	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public void goForward() {
		this.direction = FORWARD;
	}

	public void goBack() {
		this.direction = BACK;
	}



    // gets coordinates from pts and adjusts direction
    public void step(int w, int h) {
        if (pts == null) {
            return;
        }
        x = (int) pts[pNum].getX();
        y = (int) pts[pNum].getY();
        if (direction == FORWARD)
            if (++pNum == pts.length)
                direction = BACK;
        if (direction == BACK)
            if (--pNum == 0)
                direction = FORWARD;
    }

    
    /*
     * Scales the image on the fly to fit inside of the destination drawable 
     * surface.  Crops the image into quarter pieces, based on the x & y
     * coordinates scales the cropped images.
     */
    public void drawDemo(int w, int h, Graphics2D g2) {
        g2.drawImage(img,
                        0,              0,              x,              y,
                        0,              0,              iw2,            ih2,
                        this);
        g2.drawImage(img,
                        x,              0,              w,              y,
                        iw2,            0,              iw,             ih2,
                        this);
        g2.drawImage(img,
                        0,              y,              x,              h,
                        0,              ih2,            iw2,            ih,
                        this);
        g2.drawImage(img,
                        x,              y,              w,              h,
                        iw2,            ih2,            iw,             ih,
                        this);
    }


    public Graphics2D createGraphics2D(int w, int h) {
		System.out.println("warp A "+w+", "+h);
        Graphics2D g2 = null;
        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
     //       bimg = (BufferedImage) createImage(w, h);
	 		bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
					System.out.println("warp b");

            reset(w, h);
					System.out.println("warp C");

        } 
		if (bimg == null) System.out.println("warp still null!!");
        g2 = bimg.createGraphics();
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, w, h);
				System.out.println("warp D");

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }


    public void paint(Graphics g) {
	Dimension d = getSize();
        step(d.width, d.height);
        Graphics2D g2 = createGraphics2D(d.width, d.height);
        drawDemo(d.width, d.height, g2);
        g2.dispose();
        g.drawImage(bimg, 0, 0, this);
    }


    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }


    public synchronized void stop() {
        thread = null;
    }


    public void run() {
        Thread me = Thread.currentThread();
        while (thread == me) {
            repaint();
            try {
                thread.sleep(10);
            } catch (InterruptedException e) { break; }
        }
        thread = null;
    }


    public static void main(String argv[]) {
        final WarpImage demo = new WarpImage();
        demo.init();
        JFrame f = new JFrame("Java 2D(TM) Demo - WarpImage");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
            public void windowDeiconified(WindowEvent e) { demo.start(); }
            public void windowIconified(WindowEvent e) { demo.stop(); }
        });
        f.getContentPane().add("Center", demo);
        f.pack();
        f.setSize(new Dimension(400,300));
        f.show();
        demo.start();
    }
}
