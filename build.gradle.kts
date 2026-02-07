import de.florianreuth.baseproject.*

plugins {
    `java-library`
    id("de.florianreuth.baseproject")
}

setupProject()
setupPublishing()

repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("com.mojang:authlib:5.0.47")
}
