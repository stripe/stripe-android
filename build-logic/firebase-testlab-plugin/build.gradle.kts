import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.2.10"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("com.google.firebase.testlab:testlab-gradle-plugin:0.0.1-alpha13") {
        exclude(group = "com.google.apis", module = "google-api-services-storage")
        exclude(group = "com.google.apis", module = "google-api-services-testing")
        exclude(group = "com.google.apis", module = "google-api-services-toolresults")
    }
    implementation("com.android.tools.build:gradle:8.13.2")
    implementation("com.google.apis:google-api-services-storage:v1-rev20230301-2.0.0")
    implementation("com.google.apis:google-api-services-testing:v1-rev20230411-2.0.0")
    implementation("com.google.apis:google-api-services-toolresults:v1beta3-rev20230410-2.0.0")
    implementation("com.google.api-client:google-api-client:2.7.1")
    implementation("com.google.http-client:google-http-client-jackson2:1.45.2")
}

gradlePlugin {
    plugins {
        create("firebaseTestLab") {
            id = "com.google.firebase.testlab"
            implementationClass = "com.stripe.android.gradle.FirebaseTestLabPlugin"
        }
    }
}
