    plugins {
    id("java")
    id("maven-publish")
    alias(libs.plugins.loom)
    alias(libs.plugins.ploceus)
}

group = "re.lilith"
version = "1.0-SNAPSHOT"

ploceus.setIntermediaryGeneration(2)

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            mavenCentral()
        }

        filter {
            includeGroup("org.lwjgl")
        }
    }

    maven("https://maven.taumc.org/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://jitpack.io")
    maven("https://maven.legacyfabric.net")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.legacy.yarn) { classifier("v2") })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.lenis)
    modImplementation(libs.devauth.fabric)

    modImplementation(files("mods/argentum-1.0.0.jar"))

    implementation(libs.celeritas)
    implementation(libs.fastutil)

    bundled(libs.joml)
    bundled(libs.jcpp)
    bundled(libs.glsl.transformation.lib) {
        exclude(module = "antlr4")
    }
    bundled(libs.antlr4.runtime)

    ploceus.dependOsl(libs.versions.osl.get())
}

fun DependencyHandlerScope.bundled(
    dependency: Any,
    configure: ExternalModuleDependency.() -> Unit = {}
) {
    add("include", dependency)
    add("implementation", dependency).apply {
        if (this is ExternalModuleDependency) {
            configure()
        }
    }
}

loom {
    accessWidenerPath.set(
        file("src/main/resources/aurum.accesswidener")
    )

    runs.all {
        jvmArguments.add("-Ddevauth.enabled=true")
        jvmArguments.add("-Ddevauth.account=alt")
    }
}

configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")

    resolutionStrategy.eachDependency {
        if (requested.group == "org.lwjgl") {
            useVersion(libs.versions.lwjgl.get())
        }
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/vendored")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = libs.versions.java.get().toInt()
}

tasks.test {
    useJUnitPlatform()
}