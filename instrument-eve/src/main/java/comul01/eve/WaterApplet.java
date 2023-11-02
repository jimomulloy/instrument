package comul01.eve;

// ====================================================
//
// Copyright (c) 2001 Sean Wilson. All Rights Reserved.
//
// ====================================================

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

public class WaterApplet extends Applet implements Runnable, MouseListener, MouseMotionListener
{
	/** Filename of background image. */
	private String BACKGROUND_IMAGE_FILENAME = "background.jpg";

	/** Bit shift component for damping term. */
	private int DAMPING_SHIFT = 6;
	/** Bit shift component to calculate background pixel viewed through liquid. */
	private int BACKGROUND_PIXEL_SHIFT = 5;
	/** Bit shift component for light-source and liquid normal dot product. */
	private int DOT_PRODUCT_SHIFT = 9;
	/** Bit shift component for light-source distance. */
	private int DISTANCE_SHIFT = 9;
	/** Maximum amplitude of light-source and liquid normal dot product. */
	private int MAX_DOT_PRODUCT = 128;

	/** State where drips are added to liquid. */
	private final int STATE_DRIP = 0;
	/** State where dragger is in liquid. */
	private final int STATE_DRAGGER = 2;
	/** Number of states. */
	private final int NUM_STATES = 4;

	/** Number of frames before changing state. */
	private int FRAMES_PER_STATE = 200;
	
	/** Frequency light-source will move in circle. */
	private float LIGHT_MOVE_FREQUENCY = 0.03f;
	/** Frequency water dragger will move in circle. */
	private float DRAGGER_FREQUENCY = ((float)Math.PI * 2) / FRAMES_PER_STATE;
	/** Chance of adding a dent to liquid each frame. */
	private float DRIP_CHANCE = 0.06f;

	/** X/Y radius of drip dent. */
	private int DRIP_RADIUSXY = 10;
	/** Z radius of drip dent. */
	private int DRIP_RADIUSZ = 6500;

	/** X/Y radius of drag dent. */
	private int DRAG_RADIUSXY = 10;
	/** Z radius of drag dent. */
	private int DRAG_RADIUSZ = 1000;

	/** Thread used for updating applet. */
	private Thread updateThread;
	/** Double buffer image. */	
	private Image offscreen;

	/** Pixels of liquid image. */
	private int[] liquidPixel;
	/** Pixels of background image. */
	private int[] backgroundPixel;
	
	/** Source of liquid image pixels. */
	MemoryImageSource liquidImage;

	/** Current height of liquid points. */
	private int[] currentHeight;
	/** Last height of liquid points. */
	private int[] lastHeight;
		
	/** Width of applet. */
	private int appletWidth;
	/** Height of applet. */
	private int appletHeight;

	/** X co-ord of light-source. */
	private int lightSourceX = 0;
	/** Y co-ord of light-source. */
	private int lightSourceY = 0;

	/** Number of frames rendered. */
	private int frame = 0;

	/** Current liquid manipulate state. */
	private int state = STATE_DRIP;
	
	/** Whether mouse is inside component. */
	private boolean mouseInside = false;
	/** Whether background image has finished loading. */
	private boolean backgroundLoaded = false;
	
	public void setParams(double p1,
			double p2,
			double p3,
			double p4,
			double p5,
			double p6,
			double p7,
			double p8,
			double p9,
			int ps) {
		
		if(state == 1) return;
		
			DAMPING_SHIFT = (int)(p1*100.0);
			BACKGROUND_PIXEL_SHIFT = (int)(p2*100.0);
			DOT_PRODUCT_SHIFT = (int)(p3*100.0);
			DISTANCE_SHIFT = (int)(p4*100.0);
			MAX_DOT_PRODUCT = (int)(p5*100.0);
			//FRAMES_PER_STATE = (int)(p6*100.0);
			LIGHT_MOVE_FREQUENCY = (float)p6;
			DRAGGER_FREQUENCY = ((float)Math.PI * 2) / FRAMES_PER_STATE;
			DRIP_CHANCE = (float)(p7);
			DRIP_RADIUSXY = (int)(p8*100.0);
			DRIP_RADIUSZ = (int)(p9*10000.0);
			state = ps;
	}

	
	
	
	public void loadBackgroundImage()
	{
		// Set to border layout
		setLayout(new BorderLayout());
		
		// Add wait message label to center of applet
		Label messageLabel = new Label("Loading image...", Label.CENTER);
		add(messageLabel, BorderLayout.CENTER);

		// Show message
		repaint();

		// Background image ID for media tracker
		final int BACKGROUND_IMAGE_ID = 0;
		// Get background image
		Image background = getImage(getDocumentBase(), BACKGROUND_IMAGE_FILENAME);					

		// Media tracker to wait for image loading
		MediaTracker mediaTracker = new MediaTracker(this);
		// Add image to media tracker
		mediaTracker.addImage(background, BACKGROUND_IMAGE_ID);
		
		try
		{
			// Wait for background image to load
			mediaTracker.waitForID(BACKGROUND_IMAGE_ID);
		}
		catch (InterruptedException e)
		{
		}

		// If error loading background image
		if (mediaTracker.isErrorID(BACKGROUND_IMAGE_ID))
		{
			throw new RuntimeException("Error trying to load " + BACKGROUND_IMAGE_FILENAME);
		}

		// Pixel grabber for background image
		PixelGrabber pixelGrabber = new PixelGrabber(background, 0, 0, appletWidth,  appletHeight, backgroundPixel, 0, appletWidth);

		try
		{
			// Grab background image pixels
			pixelGrabber.grabPixels();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("interrupted grabbing pixels");
		}

		// Remove loading message
		remove(messageLabel);

		// Background image has been loaded
		backgroundLoaded = true;
	}

	public void init()
	{
		// Add mouse listeners to component
		addMouseListener(this);
		addMouseMotionListener(this);

		// Get applet dimensions
		appletWidth  = getSize().width;
		appletHeight = getSize().height;

		// Initialise arrays
		backgroundPixel = new int[appletWidth * appletHeight];
		liquidPixel     = new int[appletWidth * appletHeight];		
		lastHeight      = new int[appletWidth * appletHeight];
		currentHeight   = new int[appletWidth * appletHeight];
		
		// Set double buffer to use liquid image pixel buffer
		liquidImage = new MemoryImageSource(appletWidth, appletHeight, liquidPixel, 0, appletWidth);
		liquidImage.setAnimated(true);
		liquidImage.setFullBufferUpdates(true);
		offscreen = createImage(liquidImage);

		// For each liquid pixel
		for (int i = 0; i < liquidPixel.length; ++i)
		{
			// Set pixel to black
			liquidPixel[i] = Color.black.getRGB();
		}
	}

	public void start()
	{
		// Create update thread
		updateThread = new Thread(this);
		// Start update thread
		updateThread.start();
	}

	public void stop()
	{
		// Stop update thread
		updateThread = null;
	}

	public void destroy()
	{
		// Stop update thread
		updateThread = null;
	}

	public void updateLiquidHeight()
	{
		// Swap last and current height buffers
		int[] temp = lastHeight;
		lastHeight    = currentHeight;
		currentHeight = temp;

		// Offset of current liquid point
		int offset = 1 + appletWidth;

		// For each row except edges
		for (int y = 1; y < appletHeight - 1; ++y)
		{
			// For each column except edges
			for (int x = 1; x < appletWidth - 1; ++x)
			{
				// Smooth last height value and add wave velocity
				int newHeight = ((lastHeight[offset - 1 - appletWidth] +
						          lastHeight[offset     - appletWidth] +
						          lastHeight[offset + 1 - appletWidth] +
						          lastHeight[offset - 1              ] +
						          lastHeight[offset + 1              ] +
						          lastHeight[offset - 1 + appletWidth] +
						          lastHeight[offset     + appletWidth] +
						          lastHeight[offset + 1 + appletWidth]) >> 2) - currentHeight[offset];

				// Apply damping to height
				currentHeight[offset] = newHeight - (newHeight >> DAMPING_SHIFT);

				// Move to next point
				++offset;
			}

			// Move to next row
			offset += 2;
		}
	}

	public int clipValue(int value, int minValue, int maxValue)
	{
		// Return clipped value
		return (value < minValue ? minValue : (value > maxValue ? maxValue : value));
	}	

	public void updateLiquidImage()
	{
		// Offset of current liquid point
		int pointOffset = 1 + appletWidth;

		// For each row except edges
		for (int y = 1; y < appletHeight - 1; ++y)
		{
			// For each column except edges
			for (int x = 1; x < appletWidth - 1; ++x)
			{
				// Calculate gradient of point
				int deltaX = currentHeight[pointOffset] - currentHeight[pointOffset + 1          ];
				int deltaY = currentHeight[pointOffset] - currentHeight[pointOffset + appletWidth];

				// Calculate offset of light-source from point
				int lightOffsetX = (x - lightSourceX);
				int lightOffsetY = (y - lightSourceY);

				// Calculate squared distance of light-source from point
				int squaredDistance = lightOffsetX * lightOffsetX + lightOffsetY * lightOffsetY;
				// Calculate dot product of point gradient and light offset
				int dotProduct = clipValue((deltaX * lightOffsetX + deltaY * lightOffsetY) >> DOT_PRODUCT_SHIFT, -MAX_DOT_PRODUCT, +MAX_DOT_PRODUCT);
				// Calculate brightness from dot product value reduced by light-source distance
				int brightness = dotProduct - (squaredDistance >> DISTANCE_SHIFT);

				// Calculate co-ords of background pixel viewed through liquid and clip to legal values
				int backgroundX = clipValue(x + (deltaX >> BACKGROUND_PIXEL_SHIFT), 1, appletWidth  - 2);
				int backgroundY = clipValue(y + (deltaY >> BACKGROUND_PIXEL_SHIFT), 1, appletHeight - 2);
				
				// Get background pixel color
				int color = backgroundPixel[backgroundX + backgroundY * appletWidth];

				// Add brightness to color components and clip to legal values
				int red   = clipValue(((color & 0xFF0000) >> 16) + brightness, 0, 255);
				int green = clipValue(((color & 0x00FF00) >> 8 ) + brightness, 0, 255);
				int blue  = clipValue(((color & 0x0000FF)      ) + brightness, 0, 255);

				// Set pixel value
				liquidPixel[pointOffset] = 0xFF000000 | (red << 16) | (green << 8) | blue;

				// Move to next point
				++pointOffset;
			}

			// Move to next row
			pointOffset += 2;
		}

		// Update liquid image
		liquidImage.newPixels();
	}
		
	public void updateState()
	{
		// If mouse not inside component
		if (!mouseInside)
		{
			// Check state
			switch (state)
			{
				// Adding drips to liquid state
				case STATE_DRIP:

					// If should add drip dent to liquid
					if (Math.random() < DRIP_CHANCE)
					{
						// Add dent at random position
						addDent((int)(Math.random() * appletWidth), (int)(Math.random() * appletHeight), DRIP_RADIUSXY, DRIP_RADIUSZ);
					}
					break;

				// Dragger in liquid state
				case STATE_DRAGGER:

					// Move dragger in circle around center of component
					int draggerX = (int)(Math.sin(frame * -DRAGGER_FREQUENCY) * appletWidth  / 3 + appletWidth  / 2);
					int draggerY = (int)(Math.cos(frame * -DRAGGER_FREQUENCY) * appletHeight / 3 + appletHeight / 2);

					// Add dragger dent
					addDent(draggerX, draggerY, DRAG_RADIUSXY, DRAG_RADIUSZ);
					break;
			}

			// Move light-source in circle around center of component
			lightSourceX = (int)(Math.sin(frame * LIGHT_MOVE_FREQUENCY) * appletWidth  / 2 + appletWidth  / 2);
			lightSourceY = (int)(Math.cos(frame * LIGHT_MOVE_FREQUENCY) * appletHeight / 2 + appletHeight / 2);
		}

		// If should change state
		if ((frame + 1) % FRAMES_PER_STATE == 0)
		{
			// Goto next state or first state if last was reached
			state = (++state) % NUM_STATES;
		}
	}

	public void loadBackgroundImage(Image image)
		{
		// Background image ID for media tracker
		final int BACKGROUND_IMAGE_ID = 0;
		// Get background image
		Image background = image;	
		
		// Pixel grabber for background image
		PixelGrabber pixelGrabber = new PixelGrabber(background, 0, 0, appletWidth,  appletHeight, backgroundPixel, 0, appletWidth);

		try
		{
			// Grab background image pixels
			pixelGrabber.grabPixels();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("interrupted grabbing pixels");
		}

		// Background image has been loaded
		backgroundLoaded = true;
	}
	
	public BufferedImage getBufferedImage() {
		int w = getWidth();
		int h = getHeight();
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		bi.setRGB(0, 0, w, h, liquidPixel, 0, w); 
		return bi;
	}
	

	
	public void run(Image image)
	{
		// Load background image
		loadBackgroundImage(image);
		// Update current state
		updateState();			
		// Update liquid height
		updateLiquidHeight();
		// Update liquid texture
		updateLiquidImage();
		// Frame rendered
		++frame;
	}
	
	public void run()
	{
		// Load background image
		loadBackgroundImage();
		// Paint double buffer
		repaint();
		
		// While update thread is current thread
		while (Thread.currentThread() == updateThread)
		{
			// Update current state
			updateState();			
			// Update liquid height
			updateLiquidHeight();
			// Update liquid texture
			updateLiquidImage();

			// Frame rendered
			++frame;
			
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	public void update(Graphics g)
	{
		// If background image has loaded
		if (backgroundLoaded)
		{
			// Paint double buffer
			paint(g);
		}
		else
		{
			// Paint any components
			paintAll(g);
		}
	}

	public void paint(Graphics g)
	{
		// If background image has loaded
		if (backgroundLoaded)
		{
			// Paint double buffer
			g.drawImage(offscreen, 0, 0, this);
		}		
	}

	public void addDent(int deltaX, int deltaY, int radiusXY, int radiusZ)
	{
		// For each column
		for (int x = -radiusXY; x < radiusXY; ++x)
		{
			// For each row
			for (int y = -radiusXY; y < radiusXY; ++y)
			{
				// Calculate current ellipsoid point
				int ellipsoidX = deltaX + x;
				int ellipsoidY = deltaY + y;
	
				// Goto next point if current is outside of component
				if (ellipsoidX < 1 || ellipsoidX >= appletWidth - 1 || ellipsoidY < 1 || ellipsoidY >= appletHeight - 1)
					continue;
			
				// Calculate root term for point height
				float root = radiusXY * radiusXY - x * x - y * y;

				// Goto next point if root unsolveable
				if (root < 0)
					continue;

				// Calculate point height
				int height = -(int)((Math.sqrt(root) / radiusXY) * radiusZ);
				// Add ellipsoid point height to liquid height
				currentHeight[ellipsoidX + ellipsoidY * appletWidth] += height;
			}
		}
	}

	public void mousePressed(MouseEvent e)
	{
		// Add drip dent at mouse position
		addDent(e.getX(), e.getY(), DRIP_RADIUSXY, DRIP_RADIUSZ);
	}

	public void mouseDragged(MouseEvent e)
	{
		// Add drag dent at mouse position
		addDent(e.getX(), e.getY(), DRAG_RADIUSXY, DRAG_RADIUSZ);

		// Put light-source at mouse position
		lightSourceX = (int)e.getX();
		lightSourceY = (int)e.getY();
	}

	public void mouseMoved(MouseEvent e)
	{
		// Put light-source at mouse position
		lightSourceX = (int)e.getX();
		lightSourceY = (int)e.getY();
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
		// Mouse is not inside component
		mouseInside = false;
	}

	public void mouseEntered(MouseEvent e)
	{
		// Mouse is inside component
		mouseInside = true;
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public String getAppletInfo()
	{
		return "Water - Copyright (c) 2001 Sean Wilson. All Rights Reserved.";
	}
}