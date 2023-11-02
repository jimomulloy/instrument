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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.net.URL;


/**
 * The WarpImage class warps an image on a CubicCurve2D flattened path.
 */
public class WarpImage extends JApplet implements Runnable {

    private static int iw, ih, iw2, ih2;
    private static Image img;
    private static final int FORWARD = 0;
    private static final int BACK = 1;

    // the points of the curve
    private Point2D pts[];

    // initializes direction of movement forward, or left-to-right 
    private int direction = FORWARD;
    private int pNum;
    private int x, y;
    private Thread thread;
    private BufferedImage bimg;


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
                        w*.2f, h*.5f, w*.4f,0, w*.6f,h,w*.8f,h*.5f);

        // creates an iterator to define the boundary of the flattened curve
        PathIterator pi = cc.getPathIterator(null, 0.1);
        Point2D tmp[] = new Point2D[200];
        int i = 0;

        // while pi is iterating the curve, adds points to tmp array
        while ( !pi.isDone() ) {
            float[] coords = new float[6];
            switch ( pi.currentSegment(coords) ) {
                case 0:
                case 1:
                        tmp[i] = new Point2D.Float(coords[0], coords[1]);
            }
            i++;
            pi.next();
        }
        pts = new Point2D[i];

        // copies points from tmp to pts
        System.arraycopy(tmp,0,pts,0,i);
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
        Graphics2D g2 = null;
        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
            bimg = (BufferedImage) createImage(w, h);
            reset(w, h);
        } 
        g2 = bimg.createGraphics();
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, w, h);
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
