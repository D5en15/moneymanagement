const { remote } = require('webdriverio');
const { main } = require('appium');
const path = require('path');

// --- CONFIGURATION ---
const DEVICE_NAME = 'CPH2203';
const UDID = 'SKJF5HLVYSDE5DNF';
const APP_PATH = path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk');

const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': DEVICE_NAME,
    'appium:udid': UDID,
    'appium:app': APP_PATH,
    'appium:noReset': false, 
    'appium:fullReset': false,
    'appium:autoGrantPermissions': true,
    'appium:newCommandTimeout': 3600
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'error',
    capabilities,
};

process.env.JAVA_HOME = "C:\\Program Files\\Android\\Android Studio\\jbr";
process.env.PATH = process.env.PATH + ";" + process.env.JAVA_HOME + "\\bin";

async function runLanguageTest() {
    let server;
    let driver;
    const report = [];

    function log(step, status, message) {
        console.log(`[${status}] ${step}: ${message}`);
        report.push({ step, status, message });
    }

    try {
        console.log("Initializing Appium Server...");
        server = await main({ port: 4723, loglevel: 'error', relaxedSecurity: true });
        
        console.log("Connecting to Device...");
        driver = await remote(wdOpts);

        // 1. Launch & Check Default Language (English)
        try {
            await driver.pause(3000);
            const homeTab = await driver.$('~Home'); // English Accessibility Label
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("Initial State", "PASS", "App started in English (Home tab found).");
        } catch (e) {
            log("Initial State", "FAIL", "App did not start in English or Home tab not found.");
            throw e;
        }

        // 2. Go to Settings
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();
        await driver.pause(1000);

        // 3. Open Language Dialog
        // Text: "Language"
        const langOption = await driver.$('//*[contains(@text, "Language")]');
        await langOption.click();
        await driver.pause(1000);

        // 4. Select Thai
        // Text: "ไทย"
        const thaiOption = await driver.$('//*[contains(@text, "ไทย")]');
        await thaiOption.click();
        
        // Wait for Recreate
        await driver.pause(3000);

        // 5. Verify Change
        // "Settings" should now be "ตั้งค่า" (from strings.xml: nav_settings)
        // "Home" should be "หน้าหลัก" (from strings.xml: nav_home)
        // Let's check the bottom bar tab for Home
        // Note: Accessibility ID might still be "Home" if it's hardcoded in the Icon vector contentDescription?
        // Let's check MoneyManagerDestination.kt
        
        // In MoneyManagerDestination.kt:
        // title = R.string.nav_home
        // The contentDescription uses stringResource(screen.title).
        // So the Accessibility ID should change to "หน้าหลัก".
        
        try {
            const homeTabThai = await driver.$('~หน้าหลัก'); 
            await homeTabThai.waitForDisplayed({ timeout: 10000 });
            log("Language Switch (TH)", "PASS", "Home tab content description changed to 'หน้าหลัก'.");
        } catch (e) {
            log("Language Switch (TH)", "FAIL", "Home tab 'หน้าหลัก' not found. It might still be 'Home'?");
            // Fallback check by text
            const homeText = await driver.$('//*[contains(@text, "หน้าหลัก")]');
            if (await homeText.isDisplayed()) {
                 log("Language Switch (TH)", "PASS", "Found 'หน้าหลัก' text.");
            } else {
                 throw new Error("Localization failed.");
            }
        }

        // 6. Switch Back to English
        // Go to Settings tab (now "ตั้งค่า")
        const settingsTabThai = await driver.$('~ตั้งค่า');
        await settingsTabThai.click();
        await driver.pause(1000);

        // "Language" -> "ภาษา"
        const langOptionThai = await driver.$('//*[contains(@text, "ภาษา")]');
        await langOptionThai.click();
        await driver.pause(1000);

        // Select "English"
        const engOption = await driver.$('//*[contains(@text, "English")]');
        await engOption.click();
        await driver.pause(3000);

        // Verify "Home" again
        const homeTabEng = await driver.$('~Home');
        await homeTabEng.waitForDisplayed({ timeout: 10000 });
        log("Language Switch (EN)", "PASS", "Restored to English.");

    } catch (mainError) {
        console.error("Critical Test Error:", mainError);
        await driver.saveScreenshot('error_lang_test.png');
    } finally {
        console.log("\n=== TEST REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runLanguageTest();
