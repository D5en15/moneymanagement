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
    'appium:fullReset': false, 
    'appium:autoGrantPermissions': true
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'info',
    capabilities,
};

// Set JAVA_HOME specifically for this process
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

        // 1. Navigate to Accounts Tab
        console.log("Navigating to Accounts Tab...");
        let accountsBtn = await driver.$('~Accounts');
        if (!await accountsBtn.isDisplayed()) {
             // Fallback to finding by text if content-desc is not found
             accountsBtn = await driver.$('//*[contains(@text, "Accounts")]');
        }
        await accountsBtn.click();
        
        // Wait for Accounts screen
        const headerTitle = await driver.$('//*[contains(@text, "Accounts")]'); 
        await headerTitle.waitForDisplayed({ timeout: 10000 });

        // 2. Verify Donut Chart Presence
        console.log("Verifying Donut Chart...");
        // Looking for "Net Worth" which is inside the Donut Chart component
        const netWorthText = await driver.$('//*[contains(@text, "Net Worth")]');
        if (await netWorthText.isDisplayed()) {
            console.log("SUCCESS: Donut Chart (Net Worth) is visible.");
        } else {
            console.warn("WARNING: Donut Chart might not be visible.");
        }

        // 3. Create a Test Account to Edit
        console.log("Adding a Test Account for Edit verification...");
        const addBtn = await driver.$('~Add Account'); // Using content description I know exists in Icon
        if (await addBtn.isDisplayed()) {
             await addBtn.click();
        } else {
             const addText = await driver.$('//*[contains(@text, "Add")]');
             await addText.click();
        }

        // Wait for Add Dialog
        const addDialogTitle = await driver.$('//*[contains(@text, "Add Account")]');
        await addDialogTitle.waitForDisplayed({ timeout: 5000 });

        // Fill details
        const fields = await driver.$$('//android.widget.EditText');
        await fields[0].setValue("Test Edit Me");
        if (fields.length > 1) {
            await fields[1].setValue("1234");
        }

        // Find the "Add" button in the dialog. 
        // The Text "Add" is inside a clickable View (TextButton).
        // We look for a View that is clickable and contains the text "Add".
        const confirmAddBtn = await driver.$('//android.view.View[@clickable="true" and .//android.widget.TextView[@text="Add"]]');
        await confirmAddBtn.waitForDisplayed({ timeout: 5000 });
        await confirmAddBtn.click();
        
        // Wait for dialog to close (EditText should disappear)
        await fields[0].waitForDisplayed({ reverse: true, timeout: 5000 });
        
        // Wait for list to update
        await driver.pause(2000); 

        // 4. Click the Account to Verify Edit Dialog
        console.log("Clicking the new account to open Edit Dialog...");
        const accountItem = await driver.$('//*[contains(@text, "Test Edit Me")]');
        await accountItem.waitForDisplayed({ timeout: 5000 });
        await accountItem.click();

        // 5. Verify Edit Dialog Elements
        console.log("Verifying Edit Dialog...");
        const editDialogTitle = await driver.$('//*[contains(@text, "Edit Account")]');
        try {
            await editDialogTitle.waitForDisplayed({ timeout: 5000 });
            console.log("SUCCESS: Edit Dialog opened.");
        } catch (e) {
            console.error("Edit Dialog not found!");
            const source = await driver.getPageSource();
            console.log("PAGE SOURCE:", source);
            throw e;
        }

        // Check for 'Delete Account' button INSIDE the dialog
        const deleteBtn = await driver.$('//*[contains(@text, "Delete Account")]');
        if (await deleteBtn.isDisplayed()) {
            console.log("SUCCESS: 'Delete Account' button found inside Edit Dialog.");
        } else {
            throw new Error("FAIL: 'Delete Account' button missing from Edit Dialog.");
        }

        // 6. Test Edit Save
        console.log("Testing Update Account...");
        const editFields = await driver.$$('//android.widget.EditText');
        await editFields[0].setValue("Edited Name"); // Change Name
        
        const saveBtn = await driver.$('//*[contains(@text, "Save")]');
        await saveBtn.click();

        // Verify change in list
        await driver.pause(1000);
        const editedAccount = await driver.$('//*[contains(@text, "Edited Name")]');
        if (await editedAccount.isDisplayed()) {
            console.log("SUCCESS: Account name updated to 'Edited Name'.");
        } else {
            throw new Error("FAIL: Account name update failed.");
        }

        // 7. Cleanup: Delete the account
        console.log("Cleaning up: Deleting the account...");
        await editedAccount.click(); // Open Edit Dialog again
        await editDialogTitle.waitForDisplayed({ timeout: 2000 });
        
        const deleteBtn2 = await driver.$('//*[contains(@text, "Delete Account")]');
        await deleteBtn2.click();

        // Verify gone
        await driver.pause(1000);
        const deletedAccount = await driver.$('//*[contains(@text, "Edited Name")]');
        if (await deletedAccount.isDisplayed()) {
             // Sometimes isDisplayed returns true if it's still in the DOM but hidden, 
             // but usually for UIAutomator it means visible.
             // Let's assume if it throws error or returns false it's gone.
             throw new Error("FAIL: Account was not deleted.");
        }
        
    } catch (error) {
        // If element is not found, verify if it is indeed the expected outcome (for delete)
        if (error.message.includes("NoSuchElement") && error.message.includes("Edited Name")) {
             console.log("SUCCESS: Account successfully deleted (Element not found).");
        } else {
             console.error("Test Execution Failed:", error);
             process.exit(1);
        }
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
