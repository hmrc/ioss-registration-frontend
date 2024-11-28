
# ioss-registration-frontend

This is the repository for Import One Stop Shop Registration Frontend

Backend: https://github.com/hmrc/ioss-registration

Stub: https://github.com/hmrc/ioss-registration-stub

Import One Stop Shop Registration Service
------------

The main function of this service is to allow traders to register to pay VAT on imports of low value goods to consumers
in the EU, Northern Ireland or both. This will then provide them with an IOSS enrolment that will allow access to other
IOSS services - Returns (ioss-returns-frontend) and Exclusions (ioss-exclusions-frontend).

Once a trader has been registered, there are a few other functions that are available in the service:

Amend - this allows the trader to amend details they used on their original registration to keep
their information up to date.

Rejoin - Once a trader has left the Import One Stop Shop service, if they would like to rejoin, they can access this
option and all of their previous registration data will be offered to reuse and edit on the rejoin.

Summary of APIs
------------

This service utilises various APIs from other platforms in order to obtain and store information required for the
registration process.

ETMP:
- HMRC VAT registration details are retrieved
- Submitted registration details are passed to ETMP for storing and later querying against

Core:
- EU VAT registrations are verified with Core to check for any exclusions

Note: locally (and on staging) these connections are stubbed via ioss-registration-stub.

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


## Run the application locally via Service Manager

```
sm2 --start IMPORT_ONE_STOP_SHOP_ALL
```

### To run the application locally from the repository, execute the following:

The service needs to run in testOnly mode in order to access the testOnly get-passcodes endpoint which will generate a passcode for email verification.
```
sm2 --stop IOSS_REGISTRATION_FRONTEND
```
and
```
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Running correct version of mongo
Mongo 6 with a replica set is required to run the service. Please refer to the MDTP Handbook for instructions on how to run this


### Using the application

Access the Authority Wizard to log in:
http://localhost:9949/auth-login-stub/gg-sign-in

Enter the following details on this page and submit:
- Redirect URL: http://localhost:10190/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop
- Affinity Group: Organisation
- Enrolments:
- Enrolment Key: HMRC-MTD-VAT
- Identifier Name: VRN
- Identifier Value: 100000001

### Registration journey

It is recommended to use VRN 100000001 for a straightforward registration journey, hence why this one is used as
the "Identifier Value" above. Other scenarios can be found in ioss-registration-stub.

To enter the registration journey, you will need to complete the initial filter questions as follows:
1. Is your business already registered for the Import One Stop Shop scheme in an EU country?
- No
2. Does your business sell goods imported from countries outside the EU and Northern Ireland to consumers in the EU 
or Northern Ireland?
- Yes
3. Do any of these goods have a consignment value of £135 or less?
- Yes
4. Is your business registered for VAT in the UK?
- Yes
5. Is your business in Northern Ireland?
- Yes

Continue through the journey, completing each question through to the final check-your-answers page and submit the
registration.

Email verification:
Use the test-only endpoint (http://localhost:10190/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop/test-only/get-passcodes)
in a separate tab to generate a passcode that can be entered into the email verification page, following adding
an email to the /business-contact-details page


Note: you can refer to the Registration.feature within ioss-registration-journey-tests if any examples of data
to input are required.

### Amend registration journey

Access the Authority Wizard to log in:
http://localhost:9949/auth-login-stub/gg-sign-in

Enter the following details on this page and submit:
- Redirect URL: http://localhost:10190/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop/start-amend-journey
- Affinity Group: Organisation
- Enrolments (there are two rows this time):
- Enrolment Key: HMRC-MTD-VAT
- Identifier Name: VRN
- Identifier Value: 100000001
- Enrolment Key: HMRC-IOSS-ORG
- Identifier Name: IOSSNumber
- Identifier Value: IM9001234567

It is recommended to use VRN 100000001 and IOSS Number IM9001234567 for a regular amend journey, however alternatives 
can be found in the ioss-registration-stub.

### Rejoin registration journey

Access the Authority Wizard to log in:
http://localhost:9949/auth-login-stub/gg-sign-in

Enter the following details on this page and submit:
- Redirect URL: http://localhost:10190/pay-vat-on-goods-sold-to-eu/register-for-import-one-stop-shop/start-rejoin-journey
- Affinity Group: Organisation
- Enrolments (there are two rows this time):
- Enrolment Key: HMRC-MTD-VAT
- Identifier Name: VRN
- Identifier Value: 100000001
- Enrolment Key: HMRC-IOSS-ORG
- Identifier Name: IOSSNumber
- Identifier Value: IM9019999998

It is recommended to use VRN 100000001 and IOSS Number IM9019999998 for a regular rejoin journey, however alternatives 
can be found in the ioss-registration-stub.

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session on the browser.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *AddTradingNameControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *AuthenticatedSessionRepositorySpec
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
