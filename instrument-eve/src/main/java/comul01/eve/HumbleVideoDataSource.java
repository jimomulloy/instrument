/*
 * HumbleVideoDataSource.java
 *
 * A JMF-compatible DataSource that uses humble-video (FFmpeg) to read video files.
 * This enables support for formats not natively supported by FMJ, including R210.
 */
package comul01.eve;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import javax.media.*;
import javax.media.Time;
import javax.media.protocol.*;
import javax.media.format.RGBFormat;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DataSource implementation using humble-video (FFmpeg) for reading video files.
 * Supports any format that FFmpeg supports, including R210.
 */
public class HumbleVideoDataSource extends PushBufferDataSource {

    private String filePath;
    private MediaLocator locator;
    private boolean connected = false;
    private HumbleVideoPushBufferStream[] streams;

    private Demuxer demuxer;
    private Decoder videoDecoder;
    private Decoder audioDecoder;
    private int videoStreamIndex = -1;
    private int audioStreamIndex = -1;

    private int videoWidth;
    private int videoHeight;
    private double frameRate;
    private long durationNanos;

    public HumbleVideoDataSource(String filePath) {
        this.filePath = filePath;
        this.locator = new MediaLocator("file:" + filePath);
    }

    public HumbleVideoDataSource(MediaLocator locator) {
        this.locator = locator;
        // Convert MediaLocator to file path
        String url = locator.toExternalForm();
        if (url.startsWith("file:")) {
            this.filePath = url.substring(5);
            // Handle Windows paths like file:/C:/...
            if (filePath.startsWith("/") && filePath.length() > 2 && filePath.charAt(2) == ':') {
                this.filePath = filePath.substring(1);
            }
        } else {
            this.filePath = url;
        }
    }

    @Override
    public void connect() throws IOException {
        if (connected) return;

        System.out.println("HumbleVideoDataSource: Connecting to " + filePath);

        try {
            demuxer = Demuxer.make();
            demuxer.open(filePath, null, false, true, null, null);

            int numStreams = demuxer.getNumStreams();
            List<HumbleVideoPushBufferStream> streamList = new ArrayList<>();

            for (int i = 0; i < numStreams; i++) {
                DemuxerStream stream = demuxer.getStream(i);
                Decoder decoder = stream.getDecoder();

                if (decoder != null) {
                    MediaDescriptor.Type type = decoder.getCodecType();

                    if (type == MediaDescriptor.Type.MEDIA_VIDEO && videoStreamIndex < 0) {
                        videoStreamIndex = i;
                        videoDecoder = decoder;
                        videoDecoder.open(null, null);

                        videoWidth = videoDecoder.getWidth();
                        videoHeight = videoDecoder.getHeight();

                        // Get frame rate - try stream time base first
                        Rational tb = stream.getTimeBase();
                        if (tb != null && tb.getNumerator() > 0 && tb.getDenominator() > 0) {
                            // Time base is usually 1/fps or similar
                            double tbVal = tb.getDouble();
                            if (tbVal > 0 && tbVal < 1) {
                                frameRate = 1.0 / tbVal;
                            } else {
                                frameRate = 24.0; // default
                            }
                        } else {
                            frameRate = 24.0; // default
                        }
                        // Clamp to reasonable range
                        if (frameRate < 1 || frameRate > 120) {
                            frameRate = 24.0;
                        }

                        long duration = demuxer.getDuration();
                        if (duration != Global.NO_PTS) {
                            durationNanos = duration * 1000; // microseconds to nanoseconds
                        } else {
                            durationNanos = 0;
                        }

                        System.out.println("HumbleVideoDataSource: Video stream found - " +
                            videoWidth + "x" + videoHeight + " @ " + frameRate + " fps");
                        System.out.println("HumbleVideoDataSource: Codec: " + videoDecoder.getCodec().getName());

                        // Create video stream
                        RGBFormat videoFormat = new RGBFormat(
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

                        streamList.add(new HumbleVideoPushBufferStream(this, videoFormat, true));
                    }
                    else if (type == MediaDescriptor.Type.MEDIA_AUDIO && audioStreamIndex < 0) {
                        audioStreamIndex = i;
                        audioDecoder = decoder;
                        audioDecoder.open(null, null);

                        int sampleRate = audioDecoder.getSampleRate();
                        int channels = audioDecoder.getChannels();

                        System.out.println("HumbleVideoDataSource: Audio stream found - " +
                            sampleRate + " Hz, " + channels + " channels");

                        // Create audio stream using fully qualified JMF AudioFormat
                        javax.media.format.AudioFormat audioFormat = new javax.media.format.AudioFormat(
                            javax.media.format.AudioFormat.LINEAR,
                            sampleRate,
                            16,
                            channels,
                            javax.media.format.AudioFormat.LITTLE_ENDIAN,
                            javax.media.format.AudioFormat.SIGNED
                        );

                        streamList.add(new HumbleVideoPushBufferStream(this, audioFormat, false));
                    }
                }
            }

            if (streamList.isEmpty()) {
                throw new IOException("No supported streams found in " + filePath);
            }

            streams = streamList.toArray(new HumbleVideoPushBufferStream[0]);
            connected = true;

            System.out.println("HumbleVideoDataSource: Connected successfully with " + streams.length + " streams");

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while opening file", e);
        } catch (Exception e) {
            throw new IOException("Failed to open file: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        if (!connected) return;

        try {
            if (videoDecoder != null) {
                videoDecoder.delete();
                videoDecoder = null;
            }
            if (audioDecoder != null) {
                audioDecoder.delete();
                audioDecoder = null;
            }
            if (demuxer != null) {
                demuxer.close();
                demuxer = null;
            }
        } catch (Exception e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }

        connected = false;
        streams = null;
    }

    @Override
    public void start() throws IOException {
        if (!connected) {
            throw new IOException("Not connected");
        }
        for (HumbleVideoPushBufferStream stream : streams) {
            stream.start();
        }
    }

    @Override
    public void stop() throws IOException {
        if (streams != null) {
            for (HumbleVideoPushBufferStream stream : streams) {
                stream.stop();
            }
        }
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public MediaLocator getLocator() {
        return locator;
    }

    @Override
    public void setLocator(MediaLocator locator) {
        this.locator = locator;
    }

    @Override
    public Time getDuration() {
        return new Time(durationNanos);
    }

    @Override
    public Object getControl(String controlType) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    // Package-private methods for stream access
    Demuxer getDemuxer() {
        return demuxer;
    }

    Decoder getVideoDecoder() {
        return videoDecoder;
    }

    Decoder getAudioDecoder() {
        return audioDecoder;
    }

    int getVideoStreamIndex() {
        return videoStreamIndex;
    }

    int getAudioStreamIndex() {
        return audioStreamIndex;
    }

    int getVideoWidth() {
        return videoWidth;
    }

    int getVideoHeight() {
        return videoHeight;
    }

    double getFrameRate() {
        return frameRate;
    }

    /**
     * Inner class implementing PushBufferStream for video/audio data
     */
    static class HumbleVideoPushBufferStream implements PushBufferStream {

        private HumbleVideoDataSource dataSource;
        private Format format;
        private boolean isVideo;
        private BufferTransferHandler transferHandler;
        private boolean started = false;
        private boolean endOfStream = false;
        private Thread readThread;

        private MediaPicture picture;
        private MediaPictureConverter converter;
        private MediaAudio audio;
        private MediaPacket packet;
        private long frameNumber = 0;

        HumbleVideoPushBufferStream(HumbleVideoDataSource dataSource, Format format, boolean isVideo) {
            this.dataSource = dataSource;
            this.format = format;
            this.isVideo = isVideo;
        }

        void start() {
            if (started) return;
            started = true;
            endOfStream = false;
            frameNumber = 0;

            // Initialize decode buffers
            if (isVideo) {
                Decoder dec = dataSource.getVideoDecoder();
                picture = MediaPicture.make(
                    dec.getWidth(),
                    dec.getHeight(),
                    dec.getPixelFormat()
                );
            } else {
                Decoder dec = dataSource.getAudioDecoder();
                audio = MediaAudio.make(
                    dec.getFrameSize(),
                    dec.getSampleRate(),
                    dec.getChannels(),
                    dec.getChannelLayout(),
                    dec.getSampleFormat()
                );
            }
            packet = MediaPacket.make();

            // Start read thread
            readThread = new Thread(this::readLoop, "HumbleVideo-" + (isVideo ? "Video" : "Audio"));
            readThread.start();
        }

        void stop() {
            started = false;
            if (readThread != null) {
                readThread.interrupt();
                try {
                    readThread.join(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                readThread = null;
            }
        }

        private void readLoop() {
            Demuxer demuxer = dataSource.getDemuxer();
            int targetStream = isVideo ? dataSource.getVideoStreamIndex() : dataSource.getAudioStreamIndex();
            Decoder decoder = isVideo ? dataSource.getVideoDecoder() : dataSource.getAudioDecoder();

            try {
                while (started && demuxer.read(packet) >= 0) {
                    if (packet.getStreamIndex() == targetStream) {
                        int offset = 0;
                        int bytesRead = 0;

                        do {
                            if (isVideo) {
                                bytesRead += decoder.decode(picture, packet, offset);
                                if (picture.isComplete()) {
                                    deliverVideoFrame(picture);
                                }
                            } else {
                                bytesRead += decoder.decode(audio, packet, offset);
                                if (audio.isComplete()) {
                                    deliverAudioFrame(audio);
                                }
                            }
                            offset += bytesRead;
                        } while (offset < packet.getSize());
                    }
                }

                // Flush decoder
                do {
                    if (isVideo) {
                        decoder.decode(picture, null, 0);
                        if (picture.isComplete()) {
                            deliverVideoFrame(picture);
                        }
                    } else {
                        decoder.decode(audio, null, 0);
                        if (audio.isComplete()) {
                            deliverAudioFrame(audio);
                        }
                    }
                } while (isVideo ? picture.isComplete() : audio.isComplete());

                endOfStream = true;

                // Deliver EOM
                if (transferHandler != null) {
                    transferHandler.transferData(this);
                }

            } catch (InterruptedException e) {
                // Thread stopped
            } catch (Exception e) {
                System.err.println("Error in read loop: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void deliverVideoFrame(MediaPicture picture) {
            if (transferHandler == null) return;

            try {
                // Initialize converter if needed
                if (converter == null) {
                    converter = MediaPictureConverterFactory.createConverter(
                        MediaPictureConverterFactory.HUMBLE_BGR_24,
                        picture
                    );
                }

                // Notify transfer handler that data is available
                transferHandler.transferData(this);
                frameNumber++;

            } catch (Exception e) {
                System.err.println("Error delivering video frame: " + e.getMessage());
            }
        }

        private void deliverAudioFrame(MediaAudio audio) {
            if (transferHandler == null) return;

            try {
                transferHandler.transferData(this);
            } catch (Exception e) {
                System.err.println("Error delivering audio frame: " + e.getMessage());
            }
        }

        @Override
        public void read(Buffer buffer) throws IOException {
            if (endOfStream) {
                buffer.setEOM(true);
                buffer.setLength(0);
                return;
            }

            if (isVideo) {
                readVideoBuffer(buffer);
            } else {
                readAudioBuffer(buffer);
            }
        }

        private void readVideoBuffer(Buffer buffer) throws IOException {
            Decoder decoder = dataSource.getVideoDecoder();
            int width = decoder.getWidth();
            int height = decoder.getHeight();

            // Get or create converter
            if (converter == null && picture != null) {
                converter = MediaPictureConverterFactory.createConverter(
                    MediaPictureConverterFactory.HUMBLE_BGR_24,
                    picture
                );
            }

            if (picture != null && picture.isComplete() && converter != null) {
                BufferedImage image = converter.toImage(null, picture);

                // Convert BufferedImage to byte array (BGR format)
                int[] pixels = new int[width * height];
                image.getRGB(0, 0, width, height, pixels, 0, width);

                byte[] data = new byte[width * height * 3];
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    data[i * 3] = (byte) (pixel & 0xFF);         // Blue
                    data[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);  // Green
                    data[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF); // Red
                }

                buffer.setData(data);
                buffer.setOffset(0);
                buffer.setLength(data.length);
                buffer.setFormat(format);
                buffer.setTimeStamp(picture.getTimeStamp() * 1000); // to nanos
                buffer.setSequenceNumber(frameNumber);
                buffer.setFlags(0);
            }
        }

        private void readAudioBuffer(Buffer buffer) throws IOException {
            if (audio != null && audio.isComplete()) {
                int size = audio.getDataPlaneSize(0);
                byte[] data = new byte[size];
                audio.getData(0).get(0, data, 0, size);

                buffer.setData(data);
                buffer.setOffset(0);
                buffer.setLength(size);
                buffer.setFormat(format);
                buffer.setTimeStamp(audio.getTimeStamp() * 1000);
                buffer.setFlags(0);
            }
        }

        @Override
        public Format getFormat() {
            return format;
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
