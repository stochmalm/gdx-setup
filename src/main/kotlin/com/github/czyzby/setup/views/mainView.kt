package com.github.czyzby.setup.views

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Version
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import com.github.czyzby.autumn.annotation.Destroy
import com.github.czyzby.autumn.annotation.Inject
import com.github.czyzby.autumn.mvc.config.AutumnActionPriority
import com.github.czyzby.autumn.mvc.stereotype.View
import com.github.czyzby.kiwi.util.gdx.preference.ApplicationPreferences
import com.github.czyzby.lml.annotation.LmlAction
import com.github.czyzby.lml.annotation.LmlActor
import com.github.czyzby.lml.annotation.LmlAfter
import com.github.czyzby.lml.annotation.LmlInject
import com.github.czyzby.lml.parser.LmlParser
import com.github.czyzby.lml.parser.action.ActionContainer
import com.github.czyzby.lml.vis.ui.VisFormTable
import com.github.czyzby.setup.data.platforms.Android
import com.github.czyzby.setup.data.project.Project
import com.github.czyzby.setup.prefs.SdkVersionPreference
import com.github.czyzby.setup.prefs.ToolsVersionPreference
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import java.nio.ByteBuffer
import java.nio.DoubleBuffer

/**
 * Main application's view. Displays application's menu.
 * @author MJ
 */
@View(id = "main", value = "templates/main.lml", first = true)
class MainView : ActionContainer {
    @LmlInject private lateinit var basicData: BasicProjectData
    @LmlInject private lateinit var advancedData: AdvancedData
    @LmlInject @Inject private lateinit var platformsData: PlatformsData
    @LmlInject @Inject private lateinit var languagesData: LanguagesData
    @LmlInject @Inject private lateinit var extensionsData: ExtensionsData
    @LmlInject @Inject private lateinit var templatesData: TemplatesData
    @LmlActor("form") private lateinit var form: VisFormTable

    @LmlAction("chooseDirectory")
    fun chooseDirectory(file: FileHandle?) {
        if (file != null) {
            basicData.setDestination(file.path())
        }
    }

    @LmlAction("chooseSdkDirectory")
    fun chooseSdkDirectory(file: FileHandle?) {
        if (file != null) {
            basicData.setAndroidSdkPath(file.path())
        }
    }

    @LmlAction("toggleAndroid")
    fun toggleAndroidPlatform(button: Button) {
        if (button.name == Android.ID) {
            platformsData.toggleAndroidPlatform(button.isChecked)
            revalidateForm()
        }
    }

    @LmlAction("toggleClients") fun toggleClientPlatforms() = platformsData.toggleClientPlatforms()
    @LmlAction("toggleAll") fun toggleAllPlatforms() = platformsData.togglePlatforms()

    @LmlAction("mkdirs") fun createDestinationDirectory() {
        basicData.destination.mkdirs()
        revalidateForm()
    }

    @LmlAction("checkProjectDir")
    fun checkProjectDirectory() {
        basicData.revalidateDirectoryUtilityButtons()
    }

    @LmlAction("reloadSdkButtons")
    fun reloadAndroidSdkButtons() {
        basicData.revalidateSdkUtilityButtons()
    }

    @LmlAction("useLatestSdk")
    fun extractLatestAndroidApiVersions() {
        advancedData.androidSdkVersion = basicData.getLatestAndroidApiVersion().toString()
        advancedData.androidToolsVersion = basicData.getLatestBuildToolsVersion()
    }

    @LmlAction("useOldestSdk")
    fun extractOldestAndroidApiVersions() {
        advancedData.androidSdkVersion = basicData.getOldestAndroidApiVersion().toString()
        advancedData.androidToolsVersion = basicData.getOldestBuildToolsVersion()
    }

    @LmlAfter fun initiateVersions(parser: LmlParser) {
        languagesData.assignVersions(parser)
        extensionsData.assignVersions(parser)
    }

    fun revalidateForm() {
        form.formValidator.validate()
        basicData.revalidateDirectoryUtilityButtons()
        basicData.revalidateSdkUtilityButtons()
    }

    @LmlAction("platforms") fun getPlatforms(): Iterable<*> = platformsData.platforms.keys.sorted()
    @LmlAction("show") fun getTabShowingAction(): Action = Actions.sequence(Actions.alpha(0f), Actions.fadeIn(0.1f))
    @LmlAction("hide") fun getTabHidingAction(): Action = Actions.fadeOut(0.1f)
    @LmlAction("gdxVersion") fun getGdxVersion(): String = Version.VERSION
    @LmlAction("gwtVersions") fun getGwtVersions(): Array<String> = arrayOf("2.6.0", "2.6.1", "2.7.0", "2.8.0-beta1")
    @LmlAction("jvmLanguages") fun getLanguages(): Array<String> = languagesData.languages
    @LmlAction("jvmLanguagesVersions") fun getLanguagesVersions(): Array<String> = languagesData.versions
    @LmlAction("templates") fun getTemplates(): Array<String> = templatesData.templates.map { it.id }.sorted().toTypedArray()

    @LmlAction("officialExtensions") fun getOfficialExtensions(): Array<String> =
            extensionsData.official.map { it.id }.sorted().toTypedArray()

    @LmlAction("officialExtensionsUrls") fun getOfficialExtensionsUrls(): Array<String> =
            extensionsData.official.sortedBy { it.id }.map { it.url }.toTypedArray()

    @LmlAction("thirdPartyExtensions") fun getThirdPartyExtensions(): Array<String> =
            extensionsData.thirdParty.map { it.id }.sorted().toTypedArray()

    @LmlAction("thirdPartyExtensionsVersions") fun getThirdPartyExtensionsVersions(): Array<String> =
            extensionsData.thirdParty.sortedBy { it.id }.map { it.defaultVersion }.toTypedArray()

    @LmlAction("thirdPartyExtensionsUrls") fun getThirdPartyExtensionsUrls(): Array<String> =
            extensionsData.thirdParty.sortedBy { it.id }.map { it.url }.toTypedArray()

    @LmlAction("initTabs") fun initiateTabbedPane(tabbedPane: TabbedPane.TabbedPaneTable) {
        tabbedPane.tabbedPane.tabsPane.horizontalFlowGroup.spacing = 2f
    }

    fun getDestination(): FileHandle = basicData.destination

    fun createProject(): Project = Project(basicData, platformsData.getSelectedPlatforms(),
            advancedData, languagesData, extensionsData, templatesData.getSelectedTemplate())

    @LmlAction("minimize") fun iconify() = GLFW.glfwIconifyWindow(GLFW.glfwGetCurrentContext())

    @LmlAction("initTitleTable")
    fun addWindowDragListener(actor: Actor) {

        actor.addListener(object : DragListener() {
            private val context = GLFW.glfwGetCurrentContext()
            private var startX = 0
            private var startY = 0
            private var offsetX = 0
            private var offsetY = 0
            private val cursorX = BufferUtils.createDoubleBuffer(1)
            private val cursorY = BufferUtils.createDoubleBuffer(1)
            private val windowX = BufferUtils.createIntBuffer(1)
            private val windowY = BufferUtils.createIntBuffer(1)

            override fun dragStart(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                GLFW.glfwGetCursorPos(context, cursorX, cursorY)
                startX = getX()
                startY = getY()
            }

            override fun drag(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                GLFW.glfwGetCursorPos(context, cursorX, cursorY)
                offsetX = getX() - startX
                offsetY = getY() - startY
                GLFW.glfwGetWindowPos(context, windowX, windowY)
                GLFW.glfwSetWindowPos(context, windowX.get(0) + offsetX, windowY.get(0) + offsetY)
            }

            private fun getX(): Int = MathUtils.floor(cursorX.get(0).toFloat())
            private fun getY(): Int = MathUtils.floor(cursorY.get(0).toFloat())
        })
    }

    /**
     * Explicitly forces saving of Android SDK versions. They might not be properly updated as change events are not
     * fired on programmatic SDK and tools versions changes.
     */
    @Destroy(priority = AutumnActionPriority.TOP_PRIORITY)
    fun saveAndroidSdkVersions(api: SdkVersionPreference, tools: ToolsVersionPreference) {
        api.set(advancedData.androidSdkVersion)
        tools.set(advancedData.androidToolsVersion)
    }
}


