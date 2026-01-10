/*
 * HumbleVideoProcessor.java
 *
 * A JMF Processor implementation using humble-video for video processing.
 * This enables processing of formats not supported by FMJ, including R210.
 */
package comul01.eve;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.RGBFormat;
import javax.media.control.TrackControl;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Processor implementation using humble-video (FFmpeg) for video processing.
 * Supports formats that FMJ doesn't support natively, including R210.
 */
public class HumbleVideoProcessor implements Processor {

    private String filePath;
    private int state = Unrealized;
    private int targetState = Unrealized;
    private List<ControllerListener> listeners = new ArrayList<>();

    private Demuxer demuxer;
    private Decoder videoDecoder;
    private int videoStreamIndex = -1;

    private int videoWidth;
    private int videoHeight;
    private double frameRate;
    private long durationNanos;
    private long currentTimeNanos = 0;

    private MediaPicture picture;
    private MediaPictureConverter converter;
    private MediaPacket packet;
    private BufferedImage currentImage;

    private Thread processingThread;
    private boolean processing = false;
    private float rate = 1.0f;

    private Time beginTime = new Time(0);
    private Time stopTime = null;
    private ContentDescriptor contentDescriptor;
    private DataSource outputDataSource;

    private HumbleTrackControl[] trackControls;
    private RGBFormat videoFormat;

    public HumbleVideoProcessor(MediaLocator locator) {
        String url = locator.toExternalForm();
        if (url.startsWith("file:")) {
            this.filePath = url.substring(5);
            // Handle Windows paths
            if (filePath.startsWith("/") && filePath.length() > 2 && filePath.charAt(2) == ':') {
                this.filePath = filePath.substring(1);
            }
        } else {
            this.filePath = url;
        }
        System.out.println("HumbleVideoProcessor: Created for " + filePath);
    }

    public HumbleVideoProcessor(HumbleVideoDataSource dataSource) {
        // Extract file path from data source
        this.filePath = dataSource.getLocator().toExternalForm();
        if (filePath.startsWith("file:")) {
            filePath = filePath.substring(5);
            if (filePath.startsWith("/") && filePath.length() > 2 && filePath.charAt(2) == ':') {
                filePath = filePath.substring(1);
            }
        }
        System.out.println("HumbleVideoProcessor: Created from HumbleVideoDataSource for " + filePath);
    }

    private void openMedia() throws IOException {
        System.out.println("HumbleVideoProcessor: Opening " + filePath);

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

                        System.out.println("HumbleVideoProcessor: Video - " + videoWidth + "x" + videoHeight +
                            " @ " + frameRate + " fps, codec: " + videoDecoder.getCodec().getName());

                        // Create decode buffers
                        picture = MediaPicture.make(videoWidth, videoHeight, videoDecoder.getPixelFormat());
                        packet = MediaPacket.make();

                        // Create video format - RGB24
                        videoFormat = new RGBFormat(
                            new Dimension(videoWidth, videoHeight),
                            videoWidth * videoHeight * 3,
                            Format.byteArray,
                            (float) frameRate,
                            24,
                            3, 2, 1,  // BGR order
                            3,
                            videoWidth * 3,
                            Format.FALSE,
                            Format.NOT_SPECIFIED
                        );

                        break;
                    }
                }
            }

            if (videoStreamIndex < 0) {
                throw new IOException("No video stream found in " + filePath);
            }

            // Create track controls
            trackControls = new HumbleTrackControl[] {
                new HumbleTrackControl(this, videoFormat)
            };

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while opening", e);
        } catch (Exception e) {
            throw new IOException("Failed to open: " + e.getMessage(), e);
        }
    }

    // ===== Processor interface =====

    @Override
    public ContentDescriptor setContentDescriptor(ContentDescriptor outputContentDescriptor) {
        this.contentDescriptor = outputContentDescriptor;
        System.out.println("HumbleVideoProcessor: setContentDescriptor " + outputContentDescriptor);
        return contentDescriptor;
    }

    @Override
    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    @Override
    public ContentDescriptor[] getSupportedContentDescriptors() {
        return new ContentDescriptor[] {
            new ContentDescriptor(ContentDescriptor.RAW),
            new FileTypeDescriptor(FileTypeDescriptor.MSVIDEO),
            new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME)
        };
    }

    @Override
    public TrackControl[] getTrackControls() {
        return trackControls;
    }

    @Override
    public DataSource getDataOutput() {
        if (outputDataSource == null) {
            outputDataSource = new HumbleProcessorDataSource(this);
        }
        return outputDataSource;
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
            beginTime = now;
            seek(currentTimeNanos);
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
        return targetState;
    }

    @Override
    public void realize() {
        if (state >= Realized) return;
        targetState = Realized;

        // Realize is called after configure
        state = Realized;
        notifyListeners(new RealizeCompleteEvent(this, Configuring, Realized, Realized));
    }

    @Override
    public void prefetch() {
        if (state < Realized) {
            realize();
        }
        if (state >= Prefetched) return;
        targetState = Prefetched;

        // Decode first frame
        try {
            decodeNextFrame();
            state = Prefetched;
            notifyListeners(new PrefetchCompleteEvent(this, Realized, Prefetched, Prefetched));
        } catch (Exception e) {
            System.err.println("HumbleVideoProcessor: Failed to prefetch - " + e.getMessage());
        }
    }

    public void configure() {
        if (state >= Configured) return;
        targetState = Configured;

        try {
            openMedia();
            state = Configured;
            notifyListeners(new ConfigureCompleteEvent(this, Unrealized, Configured, Configured));
        } catch (Exception e) {
            System.err.println("HumbleVideoProcessor: Failed to configure - " + e.getMessage());
            e.printStackTrace();
            notifyListeners(new ResourceUnavailableEvent(this, e.getMessage()));
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
        return trackControls != null ? trackControls : new Control[0];
    }

    @Override
    public Control getControl(String forName) {
        if (forName.contains("TrackControl") && trackControls != null && trackControls.length > 0) {
            return trackControls[0];
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
        for (ControllerListener l : new ArrayList<>(listeners)) {
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

        if (processing) return;
        processing = true;
        state = Started;

        processingThread = new Thread(this::processLoop, "HumbleVideoProcessor");
        processingThread.start();

        notifyListeners(new StartEvent(this, Prefetched, Started, Started, getMediaTime(), getMediaTime()));
    }

    @Override
    public void stop() {
        processing = false;
        if (processingThread != null) {
            processingThread.interrupt();
            try {
                processingThread.join(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            processingThread = null;
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
        return null; // Processor doesn't display video
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

    // ===== Internal methods =====

    private void seek(long timeNanos) {
        if (demuxer == null) return;

        try {
            long timestamp = timeNanos / 1000; // to microseconds
            demuxer.seek(videoStreamIndex, timestamp, timestamp, timestamp, 0);
            currentTimeNanos = timeNanos;
        } catch (Exception e) {
            System.err.println("HumbleVideoProcessor: Seek failed - " + e.getMessage());
        }
    }

    private void processLoop() {
        long frameIntervalNanos = (long) (1e9 / frameRate / rate);
        long lastFrameTime = System.nanoTime();

        while (processing) {
            try {
                if (!decodeNextFrame()) {
                    // End of media
                    processing = false;
                    notifyListeners(new EndOfMediaEvent(this, Started, Prefetched, Prefetched, getMediaTime()));
                    break;
                }

                // Check stop time
                if (stopTime != null && currentTimeNanos >= stopTime.getNanoseconds()) {
                    processing = false;
                    notifyListeners(new StopAtTimeEvent(this, Started, Prefetched, Prefetched, getMediaTime()));
                    break;
                }

                // Note: Effects are applied in HumbleProcessorStream.read() when frames are read
                // for output. This processLoop is primarily for timing during processing.

                // Wait for next frame
                long elapsed = System.nanoTime() - lastFrameTime;
                long sleepNanos = frameIntervalNanos - elapsed;
                if (sleepNanos > 0) {
                    Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
                }
                lastFrameTime = System.nanoTime();

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("HumbleVideoProcessor: Processing error - " + e.getMessage());
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
            System.err.println("HumbleVideoProcessor: Decode error - " + e.getMessage());
        }

        return false;
    }

    // Getters for video info
    public int getVideoWidth() { return videoWidth; }
    public int getVideoHeight() { return videoHeight; }
    public double getFrameRate() { return frameRate; }
    public BufferedImage getCurrentImage() { return currentImage; }
    public RGBFormat getVideoFormat() { return videoFormat; }

    /**
     * Inner class implementing TrackControl for video track
     */
    static class HumbleTrackControl implements TrackControl {
        private HumbleVideoProcessor processor;
        private Format format;
        private Format setFormat;
        private boolean enabled = true;
        private javax.media.Codec[] codecChain;
        private Renderer renderer;

        HumbleTrackControl(HumbleVideoProcessor processor, Format format) {
            this.processor = processor;
            this.format = format;
            this.setFormat = format;
        }

        @Override
        public Format getFormat() {
            return format;
        }

        @Override
        public Format[] getSupportedFormats() {
            return new Format[] { format };
        }

        @Override
        public Format setFormat(Format format) {
            if (format != null) {
                this.setFormat = format;
            }
            return this.setFormat;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void setCodecChain(javax.media.Codec[] codecs) throws UnsupportedPlugInException {
            this.codecChain = codecs;
            System.out.println("HumbleTrackControl: setCodecChain with " +
                (codecs != null ? codecs.length : 0) + " codecs");

            // Initialize codecs with input/output formats
            if (codecs != null) {
                for (javax.media.Codec codec : codecs) {
                    codec.setInputFormat(format);
                    Format[] outputs = codec.getSupportedOutputFormats(format);
                    if (outputs != null && outputs.length > 0) {
                        codec.setOutputFormat(outputs[0]);
                    }
                    try {
                        codec.open();
                    } catch (ResourceUnavailableException e) {
                        throw new UnsupportedPlugInException("Failed to open codec: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        public void setRenderer(Renderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public Object getControl(String controlType) {
            return null;
        }

        @Override
        public Object[] getControls() {
            return new Object[0];
        }

        void processFrame(BufferedImage image, long timeNanos) {
            if (!enabled || codecChain == null || image == null) return;

            try {
                int w = image.getWidth();
                int h = image.getHeight();

                // Convert BufferedImage to Buffer
                int[] pixels = new int[w * h];
                image.getRGB(0, 0, w, h, pixels, 0, w);

                byte[] data = new byte[w * h * 3];
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    data[i * 3] = (byte) (pixel & 0xFF);           // Blue
                    data[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);    // Green
                    data[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF);   // Red
                }

                Buffer inputBuffer = new Buffer();
                inputBuffer.setData(data);
                inputBuffer.setOffset(0);
                inputBuffer.setLength(data.length);
                inputBuffer.setFormat(format);
                inputBuffer.setTimeStamp(timeNanos);

                // Process through codec chain
                Buffer outputBuffer = new Buffer();
                for (javax.media.Codec codec : codecChain) {
                    int result = codec.process(inputBuffer, outputBuffer);
                    if (result == javax.media.Codec.BUFFER_PROCESSED_OK) {
                        // Swap buffers for next codec
                        Buffer temp = inputBuffer;
                        inputBuffer = outputBuffer;
                        outputBuffer = temp;
                        outputBuffer.setData(null);
                    }
                }

            } catch (Exception e) {
                System.err.println("HumbleTrackControl: Error processing frame - " + e.getMessage());
            }
        }

        @Override
        public Component getControlComponent() {
            return null;
        }
    }

    /**
     * Inner class implementing DataSource for processor output
     */
    public static class HumbleProcessorDataSource extends PushBufferDataSource {
        private HumbleVideoProcessor processor;
        private HumbleProcessorStream[] streams;

        HumbleProcessorDataSource(HumbleVideoProcessor processor) {
            this.processor = processor;
            // Initialize streams immediately so they're available before connect()
            streams = new HumbleProcessorStream[] {
                new HumbleProcessorStream(processor)
            };
        }

        public HumbleVideoProcessor getProcessor() {
            return processor;
        }

        @Override
        public void connect() throws IOException {
            // Streams already initialized in constructor
            if (streams == null) {
                streams = new HumbleProcessorStream[] {
                    new HumbleProcessorStream(processor)
                };
            }
        }

        @Override
        public void disconnect() {
            streams = null;
        }

        @Override
        public void start() throws IOException {
            // Started by processor
        }

        @Override
        public void stop() throws IOException {
            // Stopped by processor
        }

        @Override
        public PushBufferStream[] getStreams() {
            return streams;
        }

        @Override
        public String getContentType() {
            return processor.contentDescriptor != null ?
                processor.contentDescriptor.getContentType() : ContentDescriptor.RAW;
        }

        @Override
        public MediaLocator getLocator() {
            return new MediaLocator("file:" + processor.filePath);
        }

        @Override
        public void setLocator(MediaLocator locator) {
            // Not supported
        }

        @Override
        public Time getDuration() {
            return processor.getDuration();
        }

        @Override
        public Object getControl(String controlType) {
            return null;
        }

        @Override
        public Object[] getControls() {
            return new Object[0];
        }
    }

    /**
     * Inner class implementing PushBufferStream for processor output
     */
    static class HumbleProcessorStream implements PushBufferStream {
        private HumbleVideoProcessor processor;
        private BufferTransferHandler transferHandler;
        private boolean endOfStream = false;

        HumbleProcessorStream(HumbleVideoProcessor processor) {
            this.processor = processor;
        }

        @Override
        public void read(Buffer buffer) throws IOException {
            if (endOfStream) {
                buffer.setEOM(true);
                buffer.setLength(0);
                return;
            }

            // Decode next frame from the processor
            boolean hasFrame = processor.decodeNextFrame();
            if (!hasFrame) {
                endOfStream = true;
                buffer.setEOM(true);
                buffer.setLength(0);
                return;
            }

            BufferedImage image = processor.getCurrentImage();
            if (image == null) {
                buffer.setEOM(true);
                buffer.setLength(0);
                return;
            }

            int w = image.getWidth();
            int h = image.getHeight();

            int[] pixels = new int[w * h];
            image.getRGB(0, 0, w, h, pixels, 0, w);

            byte[] data = new byte[w * h * 3];
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                data[i * 3] = (byte) (pixel & 0xFF);
                data[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);
                data[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF);
            }

            buffer.setData(data);
            buffer.setOffset(0);
            buffer.setLength(data.length);
            buffer.setFormat(processor.getVideoFormat());
            buffer.setTimeStamp(processor.getMediaNanoseconds());

            // Apply effects through the codec chain if configured
            if (processor.trackControls != null && processor.trackControls.length > 0) {
                HumbleTrackControl trackControl = processor.trackControls[0];
                if (trackControl.codecChain != null && trackControl.enabled) {
                    // Process through codec chain - effects modify the buffer in place
                    Buffer outputBuffer = new Buffer();
                    Buffer inputBuffer = buffer;

                    for (javax.media.Codec codec : trackControl.codecChain) {
                        int result = codec.process(inputBuffer, outputBuffer);
                        if (result == javax.media.Codec.BUFFER_PROCESSED_OK) {
                            // The effect swaps data between input and output buffers
                            // After processing, outputBuffer contains the processed data
                            // Copy the result back to the main buffer
                            buffer.setData(outputBuffer.getData());
                            buffer.setOffset(outputBuffer.getOffset());
                            buffer.setLength(outputBuffer.getLength());
                            buffer.setFormat(outputBuffer.getFormat());

                            // For next codec in chain, use output as new input
                            inputBuffer = outputBuffer;
                            outputBuffer = new Buffer();
                        }
                    }
                }
            }
        }

        @Override
        public Format getFormat() {
            return processor.getVideoFormat();
        }

        @Override
        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        @Override
        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor(ContentDescriptor.RAW);
        }

        @Override
        public long getContentLength() {
            return LENGTH_UNKNOWN;
        }

        @Override
        public boolean endOfStream() {
            return endOfStream;
        }

        @Override
        public Object getControl(String controlType) {
            return null;
        }

        @Override
        public Object[] getControls() {
            return new Object[0];
        }
    }
}
