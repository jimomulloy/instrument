/*
 * @(#)VideoCutPanel.java	1.2 99/08/05
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.Buffer;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.StopAtTimeEvent;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class VideoCutPanel extends JPanel
	implements ActionListener, ItemListener, ControllerListener, Runnable, EJFrameGrabber {

	String [] movies;
	Player player;
	JComboBox comboMovies;
	SelectionPanel2 selPanel;
	SelectionPanel selSlider;
	Component visual, control;
	int controlHeight = 20;
	Time beginTime, endTime;
	Time duration;
	int timerCount = 0;
	JPanel centerPanel, videoPanel;
	String mediaFile;
	JLabel labelTime;
	SaveFileControl saveFileControl;
	OpenFileControl openFileControl;
	JButton videoOpenB, imageOpenB, saveB;
	private File fileDirectory;
	private File file = null;
	private JLabel fileNameHeading;
	private JLabel fileNameField;
	JFrame mainFrame;
	String fileName = null;
	int mediaType = 0;
	Image image;
	JPanel external;
	JScrollPane imagePanel;
	FramePositioningControl fpc;
	FrameGrabbingControl fgc;
	public final static int VIDEO = 1;
	public final static int IMAGE = 2;
	Dimension cSize = null;
	Dimension vSize = null;
	Dimension vsSize=null;
	private EJMain ejMain = null;
	
	public VideoCutPanel(String [] movies, boolean complex, JFrame mainFrame) {

		mediaType = VIDEO;
		setDirectory(new File(System.getProperty("user.dir")));
		this.mainFrame = mainFrame;
		BorderLayout bl;
		this.movies = movies;
		bl = new BorderLayout();
		bl.setHgap(5);
		bl.setVgap(5);
		setLayout(bl);
		setOpaque(false);
		setBorder(BorderFactory.createEtchedBorder());
		String [] moviesJustNames = new String[movies.length];
		for (int i=0; i < movies.length; i++) {
			int k = movies[i].lastIndexOf("/");
			if (k < 0) k = movies[i].lastIndexOf("\\");
			moviesJustNames[i] = movies[i].substring(k + 1);
		}

		if (complex) {
			openFileControl = new OpenFileControl();
			openFileControl.setSize(openFileControl.getPreferredSize().width, 100);
			add("North", openFileControl);
		}
		else {
			mediaType = VIDEO;
			saveFileControl = new SaveFileControl();
			saveFileControl.setSize(saveFileControl.getPreferredSize().width, 100);
			add("North", saveFileControl);
		}

		

		videoPanel = new JPanel();
		videoPanel.setOpaque(false);
		videoPanel.setLayout( new BorderLayout() );
		centerPanel = new JPanel();
		centerPanel.setOpaque(false);
		centerPanel.setLayout( new BorderLayout() );
		centerPanel.add("Center", videoPanel);
		add("Center", centerPanel);

		if (complex) {
			JPanel southPanel = new JPanel();
			southPanel.setLayout(new BorderLayout());
			JPanel selP = new JPanel();
			selP.setLayout(new BorderLayout());
			selP.setOpaque(false);
			labelTime = new JLabel("Selection", JLabel.CENTER);
			labelTime.setFont(new Font("Dialog", Font.PLAIN, 10));
			selP.add("South", labelTime);
			selPanel = new SelectionPanel2( this );
			selPanel.setVisible( true );
			selP.add("North", selPanel);
			southPanel.add("North", selP);
			JPanel sliderP = new JPanel();
			sliderP.setOpaque(false);
			sliderP.setLayout( new BorderLayout() );
			JLabel sliderLabel = new JLabel("Slider label", JLabel.CENTER);
			sliderLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
			sliderP.add("South", sliderLabel);
			selSlider = new SelectionPanel( this );
			selSlider.setVisible( true );
			sliderP.add("North", selSlider);
			southPanel.add("South", sliderP);
			add("South", southPanel);

		}

		//Thread t = new Thread(this);
		//t.start();
	}

	private MediaLocator createMediaLocator(String url) {

		MediaLocator ml;

		if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
			return ml;

		if (url.startsWith(File.separator)) {
			if ((ml = new MediaLocator("file:" + url)) != null)
				return ml;
		}
		else {
			String file = "file:" + System.getProperty("user.dir") + File.separator + url;
			if ((ml = new MediaLocator(file)) != null)
				return ml;
		}

		return null;
	}



	private void updateLabel() {
		if (labelTime == null)
			return;
		String begin = formatTime(beginTime);
		String end = formatTime(endTime);
		labelTime.setText(begin + " - " + end);
	}

	private String formatTime ( Time time ) {
		long    nano;
		int     hours;
		int     minutes;
		int     seconds;
		int     hours10;
		int     minutes10;
		int     seconds10;
		long    nano10;
		String  strTime = new String ( "<unknown>" );

		if ( time == null  ||  time == Time.TIME_UNKNOWN  ||  time == javax.media.Duration.DURATION_UNKNOWN )
			return ( strTime );

		nano = time.getNanoseconds();
		seconds = (int) (nano / Time.ONE_SECOND);
		hours = seconds / 3600;
		minutes = ( seconds - hours * 3600 ) / 60;
		seconds = seconds - hours * 3600 - minutes * 60;
		nano = (long) ((nano % Time.ONE_SECOND) / (Time.ONE_SECOND/100));

		hours10 = hours / 10;
		hours = hours % 10;
		minutes10 = minutes / 10;
		minutes = minutes % 10;
		seconds10 = seconds / 10;
		seconds = seconds % 10;
		nano10 = nano / 10;
		nano = nano % 10;

		strTime = new String ( "" + hours10 + hours + ":" + minutes10 +
			minutes + ":" + seconds10 + seconds + "." + nano10 + nano );
		return ( strTime );
	}

	public String getMediaFile() {
		return mediaFile;
	}
	
	public void setEJMain(EJMain ejMain) {
		this.ejMain = ejMain;
	}

	
	public boolean testMediaFile() {
		return (mediaFile != null); 
	}

	public int getMediaType() {
		return mediaType;
	}

	public boolean isVideo() {
		return (mediaType == VIDEO);
	}
	public boolean isImage() {
		return (mediaType == IMAGE);
	}

	public Time getBeginTime() {
		return beginTime;
	}

	public Time getEndTime() {
		return endTime;
	}

	public int getBeginFrame() {
		int frameNumber = 0;
		if (mediaType == VIDEO && player != null && fgc != null) {
			frameNumber = fpc.mapTimeToFrame(beginTime);
		}
		else if (mediaType == IMAGE) {
			frameNumber = 1;
		}
		return frameNumber;
	}

	public int getEndFrame() {
		int frameNumber = 0;
		if (mediaType == VIDEO && player != null && fgc != null) {
			frameNumber = fpc.mapTimeToFrame(endTime);
		}
		else if (mediaType == IMAGE) {
			frameNumber = 1;
		}
		return frameNumber;
	}

	public Image grabImage(long grabTime) {
		Image imageOut = null;
		Image imageOut1 = null;
		Image imageS = null;
		Image imageE = null;
		if (mediaType == VIDEO && player != null && fgc != null && fpc != null) {
			// Check if HumbleVideoPlayer is ready before attempting to grab frames
			if (player instanceof HumbleVideoPlayer) {
				HumbleVideoPlayer hvp = (HumbleVideoPlayer) player;
				if (!hvp.isReady()) {
					System.err.println("VideoCutPanel.grabImage: HumbleVideoPlayer not ready for grabbing");
					return null;
				}
			}

			// grabTime appears to be in microseconds from the frame timestamp, convert to nanoseconds
			// by multiplying by 1000 (if the value seems too small for nanoseconds)
			long grabTimeNanos = grabTime;
			if (grabTime > 0 && grabTime < 1000000000L) {
				// Value is too small to be nanoseconds for any reasonable video duration
				// Assume it's microseconds and convert to nanoseconds
				grabTimeNanos = grabTime * 1000L;
			}
			Time offsetTime = new Time(grabTimeNanos + getBeginTime().getNanoseconds());
			int offsetFrame = fpc.mapTimeToFrame(offsetTime);
			Time actualOffsetTime = fpc.mapFrameToTime(offsetFrame);
			System.out.println("VideoCutPanel.grabImage: grabTime=" + grabTime +
				" (as nanos=" + grabTimeNanos + ")" +
				", beginTime=" + getBeginTime().getNanoseconds() +
				", offsetTime=" + offsetTime.getNanoseconds() +
				", offsetFrame=" + offsetFrame +
				", frameRate=" + (fpc instanceof HumbleVideoPlayer ? ((HumbleVideoPlayer)fpc).getFrameRate() : "unknown"));
			Buffer frame = null;
			VideoFormat format = null;
			BufferToImage btoimg = null;
			fpc.seek(offsetFrame);
			frame = fgc.grabFrame();

			// Validate frame before processing
			if (frame == null || frame.getData() == null) {
				System.err.println("VideoCutPanel.grabImage: Failed to grab frame at offset " + offsetFrame);
				return null;
			}

			format = (VideoFormat)frame.getFormat();
			if (format == null) {
				System.err.println("VideoCutPanel.grabImage: Frame has no format");
				return null;
			}

			btoimg = new BufferToImage(format);
			imageOut = btoimg.createImage(frame);

			// Fallback: if BufferToImage fails, create image directly from byte data
			if (imageOut == null && format instanceof javax.media.format.RGBFormat) {
				javax.media.format.RGBFormat rgbFormat = (javax.media.format.RGBFormat) format;
				java.awt.Dimension size = rgbFormat.getSize();
				if (size != null && frame.getData() instanceof byte[]) {
					byte[] data = (byte[]) frame.getData();
					int w = size.width;
					int h = size.height;
					int pixelStride = rgbFormat.getPixelStride();
					int lineStride = rgbFormat.getLineStride();
					int redMask = rgbFormat.getRedMask();

					if (pixelStride == 3 && w > 0 && h > 0) {
						java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
						int[] pixels = new int[w * h];

						// Determine byte order from red mask position
						boolean rgbOrder = (redMask == 1); // Red at offset 0 means RGB order

						for (int y = 0; y < h; y++) {
							// Video data is typically stored bottom-up, so flip vertically
							int srcY = h - 1 - y;
							for (int x = 0; x < w; x++) {
								int srcIdx = srcY * lineStride + x * pixelStride;
								if (srcIdx + 2 < data.length) {
									int r, g, b;
									if (rgbOrder) {
										r = data[srcIdx] & 0xFF;
										g = data[srcIdx + 1] & 0xFF;
										b = data[srcIdx + 2] & 0xFF;
									} else {
										b = data[srcIdx] & 0xFF;
										g = data[srcIdx + 1] & 0xFF;
										r = data[srcIdx + 2] & 0xFF;
									}
									pixels[y * w + x] = (r << 16) | (g << 8) | b;
								}
							}
						}
						bi.setRGB(0, 0, w, h, pixels, 0, w);
						imageOut = bi;
						System.out.println("VideoCutPanel.grabImage: Created image using fallback method");
					}
				}
			}

			if (imageOut == null) {
				System.err.println("VideoCutPanel.grabImage: Failed to create image (BufferToImage and fallback both failed)");
				System.err.println("  Frame details: length=" + frame.getLength() +
					", offset=" + frame.getOffset() +
					", data=" + (frame.getData() != null ? frame.getData().getClass().getSimpleName() + "[" + java.lang.reflect.Array.getLength(frame.getData()) + "]" : "null") +
					", discard=" + frame.isDiscard() +
					", eom=" + frame.isEOM());
				System.err.println("  Format: " + format);
			}

			System.out.println("vc grab Image [" + this.hashCode() + "] "+grabTime+", "+actualOffsetTime.getNanoseconds()+", "+
						offsetTime.getNanoseconds()+", "+offsetFrame + ", imageOut=" + (imageOut != null));
			long timeDiff = actualOffsetTime.getNanoseconds()-offsetTime.getNanoseconds();
			return imageOut;
	
			//if (Math.abs(timeDiff) < 1000000) {
			/*
			if (offsetTime.equals(actualOffsetTime)||offsetFrame <= 0) {
		
				System.out.println("return straight image");
				return imageOut;
			} else {
				long offsetNS = offsetTime.getNanoseconds();
				long actualOffsetNS = actualOffsetTime.getNanoseconds();
				long startOffsetNS, endOffsetNS; 
				if (offsetNS < actualOffsetNS) {
					endOffsetNS = actualOffsetNS;
					fpc.skip(-1);
					startOffsetNS = fpc.mapFrameToTime(offsetFrame-1).getNanoseconds();
					frame = fgc.grabFrame();
					format = (VideoFormat)frame.getFormat();
					btoimg = new BufferToImage(format);
					imageOut1 = btoimg.createImage(frame);
					imageS = imageOut1;
					imageE = imageOut;
					System.out.println("backup "+offsetNS+", "+startOffsetNS+", "+endOffsetNS);
				
				} else {
					startOffsetNS = actualOffsetNS;
					fpc.skip(1);
					endOffsetNS = fpc.mapFrameToTime(offsetFrame+1).getNanoseconds();
					frame = fgc.grabFrame();
					format = (VideoFormat)frame.getFormat();
					btoimg = new BufferToImage(format);
					imageOut1 = btoimg.createImage(frame);
					imageS = imageOut;
					imageE = imageOut1;
					System.out.println("forward "+offsetNS+", "+startOffsetNS+", "+endOffsetNS);

			
				}
				
				RenderedOp src1 = JAI.create("AWTImage", imageS);
				RenderedOp src2 = JAI.create("AWTImage", imageE);
		
				double factor = (double)(endOffsetNS-offsetNS)/(double)(endOffsetNS-startOffsetNS); 
				System.out.println("factor 1 "+factor);

				ParameterBlock pb = new ParameterBlock();
				pb.addSource(src1);
				double[] constants = new double[3];
				constants[0] = factor;
				constants[1] = factor;
				constants[2] = factor;
				pb.add(constants);
				RenderedOp ropOut1 = JAI.create("multiplyconst", pb);
				factor = (double)(offsetNS-startOffsetNS)/(double)(endOffsetNS-startOffsetNS); 
				System.out.println("factor 2 "+factor);
			
				pb = new ParameterBlock();
				pb.addSource(src2);
				constants = new double[3];
				constants[0] = factor;
				constants[1] = factor;
				constants[2] = factor;
				pb.add(constants);
				RenderedOp ropOut2 = JAI.create("multiplyconst", pb);
				
				pb = new ParameterBlock();
				pb.addSource(ropOut1);
				pb.addSource(ropOut2);
				RenderedOp ropOut3 = JAI.create("add", pb);
				return (Image)ropOut3.getAsBufferedImage();
			}
			*/
		}
		else if (mediaType == IMAGE) {
			return image;
		}
		// Log why we're returning null
		System.err.println("VideoCutPanel.grabImage: Returning null - mediaType=" + mediaType +
			", player=" + (player != null) + ", fgc=" + (fgc != null) + ", fpc=" + (fpc != null));
		return null;
	}

	public Image grabFrame(int frameNumber) {
		Image imageOut = null;
		if (mediaType == VIDEO && player != null && fgc != null && fpc != null) {
			// Check if HumbleVideoPlayer is ready before attempting to grab frames
			if (player instanceof HumbleVideoPlayer) {
				HumbleVideoPlayer hvp = (HumbleVideoPlayer) player;
				if (!hvp.isReady()) {
					System.err.println("VideoCutPanel.grabFrame: HumbleVideoPlayer not ready for grabbing");
					return null;
				}
			}

			Buffer frame = null;
			VideoFormat format = null;
			BufferToImage btoimg = null;
			fpc.seek(frameNumber);
			frame = fgc.grabFrame();

			// Validate frame before processing
			if (frame == null || frame.getData() == null) {
				System.err.println("VideoCutPanel.grabFrame: Failed to grab frame " + frameNumber);
				return null;
			}

			format = (VideoFormat)frame.getFormat();
			if (format == null) {
				System.err.println("VideoCutPanel.grabFrame: Frame has no format");
				return null;
			}

			btoimg = new BufferToImage(format);
			imageOut = btoimg.createImage(frame);

			// Fallback: if BufferToImage fails, create image directly from byte data
			if (imageOut == null && format instanceof javax.media.format.RGBFormat) {
				javax.media.format.RGBFormat rgbFormat = (javax.media.format.RGBFormat) format;
				java.awt.Dimension size = rgbFormat.getSize();
				if (size != null && frame.getData() instanceof byte[]) {
					byte[] data = (byte[]) frame.getData();
					int w = size.width;
					int h = size.height;
					int pixelStride = rgbFormat.getPixelStride();
					int lineStride = rgbFormat.getLineStride();
					int redMask = rgbFormat.getRedMask();

					if (pixelStride == 3 && w > 0 && h > 0) {
						java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
						int[] pixels = new int[w * h];

						// Determine byte order from red mask position
						boolean rgbOrder = (redMask == 1); // Red at offset 0 means RGB order

						for (int y = 0; y < h; y++) {
							// Video data is typically stored bottom-up, so flip vertically
							int srcY = h - 1 - y;
							for (int x = 0; x < w; x++) {
								int srcIdx = srcY * lineStride + x * pixelStride;
								if (srcIdx + 2 < data.length) {
									int r, g, b;
									if (rgbOrder) {
										r = data[srcIdx] & 0xFF;
										g = data[srcIdx + 1] & 0xFF;
										b = data[srcIdx + 2] & 0xFF;
									} else {
										b = data[srcIdx] & 0xFF;
										g = data[srcIdx + 1] & 0xFF;
										r = data[srcIdx + 2] & 0xFF;
									}
									pixels[y * w + x] = (r << 16) | (g << 8) | b;
								}
							}
						}
						bi.setRGB(0, 0, w, h, pixels, 0, w);
						imageOut = bi;
						System.out.println("VideoCutPanel.grabFrame: Created image using fallback method");
					}
				}
			}

			if (imageOut == null) {
				System.err.println("VideoCutPanel.grabFrame: Failed to create image for frame " + frameNumber);
			}

			return imageOut;
		}
		else if (mediaType == IMAGE) {
			return image;
		}
		System.err.println("VideoCutPanel.grabFrame: Returning null - mediaType=" + mediaType +
			", player=" + (player != null) + ", fgc=" + (fgc != null) + ", fpc=" + (fpc != null));
		return null;
	}


	public void stop() {
		if (player != null)
			player.stop();
	}

	public void setDirectory(File fileDirectory) {
		this.fileDirectory = fileDirectory;
		if (ejMain != null) {
			ejMain.setDirectory(fileDirectory);
		}
	}

	public File getDirectory() {
		if (ejMain != null) {
			fileDirectory = ejMain.getDirectory();
		}
		return fileDirectory;
	}


	public Dimension getPreferredSize() {
		Insets insets = getInsets();
		Dimension size = new Dimension(100 + insets.left + insets.right,
			100 + insets.top + insets.bottom);
		return size;
	}

	public Insets getInsets() {
		Insets in = super.getInsets();
		in.left += 5;
		in.right += 5;
		in.top += 5;
		in.bottom += 5;
		return in;
	}

	public boolean load(File file) {
		this.file = file;
		if (file != null && file.isFile()) {
			try {
				fileName = file.toURL().toString();
				setURL(file.toURL().toString());
				return true;
			} catch (Exception e) {
				System.out.println("Exception setURL: "+e.getMessage());
				e.printStackTrace();
				return false;
			}

		}
		else {
			return false;
		}
	}

	public boolean save(File file) {
		try {
			fileName = file.toURL().toString();
			//fileName = file.toString();
			System.out.println("set file name "+fileName);
			fileNameField.setText(getFileName());
			setDirectory(file);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		fileNameField.setText(fileName);

	}

	public void setExternal(JPanel external) {

		if (this.external != null) {
			removeExternal();
		}
		if (imagePanel != null) {
			videoPanel.remove(imagePanel);
			imagePanel = null;
		}
		if (player != null) {
			if (visual != null)
				videoPanel.remove(visual);
			if (control != null)
				videoPanel.remove(control);
			player.removeControllerListener(this);
			player.close();
			player = null;
			visual = null;
			control = null;
		}

		this.external = external;
		videoPanel.add(external);
		videoPanel.setOpaque(true);
		Dimension size = external.getSize();
		System.out.println("VCP ex size "+size.width+", "+size.height);
		videoPanel.setSize(size);
		Dimension cSize = centerPanel.getSize();
		Dimension vSize = videoPanel.getSize();
		videoPanel.setLocation((cSize.width - vSize.width) / 2,
			(cSize.height - vSize.height) / 2);
		videoPanel.invalidate();
		validate();
	}

	public void removeExternal() {
		if (external != null) {
			videoPanel.remove(external);
			external = null;
		}
	}


	public synchronized void setURL(String mediaFile) {
		System.out.println("SetURL for : "+mediaFile);
		this.mediaFile = null;
		if (external != null) {
			removeExternal();
		}

		if (imagePanel != null) {
			videoPanel.remove(imagePanel);
			imagePanel = null;
		}
		if (player != null) {
			if (visual != null)
				videoPanel.remove(visual);
			if (control != null)
				videoPanel.remove(control);
			player.removeControllerListener(this);
			player.close();
			player = null;
			visual = null;
			control = null;
		}
		if (mediaFile == null)
			return;
		URL url = null;
		try {
			// Create an url from the file name and the url to the
			// document containing this applet.
			if ((url = new URL(mediaFile)) == null) {
				System.err.println("Error creating url");
				return;
			}

			if (mediaType == VIDEO) {
				// Create an instance of a player for this media
				try {
					System.out.println("Creates player for :" + url);
					Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, new Boolean(true));
					player = Manager.createPlayer(url);
					System.out.println("Player created: " + player);
				} catch (NoPlayerException e) {
					System.err.println("FMJ player failed, trying HumbleVideoPlayer: " + e.getMessage());
					// Fall back to HumbleVideoPlayer for formats not supported by FMJ (e.g., R210)
					try {
						player = new HumbleVideoPlayer(url);
						System.out.println("HumbleVideoPlayer created successfully");
					} catch (Exception ex) {
						System.err.println("HumbleVideoPlayer also failed: " + ex.getMessage());
						ex.printStackTrace();
					}
				} catch (Exception e) {
					System.err.println("Unexpected error creating player: " + e.getMessage());
					// Fall back to HumbleVideoPlayer
					try {
						player = new HumbleVideoPlayer(url);
						System.out.println("HumbleVideoPlayer created as fallback");
					} catch (Exception ex) {
						System.err.println("HumbleVideoPlayer also failed: " + ex.getMessage());
						ex.printStackTrace();
					}
				}
			}
			else {

				// Read a RenderedImage and convert it to a BufferedImage.
				/*
				 * Create an input stream from the specified file name
				 * to be used with the file decoding operator.
				*/
				//FileSeekableStream stream = null;
				//try {
				//	stream = new FileSeekableStream(file);
				//} catch (IOException e) {
				//    e.printStackTrace();
				//    System.exit(0);
				//}
				///* Create an operator to decode the image file. */
				//RenderedOp image1 = JAI.create("stream", stream);
				System.out.println("Create Image viewer for :"+url.getPath());

				ImageIcon imageIcon = new ImageIcon(url.getPath());
				image = imageIcon.getImage();
				System.out.println("W/h "+image.getWidth(null)+", "+image.getHeight(null));
				ScrollablePicture picture = new ScrollablePicture(imageIcon, 10);
				imagePanel = new JScrollPane(picture);
				imagePanel.setPreferredSize(new Dimension(400, 350));
				imagePanel.setMinimumSize(new Dimension(400, 350));

				videoPanel.add(imagePanel);
				videoPanel.setOpaque(true);
				videoPanel.setSize(400, 350);
				videoPanel.setLocation(-400/2, 350/2);
				videoPanel.invalidate();
				validate();
				this.mediaFile = mediaFile;
				beginTime = new Time(0);
				//duration = endTime = player.getDuration();
				duration = endTime = new Time(0);
				if (selPanel != null) {
					selPanel.setStartTimeMillis(
						beginTime.getNanoseconds() / 1000000);
					selPanel.setStopTimeMillis(
						endTime.getNanoseconds() / 1000000);
				}
				if (selSlider != null) {
					selSlider.setMinPos(0.0F);
					selSlider.setMaxPos(1.0F);
				}
			}

		} catch (MalformedURLException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}

		if (player != null) {
			player.addControllerListener((ControllerListener) this);
			player.realize();
			this.mediaFile = mediaFile;
		}
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == comboMovies)
			setURL(movies[comboMovies.getSelectedIndex()]);
		//setURL((String) comboMovies.getSelectedItem());
		if (ae.getSource() == selPanel) {
			if (player != null) {
				Time newBeginTime =
					new Time(selPanel.getStartTimeMillis() * 1000000);
				Time newEndTime =
					new Time(selPanel.getStopTimeMillis() * 1000000);
				if (newBeginTime.getSeconds() != beginTime.getSeconds()) {
					if (player.getState() == Player.Started)
						player.stop();
					player.setMediaTime(newBeginTime);
					timerCount = 1000;
					beginTime = newBeginTime;
					if (selSlider != null) {
						selSlider.setMinPos((float)beginTime.getNanoseconds()/(float)duration.getNanoseconds());
					}
					updateLabel();
				}
				if (newEndTime.getSeconds() != endTime.getSeconds()) {
					if (player.getState() == Player.Started)
						player.stop();
					endTime = newEndTime;
					player.setMediaTime(newEndTime);
					timerCount = 1000;
					if (selSlider != null) {
						selSlider.setMaxPos((float)endTime.getNanoseconds()/(float)duration.getNanoseconds());
					}
					updateLabel();
				}
			}
		}
		if (ae.getSource() == selSlider) {
			if (player != null) {
				Time newBeginTime =
					new Time((long)(selSlider.getMinPos()*(double)duration.getNanoseconds()));
				Time newEndTime =
					new Time((long)(selSlider.getMaxPos()*(double)duration.getNanoseconds()));
				if (newBeginTime.getSeconds() != beginTime.getSeconds()) {
					if (player.getState() == Player.Started)
						player.stop();
					player.setMediaTime(newBeginTime);
					timerCount = 1000;
					beginTime = newBeginTime;
					if (selPanel != null) {
						selPanel.setStartTimeMillis(
							newBeginTime.getNanoseconds() / 1000000);
					}
					updateLabel();
				}
				if (newEndTime.getSeconds() != endTime.getSeconds()) {
					if (player.getState() == Player.Started)
						player.stop();
					endTime = newEndTime;
					player.setMediaTime(newEndTime);
					timerCount = 1000;
					if (selPanel != null) {
						selPanel.setStopTimeMillis(
							newEndTime.getNanoseconds() / 1000000);
					}
					updateLabel();
				}
			}
		}
	}

	public void run() {
		while ( true ) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException ie) {
			}
			timerCount -= 100;
			if (timerCount == 0 && player != null &&
				player.getState() != player.Started) {
				player.setMediaTime(beginTime);
				player.setStopTime(endTime);
				player.start();
			}
			if (timerCount < -1000)
				timerCount = 0;
		}
	}

	public void itemStateChanged(ItemEvent ie) {
		Object item = ie.getItem();
		if (item instanceof String) {
			setURL(movies[comboMovies.getSelectedIndex()]);
		}
	}

	public void controllerUpdate(ControllerEvent ce) {
		if (ce instanceof RealizeCompleteEvent) {
			if (visual != null)
				return;
			beginTime = new Time(0);
			duration = endTime = player.getDuration();
			if (selPanel != null) {
				selPanel.setStartTimeMillis(
					beginTime.getNanoseconds() / 1000000);
				selPanel.setStopTimeMillis(
					endTime.getNanoseconds() / 1000000);
			}
			if (selSlider != null) {
				selSlider.setMinPos(0.0F);
				selSlider.setMaxPos(1.0F);
			}

			updateLabel();
			fpc = (FramePositioningControl)player.getControl("javax.media.control.FramePositioningControl");
			if (fpc == null) {
				System.out.println("FPC not supported");
			}
			else {
				System.out.println("Got FPC");
			}
			fgc = (FrameGrabbingControl)player.getControl("javax.media.control.FrameGrabbingControl");
			if (fgc == null) {
				System.out.println("FGC not supported");
			}
			else {
				System.out.println("Got FGC");
			}

			player.prefetch();

		}
		else if (ce instanceof PrefetchCompleteEvent) {
			if (visual != null)
				return;
			controlHeight = 0;
			if ((visual = player.getVisualComponent()) != null) {
				vsSize = visual.getPreferredSize();
				visual.setSize(vsSize);
				//visual.setBounds(50,50,50,50);
			
				videoPanel.add("Center", visual);
		
				if ((control = player.getControlPanelComponent()) != null) {
					controlHeight = control.getPreferredSize().height;
					//control.setBounds(50,50,50,50);
			
					videoPanel.add("South", control);
					videoPanel.setSize(vsSize.width, vsSize.height + controlHeight);
					videoPanel.setPreferredSize(new Dimension(vsSize.width, vsSize.height + controlHeight));
					videoPanel.setMaximumSize(new Dimension(vsSize.width, vsSize.height + controlHeight));

					cSize = centerPanel.getSize();
					vSize = videoPanel.getSize();
					//videoPanel.setLocation((cSize.width - vSize.width) / 2,
					//	(cSize.height - vSize.height) / 2);
				}
				System.out.println("set bounds");
				
				//videoPanel.setBounds(50,50,50,50);
				//videoPanel.setBounds((cSize.width - vSize.width)/2,(cSize.height - vSize.height)/2,(int)((double)getWidth()*0.5),(int)((double)getHeight()*0.5));
				videoPanel.invalidate();
				validate();
			}
			player.start();
		}
		else if (ce instanceof EndOfMediaEvent ||
			ce instanceof StopAtTimeEvent) {
			player.setMediaTime(beginTime);
			player.setStopTime(endTime);
			player.start();
		}
	}
	
	public void paintComponent(Graphics g) { 
		if (cSize!= null && vSize != null) {
		//System.out.println("repaint "+cSize.width+", "+cSize.height+", "+vSize.width+", "+vSize.height);
		}
		super.paintComponent(g);
	}
		
	
			
	class OpenFileControl extends JPanel {


		public OpenFileControl() {

			//TitledBorder tb = new TitledBorder(new EtchedBorder());
			//tb.setTitle("Open File");
			//setBorder(tb);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setPreferredSize(new Dimension(200, 10));
			JPanel bp = new JPanel();
			JPanel ip = new JPanel();
			ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));
			//
			videoOpenB = new JButton("Load Video");
			videoOpenB.setEnabled(true);
			bp.add(videoOpenB);

			fileNameHeading = new JLabel("Source:", JLabel.LEFT);
			fileNameField = new JLabel();
			fileNameField.setHorizontalAlignment(JLabel.RIGHT);
			ip.add(fileNameHeading);
			ip.add(fileNameField);

			videoOpenB.addActionListener(new OpenBAction());
			add(bp, BorderLayout.NORTH);
			imageOpenB = new JButton("Load Image");
			imageOpenB.setEnabled(true);
			bp.add(imageOpenB);
			imageOpenB.addActionListener(new OpenBAction());
			add(bp);
			add(ip);

		}

		class OpenBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {

				JButton button = (JButton) evt.getSource();
				if (button.getText().startsWith("Load Video")) {
					mediaType = VIDEO;
				}
				else {
					mediaType = IMAGE;
				}

				try {
					file = null;
					JFileChooser fc = new JFileChooser(getDirectory());
					fc.setFileFilter(new javax.swing.filechooser.FileFilter () {
						public boolean accept(File f) {
							if (f.isDirectory()) {
								return true;
							}
							String name = f.getName();
							if (name.endsWith(".mov") || name.endsWith(".avi")|| name.endsWith(".mpg") || name.endsWith(".jpg") || name.endsWith(".jpeg")
							|| name.endsWith(".MOV") || name.endsWith(".AVI")|| name.endsWith(".MPG") || name.endsWith(".JPG") || name.endsWith(".JPEG")) {
								return true;
							}
							return false;
						}
						public String getDescription() {
							return ".mov, .avi";
						}
					});

					if (fc.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
						file = fc.getSelectedFile();
						setDirectory(file);
						if (!load(file)) return;
						fileNameField.setText(getMediaFile());

					}
				} catch (SecurityException ex) {
					//reportStatus(EC_AUDIO_PANEL);
					System.out.println("file open error 1");
				} catch (Exception ex) {
					//reportStatus(EC_AUDIO_PANEL);
					System.out.println("file open error 2");
				}
			}

		}

	}

	class SaveFileControl extends JPanel {


		public SaveFileControl() {

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			//setPreferredSize(new Dimension(200,150));
			//TitledBorder tb = new TitledBorder(new EtchedBorder());
			//tb.setTitle("Save File");
			//setBorder(tb);

			JPanel bp = new JPanel();
			JPanel ip = new JPanel();
			ip.setLayout(new BoxLayout(ip, BoxLayout.X_AXIS));

			saveB = new JButton("Send");
			saveB.setEnabled(true);
			bp.add(saveB);
			saveB.addActionListener(new SaveBAction());

			fileNameHeading = new JLabel("Destination:", JLabel.LEFT);
			fileNameField = new JLabel();
			fileNameField.setHorizontalAlignment(JLabel.RIGHT);
			ip.add(fileNameHeading);
			ip.add(fileNameField);
			add(bp);
			add(ip);


		}
		class SaveBAction implements ActionListener {

			public void actionPerformed(ActionEvent evt) {

				try {
					File file = null;
					JFileChooser fc = new JFileChooser(getDirectory());
					fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory()) {
								return true;
							}
							String name = f.getName();
							if (name.endsWith(".mov") || name.endsWith(".avi")|| name.endsWith(".mpg") || name.endsWith(".jpg") || name.endsWith(".jpeg")
							|| name.endsWith(".MOV") || name.endsWith(".AVI")|| name.endsWith(".MPG") || name.endsWith(".JPG") || name.endsWith(".JPEG")) {
								return true;
							}
							return false;
						}
						public String getDescription() {
							return "Save as .mov or .avi file.";
						}
					});
					if (fc.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
						file = fc.getSelectedFile();
						if (!save(file)) return;
						fileNameField.setText(getFileName());
						setDirectory(fc.getSelectedFile());
						return;
					}
					else {
						return;
					}
				} catch (SecurityException ex) {
					//reportStatus(EC_MIDI_SAVE_BADFILE);
					System.out.println("Save file error 1");
					return;
				} catch (Exception ex) {
					//reportStatus(EC_MIDI_SAVE_BADFILE);
					System.out.println("Save file error 2");
					return;
				}
			}
		}
	}



	public static void main(String [] args) {
		VideoCutPanel vcp;
		//frame.setSize(512, 512);

		JFrame mainFrame = new JFrame("JStation");
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		mainFrame.setContentPane(contentPane);
		contentPane.add(vcp = new VideoCutPanel(args, true, mainFrame));
		contentPane.setBackground(vcp.getBackground());
		contentPane.add(vcp = new VideoCutPanel(args, true, mainFrame));

		mainFrame.pack();

		mainFrame.setVisible(true);

	}
}
