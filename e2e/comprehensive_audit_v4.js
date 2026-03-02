const { remote } = require('webdriverio');
const { main } = require('appium');
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

process.env.JAVA_HOME = "C:\\Program Files\\Android\\Android Studio\\jbr";
process.env.PATH = process.env.PATH + ";" + process.env.JAVA_HOME + "\\bin";

async function runAudit() {
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

        // --- TEST 1: APP LAUNCH ---
        try {
            await driver.pause(3000); 
            const homeTab = await driver.$('~Home'); 
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("App Launch", "PASS", "Home screen loaded.");
        } catch (e) {
            log("App Launch", "FAIL", "Home screen not found.");
            throw e;
        }

        // --- TEST 2: NAVIGATION & ACCOUNTS UI ---
        log("Navigation", "INFO", "Switching to Accounts...");
        const accountsTab = await driver.$('~Accounts');
        await accountsTab.click();

        try {
            const header = await driver.$('//*[contains(@text, "Accounts")]');
            await header.waitForDisplayed();
            log("Accounts UI", "PASS", "Header found.");
        } catch (e) {
            log("Accounts UI", "FAIL", "Header missing.");
        }

        try {
            const netWorth = await driver.$('//*[contains(@text, "Net Worth")]');
            await netWorth.waitForDisplayed();
            log("Accounts UI", "PASS", "Donut widget found.");
        } catch (e) {
            log("Accounts UI", "FAIL", "Donut missing.");
        }

        // --- TEST 3: ADD ACCOUNT ---
        const NEW_ACC_NAME = "Audit Bank";
        try {
            let addBtn = await driver.$('~Add Account');
            if (!await addBtn.isDisplayed()) addBtn = await driver.$('//*[contains(@text, "Add")]');
            await addBtn.click();
            
            const nameField = await driver.$('//android.widget.EditText');
            await nameField.waitForDisplayed();
            await nameField.setValue(NEW_ACC_NAME);
            
            const inputs = await driver.$$('//android.widget.EditText');
            if (inputs.length > 1) {
                await inputs[1].setValue("5000");
            }

            // Ensure keyboard is closed before clicking Add
            if (await driver.isKeyboardShown()) await driver.hideKeyboard();

            const confirmBtn = await driver.$('//*[contains(@text, "Add") and not(@content-desc="Add Account")]');
            await confirmBtn.click();
            
            const newAcc = await driver.$(`//*[contains(@text, "${NEW_ACC_NAME}")]`);
            await newAcc.waitForDisplayed({ timeout: 5000 });
            log("Feature: Add Account", "PASS", "Account added.");
        } catch (e) {
            log("Feature: Add Account", "FAIL", "Add Account failed: " + e.message);
        }

        // --- TEST 4: SETTINGS & CLEANUP ---
        log("Navigation", "INFO", "Switching to Settings...");
        
        // Ensure keyboard is closed before navigation
        if (await driver.isKeyboardShown()) await driver.hideKeyboard();
        
        const settingsTab = await driver.$('~Settings');
        await settingsTab.waitForDisplayed();
        await settingsTab.click();

        try {
            const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
            await accMgmt.waitForDisplayed({ timeout: 2000 });
            log("Refactoring", "FAIL", "CRITICAL: Account Management still in Settings!");
        } catch (e) {
            log("Refactoring", "PASS", "Account Management removed from Settings.");
        }

    } catch (mainError) {
        console.error("Critical Test Error:", mainError);
    } finally {
        console.log("\n=== FINAL REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runAudit();
