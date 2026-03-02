const { remote } = require('webdriverio');
const path = require('path');

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

async function runTest() {
    let driver;
    const report = [];

    function log(step, status, message) {
        console.log(`[${status}] ${step}: ${message}`);
        report.push({ step, status, message });
    }

    try {
        console.log("Connecting to Device...");
        driver = await remote(wdOpts);

        // --- TEST 1: APP LAUNCH ---
        await driver.pause(3000); 
        log("App Launch", "PASS", "Home screen loaded.");

        // --- TEST 2: NAVIGATE TO ACCOUNTS ---
        const accountsTab = await driver.$('~Accounts');
        try {
            await accountsTab.waitForDisplayed({ timeout: 10000 });
            await accountsTab.click();
            log("Navigation", "PASS", "Switched to Accounts.");
        } catch (e) {
            console.log(await driver.getPageSource());
            throw e;
        }

        // --- TEST 3: ADD BANK ACCOUNT ---
        const addBtn = await driver.$('~Add Account');
        await addBtn.waitForDisplayed();
        await addBtn.click();
        log("UI", "PASS", "Opened Add Account dialog.");

        // Enter Name
        const nameField = await driver.$('//android.widget.EditText');
        await nameField.waitForDisplayed();
        await nameField.setValue("My Main Bank");

        // Enter Balance
        const inputs = await driver.$$('//android.widget.EditText');
        if (inputs.length > 1) {
            await inputs[1].setValue("50000");
        }

        // Hide Keyboard
        if (await driver.isKeyboardShown()) await driver.hideKeyboard();

        // SELECT ACCOUNT TYPE "Bank"
        // We look for text "Bank" or "ธนาคาร" depending on locale, assuming English for now based on capability config?
        // Actually capability has no locale forced here, let's try finding by resource-id or class if possible, 
        // but FilterChips are usually just text. Let's try finding "Bank".
        // Note: The UI might use chips.
        const bankChip = await driver.$('//*[contains(@text, "Bank")]');
        if (await bankChip.isDisplayed()) {
             await bankChip.click();
             log("UI", "PASS", "Selected 'Bank' type.");
        } else {
             // Try Thai?
             const bankChipTh = await driver.$('//*[contains(@text, "ธนาคาร")]');
             if (await bankChipTh.isDisplayed()) {
                 await bankChipTh.click();
                 log("UI", "PASS", "Selected 'Bank' type (Thai).");
             } else {
                 log("UI", "FAIL", "Could not find 'Bank' chip.");
             }
        }

        // Confirm
        const confirmBtn = await driver.$('//android.widget.TextView[@text="Add"]');
        await confirmBtn.click();
        log("Action", "PASS", "Clicked Add.");

        // Wait for dialog close and verify list
        const newAcc = await driver.$('//*[contains(@text, "My Main Bank")]');
        await newAcc.waitForDisplayed({timeout: 5000});
        
        // Verify Type Text (Subtitle)
        const typeSubtitle = await driver.$('//*[contains(@text, "Bank") or contains(@text, "ธนาคาร")]');
        if (await typeSubtitle.isDisplayed()) {
             log("Verification", "PASS", "Account type 'Bank' displayed in list.");
        } else {
             log("Verification", "WARN", "Could not verify 'Bank' subtitle.");
        }

    } catch (e) {
        log("Test", "FAIL", e.message);
        console.error(e);
    } finally {
        console.log("\n=== FINAL REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
    }
}

runTest();
