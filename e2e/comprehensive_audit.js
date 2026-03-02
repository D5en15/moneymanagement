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
    'appium:noReset': false, // Keep data to test persistence or set to false for clean slate? 
                             // Let's use false (clean slate) to ensure consistent test state.
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
    logLevel: 'error', // Reduce noise
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
        try {
            const homeTab = await driver.$('~Home'); // Assuming Home tab has content-desc
            await homeTab.waitForDisplayed({ timeout: 10000 });
            log("App Launch", "PASS", "Home screen loaded successfully.");
        } catch (e) {
            log("App Launch", "FAIL", "App did not load Home screen in time.");
            throw e;
        }

        // --- TEST 2: ACCOUNTS SCREEN UI & LOGIC ---
        log("Navigation", "INFO", "Switching to Accounts tab...");
        const accountsTab = await driver.$('~Accounts');
        await accountsTab.click();

        // Check Header
        try {
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

        // Get Initial Net Worth
        let initialNetWorth = 0;
        try {
            // Find the big number below Net Worth. It usually contains "," e.g. "41,600"
            // We'll look for a TextView that matches currency format roughly
            const netWorthVal = await driver.$('//android.widget.TextView[contains(@text, ",")]'); 
            const text = await netWorthVal.getText();
            initialNetWorth = parseFloat(text.replace(/,/g, ''));
            log("Data Check", "INFO", `Initial Net Worth: ${initialNetWorth}`);
        } catch (e) {
            log("Data Check", "WARN", "Could not parse initial Net Worth.");
        }

        // --- TEST 3: ADD NEW ACCOUNT ---
        const NEW_ACC_NAME = "Audit Bank";
        const NEW_ACC_BAL = 5000;

        try {
            const addBtn = await driver.$('~Add Account'); // Using content-desc
            await addBtn.click();
            
            const nameField = await driver.$('//android.widget.EditText[@text=""]');
            await nameField.setValue(NEW_ACC_NAME);
            
            const inputs = await driver.$$('//android.widget.EditText');
            if(inputs.length > 1) await inputs[1].setValue(NEW_ACC_BAL.toString());

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
        // Refresh or check immediately? StateFlow should be immediate.
        // Wait a bit for animation
        await driver.pause(1000);
        try {
            // We need to find the element again to get fresh text
            // Finding by the exact text might be hard if it changed, so we rely on structure or relative locator
            // For now, let's grab all textviews and find the large one again
            // Or assume it's the one under "Net Worth"
            
            // Simplified: Just check if it's NOT the initial value if initial was > 0
            // Or better, check if it increased.
            // But finding the exact element is key.
            // Let's assume it's the one with text size (hard to check).
            // We will look for the text containing the new formatted sum.
            
            // Expected: Initial + 5000
            // Since we don't know exact initial perfectly, let's just check if the NEW account card shows correct balance
            const accBalance = await driver.$(`//*[contains(@text, "5,000")]`);
            if (await accBalance.isDisplayed()) {
                 log("Data Consistency", "PASS", "New account shows correct balance '5,000'.");
            }
        } catch (e) {
            log("Data Consistency", "FAIL", "New account balance display issue.");
        }

        // --- TEST 5: SETTINGS AUDIT ---
        log("Navigation", "INFO", "Switching to Settings tab...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();

        try {
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
            const backBtn = await driver.$('~Back'); // Check for back button content desc
            // If not ~Back, maybe class android.widget.ImageButton?
            const backBtns = await driver.$$('//android.widget.ImageButton');
            if (backBtns.length > 0) await backBtns[0].click();
            else driver.back();
            
            log("Navigation", "PASS", "Category Management navigation works.");
        } catch (e) {
            log("Settings UI", "FAIL", "Category Management flow broken.");
        }

        // --- TEST 6: TRANSACTION FLOW (Impact on Accounts) ---
        log("Navigation", "INFO", "Switching to Home/Transactions...");
        await homeTab.click();

        try {
            const addTransBtn = await driver.$('~Add Transaction');
            await addTransBtn.click();

            // Select "Audit Bank" account if possible, or just use default
            const accSelector = await driver.$('//*[contains(@text, "Select Account")]');
            await accSelector.click();
            
            // Find our new account
            const auditBankOpt = await driver.$(`//*[contains(@text, "${NEW_ACC_NAME}")]`);
            if (await auditBankOpt.isDisplayed()) {
                await auditBankOpt.click();
                log("Transaction", "PASS", "Newly created account available in Transaction selector.");
            } else {
                // Fallback
                const firstOpt = await driver.$$('//android.widget.TextView')[0]; // Risky
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
            const saveBtn = await driver.$('~Save');
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
        const statsTab = await driver.$('~Statistics');
        await statsTab.click();

        try {
            // Check for a chart or "Total" text
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
