# JAVED - Java Video Effects and Design

JAVED (Java Advanced Video Effects Designer) is a comprehensive video effects processing and composition application built with Java Swing. It provides real-time visual effects processing, video composition, mixing, and rendering capabilities.

## Features

- Load and process video files (AVI, MOV, QuickTime formats)
- Apply 100+ real-time visual effects to video frames
- Video composition, mixing, and joining operations
- Interactive GUI for effect parameter configuration
- Effect modulation and randomization
- Save/load effect configurations
- Video rendering and export to QuickTime format

## Requirements

- Java 17 or higher
- Maven 3.x
- Java Media Framework (JMF) - included as local dependency
- Java Advanced Imaging (JAI) - included as local dependency

## Building

```bash
mvn clean install
```

### Platform-Specific Builds

Native application packages can be built using platform-specific profiles:

```bash
# Windows
mvn install -Djpackage

# macOS
mvn install -Djpackage

# Linux
mvn install -Djpackage
```

## Main Entry Points

### Primary Application

**`comul01.eve.EJMain`** - Main application launcher

```bash
java -cp target/instrument-eve-0.0.1-SNAPSHOT.jar comul01.eve.EJMain
```

Creates the main GUI window with:
- Menu bar (File, Help, Options)
- Toolbar with action buttons (Reset, Blank, Effect, Mix, Join)
- Tabbed interface for different views
- Video viewer with frame control
- Effect settings panel

## Architecture

### Core Components

| Component | Class | Description |
|-----------|-------|-------------|
| Main Window | `EJMain` | Application entry point and main window management |
| Video Viewer | `EJView` | Video viewing/playback panel with effect preview |
| Settings Panel | `EJSettings` | UI panel for effect parameter configuration |
| Controls | `EJControls` | Playback and processing controls |
| Video Source | `VideoCutPanel` | Video source selection and frame grabbing |

### Effect Processing Pipeline

| Component | Class | Description |
|-----------|-------|-------------|
| Effect Engine | `EJEffects` | Main effect processing engine with 30+ filter types |
| Effect Control | `EffectControl` | JMF Effect interface implementation |
| Effect Composition | `EffectCompo` | Effect composition with timing information |
| Effect Context | `EffectContext` | Effect configuration state and parameters |

### Frame Handling

| Component | Class | Description |
|-----------|-------|-------------|
| Frame | `EFrame` | Individual frame with pixel data manipulation |
| Frame Set | `EFrameSet` | Circular buffer for frame history |
| Frame Grabber | `EJFrameGrabber` | Interface for frame extraction |

### Parameter System

| Component | Class | Description |
|-----------|-------|-------------|
| Parameters | `EffectParam` | Effect parameter with value and modulation |
| Modulator | `EffectModulator` | Parameter modulation control |
| Randomiser | `EffectRandomiser` | Random variation of effect parameters |

## Supported Effects

### Color Processing
- RGB to HSI conversion
- Grayscale conversion
- Color manipulation and adjustment
- Contrast and brightness control

### Image Filters
- Blur (Gaussian, Box, Motion)
- Sharpen and Emboss
- Edge detection (Roberts, Sobel, Prewitt)
- Dither and posterize

### Morphological Operations
- Dilation and Erosion
- Opening and Closing
- Watershed segmentation

### Generative Effects
- Cellular automata
- Brownian motion
- Fractal noise
- Water ripple/caustics

### Transformations
- Warp and Affine
- Perspective transform
- Scale and Translate
- Rotation

### Time-Based Effects
- Frame delta
- Time correlation
- Temporal mixing

### Analysis
- DFT (Discrete Fourier Transform)
- DCT (Discrete Cosine Transform)
- Hough transform

## File Formats

### Project Files
- `.ejs` - Serialized effect configuration files containing effect list, modulators, randomisers, composition, and transition data

### Supported Video Formats
- AVI
- MOV (QuickTime)
- Other formats supported by JMF

## Project Structure

```
instrument-eve/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── comul01/eve/          # Main application code
│   │   │   ├── com/jhlabs/image/     # Image processing filters
│   │   │   └── javax/media/          # JMF classes
│   │   └── resources/
│   │       └── library/              # JAI libraries
│   └── logo/                         # Platform-specific icons
│       ├── linux/
│       ├── macosx/
│       └── windows/
├── jmf.jar                           # Java Media Framework
├── pom.xml                           # Maven configuration
└── README.md
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| JMF (Java Media Framework) | 2.1.1e | Video playback, capture, and processing |
| JAI (Java Advanced Imaging) | 1.1.3 | Advanced image processing |
| Humble Video | 0.3.0 | Video codec support |
| jhlabs Image Filters | - | Bundled image filter library |

## Usage Examples

### Basic Workflow

1. Launch the application
2. Use **File > Open** to load a video file
3. Select effects from the settings panel
4. Adjust effect parameters using sliders
5. Preview effects in real-time in the viewer
6. Use **Mix** or **Join** to combine video sources
7. Export the result using the rendering options
8. Save your effect configuration with **File > Save** (.ejs format)

### Effect Configuration

Effects can be configured with:
- **Parameters**: Individual effect values (0-255 range)
- **Modulation**: Low/mid/high value transitions over time
- **Randomization**: Random variation for dynamic effects

## License

Part of the instrument project suite.
