# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-05

### Added
- **Modular Firebase-style BOM Architecture**: Introduced `securekit-bom`, `securekit-core`, `securekit-integrity`, `securekit-crypto`, `securekit-network`, and `securekit-biometric`.
- **Multi-Byte XOR & Memory Wiping in C++**: Native layer uses compile-time multi-byte XOR arrays and zeroes out decrypted memory buffers immediately after use.
- **Non-Blocking Frida Socket Probe**: 500ms select timeout on Frida socket check to prevent ANR.
- **Consumer Configurable Storage**: Introduced `CryptoConfig` allowing custom SharedPreferences names and Keystore Master Key URIs.
- **Type-Safe Result Handling**: Introduced `SecureResult<T>` sealed class (`Success` / `Error`).
- **Comprehensive Unit & Instrumented Test Suite**: High test coverage across all 5 library submodules.
- **Apache 2.0 License & Open Source Infrastructure**: Included `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and GitHub Actions CI/CD workflows.
