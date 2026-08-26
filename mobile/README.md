# Repair System Mobile

Flutter application prototype for customers and technicians.

The mobile app uses the authenticated `/api/v1/mobile` backend APIs for customers and technicians. Telegram authentication requires the official Android SDK configuration and the Telegram client build defines described below.

## Included screens

- Login with customer/technician role switch
- Russian, Uzbek and English language switcher
- Customer home, requests, request details and create request
- Technician dashboard, assigned requests and job details
- Profile and notifications
- Warm brown/cream theme based on the landing page

The app currently supports request creation, device location, categories, request details, attachments, reviews, technician jobs and actions, schedules, notifications, profile updates, chat and STOMP realtime events. Firebase push delivery still requires adding the Firebase project configuration (`google-services.json`) and an FCM registration token; the backend push registration methods are present but are not called automatically until that configuration is supplied.

## Run

```bash
flutter pub get
flutter run
```

Android and iOS platform folders are already generated. iOS requires macOS/Xcode to run; Android can run on Windows with an emulator or physical device.
