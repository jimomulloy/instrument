/*
 * R210Codec.java
 *
 * Codec to convert R210 (10-bit RGB 4:4:4) video format to standard 24-bit RGB.
 * R210 stores 10 bits per channel (30 bits total) packed into 32-bit words.
 *
 * R210 pixel layout (32 bits, big-endian):
 *   Bits 31-30: unused (2 bits)
 *   Bits 29-20: Red (10 bits)
 *   Bits 19-10: Green (10 bits)
 *   Bits  9-0:  Blue (10 bits)
 */
package comul01.eve;

import javax.media.*;
import javax.media.format.*;
import java.awt.Dimension;

public class R210Codec implements Codec {

    private static final String CODEC_NAME = "R210 to RGB24 Decoder";

    // R210 format identifier - this is what JMF reports for R210 encoded video
    private static final String R210_ENCODING = "R210";

    private Format inputFormat;
    private Format outputFormat;

    // Supported input formats - R210 video
    private Format[] supportedInputFormats;

    // Supported output formats - RGB24
    private Format[] supportedOutputFormats;

    public R210Codec() {
        // Input: R210 format (reported as VideoFormat with encoding "R210")
        supportedInputFormats = new Format[] {
            new VideoFormat(R210_ENCODING),
            new VideoFormat("r210"),  // lowercase variant
            // Also handle as generic VideoFormat if encoding matches
        };

        // Output: Standard 24-bit RGB format
        supportedOutputFormats = new Format[] {
            new RGBFormat(
                null,                    // size (any)
                Format.NOT_SPECIFIED,    // maxDataLength
                Format.byteArray,        // data type
                Format.NOT_SPECIFIED,    // frame rate
                24,                      // bits per pixel
                3, 2, 1,                 // RGB masks (BGR order for JMF)
                3,                       // pixel stride
                Format.NOT_SPECIFIED,    // line stride
                Format.FALSE,            // flipped
                Format.NOT_SPECIFIED     // endian
            )
        };
    }

    @Override
    public String getName() {
        return CODEC_NAME;
    }

    @Override
    public Format[] getSupportedInputFormats() {
        return supportedInputFormats;
    }

    @Override
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return supportedOutputFormats;
        }

        // Check if input is R210 format
        if (input instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) input;
            String encoding = vf.getEncoding();

            if (encoding != null &&
                (encoding.equalsIgnoreCase(R210_ENCODING) || encoding.equalsIgnoreCase("r210"))) {

                Dimension size = vf.getSize();
                float frameRate = vf.getFrameRate();

                if (size != null) {
                    int width = size.width;
                    int height = size.height;
                    int maxDataLength = width * height * 3;
                    int lineStride = width * 3;

                    return new Format[] {
                        new RGBFormat(
                            size,
                            maxDataLength,
                            Format.byteArray,
                            frameRate,
                            24,
                            3, 2, 1,     // BGR order
                            3,           // pixel stride
                            lineStride,
                            Format.FALSE,
                            Format.NOT_SPECIFIED
                        )
                    };
                }
            }
        }

        return new Format[0];
    }

    @Override
    public Format setInputFormat(Format format) {
        if (format instanceof VideoFormat) {
            VideoFormat vf = (VideoFormat) format;
            String encoding = vf.getEncoding();

            if (encoding != null &&
                (encoding.equalsIgnoreCase(R210_ENCODING) || encoding.equalsIgnoreCase("r210"))) {
                this.inputFormat = format;
                System.out.println("R210Codec: Input format set to " + format);
                return format;
            }
        }
        return null;
    }

    @Override
    public Format setOutputFormat(Format format) {
        if (format instanceof RGBFormat) {
            this.outputFormat = format;
            System.out.println("R210Codec: Output format set to " + format);
            return format;
        }
        return null;
    }

    @Override
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (inputBuffer.isEOM()) {
            outputBuffer.setEOM(true);
            outputBuffer.setLength(0);
            return BUFFER_PROCESSED_OK;
        }

        if (inputBuffer.isDiscard()) {
            outputBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        Format inFormat = inputBuffer.getFormat();
        if (inFormat == null) {
            inFormat = inputFormat;
        }

        if (!(inFormat instanceof VideoFormat)) {
            return BUFFER_PROCESSED_FAILED;
        }

        VideoFormat videoFormat = (VideoFormat) inFormat;
        Dimension size = videoFormat.getSize();

        if (size == null) {
            System.err.println("R210Codec: No size information in input format");
            return BUFFER_PROCESSED_FAILED;
        }

        int width = size.width;
        int height = size.height;

        Object inputData = inputBuffer.getData();

        // R210 data can come as byte array or int array
        byte[] r210Data;
        if (inputData instanceof byte[]) {
            r210Data = (byte[]) inputData;
        } else if (inputData instanceof int[]) {
            // Convert int array to byte array
            int[] intData = (int[]) inputData;
            r210Data = new byte[intData.length * 4];
            for (int i = 0; i < intData.length; i++) {
                r210Data[i * 4] = (byte) ((intData[i] >> 24) & 0xFF);
                r210Data[i * 4 + 1] = (byte) ((intData[i] >> 16) & 0xFF);
                r210Data[i * 4 + 2] = (byte) ((intData[i] >> 8) & 0xFF);
                r210Data[i * 4 + 3] = (byte) (intData[i] & 0xFF);
            }
        } else {
            System.err.println("R210Codec: Unsupported input data type: " +
                (inputData != null ? inputData.getClass().getName() : "null"));
            return BUFFER_PROCESSED_FAILED;
        }

        int inputOffset = inputBuffer.getOffset();

        // Output buffer: 3 bytes per pixel (RGB24)
        int outputSize = width * height * 3;
        byte[] rgb24Data = validateByteArraySize(outputBuffer, outputSize);

        // Convert R210 to RGB24
        // R210: 4 bytes per pixel (32 bits with 10 bits per channel)
        // RGB24: 3 bytes per pixel (8 bits per channel)

        int pixelCount = width * height;
        int r210Offset = inputOffset;
        int rgb24Offset = 0;

        for (int i = 0; i < pixelCount && r210Offset + 3 < r210Data.length; i++) {
            // Read 32-bit R210 pixel (big-endian)
            int pixel = ((r210Data[r210Offset] & 0xFF) << 24) |
                       ((r210Data[r210Offset + 1] & 0xFF) << 16) |
                       ((r210Data[r210Offset + 2] & 0xFF) << 8) |
                       (r210Data[r210Offset + 3] & 0xFF);

            // Extract 10-bit components
            // R210 layout: XX RRRRRRRRRR GGGGGGGGGG BBBBBBBBBB (2 unused + 10R + 10G + 10B)
            int red10 = (pixel >> 20) & 0x3FF;   // bits 29-20
            int green10 = (pixel >> 10) & 0x3FF; // bits 19-10
            int blue10 = pixel & 0x3FF;          // bits 9-0

            // Convert 10-bit to 8-bit (take upper 8 bits)
            int red8 = red10 >> 2;
            int green8 = green10 >> 2;
            int blue8 = blue10 >> 2;

            // Write RGB24 (BGR order for JMF)
            rgb24Data[rgb24Offset] = (byte) blue8;
            rgb24Data[rgb24Offset + 1] = (byte) green8;
            rgb24Data[rgb24Offset + 2] = (byte) red8;

            r210Offset += 4;
            rgb24Offset += 3;
        }

        // Set output buffer properties
        outputBuffer.setData(rgb24Data);
        outputBuffer.setOffset(0);
        outputBuffer.setLength(outputSize);
        outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
        outputBuffer.setFlags(inputBuffer.getFlags());

        // Set output format
        if (outputFormat == null) {
            outputFormat = new RGBFormat(
                size,
                outputSize,
                Format.byteArray,
                videoFormat.getFrameRate(),
                24,
                3, 2, 1,           // BGR masks
                3,                 // pixel stride
                width * 3,         // line stride
                Format.FALSE,
                Format.NOT_SPECIFIED
            );
        }
        outputBuffer.setFormat(outputFormat);

        return BUFFER_PROCESSED_OK;
    }

    private byte[] validateByteArraySize(Buffer buffer, int newSize) {
        Object data = buffer.getData();
        byte[] typedArray;

        if (data instanceof byte[]) {
            typedArray = (byte[]) data;
            if (typedArray.length >= newSize) {
                return typedArray;
            }
        }

        typedArray = new byte[newSize];
        buffer.setData(typedArray);
        return typedArray;
    }

    @Override
    public void open() throws ResourceUnavailableException {
        System.out.println("R210Codec: Codec opened");
    }

    @Override
    public void close() {
        System.out.println("R210Codec: Codec closed");
    }

    @Override
    public void reset() {
        // Nothing to reset
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Object getControl(String controlType) {
        return null;
    }

    /**
     * Register this codec with JMF's PlugInManager.
     * Call this method early in application startup.
     */
    public static void register() {
        Format[] inputFormats = new Format[] {
            new VideoFormat("R210"),
            new VideoFormat("r210")
        };

        Format[] outputFormats = new Format[] {
            new RGBFormat(
                null,
                Format.NOT_SPECIFIED,
                Format.byteArray,
                Format.NOT_SPECIFIED,
                24,
                3, 2, 1,
                3,
                Format.NOT_SPECIFIED,
                Format.FALSE,
                Format.NOT_SPECIFIED
            )
        };

        boolean registered = PlugInManager.addPlugIn(
            "comul01.eve.R210Codec",
            inputFormats,
            outputFormats,
            PlugInManager.CODEC
        );

        if (registered) {
            System.out.println("R210Codec: Successfully registered with PlugInManager");
            try {
                PlugInManager.commit();
            } catch (Exception e) {
                System.err.println("R210Codec: Failed to commit PlugInManager changes: " + e.getMessage());
            }
        } else {
            System.err.println("R210Codec: Failed to register with PlugInManager");
        }
    }
}
