plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.isFork = true
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    (options as StandardJavadocDocletOptions).tags("apiNote:a:API Note:")
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = Charsets.UTF_8.name()
}
