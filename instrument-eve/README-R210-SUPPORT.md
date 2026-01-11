# R210 Video Format Support Implementation

This document describes the changes made to add support for R210 (10-bit RGB 4:4:4) video format in the instrument-eve application.

## Overview

The original application used JMF (Java Media Framework) which is incompatible with Java 9+ and doesn't support the R210 codec. The solution involved:

1. Migrating from JMF to FMJ (Freedom for Media in Java)
2. Integrating humble-video (FFmpeg Java bindings) for R210 decoding
3. Creating custom Processor and DataSink implementations to bypass FMJ limitations

## New Files Created

### R210Codec.java
A JMF-compatible codec that decodes R210 10-bit RGB to standard RGB24.

- **Location**: `src/main/java/comul01/eve/R210Codec.java`
- **Purpose**: Decode R210 format (32 bits per pixel, 10 bits per channel) to 24-bit RGB
- **Key Features**:
  - Implements `javax.media.Codec` interface
  - Static `register()` method for PlugInManager registration
  - Converts 10-bit color values to 8-bit by shifting

### HumbleVideoDataSource.java
A PushBufferDataSource implementation using humble-video for reading video files.

- **Location**: `src/main/java/comul01/eve/HumbleVideoDataSource.java`
- **Purpose**: Read video files using FFmpeg via humble-video
- **Key Features**:
  - Extends `PushBufferDataSource`
  - Supports any video format that FFmpeg supports
  - Contains inner class `HumbleVideoPushBufferStream` for stream delivery

### HumbleVideoPlayer.java
A Player implementation for playback of formats not supported by FMJ.

- **Location**: `src/main/java/comul01/eve/HumbleVideoPlayer.java`
- **Purpose**: Video playback using humble-video
- **Key Features**:
  - Implements `Player`, `FrameGrabbingControl`, `FramePositioningControl`
  - Full state machine implementation (Unrealized -> Realized -> Prefetched -> Started)
  - Frame-accurate seeking support

### HumbleVideoProcessor.java
A full Processor implementation using humble-video for video processing.

- **Location**: `src/main/java/comul01/eve/HumbleVideoProcessor.java`
- **Purpose**: Process video streams for effects and export
- **Key Features**:
  - Implements `Processor` interface
  - Contains `HumbleTrackControl` for codec chain support (enables `EffectControl` integration)
  - Contains `HumbleProcessorDataSource` and `HumbleProcessorStream` for output
  - Actively decodes frames when output stream is read
  - **Effects Support**: Processes frames through the codec chain during export, ensuring `EffectControl.process()` is called for each frame

### HumbleDataSink.java
A DataSink implementation using humble-video for writing video files.

- **Location**: `src/main/java/comul01/eve/HumbleDataSink.java`
- **Purpose**: Write processed video to output files
- **Key Features**:
  - Implements `DataSink` interface
  - Uses MJPEG codec for AVI output
  - Creates parent directories automatically
  - Handles RGB to YUV conversion for encoding

## Modified Files

### pom.xml
Changed from JMF to FMJ dependency:

```xml
<!-- Removed -->
<dependency>
    <groupId>javax.media</groupId>
    <artifactId>jmf</artifactId>
    <version>2.1.1e</version>
</dependency>

<!-- Added -->
<dependency>
    <groupId>org.jitsi</groupId>
    <artifactId>fmj</artifactId>
    <version>1.0.2-jitsi</version>
</dependency>
```

Added JAI (Java Advanced Imaging) codec dependency for effects like Sobel edge detection:

```xml
<dependency>
    <groupId>javax.media.jai</groupId>
    <artifactId>jai-core</artifactId>
    <version>1.1.3</version>
    <scope>system</scope>
    <systemPath>${pom.basedir}/src/main/resources/library/jai_core.jar</systemPath>
</dependency>
<dependency>
    <groupId>com.sun.media</groupId>
    <artifactId>jai-codec</artifactId>
    <version>1.1.3</version>
    <scope>system</scope>
    <systemPath>${pom.basedir}/src/main/resources/library/jai_codec.jar</systemPath>
</dependency>
```

Added JVM arguments for Java 17+ compatibility with JAI:

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--add-opens java.desktop/sun.awt.image=ALL-UNNAMED</argLine>
    </configuration>
</plugin>
```

The humble-video dependencies were already present in the project.

### EJMain.java
Added R210Codec registration in static initializer:

```java
static {
    // Register R210 codec for 10-bit RGB video support
    R210Codec.register();
}
```

### EJView.java
Added fallback mechanisms for processor and data sink creation:

```java
// Processor fallback
try {
    p = Manager.createProcessor(new MediaLocator(mediaFile1));
} catch (NoProcessorException e) {
    System.out.println("FMJ cannot handle format, using HumbleVideoProcessor");
    HumbleVideoProcessor hvp = new HumbleVideoProcessor(new MediaLocator(mediaFile1));
    p = hvp;
}

// DataSink fallback
try {
    filewriter = Manager.createDataSink(ds, new MediaLocator(outputFile));
} catch (NoDataSinkException e) {
    System.out.println("FMJ cannot create DataSink, using HumbleDataSink");
    filewriter = new HumbleDataSink(ds, new MediaLocator(outputFile));
}
```

### VideoCutPanel.java
Added HumbleVideoPlayer fallback for playback when FMJ fails.

### SuperGlueDataSource.java
Added handling for non-RGB VideoFormat types like R210.

### EFrame.java
Added fallback image conversion for JAI effects compatibility with Humble video:

- **`createImageFromBytes()`**: New private method that creates a BufferedImage directly from byte data, bypassing FMJ's `BufferToImage` which may return null for certain formats
- **Updated `getRenderedOp()`**: Falls back to `createImageFromBytes()` when `getImage()` returns null
- **Updated `getRenderedImage()`**: Same fallback mechanism
- **Updated `getPlanarImage()`**: Same fallback mechanism
- **Vertical flip handling**: The `createImageFromBytes()` method reads pixels bottom-up to correct for video data stored in bottom-up order

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                         │
│                    (EJView, VideoCutPanel)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FMJ Manager (Primary)                       │
│         Manager.createProcessor() / Manager.createDataSink()    │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │ NoProcessorException │
                    │ NoDataSinkException  │
                    └─────────┬─────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Humble-Video Layer (Fallback)                   │
│                                                                  │
│  ┌───────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │ HumbleVideoPlayer │  │HumbleVideoProc. │  │ HumbleDataSink│  │
│  │   (Playback)      │  │  (Processing)   │  │   (Export)    │  │
│  └───────────────────┘  └─────────────────┘  └───────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              HumbleVideoDataSource (Input)                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    humble-video / FFmpeg                         │
│              (Native video decoding/encoding)                    │
└─────────────────────────────────────────────────────────────────┘
```

## How It Works

1. **Loading Video**: When a video file is loaded, the application first tries FMJ's `Manager.createProcessor()`. If this fails (e.g., for R210 format), it falls back to `HumbleVideoProcessor`.

2. **Processing**: `HumbleVideoProcessor` uses humble-video to decode frames from the source file. It provides a `HumbleProcessorDataSource` that outputs decoded RGB frames.

3. **Effects Application**: When a codec chain is set via `TrackControl.setCodecChain()` (e.g., with `EffectControl`), the `HumbleProcessorStream.read()` method processes each frame through the codec chain before returning it. This ensures effects are applied to every frame during export.

4. **Export**: When exporting, the application first tries FMJ's `Manager.createDataSink()`. If this fails, it uses `HumbleDataSink` which encodes frames using MJPEG and writes to an AVI file.

5. **Playback**: For playback, `HumbleVideoPlayer` provides direct video playback with frame-accurate seeking support.

## Effects Pipeline

The effects system works as follows:

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ HumbleVideo     │     │ HumbleProcessor  │     │  HumbleData     │
│ Processor       │────▶│ Stream.read()    │────▶│  Sink           │
│ (decode frame)  │     │ (apply effects)  │     │  (encode frame) │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌──────────────────┐
                        │  Codec Chain     │
                        │  (EffectControl) │
                        │  .process()      │
                        └──────────────────┘
```

1. `HumbleVideoProcessor` decodes a frame from the source video using FFmpeg
2. `HumbleProcessorStream.read()` converts the frame to a JMF `Buffer`
3. If a codec chain is configured, the buffer is passed through each codec's `process()` method
4. `EffectControl.process()` applies visual effects to the frame data
5. The processed buffer is returned to `HumbleDataSink` for encoding and writing

## Dependencies

- **FMJ 1.0.2-jitsi**: JMF-compatible media framework
- **humble-video**: FFmpeg Java bindings (already in project)
  - humble-video-all
  - humble-video-noarch
- **JAI (Java Advanced Imaging)**: Required for image processing effects
  - jai_core.jar
  - jai_codec.jar (required for effects like Sobel edge detection)

## Supported Formats

With this implementation, the application now supports:

- **R210**: 10-bit RGB 4:4:4 (the primary format this was built for)
- **Any format FFmpeg supports**: Through humble-video fallback
- **Standard formats**: Through FMJ (MJPEG, etc.)

## Output Format

Processed videos are exported as:
- **Container**: AVI
- **Video Codec**: MJPEG
- **Pixel Format**: YUVJ420P (full range)

## Running the Application

### VS Code
A launch configuration is provided in `.vscode/launch.json` with the required JVM arguments for JAI compatibility:

```json
{
    "type": "java",
    "name": "EJMain (instrument-eve)",
    "request": "launch",
    "mainClass": "comul01.eve.EJMain",
    "projectName": "instrument-eve",
    "vmArgs": "--add-opens java.desktop/sun.awt.image=ALL-UNNAMED"
}
```

### Command Line
When running directly, include the JVM argument:

```bash
java --add-opens java.desktop/sun.awt.image=ALL-UNNAMED -jar instrument-eve.jar
```

### IntelliJ IDEA / Eclipse
Add to VM options: `--add-opens java.desktop/sun.awt.image=ALL-UNNAMED`

## Troubleshooting

### Empty Output File
If the output file is empty or very small, check that:
- The output directory exists (it's created automatically now)
- The input file is readable
- No exceptions in the console output

### "No processor found" Error
This indicates FMJ couldn't handle the format. The fallback to `HumbleVideoProcessor` should activate automatically.

### Frame Rate Issues
Frame rate detection uses the stream time base. If incorrect, it defaults to 24 fps.

### JAI IllegalAccessError
If you see an error like:
```
java.lang.IllegalAccessError: class javax.media.jai.RasterAccessor cannot access class sun.awt.image.BytePackedRaster
```
This means the JVM argument `--add-opens java.desktop/sun.awt.image=ALL-UNNAMED` is missing. Add it to your run configuration.

### JAI NoClassDefFoundError for SeekableStream
If you see:
```
java.lang.NoClassDefFoundError: com.sun.media.jai.codec.SeekableStream
```
This means `jai_codec.jar` is missing. Ensure both `jai_core.jar` and `jai_codec.jar` are in `src/main/resources/library/` and declared in pom.xml.

### Upside-down Video Frames
If video frames appear upside down after effects processing, this is typically due to video data being stored in bottom-up order. The `EFrame.createImageFromBytes()` method handles this by flipping the image during conversion.

### "null is supplied" Error for JAI Operations
If you see errors like:
```
operation "GradientMagnitude" requires all source objects to be valid input; a null is supplied
```
This indicates FMJ's `BufferToImage` failed to convert the frame. The fallback `createImageFromBytes()` method in `EFrame.java` should handle this automatically.
