# ZenChat: An AI Companion

ZenChat is a mental health-focused chat application designed to provide users with an interactive space to share their thoughts and receive support. By using a custom-built API with Node.js and OpenAI, ZenChat offers empathetic conversations tailored to the user's emotional needs. The app also includes customization options such as themes, daily reminders, and calming audio.

## Project Details

- **GitHub Repository**: https://github.com/abd-abd-123/Project---DMD
- **Team Members**:
  - Mahmoud Mirghani Abdelrahman
  - El-Ghoul Layla
  - Uritu Andra-Ioana
- **Group**: 1231EB

---

## Features

1. **Interactive AI Chat**:
   - Users can communicate with an AI assistant for emotional support.
   - Responses are generated through a Node.js backend powered by the OpenAI API.

2. **Custom Themes**:
   - Support for light and dark modes based on user preferences.

3. **Daily Notifications**:
   - Daily reminders for users to check in on their mental health.

4. **Calming Audio**:
   - An option to play soothing sounds for a relaxing experience.

5. **Chat History**:
   - Save and retrieve previous messages using Room Database.

---

## Android Components Used

The project incorporates the following components:

### 1. **Activities**
   - **MainActivity**: The primary interface for user-AI conversations.
   - **SettingsActivity**: A sepparate screen for theme selection, notifications, and audio toggle options.

### 2. **Intents**
   - Used for navigation between activities (e.g., MainActivity <-> SettingsActivity).

### 3. **Shared Preferences**
   - Save user settings such as:
     - Dark or light theme preferences.
     - Daily notification settings.
     - Audio toggle state.

### 4. **Database**
   - Implemented using Room Database to:
     - Store chat history for retrieval.
     - Ensure a seamless user experience when reopening the app.

### 5. **Notifications**
   - Daily reminders implemented using:
     - **Broadcast Receivers** for `BOOT_COMPLETED` events to reschedule notifications.
     - **AlarmManager** to schedule exact alarms for daily notifications.

### 6. **Broadcast Receivers**
   - **DailyNotificationReceiver**:
     - Handles the reception of alarm signals to trigger daily notifications.

### 7. **Foreground Services**
   - **CalmingAudioService**:
     - A foreground service that plays relaxing audio continuously when enabled, visible through persistent notifications.

### 8. **Usage of External APIs**
   - Node.js API integrated with OpenAI API:
     - Processes user messages and returns empathetic responses.


 
