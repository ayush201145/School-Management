plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.schoolmgmt.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.schoolmgmt.app"
        minSdk = 24       // Android 7.0+, covers the vast majority of devices
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Required because AppDatabase uses exportSchema = true — Room
        // writes a JSON snapshot of the schema here on every build.
        // Check this directory into version control; it's what lets you
        // (or Room) verify a Migration actually transforms version N
        // into exactly what version N+1's entities expect.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Points at your local backend during development.
            // 10.0.2.2 is the special alias the Android EMULATOR uses to
            // reach "localhost" on your development machine.
            // Replace with your real server URL for a physical device or production.
            buildConfigField("String", "BASE_URL", "\"http://10.152.40.98:4000/\"")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required for java.time.* (used in sync/IsoDates.kt) to work on
        // API 24-25 devices — java.time was only added natively in API 26.
        // Without this, IsoDates crashes with NoClassDefFoundError on
        // older devices specifically (works fine on emulators/devices
        // running API 26+, which can mask the issue during testing).
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Required by isCoreLibraryDesugaringEnabled = true (see compileOptions)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Material3 does NOT bundle icon sets automatically (confirmed: this
    // is an explicit, separate dependency regardless of version) — core
    // covers common icons (Sync, Person, People, Receipt), extended adds
    // the rest used in DashboardScreen (Checklist, Inventory,
    // AccountBalance) plus the AutoMirrored.Filled.ExitToApp variant.
    // material-icons-extended is documented as "a very large dependency"
    // — acceptable for now, but worth revisiting (e.g. swapping the few
    // extended icons actually used for custom small vector drawables)
    // if app size becomes a concern later.
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // Required for hiltViewModel() to be callable from inside a
    // Composable (used by LoginScreen, DashboardScreen, and every
    // future screen backed by a @HiltViewModel).
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (local relational DB — the offline-first store)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit (talks to the Express/Postgres backend we built)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    // Codegen-based Moshi adapters (compile-time, via KSP) instead of
    // reflection-based ones — required for every DTO annotated with
    // @JsonClass(generateAdapter = true). Verified as the officially
    // recommended path: faster than reflection AND safe under
    // ProGuard/R8 obfuscation in release builds, where reflection-based
    // Moshi adapters are documented to silently fail to find fields.
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // WorkManager (the background sync engine — survives app kill, retries, battery-aware)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Hilt (dependency injection, keeps repository/sync layer testable)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // DataStore (small key-value store — used for storing the auth token & last-sync timestamp)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
