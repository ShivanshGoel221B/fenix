/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.FeatureSettingsHelper
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.deleteDownloadFromStorage
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.downloadRobot
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.notificationShade

/**
 *  Tests for verifying basic functionality of download
 *
 *  - Initiates a download
 *  - Verifies download prompt
 *  - Verifies download notification
 *  - Verifies various file types downloads and their appearance inside the Downloads list.
 **/

class DownloadTest {
    private lateinit var mockWebServer: MockWebServer
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val featureSettingsHelper = FeatureSettingsHelper()
    /* Remote test page managed by Mozilla Mobile QA team at https://github.com/mozilla-mobile/testapp */
    private val testPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
    private var fileName: String = ""

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @get:Rule
    var mGrantPermissions = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
        // disabling the jump-back-in pop-up that interferes with the tests.
        featureSettingsHelper.setJumpBackCFREnabled(false)
        // disabling the PWA CFR on 3rd visit
        featureSettingsHelper.disablePwaCFR(true)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        deleteDownloadFromStorage(fileName)
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Test
    fun testDownloadPrompt() {
        fileName = "web_icon.png"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.clickOpen("image/png") {}
        downloadRobot {
            verifyPhotosAppOpens()
        }
    }

    @Test
    fun testCloseDownloadPrompt() {
        fileName = "smallZip.zip"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
        }
    }

    @Test
    fun testDownloadCompleteNotification() {
        fileName = "smallZip.zip"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Download completed")
        }
    }

    @SmokeTest
    @Test
    fun pauseResumeCancelDownloadTest() {
        fileName = "1GB.zip"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {}
        mDevice.openNotification()
        notificationShade {
            expandNotificationMessage()
            clickSystemNotificationControlButton("Pause")
            clickSystemNotificationControlButton("Resume")
            clickSystemNotificationControlButton("Cancel")
            mDevice.pressBack()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
        }
    }

    @SmokeTest
    @Test
        /* Verifies downloads in the Downloads Menu:
          - downloads appear in the list
          - deleting a download from device storage, removes it from the Downloads Menu too
        */
    fun manageDownloadsInDownloadsMenuTest() {
        /* test page that downloads automatically a SVG file
        - we need this to prevent opening the image in the browser
        - the image has a long filename to verify it is displayed correctly on the prompt
         */
        val downloadWebPage = TestAssetHelper.getDownloadAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(downloadWebPage.url) {
            mDevice.waitForIdle()
        }
        downloadRobot {
            verifyDownloadPrompt(TestAssetHelper.downloadFileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(TestAssetHelper.downloadFileName)
            verifyDownloadedFileIcon()
            openDownloadedFile(TestAssetHelper.downloadFileName)
            verifyPhotosAppOpens()
            mDevice.pressBack()
            deleteDownloadFromStorage(TestAssetHelper.downloadFileName)
        }.exitDownloadsManagerToBrowser {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
        }
    }

    @SmokeTest
    @Test
    fun downloadPDFTypeTest() {
        fileName = "washington.pdf"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadMP3TypeTest() {
        fileName = "audioSample.mp3"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadDocXTypeTest() {
        fileName = "MyDocument.docx"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadTxtTypeTest() {
        fileName = "textfile.txt"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadPNGTypeTest() {
        fileName = "web_icon.png"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadWebmTypeTest() {
        fileName = "videoSample.webm"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadCSVTypeTest() {
        fileName = "CSVfile.csv"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }

    @SmokeTest
    @Test
    fun downloadXMLTypeTest() {
        fileName = "XMLfile.xml"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testPage.toUri()) {
        }.clickDownloadLink(fileName) {
            verifyDownloadPrompt(fileName)
        }.clickDownload {
            verifyDownloadNotificationPopup()
        }.closePrompt {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName(fileName)
            verifyDownloadedFileIcon()
        }
    }
}
