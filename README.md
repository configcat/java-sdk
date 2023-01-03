# ConfigCat SDK for Java
https://configcat.com

ConfigCat SDK for Java provides easy integration for your application to ConfigCat.

ConfigCat is a feature flag and configuration management service that lets you separate feature releases from code deployments. You can turn features ON or OFF using the <a href="http://app.configcat.com" target="_blank">ConfigCat Dashboard</a> even after they are deployed. ConfigCat lets you target specific groups of users based on region, email, or any other custom user attribute.

ConfigCat is a <a href="https://configcat.com" target="_blank">hosted feature flag service</a> that lets you manage feature toggles across frontend, backend, mobile, and desktop apps. <a href="https://configcat.com" target="_blank">Alternative to LaunchDarkly</a>. Management app + feature flag SDKs.

[![Java CI](https://github.com/configcat/java-sdk/actions/workflows/java-ci.yml/badge.svg?branch=master)](https://github.com/configcat/java-sdk/actions/workflows/java-ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-java-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.configcat/configcat-java-client)
[![Javadocs](http://javadoc.io/badge/com.configcat/configcat-java-client.svg)](http://javadoc.io/doc/com.configcat/configcat-java-client)
[![Coverage Status](https://img.shields.io/sonar/coverage/configcat_java-sdk?logo=SonarCloud&server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/project/overview?id=configcat_java-sdk)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=configcat_java-sdk&metric=alert_status)](https://sonarcloud.io/dashboard?id=configcat_java-sdk)
![License](https://img.shields.io/github/license/configcat/java-sdk.svg)

## Getting started

### 1. Install the ConfigCat SDK
*Maven:*
```xml
<dependency>
  <groupId>com.configcat</groupId>
  <artifactId>configcat-java-client</artifactId>
  <version>[8.0.0,)</version>
</dependency>
```
*Gradle:*
```groovy
implementation "com.configcat:configcat-java-client:8.+"
```

### 2. Go to the <a href="https://app.configcat.com/sdkkey" target="_blank">ConfigCat Dashboard</a> to get your *SDK Key*:
![SDK-KEY](https://raw.githubusercontent.com/ConfigCat/java-sdk/master/media/readme02-3.png  "SDK-KEY")

### 3. Import *com.configcat.** in your application code
```java
import com.configcat.*;
```

### 4. Create a *ConfigCat* client instance
```java
ConfigCatClient client = ConfigCatClient.get("#YOUR-SDK-KEY#");
```

### 5. Get your setting value
```java
boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "isMyAwesomeFeatureEnabled", false);
if(isMyAwesomeFeatureEnabled) {
    doTheNewThing();
} else{
    doTheOldThing();
}
```
Or use the async APIs:
```java
client.getValueAsync(Boolean.class, "isMyAwesomeFeatureEnabled", false)
    .thenAccept(isMyAwesomeFeatureEnabled -> {
        if(isMyAwesomeFeatureEnabled) {
            doTheNewThing();
        } else{
            doTheOldThing();
        }
    });
```

### 6. Close the client on application exit
```java
client.close();
```

## Getting user-specific setting values with Targeting
Using this feature, you will be able to get different setting values for different users in your application by passing a `User Object` to the `getValue()` function.

Read more about Targeting [here](https://configcat.com/docs/advanced/targeting/).


## User Object
Percentage and targeted rollouts are calculated by the user object passed to the configuration requests.
The user object must be created with a **mandatory** identifier parameter which uniquely identifies each user:
```java
User user = User.newBuilder()
        .build("#USER-IDENTIFIER#"); // mandatory

boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "isMyAwesomeFeatureEnabled", user, false);
if(isMyAwesomeFeatureEnabled) {
    doTheNewThing();
} else{
    doTheOldThing();
}
```

## Sample/Demo app
* [Sample Console App](https://github.com/ConfigCat/java-sdk/tree/master/samples/console)
* [Sample Web app](https://github.com/ConfigCat/java-sdk/tree/master/samples/web)

## Polling Modes
The ConfigCat SDK supports three different polling mechanisms to acquire the setting values from ConfigCat. After the latest setting values are downloaded, they are stored in an internal cache . After that, all requests are served from the cache. Read more about Polling Modes and how to use them at [ConfigCat Java Docs](https://configcat.com/docs/sdk-reference/java/) or [ConfigCat Android Docs](https://configcat.com/docs/sdk-reference/android/).

## Support
If you need help using this SDK, feel free to contact the ConfigCat Staff at [https://configcat.com](https://configcat.com). We're happy to help.

## Contributing
Contributions are welcome. For more info please read the [Contribution Guideline](CONTRIBUTING.md).

## About ConfigCat
- [Official ConfigCat SDKs for other platforms](https://github.com/configcat)
- [Documentation](https://configcat.com/docs)
- [Blog](https://configcat.com/blog)
