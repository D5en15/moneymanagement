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
    'appium:autoGrantPermissions': true,
    'appium:fullReset': true
};

const wdOpts = {
    hostname: '127.0.0.1',
    port: 4723,
    logLevel: 'info',
    capabilities,
};

// Set JAVA_HOME for this process just in case, though gradle.properties should handle build
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
        const addBtn = await driver.$('~Add Transaction');
        await addBtn.waitForDisplayed({ timeout: 20000 });

        // --- SEED DATA ---
        
        // Add Expense: 1000
        console.log("Seeding Expense...");
        await addBtn.click();
        
        // Wait for Add Screen Title
        const addTitle = await driver.$('//*[contains(@text, "Add Transaction")]');
        await addTitle.waitForDisplayed({ timeout: 10000 });
        
        const saveBtn = await driver.$('~Save');
        await saveBtn.waitForDisplayed();
        
        // Select Account (Optional, defaults should work but let's be safe)
        const accountBtn = await driver.$('//*[contains(@text, "Select Account")]');
        if (await accountBtn.isDisplayed()) {
             await accountBtn.click();
             const cashOption = await driver.$('//*[contains(@text, "Cash")]');
             await cashOption.waitForDisplayed();
             await cashOption.click();
        }

        // Enter Amount
        const amountField = await driver.$$('//android.widget.EditText')[0];
        await amountField.setValue("1000");
        
        await saveBtn.click();
        
        // Wait for return to Home
        await addBtn.waitForDisplayed({ timeout: 10000 });

        // Add Income: 5000
        console.log("Seeding Income...");
        await addBtn.click();
        await addTitle.waitForDisplayed({ timeout: 10000 });
        
        const incomeTab = await driver.$('//*[contains(@text, "Income")]');
        await incomeTab.click();

        const amountField2 = await driver.$$('//android.widget.EditText')[0];
        await amountField2.setValue("5000");

        await saveBtn.click();
        await addBtn.waitForDisplayed({ timeout: 10000 });

        // --- NAVIGATE TO STATS ---
        console.log("Navigating to Stats...");
        const statsTab = await driver.$('~Stats');
        await statsTab.waitForDisplayed();
        await statsTab.click();

        // --- VERIFY STATS SCREEN ---
        console.log("Verifying Stats Screen...");
        const title = await driver.$('//*[contains(@text, "Statistics")]');
        await title.waitForDisplayed({ timeout: 10000 });

        // Verify "This Month"
        const periodSelector = await driver.$('//*[contains(@text, "This Month")]');
        await periodSelector.waitForDisplayed();

        // Verify Summary Cards
        console.log("Verifying Summary Cards...");
        const expenseCard = await driver.$('//*[contains(@text, "Expense")]');
        await expenseCard.waitForDisplayed();

        // Scroll down slightly to ensure visibility
        console.log("Scrolling...");
        await driver.action('pointer')
            .move({ duration: 0, x: 500, y: 1500 })
            .down({ button: 0 })
            .move({ duration: 500, x: 500, y: 1000 })
            .up({ button: 0 })
            .perform();
        await driver.pause(1000);

        // Verify Breakdown List (Expense)
        console.log("Verifying Expense Breakdown...");
        
        // We added 1000. It might be formatted as "$1,000.00" or "฿1,000.00" or similar.
        // Let's search for just "1,000" text part.
        const expenseAmount = await driver.$('//*[contains(@text, "1,000")]');
        if (await expenseAmount.isDisplayed()) {
            console.log("Found 1,000 amount!");
        } else {
            console.log("1,000 amount NOT found. Dumping source...");
            console.log(await driver.getPageSource());
            throw new Error("Expense amount not found");
        }

        // --- CHANGE FILTERS ---
        console.log("Changing Filter to Income...");
        
        // Ensure chips are visible
        await driver.action('pointer')
            .move({ duration: 0, x: 500, y: 500 })
            .down({ button: 0 })
            .move({ duration: 500, x: 500, y: 1500 }) // Swipe down
            .up({ button: 0 })
            .perform();
        
        const incomeChip = await driver.$('//*[contains(@text, "Income")]');
        await incomeChip.waitForDisplayed();
        await incomeChip.click();

        // Verify Income Breakdown
        console.log("Verifying Income Breakdown...");
        const incomeAmount = await driver.$('//*[contains(@text, "5,000")]');
        await incomeAmount.waitForDisplayed();
        console.log("Found 5,000 amount!");

        // --- TEST DRILL DOWN ---
        console.log("Testing Drill Down...");
        const percentageElement = await driver.$('//*[contains(@text, "% ")]');
        await percentageElement.click();
        
        // Verify Drill Down Sheet
        console.log("Verifying Drill Down Sheet...");
        const sheetTitle = await driver.$('//*[contains(@text, "Transactions")]');
        await sheetTitle.waitForDisplayed();
        
        const txAmount = await driver.$('//*[contains(@text, "5,000")]');
        await txAmount.waitForDisplayed();
        
        console.log("SUCCESS: Drill Down verified!");
        
        // Close sheet
        await driver.back();
        
        console.log("SUCCESS: All Stats Tests Passed!");

    } catch (error) {
        console.error("Test Failed:", error);
        // Take screenshot on failure
        if (driver) {
            await driver.saveScreenshot('./error_screenshot.png');
            console.log("Screenshot saved to error_screenshot.png");
        }
        throw error;
    } finally {
        if (driver) await driver.deleteSession();
        if (server) await server.close();
    }
}

runTest();