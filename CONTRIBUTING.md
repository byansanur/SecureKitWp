# Contributing to SecureKit

Thank you for your interest in contributing to **SecureKit**! We welcome contributions from the security and Android development communities.

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Development Workflow

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android NDK (r26b or newer) & CMake 3.22.1
- JDK 17 / Kotlin 2.0+

### Setting Up the Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/byan/SecureKitWp.git
   cd SecureKitWp
   ```
2. Build all modules and release artifacts:
   ```bash
   ./gradlew assembleRelease
   ```
3. Run unit tests:
   ```bash
   ./gradlew test
   ```

### Architecture Guidelines

SecureKit follows a modular **Bill of Materials (BOM)** structure:
- `securekit-bom`: Platform version catalog.
- `securekit-core`: Memory-safe primitives (`SecureCharArray`, `PathValidation`, `SecureResult`).
- `securekit-integrity`: Native C++ NDK checks, root/hooking/emulator detection.
- `securekit-crypto`: Tink AEAD & Streaming AEAD storage (`SecureVault`).
- `securekit-network`: Certificate Pinning & Proxy/VPN detection (`NetworkArmor`).
- `securekit-biometric`: `BiometricShield` & `UiProtection`.

### Pull Request Process

1. Create a feature branch off `main`:
   ```bash
   git checkout -b feature/my-security-fix
   ```
2. Write unit tests for all new features or bug fixes.
3. Ensure code formatting adheres to Kotlin official style guidelines.
4. Run all verification checks locally before submitting:
   ```bash
   ./gradlew assembleRelease test publishToMavenLocal
   ```
5. Open a Pull Request on GitHub with a clear summary of changes.
