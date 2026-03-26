package com.xconst.ethusdt.floatWindows

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    // 直接重写属性并初始化，不要额外定义私有变量
    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}