// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}

// ktlint: Kotlin 코드 스타일 검사/자동 정리 (규칙은 .editorconfig)
val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
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
