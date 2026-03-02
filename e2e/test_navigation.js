const { remote } = require('webdriverio');
const { main } = require('appium');
const path = require('path');

const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': 'Android',
    'appium:app': path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk'),
    'appium:noReset': false,
    'appium:autoGrantPermissions': true,
    'appium:fullReset': false
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'error',
    capabilities,
};

async function runTest() {
    let driver;
    try {
        console.log("Connecting to Appium...");
        driver = await remote(wdOpts);

        // 1. Verify Home Screen (Center Tab) is active
        console.log("Checking Home Screen...");
        const homeHeader = await driver.$('//*[contains(@text, "Balance")]'); 
        // Note: Our timeline header has "Month Balance" text.
        await homeHeader.waitForDisplayed({ timeout: 10000 });
        console.log("Home Screen (Timeline) verified.");

        // 2. Navigate to Calendar (Left Tab)
        console.log("Navigating to Calendar...");
        // Index 0 is Calendar. We can find by content description "Calendar"
        const calendarTab = await driver.$('~Calendar');
        await calendarTab.click();
        
        // Verify Calendar Header (Month Year)
        const calendarTitle = await driver.$('//android.widget.TextView[contains(@text, "202")]'); // Matches 2025, 2026
        await calendarTitle.waitForDisplayed({ timeout: 5000 });
        console.log("Calendar Screen verified.");

        // 3. Navigate to Settings (Right Tab)
        console.log("Navigating to Settings...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();

        // Verify Settings Content
        const catMgmt = await driver.$('//*[contains(@text, "Category Management")]');
        await catMgmt.waitForDisplayed({ timeout: 5000 });
        console.log("Settings Screen verified.");

        // 4. Navigate back to Home
        console.log("Navigating back to Home...");
        const homeTab = await driver.$('~Home');
        await homeTab.click();
        await homeHeader.waitForDisplayed({ timeout: 5000 });

        console.log("SUCCESS: Navigation structure verified!");

    } catch (err) {
        console.error("Test Failed:", err);
    } finally {
        if (driver) await driver.deleteSession();
    }
}

runTest();
