const { remote } = require('webdriverio');
const assert = require('assert');

const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': 'CPH2203',
    'appium:appPackage': 'com.example.moneymanager',
    'appium:appActivity': '.MainActivity',
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'info',
    capabilities,
};

async function runTest() {
    const driver = await remote(wdOpts);
    try {
        // 1. Navigate to Calendar screen
        await driver.pause(2000); // Wait for app to load
        const calendarTab = await driver.$('//android.widget.Button[2]');
        await calendarTab.click();
        await driver.pause(2000); // Wait for calendar to load

        // 2. Long-press on a day (e.g., the 15th)
        // Note: Finding elements by text in Jetpack Compose can be tricky.
        // We'll use a more robust XPath based on content-desc if available,
        // or fall back to text. Let's assume there's a day "15" visible.
        const dayToPress = await driver.$('//android.widget.TextView[@text="15"]');
        
        // WebdriverIO's long press is `touchAction` with `longPress`.
        await driver.touchAction({
            action: 'longPress',
            element: dayToPress
        });

        await driver.pause(2000); // Wait for navigation

        // 3. Verify that the "Add Transaction" screen opens
        const addTransactionTitle = await driver.$('//android.widget.TextView[contains(@text, "Expense")]');
        const isTitleDisplayed = await addTransactionTitle.isDisplayed();
        assert.strictEqual(isTitleDisplayed, true, 'Add Transaction screen did not open.');

        // 4. Verify the date
        const dateField = await driver.$('//android.widget.TextView[contains(@text, "15")]');
        const isDateCorrect = await dateField.isDisplayed();
        assert.strictEqual(isDateCorrect, true, 'The date was not pre-filled correctly.');

        console.log('Test Passed!');

    } finally {
        await driver.deleteSession();
    }
}

runTest().catch(console.error);
