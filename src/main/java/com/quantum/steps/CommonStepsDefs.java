/**
 * 
 */
package com.quantum.steps;

import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.step.QAFTestStepProvider;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.qmetry.qaf.automation.util.StringUtil;
import com.quantum.utils.AppiumUtils;
import com.quantum.utils.ConfigurationUtils;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;
import com.quantum.zapIntegration.ZAPClient;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.zaproxy.clientapi.core.ClientApi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;


@QAFTestStepProvider
public class CommonStepsDefs {

	@Then("I switch to frame \"(.*?)\"")
	public static void switchToFrame(String nameOrIndex) {
		if (StringUtil.isNumeric(nameOrIndex)) {
			int index = Integer.parseInt(nameOrIndex);
			new WebDriverTestBase().getDriver().switchTo().frame(index);
		} else {
			new WebDriverTestBase().getDriver().switchTo().frame(nameOrIndex);
		}
	}

	@Then("I switch to \"(.*?)\" frame by element")
	public static void switchToFrameByElement(String loc) {
		new WebDriverTestBase().getDriver().switchTo().frame(new QAFExtendedWebElement(loc));
	}

	@When("I am using an AppiumDriver")
	public void testForAppiumDriver() {
		if (ConfigurationUtils.getBaseBundle().getPropertyValue("driver.name").contains("Remote"))
			ConsoleUtils.logWarningBlocks("Driver is an instance of QAFExtendedWebDriver");
		else if (AppiumUtils.getAppiumDriver() instanceof IOSDriver)
			ConsoleUtils.logWarningBlocks("Driver is an instance of IOSDriver");
		else if (AppiumUtils.getAppiumDriver() instanceof AndroidDriver)
			ConsoleUtils.logWarningBlocks("Driver is an instance of AndroidDriver");
	}

	@Then("I Start the ZAPScanner")
	public static void startZAP() throws IOException {
		if (ConfigurationManager.getBundle().getString("includeSecurityTests", "false").equalsIgnoreCase("true")) {
			String ZAPServerAddress = (String) ConfigurationManager.getBundle().getProperty("ZAPServerAddress");
			String ZAPServerPort = (String) ConfigurationManager.getBundle().getProperty("ZAPServerPort");
			String ZAPAPIKey = (String) ConfigurationManager.getBundle().getProperty("ZAPAPIKey");

			ZAPServerAddress = ZAPServerAddress.replaceFirst("^(http[s]?://)", "");

			ZAPClient zapClient;

			zapClient = new ZAPClient(ZAPServerAddress);

			zapClient.terminateZAP();
			zapClient.startZap(ZAPServerAddress, ZAPServerPort);

			ConfigurationManager.getBundle().setProperty("ZapClient", zapClient);
		}
	}

	

	@Then("I Close the ZAPScanner")
	public static void closeZAP() throws IOException {
		if(ConfigurationManager.getBundle().getString("includeSecurityTests", "false").equalsIgnoreCase("true")) {
			getZapClient().terminateZAP();
		}

	}


	@Then("I run automated scans, report security issues and download html ZAP report")
	public static void zapScanAndDownloadReport() throws Exception {
		if(ConfigurationManager.getBundle().getString("includeSecurityTests", "false").equalsIgnoreCase("true")) {

			String AUTUrl = (String) ConfigurationManager.getBundle().getProperty("AUTUrl");

			Field t = null;
			try {
				t = getZapClient().getClass().getDeclaredField("api");
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}

			ClientApi api = (ClientApi) t.get(getZapClient());

			//getZapClient().spiderScanZAP(api, DeviceUtils.getQAFDriver().getCurrentUrl());
			//getZapClient().activeScanZAP(api,DeviceUtils.getQAFDriver().getCurrentUrl());


			getZapClient().spiderScanZAP(api, AUTUrl);
			getZapClient().activeScanZAP(api,AUTUrl);

			SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
			Date date = new Date();
			System.out.println(formatter.format(date));


			String currentTestName = (String) ConfigurationManager.getBundle().getProperty("current.testcase.name");

			File createZapadir = new File("zap");
			createZapadir.mkdir();

			File createDir = new File("zap/" + currentTestName  + "_" +  formatter.format(date));
			createDir.mkdir();
			String finalReportPath = createDir.getPath() + "/ZAPReport_" + formatter.format(date) + ".html";

			getZapClient().reportAndExportAlertZAP(api, DeviceUtils.getQAFDriver().getCurrentUrl(), finalReportPath);
		}

	}



	private static ZAPClient getZapClient(){
		return (ZAPClient) ConfigurationManager.getBundle().getProperty("ZapClient");
	}
}