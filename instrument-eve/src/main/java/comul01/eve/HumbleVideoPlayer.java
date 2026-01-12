/*
 * HumbleVideoPlayer.java
 *
 * A simplified JMF Player implementation using humble-video for video playback.
 * This enables playback of formats not supported by FMJ, including R210.
 */
package comul01.eve;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.RGBFormat;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A Player implementation using humble-video (FFmpeg) for video playback.
 * Supports formats that FMJ doesn't support natively, including R210.
 */
public class HumbleVideoPlayer implements Player, FrameGrabbingControl, FramePositioningControl {

    private String filePath;
    private int state = Unrealized;
    private List<ControllerListener> listeners = new ArrayList<>();

    private Demuxer demuxer;
    private Decoder videoDecoder;
    private int videoStreamIndex = -1;

    private int videoWidth;
    private int videoHeight;
    private double frameRate;
    private long durationNanos;
    private long currentTimeNanos = 0;
    private int currentFrame = 0;

    private MediaPicture picture;
    private MediaPictureConverter converter;
    private MediaPacket packet;
    private BufferedImage currentImage;

    private Component visualComponent;
    private Thread playbackThread;
    private boolean playing = false;
    private float rate = 1.0f;

    private Time stopTime = null;

    public HumbleVideoPlayer(URL url) {
        String urlStr = url.toExternalForm();
        if (urlStr.startsWith("file:")) {
            this.filePath = urlStr.substring(5);
            // Handle Windows paths
            if (filePath.startsWith("/") && filePath.length() > 2 && filePath.charAt(2) == ':') {
                this.filePath = filePath.substring(1);
            }
        } else {
            this.filePath = urlStr;
        }
        System.out.println("HumbleVideoPlayer: Created for " + filePath);
    }

    public HumbleVideoPlayer(MediaLocator locator) {
        this(locatorToURL(locator));
    }

    private static URL locatorToURL(MediaLocator locator) {
        try {
            return locator.getURL();
        } catch (Exception e) {
            throw new RuntimeException("Invalid MediaLocator: " + locator, e);
        }
    }

    private void openMedia() throws IOException {
        System.out.println("HumbleVideoPlayer: Opening " + filePath);

        try {
            demuxer = Demuxer.make();
            demuxer.open(filePath, null, false, true, null, null);

            int numStreams = demuxer.getNumStreams();

            for (int i = 0; i < numStreams; i++) {
                DemuxerStream stream = demuxer.getStream(i);
                Decoder decoder = stream.getDecoder();

                if (decoder != null && decoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO) {
                    if (videoStreamIndex < 0) {
                        videoStreamIndex = i;
                        videoDecoder = decoder;
                        videoDecoder.open(null, null);

                        videoWidth = videoDecoder.getWidth();
                        videoHeight = videoDecoder.getHeight();

                        // Calculate frame rate
                        Rational tb = stream.getTimeBase();
                        if (tb != null && tb.getNumerator() > 0 && tb.getDenominator() > 0) {
                            double tbVal = tb.getDouble();
                            if (tbVal > 0 && tbVal < 1) {
                                frameRate = 1.0 / tbVal;
                            } else {
                                frameRate = 24.0;
                            }
                        } else {
                            frameRate = 24.0;
                        }
                        if (frameRate < 1 || frameRate > 120) {
                            frameRate = 24.0;
                        }

                        long duration = demuxer.getDuration();
                        if (duration != Global.NO_PTS) {
                            durationNanos = duration * 1000;
                        }

                        System.out.println("HumbleVideoPlayer: Video - " + videoWidth + "x" + videoHeight +
                            " @ " + frameRate + " fps, codec: " + videoDecoder.getCodec().getName());

                        // Create decode buffers
                        picture = MediaPicture.make(videoWidth, videoHeight, videoDecoder.getPixelFormat());
                        packet = MediaPacket.make();

                        break;
                    }
                }
            }

            if (videoStreamIndex < 0) {
                throw new IOException("No video stream found in " + filePath);
            }

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while opening", e);
        } catch (Exception e) {
            throw new IOException("Failed to open: " + e.getMessage(), e);
        }
    }

    // ===== Clock interface =====

    @Override
    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException {
        // Use internal time base
    }

    @Override
    public TimeBase getTimeBase() {
        return null;
    }

    @Override
    public void setMediaTime(Time now) {
        if (now != null) {
            currentTimeNanos = now.getNanoseconds();
            currentFrame = (int) (currentTimeNanos / 1e9 * frameRate);
            seek(currentFrame);
        }
    }

    @Override
    public Time getMediaTime() {
        return new Time(currentTimeNanos);
    }

    @Override
    public long getMediaNanoseconds() {
        return currentTimeNanos;
    }

    @Override
    public Time getSyncTime() {
        return getMediaTime();
    }

    @Override
    public float setRate(float factor) {
        this.rate = factor;
        return rate;
    }

    @Override
    public float getRate() {
        return rate;
    }

    @Override
    public void syncStart(Time at) {
        start();
    }

    @Override
    public void setStopTime(Time stopTime) {
        this.stopTime = stopTime;
    }

    @Override
    public Time getStopTime() {
        return stopTime;
    }

    @Override
    public Time mapToTimeBase(Time t) throws ClockStoppedException {
        return t;
    }

    // ===== Controller interface =====

    @Override
    public int getState() {
        return state;
    }

    @Override
    public int getTargetState() {
        return state;
    }

    @Override
    public void realize() {
        if (state >= Realized) return;

        try {
            openMedia();
            state = Realized;
            notifyListeners(new RealizeCompleteEvent(this, Unrealized, Realized, Realized));
        } catch (Exception e) {
            System.err.println("HumbleVideoPlayer: Failed to realize - " + e.getMessage());
            e.printStackTrace();
            notifyListeners(new ResourceUnavailableEvent(this, e.getMessage()));
        }
    }

    @Override
    public void prefetch() {
        if (state < Realized) {
            realize();
        }
        if (state >= Prefetched) return;

        // Decode first frame
        try {
            decodeNextFrame();
            state = Prefetched;
            notifyListeners(new PrefetchCompleteEvent(this, Realized, Prefetched, Prefetched));
        } catch (Exception e) {
            System.err.println("HumbleVideoPlayer: Failed to prefetch - " + e.getMessage());
        }
    }

    @Override
    public void deallocate() {
        stop();
        if (videoDecoder != null) {
            videoDecoder.delete();
            videoDecoder = null;
        }
        if (demuxer != null) {
            try {
                demuxer.close();
            } catch (Exception e) {
                // ignore
            }
            demuxer = null;
        }
        state = Unrealized;
    }

    @Override
    public void close() {
        deallocate();
        notifyListeners(new ControllerClosedEvent(this));
    }

    @Override
    public Time getStartLatency() {
        return new Time(0);
    }

    @Override
    public Control[] getControls() {
        return new Control[] { this };
    }

    @Override
    public Control getControl(String forName) {
        if (forName.contains("FrameGrabbingControl")) {
            return this;
        }
        if (forName.contains("FramePositioningControl")) {
            return this;
        }
        return null;
    }

    @Override
    public void addControllerListener(ControllerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeControllerListener(ControllerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ControllerEvent event) {
        for (ControllerListener l : listeners) {
            l.controllerUpdate(event);
        }
    }

    // ===== Duration interface =====

    @Override
    public Time getDuration() {
        return new Time(durationNanos);
    }

    // ===== Player interface =====

    @Override
    public void start() {
        if (state < Prefetched) {
            prefetch();
        }

        if (playing) return;
        playing = true;
        state = Started;

        playbackThread = new Thread(this::playLoop, "HumbleVideoPlayer");
        playbackThread.start();

        notifyListeners(new StartEvent(this, Prefetched, Started, Started, getMediaTime(), getMediaTime()));
    }

    @Override
    public void stop() {
        playing = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            playbackThread = null;
        }
        if (state == Started) {
            state = Prefetched;
            notifyListeners(new StopEvent(this, Started, Prefetched, Prefetched, getMediaTime()));
        }
    }

    @Override
    public void setSource(DataSource source) throws IOException, IncompatibleSourceException {
        // Not used - we open directly from file
    }

    @Override
    public Component getVisualComponent() {
        if (visualComponent == null) {
            visualComponent = new VideoPanel();
        }
        return visualComponent;
    }

    @Override
    public GainControl getGainControl() {
        return null;
    }

    @Override
    public Component getControlPanelComponent() {
        return null;
    }

    @Override
    public void addController(Controller newController) throws IncompatibleTimeBaseException {
        // Not supported
    }

    @Override
    public void removeController(Controller oldController) {
        // Not supported
    }

    // ===== FrameGrabbingControl interface =====

    @Override
    public Buffer grabFrame() {
        if (currentImage == null) return null;

        Buffer buffer = new Buffer();
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();

        int[] pixels = new int[w * h];
        currentImage.getRGB(0, 0, w, h, pixels, 0, w);

        byte[] data = new byte[w * h * 3];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            data[i * 3] = (byte) (pixel & 0xFF);           // Blue
            data[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);    // Green
            data[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF);   // Red
        }

        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(data.length);
        buffer.setFormat(new RGBFormat(
            new Dimension(w, h),
            w * h * 3,
            Format.byteArray,
            (float) frameRate,
            24,
            3, 2, 1,
            3,
            w * 3,
            Format.FALSE,
            Format.NOT_SPECIFIED
        ));

        return buffer;
    }

    @Override
    public Component getControlComponent() {
        return null;
    }

    // ===== FramePositioningControl interface =====

    @Override
    public int seek(int frameNumber) {
        if (demuxer == null) return 0;

        try {
            long timestamp = (long) (frameNumber / frameRate * 1000000); // to microseconds
            demuxer.seek(videoStreamIndex, timestamp, timestamp, timestamp, 0);
            currentFrame = frameNumber;
            currentTimeNanos = (long) (frameNumber / frameRate * 1e9);

            // Decode to update current image
            decodeNextFrame();
        } catch (Exception e) {
            System.err.println("HumbleVideoPlayer: Seek failed - " + e.getMessage());
        }

        return currentFrame;
    }

    @Override
    public int skip(int framesToSkip) {
        return seek(currentFrame + framesToSkip);
    }

    @Override
    public Time mapFrameToTime(int frameNumber) {
        return new Time((long) (frameNumber / frameRate * 1e9));
    }

    @Override
    public int mapTimeToFrame(Time mediaTime) {
        return (int) (mediaTime.getNanoseconds() / 1e9 * frameRate);
    }

    // ===== Internal methods =====

    private void playLoop() {
        long frameIntervalNanos = (long) (1e9 / frameRate / rate);
        long lastFrameTime = System.nanoTime();

        while (playing) {
            try {
                if (!decodeNextFrame()) {
                    // End of media
                    playing = false;
                    notifyListeners(new EndOfMediaEvent(this, Started, Prefetched, Prefetched, getMediaTime()));
                    break;
                }

                // Check stop time
                if (stopTime != null && currentTimeNanos >= stopTime.getNanoseconds()) {
                    playing = false;
                    notifyListeners(new StopAtTimeEvent(this, Started, Prefetched, Prefetched, getMediaTime()));
                    break;
                }

                // Wait for next frame
                long elapsed = System.nanoTime() - lastFrameTime;
                long sleepNanos = frameIntervalNanos - elapsed;
                if (sleepNanos > 0) {
                    Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
                }
                lastFrameTime = System.nanoTime();

                // Repaint
                if (visualComponent != null) {
                    visualComponent.repaint();
                }

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("HumbleVideoPlayer: Playback error - " + e.getMessage());
                break;
            }
        }

        state = Prefetched;
    }

    private boolean decodeNextFrame() {
        if (demuxer == null || videoDecoder == null) return false;

        try {
            while (demuxer.read(packet) >= 0) {
                if (packet.getStreamIndex() == videoStreamIndex) {
                    int offset = 0;
                    int bytesRead = 0;

                    do {
                        bytesRead += videoDecoder.decode(picture, packet, offset);
                        if (picture.isComplete()) {
                            // Convert to BufferedImage
                            if (converter == null) {
                                converter = MediaPictureConverterFactory.createConverter(
                                    MediaPictureConverterFactory.HUMBLE_BGR_24,
                                    picture
                                );
                            }
                            currentImage = converter.toImage(currentImage, picture);

                            // Update time
                            long pts = picture.getTimeStamp();
                            if (pts != Global.NO_PTS) {
                                currentTimeNanos = pts * 1000;
                            }
                            currentFrame++;

                            return true;
                        }
                        offset += bytesRead;
                    } while (offset < packet.getSize());
                }
            }

            // Flush decoder
            videoDecoder.decode(picture, null, 0);
            if (picture.isComplete()) {
                if (converter == null) {
                    converter = MediaPictureConverterFactory.createConverter(
                        MediaPictureConverterFactory.HUMBLE_BGR_24,
                        picture
                    );
                }
                currentImage = converter.toImage(currentImage, picture);
                return true;
            }

        } catch (Exception e) {
            System.err.println("HumbleVideoPlayer: Decode error - " + e.getMessage());
            System.err.println("  Frame: " + currentFrame + ", Time: " + (currentTimeNanos / 1_000_000) + "ms");
            if (packet != null) {
                System.err.println("  Packet: size=" + packet.getSize() +
                    ", pts=" + packet.getPts() +
                    ", dts=" + packet.getDts() +
                    ", streamIndex=" + packet.getStreamIndex() +
                    ", isKey=" + packet.isKeyPacket() +
                    ", isComplete=" + packet.isComplete());
            }
            if (videoDecoder != null) {
                System.err.println("  Decoder: codec=" + videoDecoder.getCodec().getName() +
                    ", pixelFormat=" + videoDecoder.getPixelFormat() +
                    ", width=" + videoDecoder.getWidth() +
                    ", height=" + videoDecoder.getHeight());
            }
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Panel for displaying video frames
     */
    private class VideoPanel extends Component {
        @Override
        public void paint(Graphics g) {
            if (currentImage != null) {
                g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(videoWidth > 0 ? videoWidth : 640, videoHeight > 0 ? videoHeight : 480);
        }
    }

    // Getters for video info
    public int getVideoWidth() { return videoWidth; }
    public int getVideoHeight() { return videoHeight; }
    public double getFrameRate() { return frameRate; }
    public BufferedImage getCurrentImage() { return currentImage; }
}
