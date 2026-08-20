# Repair System Mobile

Flutter application prototype for customers and technicians.

The current UI uses mock data because the backend does not yet have customer-specific endpoints or a `CUSTOMER` role. The screens are prepared for API integration later.

## Included screens

- Login with customer/technician role switch
- Russian, Uzbek and English language switcher
- Customer home, requests, request details and create request
- Technician dashboard, assigned requests and job details
- Profile and notifications
- Warm brown/cream theme based on the landing page

## Run

```bash
flutter pub get
flutter run
```

Android and iOS platform folders are already generated. iOS requires macOS/Xcode to run; Android can run on Windows with an emulator or physical device.
