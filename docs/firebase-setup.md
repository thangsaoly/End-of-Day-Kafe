# Firebase setup

Firebase project: **`end-of-day-kafe`** ([console](https://console.firebase.google.com/project/end-of-day-kafe/overview))
Android app: `com.kafe.android` (app ID `1:959549827163:android:e661a5c47aae1395d5b77e`)

Firebase is used for **sign-in** (email/password and Google) and **push notifications**. Deals, reservations and everything else stay in the Spring Boot backend and MySQL.

## How sign-in works

1. The app signs the user in with Firebase Authentication.
2. Every API request carries `Authorization: Bearer <Firebase ID token>` (added by `FirebaseTokenInterceptor`).
3. The backend verifies the token (`FirebaseAuthenticationFilter`) and finds the user by `users.firebase_uid`.
4. After each sign-in the app calls `POST /api/auth/session`, which creates the backend account the first time.

Password resets are Firebase emails. The old phone + password login, the backend JWTs, and the mock `123456` / `654321` email codes have been removed.

## One-time console setup

1. **Sign-in providers**: Authentication → Sign-in method → enable **Email/Password** and **Google** (pick a support email).
2. **App config** (after step 1, so it includes the Google web client ID):
   ```bash
   firebase apps:sdkconfig ANDROID 1:959549827163:android:e661a5c47aae1395d5b77e \
     --project end-of-day-kafe > android/app/google-services.json
   ```
3. **Server key** (needed only for sending pushes): Project settings → Service accounts → Generate new private key. Save it **outside the repo** (`.gitignore` blocks `*firebase-adminsdk*.json` as a safety net).

Each developer's debug signing key must be registered for Google Sign-In:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
firebase apps:android:sha:create 1:959549827163:android:e661a5c47aae1395d5b77e <SHA1> --project end-of-day-kafe
```

## Running the backend

```bash
cd backend
FIREBASE_CREDENTIALS=/path/to/end-of-day-kafe-firebase-adminsdk-xxxx.json \
DEV_MERCHANT_EMAIL=you@example.com \
mvn spring-boot:run
```

- Without `FIREBASE_CREDENTIALS`, sign-in still works (verifying tokens needs only Google's public keys) and pushes are just logged.
- `DEV_MERCHANT_EMAIL` (optional, local development only): the account with this email becomes the merchant of the first cafe the first time it signs in. Otherwise, make a merchant in MySQL:
  ```sql
  UPDATE users SET role = 'MERCHANT', cafe_id = 1 WHERE email = 'you@example.com';
  ```

Old users from the phone-number login can't sign in any more; their rows stay in the database.
