// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}

// ktlint: Kotlin 코드 스타일 검사/자동 정리 (규칙은 .editorconfig)
val ktlint: Configuration = configurations.create("ktlint")

// detekt: 정적 분석 (복잡도, 잠재 버그, 이름 규칙 등). 규칙은 config/detekt/detekt.yml
val detekt: Configuration = configurations.create("detekt")

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    detekt(libs.detekt.cli)
}

// ktlint와 같은 이유로 Gradle 플러그인 대신 CLI로 실행 (AGP 9 내장 Kotlin과 플러그인 호환 문제 회피)
tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Kotlin 정적 분석 (결과: build/reports/detekt/detekt.html)"
    classpath = detekt
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args(
        "--input", "app/src/main/java,app/src/test/java",
        "--config", "config/detekt/detekt.yml",
        "--build-upon-default-config",
        "--report", "html:build/reports/detekt/detekt.html",
        "--report", "txt:build/reports/detekt/detekt.txt"
    )
}

val ktlintTargets = listOf("app/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Kotlin 코드 스타일 검사"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(ktlintTargets)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Kotlin 코드 스타일 자동 정리"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + ktlintTargets)
}
