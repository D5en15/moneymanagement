const { remote } = require('webdriverio');
const { main } = require('appium');
const path = require('path');

const capabilities = {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': 'Android Device',
    'appium:app': path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk'),
    'appium:noReset': false,
    'appium:fullReset': true,
    'appium:autoGrantPermissions': true,
    'appium:newCommandTimeout': 300
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

        // 1. Wait for Home Screen
        console.log("Waiting for Home Screen...");
        const noTransText = await driver.$('//*[contains(@text, "No transactions")]');
        try {
            await noTransText.waitForDisplayed({timeout: 15000});
        } catch(e) {
            // Fallback
        }

        // 2. Add Transaction 1 (Older) - Amount 100
        console.log("Adding Transaction 1 (Older)...");
        let addBtn = await driver.$('//android.view.View[@content-desc="Add Transaction"]');
        await addBtn.waitForDisplayed({ timeout: 10000 });
        await addBtn.click();

        let amountField = await driver.$('//android.widget.EditText');
        await amountField.waitForDisplayed({ timeout: 10000 });
        await amountField.setValue("100");
        
        // Select Account/Category to be safe
        const selectAccountBtn = await driver.$('//*[contains(@text, "Select Account")]');
        await selectAccountBtn.click();
        const cashAccount = await driver.$('//*[contains(@text, "Cash")]');
        await cashAccount.waitForDisplayed({timeout: 5000});
        await cashAccount.click();
        await driver.pause(500);

        const selectCategoryBtn = await driver.$('//*[contains(@text, "Select Category")]');
        await selectCategoryBtn.click();
        let category = await driver.$('//android.widget.TextView'); // First text view in list usually
        // Find a category item
        const categories = await driver.$$('//android.widget.TextView');
        if (categories.length > 1) await categories[1].click(); // Skip title
        await driver.pause(500);

        // Note: "Old"
        const noteFields = await driver.$$('//android.widget.EditText');
        if (noteFields.length > 1) {
            await noteFields[1].setValue("Old");
        }

        let saveBtn = await driver.$('~Save');
        await saveBtn.click();
        await driver.pause(2000);

        // 3. Add Transaction 2 (Newer) - Amount 200
        console.log("Adding Transaction 2 (Newer)...");
        addBtn = await driver.$('//android.view.View[@content-desc="Add Transaction"]');
        await addBtn.waitForDisplayed({ timeout: 10000 });
        await addBtn.click();

        amountField = await driver.$('//android.widget.EditText');
        await amountField.waitForDisplayed({ timeout: 10000 });
        await amountField.setValue("200");
        
        // Account/Category (Required)
        const selectAccountBtn2 = await driver.$('//*[contains(@text, "Select Account")]');
        await selectAccountBtn2.click();
        const cashAccount2 = await driver.$('//*[contains(@text, "Cash")]');
        await cashAccount2.waitForDisplayed({timeout: 5000});
        await cashAccount2.click();
        await driver.pause(500);

        const selectCategoryBtn2 = await driver.$('//*[contains(@text, "Select Category")]');
        await selectCategoryBtn2.click();
        const categories2 = await driver.$$('//android.widget.TextView');
        if (categories2.length > 1) await categories2[1].click();
        await driver.pause(500);

        // Note: "New"
        const noteFields2 = await driver.$$('//android.widget.EditText');
        if (noteFields2.length > 1) {
            await noteFields2[1].setValue("New");
        }

        saveBtn = await driver.$('~Save');
        await saveBtn.click();
        await driver.pause(2000);

        // 4. Verify Sorting
        console.log("Verifying Order...");
        // Find all list items. They usually have a specific ID or structure.
        // My TransactionItem has `semantics { contentDescription = "transaction_$id" }`.
        // But I don't know the IDs.
        // I can find by Text "Old" and "New".
        
        const newTrans = await driver.$('//*[contains(@text, "New")]');
        const oldTrans = await driver.$('//*[contains(@text, "Old")]');
        
        const newRect = await newTrans.getLocation();
        const oldRect = await oldTrans.getLocation();
        
        console.log(`New Transaction Y: ${newRect.y}`);
        console.log(`Old Transaction Y: ${oldRect.y}`);

        if (newRect.y < oldRect.y) {
            console.log("SUCCESS: 'New' is above 'Old' (Sorted by Date DESC).");
        } else {
            console.error("FAILURE: 'Old' is above 'New'. Sorting might be wrong.");
            throw new Error("Sorting verification failed.");
        }

    } catch (error) {
        console.error("Test Execution Failed:", error);
        process.exit(1);
    } finally {
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runTest();
