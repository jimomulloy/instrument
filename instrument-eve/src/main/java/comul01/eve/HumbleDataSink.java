/*
 * HumbleDataSink.java
 *
 * A JMF-compatible DataSink that uses humble-video (FFmpeg) to write video files.
 * This enables writing to formats that FMJ doesn't support natively.
 */
package comul01.eve;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.media.*;
import javax.media.datasink.*;
import javax.media.protocol.*;
import javax.media.format.RGBFormat;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DataSink implementation using humble-video (FFmpeg) for writing video files.
 * Supports writing AVI, MOV, and other formats supported by FFmpeg.
 */
public class HumbleDataSink implements DataSink {

    private DataSource inputDataSource;
    private MediaLocator outputLocator;
    private String outputPath;
    private List<DataSinkListener> listeners = new ArrayList<>();

    private Muxer muxer;
    private Encoder videoEncoder;
    private int videoStreamIndex = -1;

    private int videoWidth;
    private int videoHeight;
    private double frameRate;

    private MediaPicture picture;
    private MediaPictureConverter converter;
    private MediaPacket packet;

    private Thread writeThread;
    private boolean writing = false;
    private boolean opened = false;

    public HumbleDataSink(DataSource source, MediaLocator outputLocator) {
        this.inputDataSource = source;
        this.outputLocator = outputLocator;

        // Convert MediaLocator to file path
        String url = outputLocator.toExternalForm();
        if (url.startsWith("file:")) {
            this.outputPath = url.substring(5);
            // Handle Windows paths
            if (outputPath.startsWith("/") && outputPath.length() > 2 && outputPath.charAt(2) == ':') {
                this.outputPath = outputPath.substring(1);
            }
        } else {
            this.outputPath = url;
        }
        System.out.println("HumbleDataSink: Created for output " + outputPath);
    }

    @Override
    public void setSource(DataSource source) throws IOException, IncompatibleSourceException {
        this.inputDataSource = source;
    }

    @Override
    public void setOutputLocator(MediaLocator output) {
        this.outputLocator = output;
        String url = output.toExternalForm();
        if (url.startsWith("file:")) {
            this.outputPath = url.substring(5);
            if (outputPath.startsWith("/") && outputPath.length() > 2 && outputPath.charAt(2) == ':') {
                this.outputPath = outputPath.substring(1);
            }
        } else {
            this.outputPath = url;
        }
    }

    @Override
    public MediaLocator getOutputLocator() {
        return outputLocator;
    }

    @Override
    public String getContentType() {
        return inputDataSource != null ? inputDataSource.getContentType() : ContentDescriptor.RAW;
    }

    @Override
    public void open() throws IOException, SecurityException {
        if (opened) return;

        System.out.println("HumbleDataSink: Opening for write to " + outputPath);

        try {
            // Connect the input DataSource first if needed
            if (inputDataSource != null) {
                try {
                    inputDataSource.connect();
                    System.out.println("HumbleDataSink: Input DataSource connected");
                } catch (Exception e) {
                    System.out.println("HumbleDataSink: DataSource connect (may already be connected): " + e.getMessage());
                }
            }

            // Determine format from input DataSource
            if (inputDataSource instanceof PushBufferDataSource) {
                PushBufferDataSource pbds = (PushBufferDataSource) inputDataSource;
                PushBufferStream[] streams = pbds.getStreams();

                if (streams != null) {
                    for (PushBufferStream stream : streams) {
                        Format format = stream.getFormat();
                        if (format instanceof RGBFormat) {
                            RGBFormat rgbFormat = (RGBFormat) format;
                            if (rgbFormat.getSize() != null) {
                                videoWidth = rgbFormat.getSize().width;
                                videoHeight = rgbFormat.getSize().height;
                            }
                            frameRate = rgbFormat.getFrameRate();
                            if (frameRate <= 0) frameRate = 24.0;

                            System.out.println("HumbleDataSink: Video format " + videoWidth + "x" + videoHeight + " @ " + frameRate + " fps");
                            break;
                        }
                    }
                } else {
                    System.out.println("HumbleDataSink: No streams from DataSource, using defaults");
                }
            }

            // Try to get dimensions from HumbleVideoProcessor if available
            if ((videoWidth <= 0 || videoHeight <= 0) && inputDataSource instanceof HumbleVideoProcessor.HumbleProcessorDataSource) {
                HumbleVideoProcessor.HumbleProcessorDataSource hpds = (HumbleVideoProcessor.HumbleProcessorDataSource) inputDataSource;
                HumbleVideoProcessor hvp = hpds.getProcessor();
                if (hvp != null) {
                    videoWidth = hvp.getVideoWidth();
                    videoHeight = hvp.getVideoHeight();
                    frameRate = hvp.getFrameRate();
                    System.out.println("HumbleDataSink: Got dimensions from HumbleVideoProcessor: " + videoWidth + "x" + videoHeight + " @ " + frameRate + " fps");
                }
            }

            // Default dimensions if not found
            if (videoWidth <= 0) videoWidth = 1920;
            if (videoHeight <= 0) videoHeight = 1080;
            if (frameRate <= 0) frameRate = 24.0;

            // Ensure parent directory exists
            java.io.File outputFile = new java.io.File(outputPath);
            java.io.File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                System.out.println("HumbleDataSink: Creating directory " + parentDir.getAbsolutePath());
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            // Create muxer
            muxer = Muxer.make(outputPath, null, null);

            // Find encoder codec - use MJPEG for AVI compatibility
            io.humble.video.Codec codec = io.humble.video.Codec.findEncodingCodec(io.humble.video.Codec.ID.CODEC_ID_MJPEG);
            if (codec == null) {
                // Fallback to raw video
                codec = io.humble.video.Codec.findEncodingCodec(io.humble.video.Codec.ID.CODEC_ID_RAWVIDEO);
            }
            if (codec == null) {
                throw new IOException("No suitable video encoder found");
            }

            System.out.println("HumbleDataSink: Using codec " + codec.getName());

            // Create encoder
            videoEncoder = Encoder.make(codec);
            videoEncoder.setWidth(videoWidth);
            videoEncoder.setHeight(videoHeight);
            // Use YUVJ420P for MJPEG (full range), YUV420P for other codecs
            if (codec.getID() == io.humble.video.Codec.ID.CODEC_ID_MJPEG) {
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUVJ420P);
            } else {
                videoEncoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
            }
            videoEncoder.setTimeBase(Rational.make(1, (int) frameRate));

            // Open encoder (use default quality settings)
            videoEncoder.open(null, null);

            // Add stream to muxer
            muxer.addNewStream(videoEncoder);
            videoStreamIndex = 0;

            // Create buffers with same pixel format as encoder
            PixelFormat.Type pixFmt = videoEncoder.getPixelFormat();
            picture = MediaPicture.make(videoWidth, videoHeight, pixFmt);
            picture.setTimeBase(Rational.make(1, (int) frameRate));
            packet = MediaPacket.make();

            // Open muxer
            muxer.open(null, null);

            opened = true;
            System.out.println("HumbleDataSink: Opened successfully");

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while opening", e);
        } catch (Exception e) {
            throw new IOException("Failed to open: " + e.getMessage(), e);
        }
    }

    @Override
    public void start() throws IOException {
        if (!opened) {
            open();
        }

        if (writing) return;
        writing = true;

        System.out.println("HumbleDataSink: Starting write");

        writeThread = new Thread(this::writeLoop, "HumbleDataSink-Writer");
        writeThread.start();
    }

    @Override
    public void stop() throws IOException {
        writing = false;
        if (writeThread != null) {
            writeThread.interrupt();
            try {
                writeThread.join(5000);
            } catch (InterruptedException e) {
                // ignore
            }
            writeThread = null;
        }
    }

    @Override
    public void close() {
        try {
            stop();
        } catch (IOException e) {
            // ignore
        }

        try {
            // Flush encoder
            if (videoEncoder != null && muxer != null) {
                do {
                    videoEncoder.encode(packet, null);
                    if (packet.isComplete()) {
                        muxer.write(packet, false);
                    }
                } while (packet.isComplete());
            }

            if (muxer != null) {
                muxer.close();
                muxer = null;
            }
            if (videoEncoder != null) {
                videoEncoder.delete();
                videoEncoder = null;
            }
        } catch (Exception e) {
            System.err.println("HumbleDataSink: Error closing - " + e.getMessage());
        }

        opened = false;
        notifyListeners(new DataSinkEvent(this) {});
    }

    @Override
    public void addDataSinkListener(DataSinkListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeDataSinkListener(DataSinkListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(DataSinkEvent event) {
        for (DataSinkListener l : new ArrayList<>(listeners)) {
            l.dataSinkUpdate(event);
        }
    }

    @Override
    public Object getControl(String controlType) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    private void writeLoop() {
        long frameNumber = 0;

        try {
            if (inputDataSource instanceof PushBufferDataSource) {
                PushBufferDataSource pbds = (PushBufferDataSource) inputDataSource;
                PushBufferStream[] streams = pbds.getStreams();

                // Find video stream
                PushBufferStream videoStream = null;
                for (PushBufferStream stream : streams) {
                    if (stream.getFormat() instanceof RGBFormat) {
                        videoStream = stream;
                        break;
                    }
                }

                if (videoStream == null) {
                    System.err.println("HumbleDataSink: No video stream found");
                    return;
                }

                Buffer buffer = new Buffer();

                while (writing && !videoStream.endOfStream()) {
                    try {
                        videoStream.read(buffer);

                        if (buffer.isEOM()) {
                            break;
                        }

                        if (buffer.getData() != null && buffer.getLength() > 0) {
                            writeFrame(buffer, frameNumber);
                            frameNumber++;
                        }

                        // Small delay to prevent CPU spinning
                        Thread.sleep(1);

                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.err.println("HumbleDataSink: Error reading frame - " + e.getMessage());
                    }
                }
            }

            System.out.println("HumbleDataSink: Write loop completed, wrote " + frameNumber + " frames");

            // Signal end of stream
            notifyListeners(new EndOfStreamEvent(this));

        } catch (Exception e) {
            System.err.println("HumbleDataSink: Error in write loop - " + e.getMessage());
            e.printStackTrace();
            notifyListeners(new DataSinkErrorEvent(this, e.getMessage()));
        }
    }

    private void writeFrame(Buffer buffer, long frameNumber) {
        try {
            Object data = buffer.getData();
            if (!(data instanceof byte[])) {
                return;
            }

            byte[] rgbData = (byte[]) data;

            // Convert RGB to BufferedImage
            BufferedImage image = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_3BYTE_BGR);
            byte[] imageData = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();

            // Copy RGB data (assuming BGR order from the codec chain)
            int copyLen = Math.min(rgbData.length, imageData.length);
            System.arraycopy(rgbData, 0, imageData, 0, copyLen);

            // Convert BufferedImage to MediaPicture using converter
            if (converter == null) {
                converter = MediaPictureConverterFactory.createConverter(image, picture);
            }

            converter.toPicture(picture, image, frameNumber);
            picture.setTimeStamp(frameNumber);
            picture.setComplete(true);

            // Encode
            do {
                videoEncoder.encode(packet, picture);
                if (packet.isComplete()) {
                    muxer.write(packet, false);
                }
            } while (packet.isComplete());

        } catch (Exception e) {
            System.err.println("HumbleDataSink: Error writing frame " + frameNumber + " - " + e.getMessage());
        }
    }
}
