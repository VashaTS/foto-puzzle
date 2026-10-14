import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val ks = Properties().apply { rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
android {
    namespace = "pl.fotopuzzle"
    compileSdk = 34
    defaultConfig { applicationId = "pl.fotopuzzle"; minSdk = 24; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    signingConfigs { create("release") {
        if (ks.isNotEmpty()) { storeFile = rootProject.file(ks["storeFile"] as String); storePassword = ks["storePassword"] as String; keyAlias = ks["keyAlias"] as String; keyPassword = ks["keyPassword"] as String }
    } }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        signingConfig = if (ks.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.android.material:material:1.12.0")
}
