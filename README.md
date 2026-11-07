# SS Bedrock Client (Android)

Native Android companion launcher built with Kotlin + Jetpack Compose.

- Package: `com.ss.client`
- Minimum Android: 8.0 (API 26)
- Compile / target SDK: 36
- Java: 17
- Default AI provider: Grok (xAI-compatible API)
- Theme: SS Blue Glow

## Build APK with GitHub Actions

1. Extract this ZIP.
2. Open the `SSBedrockClient` folder.
3. Upload the **contents of that folder** to the root of a GitHub repository (so `settings.gradle.kts`, `app/`, and `.github/` are at the repository root).
4. Open **Actions → Build SS Bedrock Client APK → Run workflow**.
5. When the workflow finishes successfully, open its run and download the artifact named `SS-Bedrock-Client-debug-apk`.

The workflow installs Gradle 8.11.1 and Android SDK 36, then builds `assembleDebug`. A Gradle wrapper JAR is not required for this workflow because it invokes the installed Gradle executable directly.

## Local build

Open this folder in Android Studio (a recent version with Android SDK 36 installed), then run the `app` configuration or execute `gradle assembleDebug` from this folder.

## Important limitations

This is a companion app, not a modified Minecraft client. It shows an SS-branded loading screen and then launches the official Minecraft Bedrock app (`com.mojang.minecraftpe`). A normal Android app cannot replace Minecraft's own loading logo, read private game state, inject game inputs, or modify the protected game renderer. Features that need those capabilities are labelled as limited, unsupported, or requiring a compatible resource pack/add-on/server integration. CPS counts taps on the SS Client test pad, not Minecraft taps.

The sign-in feature in this starter is local to this device; it is not a cloud account system. Never enter a real password you use elsewhere. Grok API keys are stored with Android encrypted preferences when that facility is available. API use may incur charges from the provider.

## Testing status

The uploaded source has been patched for the GitHub Actions project-directory and artifact-path problems. It has **not** been compiled in this environment because an Android SDK/Gradle build environment is unavailable here. A successful GitHub Actions run is still required to confirm compilation, and the APK should be installed and tested on the target phone before distribution.
