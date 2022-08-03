package com.quantum.zapIntegration;

import com.perfecto.reportium.client.ReportiumClient;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.quantum.utils.DeviceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.OutputType;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ZAPClient {

    private String ZAPServerAddress;
    public ClientApi api;

    public ZAPClient(String baseUrl) {
        this.ZAPServerAddress = baseUrl;
        String ZAPServerPort = (String) ConfigurationManager.getBundle().getProperty("ZAPServerPort");
        String ZAPAPIKey = (String) ConfigurationManager.getBundle().getProperty("ZAPAPIKey");

        api = new ClientApi(ZAPServerAddress, Integer.parseInt(ZAPServerPort), ZAPAPIKey);
    }

    public void startZap(String ZAPServerAddress, String ZAPServerPort) throws IOException {

        ZAPServerAddress = ZAPServerAddress.replaceFirst("^(http[s]?://)","");
        Process process;

        String ZAPInstallationPath = (String) ConfigurationManager.getBundle().getProperty("ZAPInstallationPath");

        // String[] cmd = {"sh", "zap/zap.sh", "-daemon", "-port", ZAPServerPort, "-host", ZAPServerAddress};

        if(System.getProperty("os.name").contains("Windows")) {
            process = Runtime.getRuntime().exec(
            		"cmd /c zap.bat" + " -daemon" + " -port "+ ZAPServerPort+  " -host "+ ZAPServerAddress, null, new File(ZAPInstallationPath + "\\"));

        } else {  	
        	String[] cmd = {"sh", ZAPInstallationPath + "/Java/zap.sh", "-daemon", "-port", ZAPServerPort, "-host", ZAPServerAddress};
        	process = Runtime.getRuntime().exec(cmd);
        }
        
        printConsoleLog(process);
    }

    public void terminateZAP() throws IOException {
        String port = (String) ConfigurationManager.getBundle().getProperty("ZAPServerPort");
        Process process = null;
        if(System.getProperty("os.name").contains("Windows")) 
        	process = Runtime.getRuntime().exec("npx.cmd kill-port " + Integer.parseInt(port));
        else
        	process = Runtime.getRuntime().exec("npx kill-port " + Integer.parseInt(port));
        printConsoleLog(process);
    }


    public void reportAndExportAlertZAP(ClientApi api, String target, String filepath) throws Exception {
        boolean violationsFound = false;
        System.out.println("Alerts:");
        ReportiumClient reportiumClient = (ReportiumClient) ConfigurationManager.getBundle().getObject("perfecto.report.client");

        FileWriter fw = new FileWriter(new File(filepath));
        fw.write(new String(api.core.htmlreport()));
        fw.close();

//        fw = new FileWriter(new File("ZAP/jsonreport.json"));
//        fw.write(new String(api.core.jsonreport()));
//        fw.close();

        JSONObject jsonObject = new JSONObject(new String(api.core.jsonreport(), StandardCharsets.UTF_8));
        JSONArray sites = (JSONArray) jsonObject.get("site");
        System.out.println(sites.length());
        for (int i=0; i < sites.length(); i++){
            //System.out.println(sites.get(i));
            JSONObject site = (JSONObject) sites.get(i);
            if(target.contains(site.get("@name").toString())){
                JSONArray alerts = (JSONArray) site.get("alerts");

                String perfectoReportMessage = null;

                System.out.println("Total Security Issues found = " + alerts.length());
                if(alerts.length() > 0) {
                    DeviceUtils.getQAFDriver().getScreenshotAs(OutputType.BASE64);
                    violationsFound = true;
                }
                for (int j=0; j < alerts.length(); j++){
                    JSONObject alert = (JSONObject) alerts.get(j);
                    perfectoReportMessage = String.format("Security Issue: "+ (j+1) +System.lineSeparator()+System.lineSeparator() + "URL: %s%n%n Alert Name: %s%n%n; Severity:\t%s%n%n; Confidence:\t\t%s%n%n; Recommended Solution:\t\t%s%n%n; IssueBackGround:\t\t%s%n%n; Instances:\t\t%s%n%n;",
                            site.get("@name"),alert.get("alert"), alert.get("riskdesc"), alert.get("confidence"), alert.get("solution"), alert.get("desc"), alert.get("instances"));
                    reportiumClient.reportiumAssert(perfectoReportMessage, false);
                    System.out.println(perfectoReportMessage);
                }
            }
        }

        if(violationsFound)
            throw new Exception("Security Vulnerability found on the current page. Please check the report");
    }
    public void activeScanZAP(ClientApi api, String target ) throws ClientApiException, InterruptedException {
        System.out.println("Active scan : " + target);
        int progress;
        String scanid;
        ApiResponse resp = api.ascan.scan(target, "True", "false", null, null, null);

        // The scan now returns a scan id to support concurrent scanning
        scanid = ((ApiResponseElement) resp).getValue();

        // Poll the status until it completes
        while (true) {
            Thread.sleep(5000);
            progress =
                    Integer.parseInt(
                            ((ApiResponseElement) api.ascan.status(scanid)).getValue());
            System.out.println("Active Scan progress : " + progress + "%");
            if (progress >= 100) {
                break;
            }
        }
        System.out.println("Active Scan complete");
    }

    public void spiderScanZAP(ClientApi api, String target ) throws ClientApiException, InterruptedException {
        // Start spidering the target
        System.out.println("Spider : " + target);
        // It's not necessary to pass the ZAP API key again, already set when creating the
        // ClientApi.
        ApiResponse resp = api.spider.scan(target,  null, null, null, null);
        String scanid;
        int progress;

        // The scan now returns a scan id to support concurrent scanning
        scanid = ((ApiResponseElement) resp).getValue();

        // Poll the status until it completes
        while (true) {
            Thread.sleep(1000);
            progress =
                    Integer.parseInt(
                            ((ApiResponseElement) api.spider.status(scanid)).getValue());
            System.out.println("Spider progress : " + progress + "%");
            if (progress >= 100) {
                break;
            }
        }
        System.out.println("Spider complete");
    }

    private static void printConsoleLog(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if ((line.contains("ZAP is now listening")) || (line.contains("Started BurpApplication in")) || (line.contains("Web server failed to start.")))
                break;
        }
    }
}
