plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "upinn.tech.upinnsecretsandroid"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("net.java.dev.jna:jna:5.12.0@aar")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}



// Configuración de publicación para GitHub Packages
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "upinn.tech"
                artifactId = "upinnsecretsandroid"
                version = "1.0.3"

                from(components["release"])

                pom {
                    name.set("Upinn Secrets Android")
                    description.set("Librería para manejo seguro de secretos en Android")
                    url.set("https://github.com/upinn29/upinnsecretsandroid.git")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    developers {
                        developer {
                            id.set("upinn29")
                            name.set("Upinn Technologies")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/upinn29/upinnsecretsandroid.git")
                        developerConnection.set("scm:git:ssh://github.com/upinn29/upinnsecretsandroid.git")
                        url.set("https://github.com/upinn29/upinnsecretsandroid/tree/main")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/upinn29/upinnsecretsandroid")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}