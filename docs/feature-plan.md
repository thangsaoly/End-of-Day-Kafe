# Feature Plan: QR Claim → Favourites & Alerts → Pickup Reminders → Ratings

Features chosen from research on similar apps (Too Good To Go, Yindii, Karma, Flashfood, Olio, Surplus Indonesia, ResQ Club), built in this order:

1. QR code claiming
2. Favourite cafes + new-deal push alerts
3. Pickup reminders
4. Ratings

Estimated total: **~2 weeks** for one developer. Use one branch and one PR per phase, and run `mvn test` before each PR.

---

## Phase 0: Fix these first (~½ day) ✅ Done (2026-10-15)

> Implemented as planned. One merchant manages exactly one cafe (`User.cafeId`). `GET /api/merchant/reservations` now returns only the merchant's own cafe. The removed "dummy merchant" overloads in `KafeService` used to skip every check. A `Clock` bean (`config/TimeConfig.java`) fixes "today" to Asia/Phnom_Penh. Merchants who sign up through the app are not linked to a cafe, so they get 403 until one is assigned in the database.

Problems found in the current code that must be fixed before the features.

1. **Security leak: password hashes are sent to the app.**
   `User` has no `@JsonIgnore`, and every `Reservation` response includes the full `User` object, so `passwordHash` and `emailVerificationCode` go out with every reservation. `GET /api/merchant/reservations` returns them for **every user**.
   **Fix:** add `@JsonIgnore` to both fields in `backend/src/main/java/com/kafe/model/User.java`.

2. **Merchants aren't linked to a cafe.**
   Any merchant can see and claim every cafe's reservations. `/api/merchant/reservations/{id}/expire` has no role check, so any logged-in user can mark someone as a no-show, and three no-shows bans the account.
   **Fix:** add a `cafe_id` column to `User` for merchants, and check `reservation.deal.cafe.id == merchant.cafeId` in claim, claim-by-code and expire. Phases 1–3 depend on this.

3. **Deals have no date.**
   `pickupStart` and `pickupEnd` are plain `"19:30"` strings.
   **Fix:** add a `pickupDate` (`LocalDate`) field and treat times as `Asia/Phnom_Penh`. Reminders (Phase 3) need this.

4. **There's no way to create a deal.**
   Deals only come from `DatabaseSeeder`. Phase 2's "new deal" alert needs a trigger.
   **Fix:** add `POST /api/merchant/deals`, limited to the merchant's own cafe.

5. **Enable scheduling.** Add `@EnableScheduling` to `EndOfDayKafeApplication` (Phase 3 needs it).

---

## Phase 1: QR code claiming (1–2 days)

### Backend
- Almost nothing to do. `POST /api/merchant/reservations/claim-by-code/{claimCode}` already exists; the cafe check from Phase 0 is what makes it safe.

### Android
- **Customer side:** `ReservationCard` in `ui/MainScreen.kt` shows a QR code of `claimCode` for reservations still waiting to be picked up (`PENDING`).
  - Generate the QR with `com.google.zxing:core` and show the text code underneath.
  - Turn screen brightness up while the QR code is displayed.
- **Merchant side:** add a "Scan" button to `MerchantReservationsTab`.
  - Use the Google code scanner (`com.google.android.gms:play-services-code-scanner`), which doesn't need the CAMERA permission.
  - Pass the scanned value to the existing `viewModel.claimReservationByCode()`.
  - Keep typing the code by hand as a fallback.

### Tests
- `KafeServiceTest`: claiming another cafe's code returns 403; claiming your own cafe's code works.

---

## Phase 2: Favourite cafes + new-deal alerts (3–4 days)

### Backend
- New `Favorite` entity linking a user to a cafe, unique per pair. Endpoints:
  - `GET /api/favorites`
  - `POST /api/favorites/{cafeId}`
  - `DELETE /api/favorites/{cafeId}`
- New `DeviceToken` entity (user, FCM token, updatedAt) and `POST /api/devices` to register a phone.
- Add the `com.google.firebase:firebase-admin` dependency and a `NotificationService`.
  - The service-account JSON comes from an environment variable and is **never committed**.
- When a merchant creates a deal, notify the users who favourited that cafe.
  - Use `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`, so a failed push can never roll back the deal.
  - Delete tokens that FCM reports as no longer valid.

### Android
- Add `firebase-messaging`; Firebase is already set up through the BoM.
- Add a `KafeMessagingService` whose `onNewToken` sends the token to `/api/devices`. Also send it after login.
- Add `POST_NOTIFICATIONS` to the manifest and request it at runtime on Android 13+. Create a `new_deals` notification channel.
- Put a heart icon on `DealCard` and add a "Favourites" filter chip in `DealsTab`.
- Tapping a notification opens the app on that deal (pass `dealId` as an intent extra).

---

## Phase 3: Pickup reminders (1–2 days)

Reuses the notification setup from Phase 2.

### Backend
- Add a `reminderSentAt` field to `Reservation`.
- A `@Scheduled` job runs every 5 minutes:
  - Finds `PENDING` reservations with less than 30 minutes left in the pickup window and no reminder sent yet.
  - Sends the push, e.g. *"Your bag at Brown Coffee closes at 21:00, code KAFE-1A2B"*, and sets `reminderSentAt`.
- **Optional:** a second job that automatically expires reservations 15 minutes after the pickup window ends (currently a merchant has to do this by hand).
- Inject a `java.time.Clock` so the jobs can be tested.

### Android
- Add a `pickup_reminders` notification channel.
- Tapping the notification opens the Reservations tab with the QR code showing.

---

## Phase 4: Ratings (2–3 days)

### Backend
- New `Rating` entity: one per reservation, 1–5 stars, optional comment (max 300 characters), createdAt.
- `POST /api/reservations/{id}/rating`. Rules:
  - only the customer who made the reservation can rate it
  - only after the pickup is completed (`COMPLETED`)
  - only within 48 hours
  - only once
- Store `avgRating` and `ratingCount` directly on `Cafe` and update them on each new rating, so the deals list loads without extra queries.
- `GET /api/merchant/ratings` lets merchants see their cafe's ratings.

### Android
- Completed reservations get a "Rate" button that opens a star-rating bottom sheet.
- `DealCard` shows something like **★ 4.6 (23)**.
- Optional: a "How was your bag?" push one hour after pickup, using the Phase 2 setup.
- Photo reviews are left out for now because they need file storage.

---

## Notes
- `ddl-auto: update` adds new columns automatically. Give any new non-nullable column a default so existing rows don't break.

## Open decisions
1. Should missed pickups be marked as no-shows automatically (Phase 3), or does the merchant keep doing it by hand?
2. Is there a Firebase service account for the backend, or are setup steps needed?
3. Does each merchant manage exactly one cafe, or can one account manage several?
