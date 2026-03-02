const { remote } = require('webdriverio');
const { main } = require('appium');
const path = require('path');

const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': 'CPH2203',
    'appium:udid': 'SKJF5HLVYSDE5DNF',
    'appium:app': path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk'),
    'appium:noReset': false,
    'appium:fullReset': false, // Don't wipe every time for speed
    'appium:autoGrantPermissions': true
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'info',
    capabilities,
};

process.env.JAVA_HOME = "C:\\Program Files\\Android\\Android Studio\\jbr";
process.env.PATH = process.env.PATH + ";" + process.env.JAVA_HOME + "\\bin";

async function runTest() {
    let server;
    let driver;

    try {
        console.log("Starting Appium Server...");
        server = await main({
            port: 4723,
            loglevel: 'info',
            relaxedSecurity: true
        });
        console.log("Appium Server started.");

        console.log("Connecting to Appium Driver...");
        driver = await remote(wdOpts);
        console.log("Connected!");

        // 1. Navigate to Settings
        console.log("Navigating to Settings...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.waitForDisplayed({ timeout: 20000 });
        await settingsTab.click();

        // 2. Verify Account Management is GONE
        console.log("Verifying Account Management is GONE...");
        // We expect this to NOT be found. 
        // We can't easily wait for it NOT to exist, so we check if it exists with short timeout.
        try {
            const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
            await accMgmt.waitForDisplayed({ timeout: 3000 });
            throw new Error("FAIL: Account Management still exists in Settings!");
        } catch (e) {
            if (e.message.includes("FAIL")) throw e;
            console.log("SUCCESS: Account Management not found in Settings (as expected).");
        }

        // 3. Navigate to Accounts
        console.log("Navigating to Accounts...");
        const accountsTab = await driver.$('~Accounts'); // Assuming I have content desc for Accounts tab. 
        // If not, I might need to find by text.
        // Let's check MainScreen.kt for content description.
        // It uses `stringResource(screen.title)`. Screen title for Accounts is `nav_accounts`.
        // I need to be sure. But usually tab content desc is the title.
        
        // If "Accounts" text is visible
        const accountsText = await driver.$('//*[contains(@text, "Accounts") and @resource-id=""]'); // Text in nav bar
        // Or find by accessibility id "Accounts"
        
        let accountsBtn = await driver.$('~Accounts');
        if (!await accountsBtn.isDisplayed()) {
             accountsBtn = await driver.$('//*[contains(@text, "Accounts")]');
        }
        await accountsBtn.click();

        // 4. Verify Header "Accounts" and "+ Add" button
        console.log("Verifying Accounts Header...");
        const headerTitle = await driver.$('//*[contains(@text, "Accounts") and @class="android.widget.TextView"]'); // Big title
        await headerTitle.waitForDisplayed({ timeout: 10000 });
        
        const addBtn = await driver.$('~Add Account'); // I added contentDescription="Add Account" to the Icon
        // Or text "Add"
        const addText = await driver.$('//*[contains(@text, "Add")]');
        
        if (await addBtn.isDisplayed() || await addText.isDisplayed()) {
            console.log("SUCCESS: 'Add' button found.");
        } else {
            throw new Error("FAIL: 'Add' button not found on Accounts screen.");
        }

        // 5. Verify Net Worth Donut
        console.log("Verifying Net Worth Donut...");
        const netWorthText = await driver.$('//*[contains(@text, "Net Worth")]');
        await netWorthText.waitForDisplayed({ timeout: 5000 });
        console.log("SUCCESS: Net Worth widget found.");

        // 6. Test Add Account Flow
        console.log("Testing Add Account...");
        if (await addBtn.isDisplayed()) await addBtn.click();
        else await addText.click();

        const dialogTitle = await driver.$('//*[contains(@text, "Add Account")]');
        await dialogTitle.waitForDisplayed({ timeout: 5000 });
        
        const nameField = await driver.$('//android.widget.EditText[@text=""]'); // First empty field
        await nameField.setValue("Test Bank");
        
        // Use tab or find second edit text
        const fields = await driver.$$('//android.widget.EditText');
        if (fields.length > 1) {
            await fields[1].setValue("5000");
        }

        const confirmBtn = await driver.$('//*[contains(@text, "Add") and @class="android.widget.TextView"]'); 
        // Button usually has text "Add"
        await confirmBtn.click();

        // Verify "Test Bank" appears in list
        console.log("Verifying new account in list...");
        const newAccount = await driver.$('//*[contains(@text, "Test Bank")]');
        await newAccount.waitForDisplayed({ timeout: 5000 });
        console.log("SUCCESS: New account 'Test Bank' added.");

    } catch (error) {
        console.error("Test Execution Failed:", error);
        process.exit(1);
    } finally {
        if (driver) {
            await driver.deleteSession();
        }
        if (server) {
            console.log("Stopping Appium Server...");
            await server.close();
        }
    }
}

runTest();
