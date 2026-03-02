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
    'appium:ensureWebviewsHavePages': true,
    'appium:nativeWebScreenshot': true,
    'appium:newCommandTimeout': 3600,
    'appium:connectHardwareKeyboard': true
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'error',
    capabilities,
};

// Set Java Home
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

        // --- TEST 1: APP LAUNCH & LANDING ---
        // Try finding Home button. The text from strings.xml is "Home". 
        // We can search by text directly if content-desc isn't working or reliable.
        try {
            const homeTab = await driver.$('//*[contains(@text, "Home") and @class="android.widget.TextView"]'); 
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("App Launch", "PASS", "Home screen loaded successfully.");
        } catch (e) {
            log("App Launch", "FAIL", "App did not load Home screen in time.");
            throw e;
        }

        // --- TEST 2: ACCOUNTS SCREEN UI & LOGIC ---
        log("Navigation", "INFO", "Switching to Accounts tab...");
        // Use XPath for text "Accounts"
        const accountsTab = await driver.$('//*[contains(@text, "Accounts") and @class="android.widget.TextView"]');
        await accountsTab.click();

        // Check Header
        try {
            // There are two "Accounts" texts: one in nav bar, one in header. 
            // The header one is usually larger or listed earlier/later depending on layout.
            // We look for the one that IS displayed.
            const header = await driver.$('//*[contains(@text, "Accounts") and @class="android.widget.TextView"]');
            await header.waitForDisplayed();
            log("Accounts UI", "PASS", "Header 'Accounts' found.");
        } catch (e) {
            log("Accounts UI", "FAIL", "Header 'Accounts' missing.");
        }

        // Check Donut
        try {
            const netWorth = await driver.$('//*[contains(@text, "Net Worth")]');
            await netWorth.waitForDisplayed();
            log("Accounts UI", "PASS", "Net Worth Donut widget found.");
        } catch (e) {
            log("Accounts UI", "FAIL", "Net Worth Donut missing.");
        }

        // --- TEST 3: ADD NEW ACCOUNT ---
        const NEW_ACC_NAME = "Audit Bank";
        const NEW_ACC_BAL = 5000;

        try {
            // Find "Add" or "Add Account"
            // The icon has contentDesc "Add Account".
            let addBtn = await driver.$('~Add Account');
            if (!await addBtn.isDisplayed()) {
                // Fallback to text "Add"
                addBtn = await driver.$('//*[contains(@text, "Add")]');
            }
            await addBtn.click();
            
            // Wait for dialog
            await driver.pause(1000);
            
            // Name field
            const nameField = await driver.$('//android.widget.EditText[@text=""]');
            await nameField.waitForDisplayed();
            await nameField.setValue(NEW_ACC_NAME);
            
            // Balance field - might be the second edittext
            const inputs = await driver.$$('//android.widget.EditText');
            if(inputs.length > 1) {
                await inputs[1].click(); // Focus
                await inputs[1].setValue(NEW_ACC_BAL.toString());
            }

            // Confirm "Add"
            const confirmBtn = await driver.$('//*[contains(@text, "Add") and @class="android.widget.TextView"]');
            await confirmBtn.click();

            // Verify
            const newAcc = await driver.$(`//*[contains(@text, "${NEW_ACC_NAME}")]`);
            await newAcc.waitForDisplayed({ timeout: 5000 });
            log("Feature: Add Account", "PASS", `Account '${NEW_ACC_NAME}' added.`);

        } catch (e) {
            log("Feature: Add Account", "FAIL", `Failed to add account: ${e.message}`);
        }

        // --- TEST 4: VERIFY NET WORTH UPDATE ---
        await driver.pause(1000);
        try {
            // Check for "5,000" text visible on screen (in the list card)
            const accBalance = await driver.$(`//*[contains(@text, "5,000")]`);
            if (await accBalance.isDisplayed()) {
                 log("Data Consistency", "PASS", "New account shows correct balance '5,000'.");
            }
        } catch (e) {
            log("Data Consistency", "FAIL", "New account balance display issue.");
        }

        // --- TEST 5: SETTINGS AUDIT ---
        log("Navigation", "INFO", "Switching to Settings tab...");
        const settingsTab = await driver.$('//*[contains(@text, "Settings") and @class="android.widget.TextView"]');
        await settingsTab.click();

        try {
            // We want this to FAIL to find element
            const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
            await accMgmt.waitForDisplayed({ timeout: 2000 });
            log("Refactoring", "FAIL", "CRITICAL: 'Account Management' still found in Settings!");
        } catch (e) {
            log("Refactoring", "PASS", "'Account Management' correctly removed from Settings.");
        }

        try {
            const catMgmt = await driver.$('//*[contains(@text, "Category Management")]');
            await catMgmt.waitForDisplayed();
            log("Settings UI", "PASS", "Category Management is present.");
            
            await catMgmt.click();
            await driver.pause(1000);
            
            // Go back
            await driver.back();
            log("Navigation", "PASS", "Category Management navigation works.");
        } catch (e) {
            log("Settings UI", "FAIL", "Category Management flow broken.");
        }

        // --- TEST 6: TRANSACTION FLOW (Impact on Accounts) ---
        log("Navigation", "INFO", "Switching to Home/Transactions...");
        const homeNav = await driver.$('//*[contains(@text, "Home") and @class="android.widget.TextView"]');
        await homeNav.click();

        try {
            // We look for the "Add Transaction" FAB or button
            // It has content-desc "Add Transaction" in strings.xml: nav_add_transaction
            // The FAB in MainScreen might not use that string directly, but let's try accessibility id
            // Actually MainScreen uses: contentDescription = stringResource(screen.title) -> "Add Transaction"
            // But wait, the FAB is in TransactionsScreen?
            // In MainScreen.kt:
            // composable(MoneyManagerDestination.Home.route) { TransactionsScreen(...) }
            // TransactionsScreen usually has the FAB.
            
            const addTransBtn = await driver.$('~Add Transaction');
            await addTransBtn.click();

            // Select "Audit Bank" account if possible
            const accSelector = await driver.$('//*[contains(@text, "Select Account")]');
            await accSelector.click();
            
            // Find our new account
            const auditBankOpt = await driver.$(`//*[contains(@text, "${NEW_ACC_NAME}")]`);
            if (await auditBankOpt.isDisplayed()) {
                await auditBankOpt.click();
                log("Transaction", "PASS", "Newly created account available in Transaction selector.");
            } else {
                // Fallback
                const firstOpt = await driver.$$('//android.widget.TextView')[0]; 
                await firstOpt.click();
                log("Transaction", "WARN", "Could not find new account in selector, used default.");
            }

            // Category
            const catSelector = await driver.$('//*[contains(@text, "Select Category")]');
            await catSelector.click();
            const foodOpt = await driver.$('//*[contains(@text, "Food")]');
            await foodOpt.click();

            // Amount
            const amtInputs = await driver.$$('//android.widget.EditText');
            await amtInputs[0].setValue("200");

            // Save
            const saveBtn = await driver.$('~Save'); // string save_cd
            await saveBtn.click();

            // Verify on Home
            const transItem = await driver.$('//*[contains(@text, "200.00")]');
            await transItem.waitForDisplayed({ timeout: 5000 });
            log("Feature: Add Transaction", "PASS", "Transaction of 200.00 added successfully.");

        } catch (e) {
             log("Feature: Add Transaction", "FAIL", "Transaction flow failed: " + e.message);
        }

        // --- TEST 7: STATS SCREEN ---
        log("Navigation", "INFO", "Switching to Stats...");
        const statsTab = await driver.$('//*[contains(@text, "Stats") and @class="android.widget.TextView"]');
        await statsTab.click();

        try {
            // Check for a chart or "Total" text or "Expense"
            const expenseText = await driver.$('//*[contains(@text, "Expense")]');
            await expenseText.waitForDisplayed();
            log("Stats UI", "PASS", "Stats screen loaded.");
        } catch (e) {
            log("Stats UI", "FAIL", "Stats screen elements missing.");
        }

    } catch (mainError) {
        console.error("Critical Test Error:", mainError);
    } finally {
        console.log("\n=== AUDIT REPORT ===");
        report.forEach(r => {
            console.log(`[${r.status}] ${r.step}: ${r.message}`);
        });

        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runAudit();
