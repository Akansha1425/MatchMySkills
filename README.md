# MatchMySkills: Skill-Based Recommendation Platform

## 📋 Overview

**MatchMySkills** is an innovative Android application that leverages AI-driven skill matching to connect students with personalized job opportunities, internships, and hackathons. The platform provides intelligent recommendations based on user profiles, skills, and preferences, creating a seamless bridge between talented individuals and exciting career opportunities.

### Topic Description
MatchMySkills is a skill-based recommendation system designed specifically for the modern job market. It utilizes advanced matching algorithms and AI analysis to evaluate candidate profiles against available opportunities, ensuring optimal compatibility between candidates and positions. The platform supports multiple opportunity types including full-time jobs, internships, and hackathons, with location-based recommendations and real-time notifications.

---

## 🚀 Features

### Core Features
- **Skill-Based Matching Engine**: AI-powered algorithm that analyzes candidate skills and matches them with suitable opportunities
- **Multi-Role Support**: Separate dashboards for students and recruiters with role-specific functionalities
- **Resume Management**: Built-in resume upload and preview system with Cloudinary integration

### Opportunity Management
- **Job Listings**: Browse, apply, and receive recommendations for job positions
- **Internship Portal**: Dedicated section for internship opportunities with filtering options
- **Hackathon Hub**: Discover and participate in hackathons with event details and registration
- **Application Tracking**: Track all applications with status updates and recruiter feedback

### Student Dashboard
- **Personalized Dashboard**: Overview of recommended opportunities based on skill match
- **Profile Management**: Create and edit student profiles with skills, experience, and achievements
- **Application History**: View all submitted applications and their statuses
- **Saved Opportunities**: Save opportunities for later review
- **Notification Center**: Manage and view all notifications in one place

### Recruiter Dashboard
- **Opportunity Management**: Create and manage job posts, internships, and hackathons
- **Applicant Management**: Review applications, screen candidates, and track hiring progress
- **Analytics & Insights**: View application statistics, candidate quality metrics, and posting performance
- **Batch Operations**: Handle multiple applications efficiently

### Advanced Features
- **Resume Analysis via AI Service**: External AI-powered analysis to evaluate and match candidate profiles with opportunities
- **Offline Functionality**: Work continues even without internet connection (synced when online)
- **Dark Mode Support**: Comfortable viewing in all lighting conditions
- **MVVM Architecture**: Clean, maintainable, and testable codebase
- **Firebase Integration**: Real-time database and cloud-based storage
- **Hilt Dependency Injection**: Modern DI framework for cleaner code

---

## 🛠️ Tech Stack

### Frontend
- **Kotlin**: Modern Android development language
- **XML Layouts**: Traditional XML-based UI design
- **Core Android Libraries**:
  - ViewModel & LiveData for state management
  - Room Database for local persistence
  - Hilt for dependency injection

### Backend & Cloud Services
- **Firebase**:
  - Firestore (Real-time Database)
  - Firebase Authentication
  - Cloud Storage
  - Cloud Functions
- **Cloudinary**: Resume and media file hosting
- **Google Location Services**: GPS-based location services

### Development Tools & Libraries
- **Hilt**: Dependency injection
- **Retrofit**: HTTP client for API calls
- **Glide**: Image loading and caching
- **Compose** (UI Framework)
- **JUnit & Espresso**: Testing frameworks

### Minimum Requirements
- **Android SDK**: 24 (API Level 24) and above
- **Target SDK**: 35
- **Kotlin Version**: 1.9+
- **Gradle Version**: 8.0+

---

## 📁 Project Structure

```
MatchMySkills/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/matchmyskills/
│   │   │   │   ├── Activities/
│   │   │   │   │   ├── SplashActivity.kt
│   │   │   │   │   ├── LoginActivity.kt
│   │   │   │   │   ├── RegisterActivity.kt
│   │   │   │   │   ├── OnboardingActivity.kt
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── JobDetailActivity.kt
│   │   │   │   │   ├── OpportunityDetailActivity.kt
│   │   │   │   │   ├── ImagePreviewActivity.kt
│   │   │   │   │   └── ResumePreviewActivity.kt
│   │   │   │   ├── Fragments/
│   │   │   │   │   ├── HomeFragment.kt
│   │   │   │   │   ├── StudentDashboard.kt
│   │   │   │   │   ├── RecruiterDashboard.kt
│   │   │   │   │   ├── JobsFragment.kt
│   │   │   │   │   ├── InternshipFragment.kt
│   │   │   │   │   ├── HackathonFragment.kt
│   │   │   │   │   ├── JobDetailFragment.kt
│   │   │   │   │   ├── HackathonDetailFragment.kt
│   │   │   │   │   ├── ProfileFragment.kt
│   │   │   │   │   ├── StudentProfileFragment.kt
│   │   │   │   │   ├── ApplicantListFragment.kt
│   │   │   │   │   ├── NotificationsFragment.kt
│   │   │   │   │   ├── UserNotificationsFragment.kt
│   │   │   │   │   ├── AnalyticsFragment.kt
│   │   │   │   │   ├── DashboardFragment.kt
│   │   │   │   │   ├── ApplicantDetailFragment.kt
│   │   │   │   │   ├── CreateJobFragment.kt
│   │   │   │   │   ├── CreateInternshipFragment.kt
│   │   │   │   │   └── CreateHackathonFragment.kt
│   │   │   │   ├── Database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── Models.kt
│   │   │   │   │   ├── Converters.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── ModelMappers.kt
│   │   │   │   ├── Repository/
│   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   ├── JobRepository.kt
│   │   │   │   │   ├── JobOpportunityRepository.kt
│   │   │   │   │   ├── HackathonRepository.kt
│   │   │   │   │   ├── ApplicationRepository.kt
│   │   │   │   │   ├── NotificationRepository.kt
│   │   │   │   │   ├── StudentDashboardRepository.kt
│   │   │   │   │   └── ExternalOpportunityDataSource.kt
│   │   │   │   ├── ViewModel/
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   ├── StudentDashboardViewModel.kt
│   │   │   │   │   ├── ApplicantViewModel.kt
│   │   │   │   │   ├── NotificationViewModel.kt
│   │   │   │   │   ├── UserNotificationViewModel.kt
│   │   │   │   │   ├── OpportunityViewModel.kt
│   │   │   │   │   └── AnalyticsViewModel.kt
│   │   │   │   ├── Adapters/
│   │   │   │   │   ├── JobOpportunityAdapter.kt
│   │   │   │   │   ├── HackathonAdapter.kt
│   │   │   │   │   ├── DashboardAdapter.kt
│   │   │   │   │   ├── ApplicantAdapter.kt
│   │   │   │   │   ├── PostedJobsAdapter.kt
│   │   │   │   │   ├── NotificationAdapter.kt
│   │   │   │   │   ├── UserNotificationAdapter.kt
│   │   │   │   │   └── ApplicationFormUtils.kt
│   │   │   │   ├── Utils/
│   │   │   │   │   ├── LocationHelper.kt
│   │   │   │   │   ├── NotificationPermissionHelper.kt
│   │   │   │   │   ├── NetworkObserver.kt
│   │   │   │   │   ├── UiState.kt
│   │   │   │   │   └── FirestoreExt.kt
│   │   │   │   ├── Services/
│   │   │   │   │   ├── CandidateAiAnalyzer.kt
│   │   │   │   │   ├── CloudinaryResumeUploader.kt
│   │   │   │   │   ├── MatchingEngine.kt
│   │   │   │   │   ├── OpportunitySyncWorker.kt
│   │   │   │   │   ├── OpportunitySyncScheduler.kt
│   │   │   │   │   └── OpportunityNotificationHelper.kt
│   │   │   │   ├── DI/
│   │   │   │   │   ├── FirebaseModule.kt
│   │   │   │   │   ├── RepositoryModule.kt
│   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   ├── App/
│   │   │   │   │   ├── MatchMySkillsApp.kt
│   │   │   │   │   └── BottomSheetCreateOpportunity.kt
│   │   │   │   └── UI/
│   │   │   │       ├── EditProfileBottomSheet.kt
│   │   │   │       └── AdminDashboard.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   └── navigation/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── google-services.json
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── firestore.rules
├── storage.rules
└── README.md
```

### Key Components

**Activities**: Entry points for different user flows (authentication, main app, opportunity details)

**Fragments**: Reusable UI components for displaying opportunities, profiles, and management screens

**ViewModels**: Business logic and state management using Android Architecture Components

**Repositories**: Data abstraction layer handling both local (Room) and remote (Firestore) data

**Database**: Room database with Firestore synchronization for offline support

**Services**: Background services for sync, notifications, and AI analysis

**Utils**: Helper functions for location, permissions, and Firebase operations

---

## 👥 Authors

- **Akansha Bibishan Zambare**
- **Sailee Khedekar**

*This is a mini-project created as part of academic coursework in Mobile Application Development (MAD)*

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK 24 or higher
- Firebase Project Setup
- Cloudinary Account (for resume uploads)

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/MatchMySkills.git
   cd MatchMySkills
   ```

2. **Configure Firebase**
   - Create a Firebase project on [Firebase Console](https://console.firebase.google.com)
   - Download `google-services.json` and place it in the `app/` directory
   - Enable Authentication, Firestore, and Cloud Storage

3. **Set Up Local Properties**
   - Create `local.properties` in the project root:
   ```properties
   CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
   CLOUDINARY_UNSIGNED_PRESET=your_unsigned_preset
   AI_ANALYSIS_ENDPOINT=your_ai_endpoint
   ```

4. **Build and Run**
   ```bash
   ./gradlew build
   # Run on emulator or connected device
   ./gradlew installDebug
   ```

---

## 📝 Usage

### For Students
1. Sign up with email and create a profile
2. Add your skills and experience
3. Browse job opportunities, internships, and hackathons
4. Apply to opportunities matching your skills
5. Track application status and receive notifications

### For Recruiters
1. Register as a recruiter
2. Create job postings, internship programs, or hackathon events
3. Review incoming applications
4. Manage and communicate with applicants
5. View analytics and hiring metrics

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes (`git commit -m 'Add YourFeature'`)
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a Pull Request

---

## 📄 License

This project is provided as-is for educational purposes. All rights reserved by the authors.

---

## 📧 Support & Contact

For questions, bug reports, or suggestions, please contact the authors or open an issue in the repository.

---

**Happy Matching! 🎯**
