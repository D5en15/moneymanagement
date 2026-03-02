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
        // Wait for "No transactions" text which confirms we are on Home
        const noTransText = await driver.$('//*[contains(@text, "No transactions")]');
        try {
            await noTransText.waitForDisplayed({timeout: 15000});
        } catch(e) {
            console.log("Home text not found, trying generic wait...");
            await driver.pause(5000);
        }

        // ==========================================
        // 2. Add Income Transaction
        // ==========================================
        console.log("Adding Income Transaction...");
        // Use XPath to find the element with content-desc="Add Transaction"
        // Note: The previous failure was finding the button on Home screen.
        // It failed because we were ALREADY on Add Transaction screen (due to no Reset? No, fullReset=true).
        // Wait, if Step 2 succeeded in clicking, we are on Add Screen.
        // If it failed to click, we are on Home.
        // Let's ensure we are on Home.
        
        let addBtn = await driver.$('//android.view.View[@content-desc="Add Transaction"]');
        if (!await addBtn.isDisplayed()) {
             // Maybe we are already on Add Screen?
             const addTitle = await driver.$('//android.widget.TextView[@text="Add Transaction"]');
             if (await addTitle.isDisplayed()) {
                 console.log("Already on Add Transaction screen.");
             } else {
                 throw new Error("Cannot find Add Transaction button or screen.");
             }
        } else {
             await addBtn.click();
        }

        // Wait for Add Screen
        const amountField = await driver.$('//android.widget.EditText');
        await amountField.waitForDisplayed({ timeout: 10000 });

        // Select "Income" Tab
        console.log("Selecting Income...");
        const incomeTab = await driver.$('//*[contains(@text, "Income")]');
        await incomeTab.click();
        await driver.pause(1000);

        // Enter Amount
        console.log("Entering Amount...");
        await amountField.setValue("5000");
        
        // Select Account
        console.log("Selecting Account...");
        const selectAccountBtn = await driver.$('//*[contains(@text, "Select Account")]');
        await selectAccountBtn.click();
        
        // Wait for Sheet
        const cashAccount = await driver.$('//*[contains(@text, "Cash")]');
        await cashAccount.waitForDisplayed({ timeout: 5000 });
        await cashAccount.click();
        await driver.pause(1000);

        // Select Category
        console.log("Selecting Category...");
        const selectCategoryBtn = await driver.$('//*[contains(@text, "Select Category")]');
        await selectCategoryBtn.click();
        
        // Wait for Sheet - Select "Salary" (assuming it exists in DummyData for Income)
        // If not, just pick the first one.
        // DummyData categories? "Salary", "Gift" usually.
        let category = await driver.$('//*[contains(@text, "Salary")]');
        if (!await category.isExisting()) {
             // Fallback to first item in list (not the title)
             // The list has many text views.
             // Just try finding text "Others" or generic click.
             const texts = await driver.$$('//android.widget.TextView');
             // texts[0] is title "Select Category"
             if (texts.length > 1) {
                 category = texts[1]; 
             }
        }
        await category.click();
        await driver.pause(1000);

        // Save
        console.log("Saving...");
        const saveBtn = await driver.$('~Save'); // Accessibility ID
        await saveBtn.waitForDisplayed({ timeout: 5000 });
        await saveBtn.click();
        
        await driver.pause(2000);

        // ==========================================
        // 3. Add Expense Transaction
        // ==========================================
        console.log("Adding Expense Transaction...");
        // Re-find add button
        const addBtn2 = await driver.$('//android.view.View[@content-desc="Add Transaction"]');
        await addBtn2.waitForDisplayed({ timeout: 5000 });
        await addBtn2.click();
        
        // Find Amount field again
        const amountField2 = await driver.$('//android.widget.EditText');
        await amountField2.waitForDisplayed({ timeout: 10000 });
        
        // Default is Expense.
        console.log("Entering Amount...");
        await amountField2.setValue("500");
        
        // Select Account
        console.log("Selecting Account...");
        const selectAccountBtn2 = await driver.$('//*[contains(@text, "Select Account")]');
        await selectAccountBtn2.click();
        const cashAccount2 = await driver.$('//*[contains(@text, "Cash")]');
        await cashAccount2.waitForDisplayed({timeout: 5000});
        await cashAccount2.click();
        await driver.pause(1000);

        // Select Category
        console.log("Selecting Category...");
        const selectCategoryBtn2 = await driver.$('//*[contains(@text, "Select Category")]');
        await selectCategoryBtn2.click();
        
        // Select "Food"
        let category2 = await driver.$('//*[contains(@text, "Food")]');
        if (!await category2.isExisting()) {
             const texts = await driver.$$('//android.widget.TextView');
             if (texts.length > 1) category2 = texts[1];
        }
        await category2.click();
        await driver.pause(1000);
        
        console.log("Saving...");
        const saveBtn2 = await driver.$('~Save');
        await saveBtn2.waitForDisplayed({ timeout: 5000 });
        await saveBtn2.click();

        await driver.pause(2000);

        // ==========================================
        // 4. Verify Chart Presence
        // ==========================================
        console.log("Verifying Trend Line Chart...");
        
        // The chart should be visible now.
        const chart = await driver.$('~Trend Line Chart');
        await chart.waitForDisplayed({ timeout: 10000 });
        
        if (await chart.isDisplayed()) {
            console.log("SUCCESS: Trend Line Chart is visible.");
        } else {
            throw new Error("Trend Line Chart not found or not visible.");
        }

    } catch (error) {
        console.error("Test Execution Failed:", error);
        if (driver) {
             try {
                 const source = await driver.getPageSource();
                 console.log("PAGE SOURCE DUMP (on failure):\n", source);
             } catch(e) {}
        }
        process.exit(1);
    } finally {
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runTest();
