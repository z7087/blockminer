plugins {
    // If you're using Kotlin, it needs to be applied before the multi-version plugin
    kotlin("jvm")
    // Apply the multi-version plugin, this does all the configuration necessary for the preprocessor to
    // work. In particular it also applies `com.replaymod.preprocess`.
    // In addition it primarily also provides a `platform` extension which you can use in this build script
    // to get the version and mod loader of the current project.
    id("gg.essential.multi-version")
    // If you do not care too much about the details, you can just apply essential-gradle-toolkits' defaults for
    // Minecraft, fabric-loader, forge, mappings, etc. versions.
    // You can also overwrite some of these if need be. See the `gg.essential.defaults.loom` README section.
    // Otherwise you'll need to configure those as usual for (architectury) loom.
    id("gg.essential.defaults")
}

dependencies {
    // If you are depending on a multi-version library following the same scheme as the Essential libraries (that is
    // e.g. `elementa-1.8.9-forge`), you can `toString` `platform` directly to get the respective artifact id.
    modImplementation("gg.essential:elementa-$platform:428")
}

tasks.processResources {
    // Expansions are already set up for `version` (or `file.jarVersion`) and `mcVersionStr`.
    // You do not need to set those up manually.
}

loom {
    // If you need to use a tweaker on legacy (1.12.2 and below) forge:
    if (platform.isLegacyForge) {
        launchConfigs.named("client") {
            arg("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
            // And maybe a core mod?
            property("fml.coreMods.load", "com.example.asm.CoreMod")
        }
    }
    // Mixin on forge? (for legacy forge you will still need to register a tweaker to set up mixin)
    if (platform.isForge) {
        forge {
            mixinConfig("blockminer.mixins.json")
            // And maybe an access transformer?
            // Though try to avoid these, cause they are not automatically translated to Fabric's access widener
            //accessTransformer(project.parent.file("src/main/resources/example_at.cfg"))
        }
    }
}

/*
plugins {
    id 'fabric-loom' version '1.8.13'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}


repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"

    // Make a collection of all api modules we wish to use
    Set<String> apiModules = [
            "fabric-command-api-v2",
            "fabric-resource-loader-v0"
    ]
    // Add each module as a dependency
    apiModules.forEach {
        modImplementation(fabricApi.module(it, project.fabric_version))
    }
}

processResources {
    inputs.property "version", project.version
    inputs.property "minecraft_version", project.minecraft_version
    inputs.property "loader_version", project.loader_version
    filteringCharset "UTF-8"

    filesMatching("fabric.mod.json") {
        expand "version": project.version,
                "minecraft_version": project.minecraft_version,
                "loader_version": project.loader_version
    }
}

def targetJavaVersion = 17
tasks.withType(JavaCompile).configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    it.options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        it.options.release.set(targetJavaVersion)
    }
}

java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    //withSourcesJar()
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.archivesBaseName}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create("mavenJava", MavenPublication) {
            artifactId = project.archives_base_name
            from components.java
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
*/