package expo.modules.devmenu.extensions

import android.util.Log
import android.view.KeyEvent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.devsupport.HMRClient
import com.facebook.react.module.annotations.ReactModule
import expo.interfaces.devmenu.DevMenuExtensionInterface
import expo.interfaces.devmenu.DevMenuExtensionSettingsInterface
import expo.interfaces.devmenu.items.DevMenuDataSourceInterface
import expo.interfaces.devmenu.items.DevMenuItemImportance
import expo.interfaces.devmenu.items.DevMenuItemsContainer
import expo.interfaces.devmenu.items.DevMenuScreen
import expo.interfaces.devmenu.items.KeyCommand
import expo.modules.devmenu.DEV_MENU_TAG
import expo.modules.devmenu.DevMenuManager
import expo.modules.devmenu.devtools.DevMenuDevToolsDelegate

@ReactModule(name = "ExpoDevMenuExtensions")
class DevMenuExtension(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), DevMenuExtensionInterface {
  override fun getName() = "ExpoDevMenuExtensions"

  init {
    DevMenuManager.registerExtensionInterface(this)
  }

  override fun devMenuItems(settings: DevMenuExtensionSettingsInterface) = DevMenuItemsContainer.export {
    if (!settings.wasRunOnDevelopmentBridge()) {
      return@export
    }

    val manager = DevMenuManager
    val reactHost = manager.getReactHost()
    if (reactHost == null) {
      Log.w(DEV_MENU_TAG, "Couldn't export dev-menu items, because the react instance isn't present.")
      return@export
    }

    val devDelegate = DevMenuDevToolsDelegate(settings.manager, reactHost)
    val reactDevManager = devDelegate.reactDevManager
    val devSettings = devDelegate.devSettings

    if (reactDevManager == null || devSettings == null) {
      Log.w(DEV_MENU_TAG, "Couldn't export dev-menu items, because react-native bridge doesn't contain the dev support manager.")
      return@export
    }

    action("reload", devDelegate::reload) {
      label = { "Reload" }
      glyphName = { "reload" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_R)
      importance = DevMenuItemImportance.HIGHEST.value
    }

    action(
      "performance-monitor",
      {
        currentActivity?.let {
          devDelegate.togglePerformanceMonitor(it)
        }
      }
    ) {
      isEnabled = { devSettings.isFpsDebugEnabled }
      label = { if (isEnabled()) "Hide Performance Monitor" else "Show Performance Monitor" }
      glyphName = { "speedometer" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_P)
      importance = DevMenuItemImportance.HIGH.value
    }

    action("inspector", devDelegate::toggleElementInspector) {
      isEnabled = { devSettings.isElementInspectorEnabled }
      label = { if (isEnabled()) "Hide Element Inspector" else "Show Element Inspector" }
      glyphName = { "border-style" }
      keyCommand = KeyCommand(KeyEvent.KEYCODE_I)
      importance = DevMenuItemImportance.HIGH.value
    }

    action("remote-debug", devDelegate::toggleRemoteDebugging) {
      isEnabled = {
        devSettings.isRemoteJSDebugEnabled
      }
      label = { if (isEnabled()) "Stop Remote Debugging" else "Debug Remote JS" }
      glyphName = { "remote-desktop" }
      importance = DevMenuItemImportance.LOW.value
    }

    devDelegate.devInternalSettings?.let { devInternalSettings ->
      action("js-inspector", devDelegate::openJSInspector) {
        label = { "Open JavaScript debugger" }
        glyphName = { "language-javascript" }
        importance = DevMenuItemImportance.LOW.value
      }

      val fastRefreshAction = {
        val nextEnabled = !devInternalSettings.isHotModuleReplacementEnabled
        devInternalSettings.isHotModuleReplacementEnabled = nextEnabled

        if (reactApplicationContext != null) {
          if (nextEnabled) {
            reactApplicationContext.getJSModule(HMRClient::class.java).enable()
          } else {
            reactApplicationContext.getJSModule(HMRClient::class.java).disable()
          }
        }
        if (nextEnabled && !devInternalSettings.isJSDevModeEnabled) {
          devInternalSettings.isJSDevModeEnabled = true
          reactDevManager.handleReloadJS()
        }
      }

      action("fast-refresh", fastRefreshAction) {
        isEnabled = { devInternalSettings.isHotModuleReplacementEnabled }
        label = { if (isEnabled()) "Disable Fast Refresh" else "Enable Fast Refresh" }
        glyphName = { "run-fast" }
        importance = DevMenuItemImportance.LOW.value
      }
    }
  }

  override fun devMenuScreens(settings: DevMenuExtensionSettingsInterface): List<DevMenuScreen>? = null

  override fun devMenuDataSources(settings: DevMenuExtensionSettingsInterface): List<DevMenuDataSourceInterface>? = null
}
