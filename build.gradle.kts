plugins {
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    id("io.gatling.gradle") version "3.9.3"

}

extra["protobufVersion"] = "4.28.2"


allprojects {
    repositories {
        mavenCentral()
    }
}
