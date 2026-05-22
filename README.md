<div align="center">
  <h1>🎯 Echo-Location</h1>
  <p><strong>Indoor Active Acoustic Echolocation Mapping for the Visually Impaired</strong></p>
  <img width="1200" height="475" alt="Echo-Location Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

---

## 🌍 About Echo-Location

GPS stops working the second you walk inside a building. While some malls use expensive Bluetooth beacons for indoor navigation, **99% of the indoor world is a black box for visually impaired individuals**.

**Echo-Location** is an innovative solution that uses active acoustic echolocation to create real-time indoor navigation. It translates room depths and spatial information into tactile feedback and spatial audio guides, enabling visually impaired users to navigate complex indoor environments independently.

### 🎯 Key Features

- 🔊 **Acoustic Echolocation** - Uses sound-based mapping to detect obstacles and room structure
- 📍 **Real-Time Navigation** - Live spatial audio and tactile feedback
- 🏢 **Indoor Mapping** - Works in buildings where GPS fails
- ♿ **Accessibility-First** - Designed specifically for visually impaired users
- 🚀 **AI-Powered** - Powered by Google's Gemini AI API
- 📱 **Mobile Application** - Android-based with intuitive interface

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- [Android Studio](https://developer.android.com/studio) (Latest version)
- Android SDK 21 or higher
- Java Development Kit (JDK) 11 or higher
- [Gemini API Key](https://ai.google.dev) (Free tier available)

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/himanshudangwal16-Gemini/Echo-Location.git
cd Echo-Location
```

### 2. Open in Android Studio

1. Launch **Android Studio**
2. Click **File** → **Open**
3. Navigate to the cloned `Echo-Location` directory and select it
4. Allow Android Studio to sync and resolve dependencies automatically

### 3. Configure Gemini API Key

1. Get your free Gemini API Key from [ai.google.dev](https://ai.google.dev)
2. Create a `.env` file in the project root directory:

```bash
touch .env
```

3. Add your API key to `.env`:

```dotenv
GEMINI_API_KEY=your_gemini_api_key_here
```

4. Refer to `.env.example` for reference:

```dotenv
GEMINI_API_KEY=MY_GEMINI_API_KEY
```

### 4. Fix Signing Configuration (For Local Development)

1. Open `app/build.gradle.kts`
2. Remove or comment out this line:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

### 5. Build and Run

1. **Build the project**: `Build` → `Make Project` (or `Ctrl+F9`)
2. **Run the app**: 
   - Select an emulator or connect a physical Android device
   - Click the **Run** button (or press `Shift+F10`)

---

## 🔧 Project Structure

```
Echo-Location/
├── app/                          # Main application code
│   └── src/
│       ├── main/
│       │   ├── java/            # Kotlin/Java source files
│       │   ├── res/             # Resources (layouts, strings, drawables)
│       │   └── AndroidManifest.xml
│       └── test/                # Unit tests
├── gradle/                       # Gradle wrapper
├── build.gradle.kts             # Project-level build configuration
├── settings.gradle.kts          # Gradle settings
├── gradle.properties            # Gradle properties
├── .env.example                 # Example environment variables
├── metadata.json                # Project metadata
├── LICENSE                      # MIT License
└── README.md                    # This file
```

---

## 🤖 Technology Stack

- **Language**: Kotlin
- **Platform**: Android
- **AI Engine**: Google Gemini API
- **Build System**: Gradle
- **IDE**: Android Studio

---

## 📝 Configuration Files

### `.env.example`
Contains example environment variable setup. Copy to `.env` and add your actual API key.

### `metadata.json`
Project metadata including:
- Project name and description
- Required capabilities (Server-side Gemini API)
- Frame permissions configuration

### `build.gradle.kts`
Gradle build configuration with dependencies and build settings for the Android project.

---

## 🎓 How It Works

1. **Sound Emission** - The app emits controlled acoustic signals
2. **Echo Detection** - Captures returning echoes from obstacles
3. **Spatial Mapping** - Constructs a mental map of the environment
4. **AI Processing** - Gemini API analyzes spatial data for patterns
5. **Audio/Tactile Feedback** - Provides real-time navigation guidance
6. **Continuous Updates** - Dynamically adjusts as user moves

---

## 🤝 Contributing

We welcome contributions from developers and accessibility advocates! 

### How to Contribute:
1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feature/your-feature`
3. **Commit changes**: `git commit -m "Add your feature"`
4. **Push to branch**: `git push origin feature/your-feature`
5. **Open a Pull Request** with detailed description

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### MIT License Summary:
- ✅ **Free to use, modify, and distribute**
- ✅ **Works in commercial projects**
- ✅ **Requires attribution**
- ✅ **No warranty provided**

---

## 📞 Support & Contact

- 📧 **GitHub Issues**: [Report bugs or request features](https://github.com/himanshudangwal16-Gemini/Echo-Location/issues)
- 💬 **Discussions**: Join our community discussions for questions
- 🌐 **Google AI Studio**: [View the app in AI Studio](https://ai.studio/apps/8daddca3-2efd-473f-948f-438cda701ce8)

---

## 🙏 Acknowledgments

- **Google AI Studio** - For providing the AI framework
- **Gemini API** - For powerful AI capabilities
- **Android Community** - For excellent documentation and tools
- **Accessibility Advocates** - For their valuable insights and feedback

---

## 📊 Project Status

- ✅ Core functionality implemented
- ✅ Gemini API integration complete
- 🔄 Testing in progress
- 🔄 Accessibility improvements ongoing

---

<div align="center">
  <p><strong>Making indoor navigation accessible to everyone! 🌟</strong></p>
  <p>⭐ If you find this project helpful, please star the repository!</p>
</div>
