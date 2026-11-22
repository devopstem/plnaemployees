plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.sonar)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    ignoreFailures = false
    basePath = rootDir.absolutePath
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    source.setFrom(
        files(
            "$rootDir/app/src/main/java",
            "$rootDir/app/src/test/java",
        ),
    )
}

// Enable when turning formatting.active=true in config/detekt/detekt.yml:
// dependencies { detektPlugins(libs.detekt.formatting) }

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

sonar {
    properties {
        // Override via -Dsonar.organization / -Dsonar.projectKey or GitHub vars
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY") ?: "devopstem_plnaemployees")
        property("sonar.organization", System.getenv("SONAR_ORGANIZATION") ?: "devopstem")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "Smart Planogram")
        property("sonar.projectVersion", "1.2.0")
        property("sonar.sourceEncoding", "UTF-8")
        property(
            "sonar.sources",
            "app/src/main/java",
        )
        property(
            "sonar.tests",
            "app/src/test/java",
        )
        property(
            "sonar.exclusions",
            "**/build/**," +
                "**/generated/**," +
                "**/.cxx/**," +
                "**/R.class," +
                "**/BuildConfig.*," +
                "**/third_party/**",
        )
        property(
            "sonar.coverage.exclusions",
            "**/ui/**," +
                "**/theme/**," +
                "**/MainActivity.kt," +
                "**/SpmobApp.kt",
        )
        property(
            "sonar.cpd.exclusions",
            "**/values*/strings.xml",
        )
        property("sonar.androidVariant", "debug")
        property(
            "sonar.kotlin.detekt.reportPaths",
            "$rootDir/build/reports/detekt/detekt.xml",
        )
        property(
            "sonar.sarifReportPaths",
            "$rootDir/build/reports/detekt/detekt.sarif",
        )
        // Quality gate must pass (configure gate in SonarCloud UI as "Sonar way" or stricter)
        property("sonar.qualitygate.wait", "true")
    }
}
