# EduNewFin 📚

A modern Android application for students to easily access their school timetables and substitution plans through the Edupage platform.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📖 Overview

EduNewFin is a streamlined Android application that connects to the Edupage platform, providing students with quick and easy access to:
- Daily substitution plans
- Personal timetables
- Class schedules
- Real-time updates

The app offers a clean, intuitive interface built with modern Android development practices using Jetpack Compose and Material Design 3.

## ✨ Features

- 🔐 **Secure Login** - Safe authentication with Edupage credentials
- 📅 **Timetable View** - View your personal class schedule
- 🔄 **Substitution Plans** - Stay updated with daily substitutions
- 🎨 **Modern UI** - Beautiful Material Design 3 interface
- 🌓 **Dark Mode** - Easy on the eyes with theme support
- 💾 **Offline Storage** - Access your data even without connection
- ⚡ **Fast & Responsive** - Optimized performance with coroutines
- 🔔 **Real-time Updates** - Get notified about schedule changes

## 📱 Screenshots

*Screenshots coming soon*

## 🛠️ Technologies Used

### Core
- **Kotlin** - Modern, concise programming language
- **Jetpack Compose** - Modern declarative UI framework
- **Material Design 3** - Latest Material Design components
- **View Binding** - Type-safe view access

### Architecture & Patterns
- **MVVM (Model-View-ViewModel)** - Clean architecture pattern
- **Kotlin Coroutines** - Asynchronous operations
- **Lifecycle Components** - Android Architecture Components

### Networking & Data
- **Retrofit 2** - Type-safe HTTP client
- **OkHttp** - HTTP client with logging interceptor
- **Gson** - JSON serialization/deserialization
- **JSoup** - HTML parsing
- **DataStore** - Modern data storage solution

### UI Components
- **RecyclerView** - Efficient list display
- **CardView** - Material card components
- **ConstraintLayout** - Flexible layouts
- **Bottom Navigation** - Tab navigation

## 📋 Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or higher
- Android SDK API 26 (Android 8.0) or higher
- Gradle 8.0+

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ProfessorQuantumUniverse/EduNew9.git
   cd EduNew9
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio
   - Or use: `./gradlew installDebug`

## 🔧 Configuration

### API Configuration

The app connects to the Edupage API. You'll need:
- A valid Edupage school subdomain
- Valid user credentials

Configuration is managed through the app's settings interface.

### Build Variants

- **Debug** - Development build with logging enabled
- **Release** - Production build with ProGuard optimization

## 📱 Usage

1. **First Launch**
   - Open the app
   - Enter your school's Edupage subdomain
   - Login with your Edupage credentials

2. **View Substitutions**
   - Navigate to the "Substitutions" tab
   - View daily substitution plans
   - Pull to refresh for updates

3. **Check Timetable**
   - Navigate to the "Timetable" tab
   - View your personal schedule
   - See merged view with substitutions

4. **Settings**
   - Access settings via the bottom navigation
   - Customize app preferences
   - Manage your account

## 📂 Project Structure

```
EduNew9/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/quantumprof/edunew9/
│   │   │   │   ├── data/              # Data models and managers
│   │   │   │   │   ├── SessionManager.kt
│   │   │   │   │   ├── SettingsManager.kt
│   │   │   │   │   ├── TimetableEntry.kt
│   │   │   │   │   └── UserSettings.kt
│   │   │   │   ├── network/           # API services
│   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   └── EdupageApiService.kt
│   │   │   │   ├── ui/                # UI components
│   │   │   │   │   ├── login/
│   │   │   │   │   ├── main/
│   │   │   │   │   ├── settings/
│   │   │   │   │   └── theme/
│   │   │   │   └── utils/             # Utility classes
│   │   │   │       └── TimetableMerger.kt
│   │   │   ├── res/                   # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                      # Unit tests
│   │   └── androidTest/               # Instrumented tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🏗️ Building for Production

1. **Generate Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

2. **Generate App Bundle (AAB)**
   ```bash
   ./gradlew bundleRelease
   ```

The output will be in `app/build/outputs/`

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔒 Privacy & Security

- User credentials are securely stored using Android DataStore
- Network communication uses HTTPS
- Session management with secure cookie handling
- No user data is collected or shared with third parties

## 🐛 Known Issues

- Check the [Issues](https://github.com/ProfessorQuantumUniverse/EduNew9/issues) page for current known issues
- Report new issues with detailed information

## 📮 Contact & Support

- **Author**: ProfessorQuantumUniverse
- **GitHub**: [@ProfessorQuantumUniverse](https://github.com/ProfessorQuantumUniverse)
- **Issues**: [GitHub Issues](https://github.com/ProfessorQuantumUniverse/EduNew9/issues)

## 🙏 Acknowledgments

- Thanks to the Edupage platform for their services
- Android community for excellent libraries and tools
- All contributors who help improve this project

## 📚 Documentation

For more detailed documentation, please visit:
- [Android Developer Guide](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

Made with ❤️ for students by ProfessorQuantumUniverse
