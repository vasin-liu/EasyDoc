import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "org.gensokyo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://172.25.20.192:8081/nexus/content/repositories/releases/")
    }
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://172.25.20.192:8081/nexus/content/repositories/thirdparty/")
    }
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://172.25.20.192:8081/nexus/content/repositories/snapshots/")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {

    pluginName.set("EasyDoc")

    // 沙箱目录位置，用于保存IDEA的设置，默认在build文件下面，防止clean，放在根目录下。
    sandboxDir.set("${rootProject.rootDir}/idea-sandbox")

    version.set("2023.2.5")

    type.set("IU") // Target IDE Platform

    plugins.set(listOf("com.intellij.java", "com.intellij.database"))

    updateSinceUntilBuild.set(false)

    downloadSources.set(true)
}

tasks {
    //编译设置
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    //兼容版本
    patchPluginXml {
        //please see http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html for description
        sinceBuild.set("232")
        //untilBuild.set("242.*")
    }

    //签名
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    //发布
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    //调试
    runIde {
        jvmArgs = listOf(
            "-Xmx512m",
            "-Xms256m",
            "-XX:ReservedCodeCacheSize=512m",
            "-XX:+IgnoreUnrecognizedVMOptions",
            "-XX:+UseG1GC",
            "-XX:SoftRefLRUPolicyMSPerMB=50",
            "-XX:CICompilerCount=2",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:-OmitStackTraceInFastThrow",
            "-ea",
            "-Dsun.io.useCanonCaches=false",
            "-Djdk.attach.allowAttachSelf=true",
            "-Djdk.module.illegalAccess.silent=true",
            "-XX:HeapDumpPath=./idea-sandbox/system/log/heapdump.hprof",
            //注册
            "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
            "-javaagent:D:\\Software\\JetBrains\\jetbra\\ja-netfilter.jar=jetbrains"
        )
    }
}

dependencies {
//    implementation("org.gensokyo:kits:1.0.0")
    implementation("com.deepoove:poi-tl:1.12.2") {
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
        //跟IDEA冲突
        exclude(group= "xerces", module= "xercesImpl")
        exclude(group= "xml-apis", module= "xml-apis")
    }

    implementation("org.springframework:spring-expression:5.3.20")

//    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}
