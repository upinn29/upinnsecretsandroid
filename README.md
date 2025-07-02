# 📦 upinnsecretsandroid


<p align="center">
  <a href="https://github.com/upinn29/upinnsecretsandroid/releases">
    <img alt="Version" src="https://img.shields.io/github/v/tag/upinn29/upinnsecretsandroid?style=flat-square" />
  </a>
  <a href="https://github.com/upinn29/upinnsecretsandroid/packages">
    <img alt="Maven" src="https://img.shields.io/badge/maven-GitHub_Packages-blue?style=flat-square&logo=apachemaven" />
  </a>
  <a href="https://github.com/upinn29/upinnsecretsandroid/stargazers">
    <img alt="Stars" src="https://img.shields.io/github/stars/upinn29/upinnsecretsandroid?style=flat-square" />
  </a>
  <a href="https://github.com/upinn29/upinnsecretsandroid/blob/main/LICENSE.txt">
    <img alt="License" src="https://img.shields.io/github/license/upinn29/upinnsecretsandroid?style=flat-square" />
  </a>

</p>

Android library for consuming protected secrets from the [Upinn](https://upinn.tech). It offers secure on-device authentication and retrieval of encrypted secrets from your backend using a native Rust-based integration.

---

## 🛠 Installation

### 1. Add the GitHub Packages Repository

In your file `settings.gradle.kts` or `settings.gradle`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/upinn29/upinnsecretsandroid")
            credentials {
                username = "GITHUB_USER"     // Your GitHub username
                password = "GITHUB_TOKEN"    // Your GitHub token with `read:packages` permissions
            }
        }
        google()
        mavenCentral()
    }
}
```
### 2. Add the Dependency

In your module's `:app` file `build.gradle.kts`:
```kotlin
dependencies {
    implementation("upinn.tech:upinnsecretsandroid:VERSION") // Replace with the desired version
}

```
---
## 🚀 Usage

### 1. Prepare your file `.bin`

Place the `filename.bin` file in the directory:
```bash
app/src/main/res/raw/filename.bin
```
**_NOTE:_**  ⚠️ Do not rename the file. It must match the name you configure in the code.

### 2. Initialize the Class

```kotlin
val upinn = UpinnSecretsAndroid(
    isDebug = true,                  // Enable logs
    context = applicationContext,
    fileName = "filename.bin"        // File name in /res/raw
)
```

### 3. Authentication (Login)
```kotlin
try {
    val result = upinn.login()
    if (result == 200L) {
        Log.d("Upinn", "✅ Authentication OK")
    }
} catch (e: Exception) {
    Log.e("Upinn", "❌ Error authenticating: ${e.message}")
}

```

### 4. Get Secret
```kotlin
try {
    val response = upinn.get_secret(
        secretName = "SECRET_NAME",
        version = "1" // or null
    )

    Log.d("Upinn", "🔐 Secret: ${response.secretValue}")
} catch (e: Exception) {
    Log.e("Upinn", "❌ Error Getting secret: ${e.message}")
}
```

✅ `SecretsResponse` contains:
```kotlin
data class SecretsResponse(
    val secretValue: String,
    val statusCode: Long
)

```
- 🧪 Error Handling
    - `login()` and `get_secret()` can throw exceptions if any network, authentication, or execution errors occur.

    - You can handle the status codes (`statusCode`) to customize responses.

- 🧰 Requirements
    - Android 5.0 (API 21) or higher.

## 📄 License

This software is exclusively licensed for commercial use under contract with [upinn.tech](https://upinn.tech).

Its use, modification, or redistribution is not permitted without express authorization.

🔒 To obtain a license, contact: [support@upinn.tech](mailto:contacto@upinn.tech)



🧑‍💻 Author

Developed by Upinn.tech

