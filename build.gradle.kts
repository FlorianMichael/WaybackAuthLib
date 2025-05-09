import de.florianmichael.baseproject.*

plugins {
    `java-library`
    id("de.florianmichael.baseproject.BaseProject")
}

setupProject()
setupPublishing()


repositories {
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("com.mojang:authlib:5.0.47")
}
