plugins {
//    id("java-library")
    kotlin("jvm") version "1.9.22"
    id("com.google.protobuf") version "0.9.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val protobufVersion: String by rootProject.extra

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
}

dependencies {
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    api("com.google.protobuf:protobuf-kotlin:$protobufVersion")
}

sourceSets {
    main {
        java {
            srcDir("$projectDir/gen/java")
        }
        kotlin {
            srcDir("$projectDir/gen/kotlin/")
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}