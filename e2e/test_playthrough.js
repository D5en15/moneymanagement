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
    'appium:newCommandTimeout': 3600,
    'appium:connectHardwareKeyboard': true
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

        // --- 1. APP LAUNCH & THEME SETUP ---
        try {
            await driver.pause(3000); 
            // Attempt to switch to Light mode first via App Settings if possible, or assume defaults.
            // For now, let's just verify we are on Home.
            // Using accessibility id "~Home" or text match
            const homeTab = await driver.$('~Home'); 
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("App Launch", "PASS", "Home screen loaded.");
            
            await driver.saveScreenshot('./e2e/screenshot_launch.png');
        } catch (e) {
            log("App Launch", "FAIL", "Home screen not found.");
            await driver.saveScreenshot('./e2e/error_launch.png');
            throw e;
        }

        // --- 2. ACCOUNTS SCREEN UI CHECK ---
        log("Navigation", "INFO", "Switching to Accounts...");
        const accountsTab = await driver.$('~Accounts');
        await accountsTab.click();
        await driver.pause(1000);

        try {
            const header = await driver.$('//*[contains(@text, "Accounts")]');
            await header.waitForDisplayed();
            log("Accounts UI", "PASS", "Header 'Accounts' found.");
            
            const netWorth = await driver.$('//*[contains(@text, "Net Worth")]');
            await netWorth.waitForDisplayed();
            log("Accounts UI", "PASS", "Donut chart 'Net Worth' found.");
            
            const addBtn = await driver.$('~Add Account'); // Icon
            await addBtn.waitForDisplayed();
            log("Accounts UI", "PASS", "Add Account button found.");

            await driver.saveScreenshot('./e2e/screenshot_accounts_empty.png');
        } catch (e) {
            log("Accounts UI", "FAIL", "Accounts UI elements missing: " + e.message);
        }

        // --- 3. CREATE ACCOUNTS ---
        const accountsToCreate = [
            { name: "Main Bank", balance: "15000" },
            { name: "Cash Wallet", balance: "2000" },
            { name: "Crypto Stash", balance: "50000" }
        ];

        for (const acc of accountsToCreate) {
            try {
                let addBtn = await driver.$('~Add Account');
                await addBtn.click();
                await driver.pause(500);

                const nameField = await driver.$('//android.widget.EditText');
                await nameField.setValue(acc.name);
                
                const inputs = await driver.$$('//android.widget.EditText');
                if(inputs.length > 1) await inputs[1].setValue(acc.balance);

                if (await driver.isKeyboardShown()) await driver.hideKeyboard();

                const confirmBtn = await driver.$('//*[contains(@text, "Add") and not(@content-desc="Add Account")]');
                await confirmBtn.click();
                await driver.pause(1000); // Wait for list update

                log("Data Entry", "PASS", `Account '${acc.name}' created.`);
            } catch (e) {
                log("Data Entry", "FAIL", `Failed to create account '${acc.name}': ${e.message}`);
            }
        }
        await driver.saveScreenshot('./e2e/screenshot_accounts_populated.png');

        // --- 4. CREATE TRANSACTIONS ---
        log("Navigation", "INFO", "Switching to Home for Transactions...");
        const homeTab = await driver.$('~Home');
        await homeTab.click();
        await driver.pause(1000);

        const transactions = [
            { amount: "500", note: "Groceries", type: "Expense" },
            { amount: "2000", note: "Freelance", type: "Income" },
            { amount: "150", note: "Coffee", type: "Expense" }
        ];

        for (const tx of transactions) {
            try {
                // Find FAB
                const fab = await driver.$('~Add Transaction');
                await fab.click();
                await driver.pause(500);

                // Select Type (Tabs)
                if (tx.type === "Income") {
                    const incomeTab = await driver.$('//*[contains(@text, "Income")]');
                    await incomeTab.click();
                } else {
                    const expenseTab = await driver.$('//*[contains(@text, "Expense")]');
                    await expenseTab.click();
                }

                // Amount (First EditText usually)
                const amtField = await driver.$('//android.widget.EditText');
                await amtField.setValue(tx.amount);

                // Note (Second EditText usually)
                const inputs = await driver.$$('//android.widget.EditText');
                if(inputs.length > 1) await inputs[1].setValue(tx.note);

                // Account Selection (Optional - skip for speed, use default)
                // Category Selection (Optional - skip for speed, use default)

                if (await driver.isKeyboardShown()) await driver.hideKeyboard();

                const saveBtn = await driver.$('~Save');
                await saveBtn.click();
                await driver.pause(1000); // Wait for save

                log("Data Entry", "PASS", `Transaction '${tx.note}' added.`);
            } catch (e) {
                log("Data Entry", "FAIL", `Failed to add transaction '${tx.note}': ${e.message}`);
            }
        }
        await driver.saveScreenshot('./e2e/screenshot_home_transactions.png');

        // --- 5. SETTINGS CHECK ---
        log("Navigation", "INFO", "Checking Settings...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();
        await driver.pause(1000);

        try {
            const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
            if (await accMgmt.isDisplayed()) {
                log("Refactoring", "FAIL", "Account Management found in Settings (Should be gone).");
            }
        } catch (e) {
            log("Refactoring", "PASS", "Account Management correctly absent from Settings.");
        }
        await driver.saveScreenshot('./e2e/screenshot_settings.png');

        // --- 6. THEME SWITCH CHECK (Optional) ---
        // Try to toggle theme via Settings -> Appearance -> Dark
        try {
            const appearance = await driver.$('//*[contains(@text, "Appearance")]');
            await appearance.click();
            await driver.pause(500);
            
            const darkOption = await driver.$('//*[contains(@text, "Dark Mode")]');
            await darkOption.click();
            await driver.pause(1000);
            
            await driver.saveScreenshot('./e2e/screenshot_dark_mode.png');
            log("Theme", "PASS", "Switched to Dark Mode.");
            
            // Go back to Accounts to verify Dark Mode look
            await accountsTab.click();
            await driver.pause(1000);
            await driver.saveScreenshot('./e2e/screenshot_accounts_dark.png');
            
        } catch (e) {
            log("Theme", "WARN", "Could not automated theme switch: " + e.message);
        }

    } catch (mainError) {
        console.error("Critical Test Error:", mainError);
        await driver.saveScreenshot('./e2e/error_critical.png');
    } finally {
        console.log("\n=== TEST REPORT ===");
        report.forEach(r => console.log(`[${r.status}] ${r.step}: ${r.message}`));
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runPlaythrough();
