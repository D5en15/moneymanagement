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
    'appium:noReset': false, // Keep data
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

async function runPlaythrough() {
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

        // --- 1. APP LAUNCH ---
        try {
            await driver.pause(3000); 
            // Try to find ANY known tab to confirm launch
            const homeTab = await driver.$('~Home'); 
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("App Launch", "PASS", "Home screen loaded.");
            await driver.saveScreenshot('screenshot_1_launch.png');
        } catch (e) {
            log("App Launch", "FAIL", "Home screen not found.");
            await driver.saveScreenshot('error_launch.png');
            throw e;
        }

        // --- 2. ACCOUNTS SCREEN UI ---
        log("Navigation", "INFO", "Switching to Accounts...");
        const accountsTab = await driver.$('~Accounts');
        await accountsTab.click();
        await driver.pause(1000);

        try {
            // Verify Donut
            const netWorth = await driver.$('//*[contains(@text, "Net Worth")]');
            await netWorth.waitForDisplayed();
            log("Accounts UI", "PASS", "Donut chart 'Net Worth' found.");
            
            await driver.saveScreenshot('screenshot_2_accounts_ui.png');
        } catch (e) {
            log("Accounts UI", "FAIL", "Accounts UI incomplete: " + e.message);
        }

        // --- 3. CREATE ACCOUNTS ---
        const accountsToCreate = [
            { name: "My Bank", balance: "15000" },
            { name: "Cash", balance: "500" }
        ];

        for (const acc of accountsToCreate) {
            try {
                let addBtn = await driver.$('~Add Account');
                // Fallback if icon content-desc isn't found
                if (!await addBtn.isDisplayed()) addBtn = await driver.$('//*[contains(@text, "Add")]');
                
                await addBtn.click();
                await driver.pause(1000);

                // Assuming first EditText is Name, second is Balance
                const nameField = await driver.$('//android.widget.EditText');
                await nameField.setValue(acc.name);
                
                const inputs = await driver.$$('//android.widget.EditText');
                if(inputs.length > 1) await inputs[1].setValue(acc.balance);

                // Hide Keyboard
                if (await driver.isKeyboardShown()) await driver.hideKeyboard();

                // Click Add (Dialog button)
                const confirmBtn = await driver.$('//*[contains(@text, "Add") and not(@content-desc="Add Account")]');
                await confirmBtn.click();
                await driver.pause(1000);

                log("Data Entry", "PASS", `Account '${acc.name}' created.`);
            } catch (e) {
                log("Data Entry", "FAIL", `Failed to create account '${acc.name}': ${e.message}`);
            }
        }
        await driver.saveScreenshot('screenshot_3_accounts_populated.png');

        // --- 4. CREATE TRANSACTIONS ---
        log("Navigation", "INFO", "Switching to Home...");
        const homeTab = await driver.$('~Home');
        await homeTab.click();
        await driver.pause(1000);

        try {
            const fab = await driver.$('~Add Transaction');
            await fab.click();
            await driver.pause(1000);

            const amtField = await driver.$('//android.widget.EditText');
            await amtField.setValue("120");

            if (await driver.isKeyboardShown()) await driver.hideKeyboard();

            const saveBtn = await driver.$('~Save');
            await saveBtn.click();
            await driver.pause(1000);

            log("Data Entry", "PASS", "Transaction added.");
            await driver.saveScreenshot('screenshot_4_transaction_added.png');
        } catch (e) {
            log("Data Entry", "FAIL", "Transaction add failed: " + e.message);
        }

        // --- 5. SETTINGS CHECK ---
        log("Navigation", "INFO", "Checking Settings...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();
        await driver.pause(1000);

        try {
            const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
            if (await accMgmt.isDisplayed()) {
                log("Refactoring", "FAIL", "Account Management IS visible in Settings.");
            }
        } catch (e) {
            log("Refactoring", "PASS", "Account Management is correctly NOT found.");
        }
        await driver.saveScreenshot('screenshot_5_settings.png');

    } catch (mainError) {
        console.error("Critical Test Error:", mainError);
        // Try to save error screenshot
        try { await driver.saveScreenshot('error_critical.png'); } catch {}
    } finally {
        console.log("\n=== TEST REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runPlaythrough();
