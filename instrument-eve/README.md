# Instrument-Eve (JAVED)

Java Audio/Video Effects & Digital Processing Module

## Overview

Instrument-Eve is a specialized audio and video effect processing engine that is part of the larger [Instrument](https://github.com/jimomulloy/instrument) project. It provides advanced image filtering, effect composition, and multimedia processing capabilities for creative music and film applications.

## Main Entry Points

### 1. EJMain - Desktop Application

**Location:** `src/main/java/comul01/eve/EJMain.java`

The primary entry point for the ToneMap desktop application.

**Usage:**
```bash
# Run from compiled classes
java -cp target/classes:target/lib/* comul01.eve.EJMain

# Or with Maven
mvn javafx:run
```

**Features:**
- GUI-based video/audio effect processing
- File menu for opening and saving media files
- Toolbar with Reset, Blank, Effect, Mix, and Join operations
- Settings panel for effect parameter configuration
- Real-time status reporting

### 2. OrchestratorService - Service Orchestration

**Location:** `src/main/java/OrchestratorService.java`

Orchestrates multi-service data aggregation for business workflows.

**Key Method:**
```java
public WorklistInformation buildWorklistInformation(WorklistItemResponse worklistItem)
```

**Supported Service Types:**
- Citizen Account Info
- Property Search (FAP, FPI, INSPIRE)
- Property Alerts
- Map Search
- Application Enquiry
- Digital Registration Service (DRS)
- Business Gateway
- View Colleagues Applications

### 3. TransactionService - Transaction Handling

**Location:** `src/main/java/TransactionService.java`

Handles transaction and application data retrieval.

**REST Endpoints:**
| Endpoint | Description |
|----------|-------------|
| `/transactions/map-search` | Map-based property search |
| `/transactions/property-alert` | Property alert queries |
| `/transactions/application-enquiry` | Application enquiries |
| `/transactions/property-search` | Property search |
| `/transactions/digital-registration-service` | DRS transactions |
| `/transactions/view-colleagues-applications` | VCA queries |
| `/transactions/application-channel/{ref}` | Application channel lookup |

### 4. RegistrationService - Registry Operations

**Location:** `src/main/java/RegistrationService.java`

Manages property register and title information via Digital Register API.

**Key Methods:**
```java
// Async retrieval
CompletableFuture<Map<String, Register>> getAsyncRegisters(List<String> titleNumbers)

// Sync retrieval
Map<String, Register> getRegisters(List<String> titleNumbers)
```

## Project Structure

```
instrument-eve/
├── src/main/java/
│   ├── comul01/eve/           # Core application package
│   │   ├── EJMain.java        # Main entry point
│   │   ├── EJView.java        # Media view/playback panel
│   │   ├── EJSettings.java    # Effect settings UI
│   │   ├── EJData.java        # Serializable data container
│   │   ├── EJConstants.java   # Configuration constants
│   │   ├── EJFrameGrabber.java # Video frame extraction
│   │   ├── effect/            # Effect system components
│   │   │   ├── EffectContext.java
│   │   │   ├── EffectParam.java
│   │   │   ├── EffectModulator.java
│   │   │   ├── EffectRandomiser.java
│   │   │   └── EffectCompo.java
│   │   └── ...
│   ├── com/jhlabs/image/      # Image filter library (46 filters)
│   ├── OrchestratorService.java
│   ├── TransactionService.java
│   └── RegistrationService.java
├── src/main/resources/
│   └── logo/                  # Application icons
├── lib/                       # External JARs (JMF, JAI)
└── pom.xml
```

## Available Effects

The application provides 50+ effects including:

| Category | Effects |
|----------|---------|
| **Image Processing** | Blur, Contrast, Sharpen, Invert, Edge Detection |
| **Morphological** | Dilation, Erosion, Opening, Closing |
| **Frequency Domain** | DFT, IDFT, DCT, IDCT |
| **Special Effects** | Waterripple, Warp, Pointillism, Zebra |
| **Time-based** | TimeConvolve, TimeFlow, TimeCorelate |
| **Composite** | Compose, Combine, Mix |

## Building

### Prerequisites
- Java 17+
- Maven 3.x

### Build Commands
```bash
# Build entire project
cd instrument
mvn clean install

# Build only instrument-eve
cd instrument-eve
mvn clean package
```

## Configuration

### Audio Settings (EJConstants)
- Sample sizes: 8-bit, 16-bit
- Duration: 0-60 seconds
- Pan and volume controls

### Video Settings
- Supported formats: AVI, MOV
- Frame-by-frame processing
- Multiple processing modes (Blank, Effect, Mix, Join)

### MIDI Parameters
- Pitch range: 12-108
- Default BPM: 120
- Velocity control

## File Formats

| Format | Extension | Description |
|--------|-----------|-------------|
| AVI | .avi | Video container |
| MOV | .mov | QuickTime video |
| WAV | .wav | Audio |
| EJS | .ejs | Serialized effect configurations |

## Dependencies

- **Humble Video** (0.3.0) - Audio/video handling
- **JAI** (1.1.3) - Java Advanced Imaging
- **JMF** - Java Media Framework
- **Spring Framework** - Service orchestration
- **JHLabs Image** - Image filter library

## Native Libraries

The application uses JNI for wavelet transform operations:
```java
System.loadLibrary("WavletJNI");
```

Ensure the native library is available in your library path.

## License

MIT License

## Related Modules

- **instrument-core** - Core signal processing framework
- **instrument-desktop** - Desktop UI with MicroStream DB
- **instrument-command** - CLI implementation
- **instrument-ws** - Web Services module
- **instrument-aws** - AWS Cloud Services

## Resources

- [Project Wiki](https://github.com/jimomulloy/instrument/wiki)
- [GitHub Repository](https://github.com/jimomulloy/instrument)
