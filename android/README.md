# Claims Clerk — Android app

Jetpack Compose front-end for the clerk: **list + filter + detail** of synthetic claims, and
**scan → on-device OCR → deterministic parse → confirm → ingest** of paper claim documents.

Talks only to the App #2 backend (`https://danovich.ddns.net:28587` by default). Auth is
platform-stack per-user login (email/password + Google + GitHub + refresh-backed session),
consumed as **source subprojects** from `../vendor/platform-stack` (`:android-auth`,
`:platform-login-ui`).

## Build

Gradle can't run on Java 25 here — use JDK 21:

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/vdanovich/Android/Sdk \
  ./gradlew :app:assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

### Build-time config (all optional)

| Gradle property | Effect |
| --- | --- |
| `-PbaseUrl=...` | Override the API base URL (e.g. `http://10.0.2.2:8090` for an emulator hitting the local backend — the debug build allows cleartext for exactly this). |
| `-PgoogleServerClientId=...` | Google **Web** OAuth client id. Blank → the Google button is disabled. |
| `-PgithubClientId=...` | GitHub OAuth App client id. Blank → the GitHub button is disabled. |

The GitHub **secret** is never in the app — it lives only on the backend. The app carries just
the public client id. No secrets are baked into source.

Example pointing at the local backend with Google enabled:

```bash
./gradlew :app:assembleDebug -PbaseUrl=http://10.0.2.2:8090 -PgoogleServerClientId=xxxx.apps.googleusercontent.com
```

## How it's wired

- **Networking** (`data/AppGraph.kt`): two OkHttp clients. The data client stacks
  `TokenRefreshInterceptor` **outer** + `BearerAuthInterceptor` **inner** (the order the
  platform-auth skill requires); the auth client is plain to avoid refresh recursion. The
  `TokenProvider` trades the stored refresh token for a fresh JWT on a 401.
- **Session** (`data/SessionStore.kt`): refresh token + identity persisted (foundation for PIN
  quick-unlock); the JWT stays in memory (`TokenStore`).
- **Login** (`ui/LoginRoute.kt`): hands a `LoginConfig` to `PlatformLoginScreen`.
- **Scan** (`scan/`): ML Kit Document Scanner (edge detect/deskew/crop) → ML Kit Latin text
  recognition → `ClaimParser` (labelled-field regex over the fixed synthetic format) → editable
  confirm form → multipart `POST /api/claims`. All on-device; nothing leaves the phone for OCR.
- **Images**: loaded via a Coil `ImageLoader` sharing the authed data client, so
  `GET /api/claims/{id}/image` carries the bearer.

## Running it

ML Kit Document Scanner needs Google Play services, so use a Play-enabled emulator or a real
device. Point `baseUrl` at a reachable backend (the public TLS endpoint, or `10.0.2.2:8090`
from an emulator). Sign in (or register) → browse/filter → tap a claim for detail → FAB to scan.

> Note: parsing is deterministic and on-device because the synthetic documents are a fixed
> format. Messy/varied-layout parsing would be done **in-environment on the backend**, never by
> shipping the image off-device.
