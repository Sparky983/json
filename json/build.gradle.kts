plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

tasks.test {
    useJUnitPlatform()
}
