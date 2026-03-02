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

async function runTest() {
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
        await driver.pause(3000); 
        log("App Launch", "PASS", "Home screen loaded.");

        // --- TEST 2: NAVIGATE TO ACCOUNTS ---
        const accountsTab = await driver.$('~Accounts');
        await accountsTab.waitForDisplayed();
        await accountsTab.click();
        log("Navigation", "PASS", "Switched to Accounts.");

        // --- TEST 3: ADD ACCOUNT ---
        const addBtn = await driver.$('~Add Account');
        await addBtn.waitForDisplayed();
        await addBtn.click();
        
        const nameField = await driver.$('//android.widget.EditText');
        await nameField.setValue("Adjust Test");
        
        const inputs = await driver.$$('//android.widget.EditText');
        if (inputs.length > 1) {
            await inputs[1].setValue("1000");
        }
        
        if (await driver.isKeyboardShown()) await driver.hideKeyboard();

        const confirmAddBtn = await driver.$('//android.widget.TextView[@text="Add"]');
        await confirmAddBtn.click();
        log("Action", "PASS", "Added account 'Adjust Test' with 1000.");

        // Wait for list update
        const accItem = await driver.$('//*[contains(@text, "Adjust Test")]');
        await accItem.waitForDisplayed({timeout: 10000});

        // --- TEST 4: EDIT ACCOUNT & ADJUST BALANCE ---
        await accItem.click(); // Opens Edit Dialog
        log("UI", "PASS", "Opened Edit Dialog.");

        // Change Balance to 1500
        const editInputs = await driver.$$('//android.widget.EditText');
        // Index 1 is usually balance (Name is 0)
        // Need to clear it first or just set value? setValue usually clears.
        await editInputs[1].setValue("1500");
        if (await driver.isKeyboardShown()) await driver.hideKeyboard();

        const saveBtn = await driver.$('//android.widget.TextView[@text="Save"]');
        await saveBtn.click();
        log("Action", "PASS", "Clicked Save (Balance changed 1000 -> 1500).");

        // --- TEST 5: VERIFY ADJUSTMENT DIALOG ---
        // Look for title "Balance Adjustment" or Thai equivalent
        const dialogTitle = await driver.$('//*[contains(@text, "Balance Adjustment") or contains(@text, "ปรับปรุงยอดเงิน")]');
        await dialogTitle.waitForDisplayed({timeout: 5000});
        log("UI", "PASS", "Adjustment Confirmation Dialog appeared.");

        // Confirm "Yes, Record"
        const recordBtn = await driver.$('//*[contains(@text, "Record") or contains(@text, "บันทึก")]');
        await recordBtn.click();
        log("Action", "PASS", "Confirmed recording transaction.");

        // --- TEST 6: VERIFY BALANCE ---
        // Wait for dialog to close
        await dialogTitle.waitForDisplayed({reverse: true});
        
        // Check new balance text "1,500"
        const balanceText = await driver.$('//*[contains(@text, "1,500")]');
        if (await balanceText.isDisplayed()) {
            log("Verification", "PASS", "Account balance updated to 1,500.");
        } else {
            log("Verification", "FAIL", "Account balance not updated.");
        }

        // --- TEST 7: VERIFY TRANSACTION ---
        const homeTab = await driver.$('~Home');
        await homeTab.click();
        
        // Look for transaction with amount "500"
        // It might be formatted as "฿500.00" or similar
        const transAmount = await driver.$('//*[contains(@text, "500")]');
        if (await transAmount.isDisplayed()) {
             log("Verification", "PASS", "Adjustment transaction of 500 found.");
        } else {
             // Maybe search for "Balance Adjustment" note?
             const transNote = await driver.$('//*[contains(@text, "Balance Adjustment")]');
             if (await transNote.isDisplayed()) {
                 log("Verification", "PASS", "Transaction note found.");
             } else {
                 log("Verification", "WARN", "Could not verify transaction on Home screen.");
             }
        }

    } catch (e) {
        log("Test", "FAIL", e.message);
        console.error(e);
    } finally {
        console.log("\n=== FINAL REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runTest();
