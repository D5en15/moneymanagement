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

console.log("App Path:", capabilities['appium:app']);

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
        const addBtn = await driver.$('~Add Transaction');
        await addBtn.waitForDisplayed({ timeout: 20000 });

        // 2. Click Add
        console.log("Clicking Add...");
        await addBtn.click();

        // 3. Wait for Add Screen
        const saveBtn = await driver.$('~Save');
        await saveBtn.waitForDisplayed({ timeout: 10000 });

        // 4. Select Account
        console.log("Selecting Account...");
        const accountBtn = await driver.$('//*[contains(@text, "Select Account")]');
        await accountBtn.click();
        const cashOption = await driver.$('//*[contains(@text, "Cash")]');
        await cashOption.waitForDisplayed();
        await cashOption.click();

        // 5. Select Category
        console.log("Selecting Category...");
        const categoryBtn = await driver.$('//*[contains(@text, "Select Category")]');
        await categoryBtn.click();
        const foodOption = await driver.$('//*[contains(@text, "Food")]');
        await foodOption.waitForDisplayed();
        await foodOption.click();

        // 6. Enter Amount "50"
        console.log("Entering Amount...");
        const textFields = await driver.$$('//android.widget.EditText');
        if (textFields.length > 0) {
            await textFields[0].setValue("50");
        }

        // 7. Click Save
        console.log("Clicking Save...");
        if (await driver.isKeyboardShown()) {
            await driver.hideKeyboard();
        }
        await saveBtn.click();

        // 8. Verify on Home Screen
        console.log("Verifying Result...");
        const homeAddBtn = await driver.$('~Add Transaction');
        await homeAddBtn.waitForDisplayed({ timeout: 10000 });
        const amountElement = await driver.$('//*[contains(@text, "\u0E3F50.00")]');
        await amountElement.waitForDisplayed({ timeout: 5000 });
        console.log("SUCCESS: Transaction '฿50.00' found on Home Screen!");

        // --- EDIT TRANSACTION ---
        console.log("Clicking Transaction to Edit...");
        const transactionItem = await driver.$('~transaction_1');
        await transactionItem.waitForDisplayed({ timeout: 5000 });
        await transactionItem.click();

        console.log("Waiting for Edit Screen...");
        const editTitle = await driver.$('//*[contains(@text, "Edit Transaction")]');
        await editTitle.waitForDisplayed({ timeout: 10000 });

        console.log("Changing Amount to 100...");
        const amountField = await driver.$('//android.widget.EditText');
        await amountField.clearValue();
        await amountField.setValue("100");

        console.log("Clicking Save...");
        const saveEditBtn = await driver.$('~Save');
        await saveEditBtn.click();

        console.log("Verifying Edit Result...");
        const newAmountElement = await driver.$('//*[contains(@text, "\u0E3F100.00")]');
        await newAmountElement.waitForDisplayed({ timeout: 5000 });
        console.log("SUCCESS: Transaction updated to '฿100.00'!");

        // --- DELETE TRANSACTION ---
        console.log("Clicking Transaction to Delete...");
        const transactionItemToDelete = await driver.$('~transaction_1');
        await transactionItemToDelete.waitForDisplayed({ timeout: 5000 });
        await transactionItemToDelete.click();

        console.log("Waiting for Edit Screen (before delete)...");
        await editTitle.waitForDisplayed({ timeout: 10000 });

        console.log("Clicking Delete Icon...");
        const deleteIcon = await driver.$('~Delete');
        await deleteIcon.waitForDisplayed({ timeout: 5000 });
        await deleteIcon.click();

        console.log("Confirming Delete...");
        const confirmDeleteBtn = await driver.$('//*[contains(@text, "Delete") and not(contains(@text, "Edit")) and not(contains(@text, "Transaction"))]');
        await confirmDeleteBtn.waitForDisplayed({ timeout: 10000 });
        await confirmDeleteBtn.click();

        console.log("Verifying Deletion...");
        await driver.pause(2000);
        const deletedElement = await driver.$('//*[contains(@text, "\u0E3F100.00")]');
        const isDisplayed = await deletedElement.isDisplayed().catch(() => false);
        if (!isDisplayed) {
             console.log("SUCCESS: Transaction deleted!");
        } else {
             throw new Error("Transaction still visible after delete!");
        }

        // --- VERIFY SETTINGS ITEMS ---
        console.log("Navigating to Settings...");
        const settingsTab = await driver.$('~Settings');
        await settingsTab.click();

        console.log("Verifying Category Management...");
        const catMgmt = await driver.$('//*[contains(@text, "Category Management")]');
        await catMgmt.waitForDisplayed({ timeout: 5000 });

        console.log("Verifying Account Management...");
        const accMgmt = await driver.$('//*[contains(@text, "Account Management")]');
        await accMgmt.waitForDisplayed({ timeout: 5000 });

        console.log("Verifying Export CSV...");
        const exportCsv = await driver.$('//*[contains(@text, "Export CSV")]');
        await exportCsv.waitForDisplayed({ timeout: 5000 });

        console.log("Verifying Passcode Lock...");
        const lockItem = await driver.$('//*[contains(@text, "Passcode Lock")]');
        await lockItem.waitForDisplayed({ timeout: 5000 });
        
        console.log("Toggling Passcode Lock ON...");
        await lockItem.click();

        console.log("Waiting for Setup Passcode Screen...");
        const setupTitle = await driver.$('//*[contains(@text, "Setup Passcode")]');
        await setupTitle.waitForDisplayed({ timeout: 5000 });

        console.log("Entering Pin 1234...");
        const keys = ["1", "2", "3", "4"];
        for (const key of keys) {
            const btn = await driver.$(`//*[contains(@text, "${key}")]`);
            await btn.click();
            await driver.pause(500);
        }

        console.log("Verifying return to Settings...");
        await catMgmt.waitForDisplayed({ timeout: 5000 });
        console.log("SUCCESS: Passcode Setup completed and verified!");

        console.log("SUCCESS: All new features verified in Settings!");

    } catch (error) {
        console.error("Test Execution Failed:", error);
    } finally {
        if (driver) {
            await driver.deleteSession();
        }
        if (server) {
            console.log("Stopping Appium Server...");
            await server.close();
            console.log("Appium Server stopped.");
        }
    }
}

runTest();