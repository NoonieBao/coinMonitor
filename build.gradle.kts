//////plugins {
//////    id("com.android.application") version "8.4.2" apply false
//////    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
//////    id("com.android.application") version "8.6.0" apply false
//////    id("com.android.library") version "8.6.0" apply false
//////    kotlin("plugin.serialization") version "1.9.25" apply false
//////}
////
////// 项目级 build.gradle.kts
////plugins {
////    // 删除了之前的 8.4.2，统一使用 8.6.0
////    id("com.android.application") version "8.6.0" apply false
////    id("com.android.library") version "8.6.0" apply false
////
////    // Kotlin 版本保持 1.9.25 是可以的
////    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
////    kotlin("plugin.serialization") version "1.9.25" apply false
////}
//plugins {
//    id("com.android.application") version "8.6.0" apply false
//    id("org.jetbrains.kotlin.android") version "2.0.21" apply false // 👈 升级这里
//    kotlin("plugin.serialization") version "2.0.21" apply false    // 👈 保持同步
//}

plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false

    // 升级 Kotlin 到 2.0.x
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    // ✅ 新增：添加 Compose 编译器插件声明
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}