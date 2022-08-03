@ZAPSecurityTest
Feature: Appium Example With Automated Security Scan Feature
  #Sample Test Scenario Description

 @WebGoatSecurityTest_LoginPage
Scenario: Verify WebGoat login Page with Automated Security Scan using ZAP
	Given I Start the ZAPScanner
	Then I launch WebGoat Login page
	And I run automated scans, report security issues and download html ZAP report
	Then I Close the ZAPScanner

	@WebGoatSecurityTest_HomePage
	Scenario: Verify WebGoat Home Page with Automated Security Scan using ZAP
		When I start the ZAPScanner
		Then I login the WebGoat app with "guestuser" and "guest123" credentials
		And Verify the home page
		Then I run automated scans, report security issues and download html ZAP report
		Then I Close the ZAPScanner