# Steps to deploy

## Preparation

1. Run tests
3. Increase the version in the `gradle.properties` `src/main/java/com.configcat/ConfigFetcher.java` file.
4. Commit & Push

## Publish

Use the **same version** for the git tag as in the properties file.

- Via git tag
    1. Create a new version tag.
       ```bash
       git tag v[MAJOR].[MINOR].[PATCH]
       ```
       > Example: `git tag v2.5.5`
    2. Push the tag.
       ```bash
       git push origin --tags
       ```
- Via Github release

  Create a new [Github release](https://github.com/configcat/java-sdk/releases) with a new version tag and release
  notes.

## Sync

1. Log in to [Maven Repository](https://oss.sonatype.org/) and follow these steps:
   1. Select `Staging Repositories` and select the version you published.
   2. Click `Close`. The process might take some time, click `Refresh` to get the latest state.
   3. When the repo is closed click `Release`, tick the `Automatically drop when released` option.
2. Make sure the new version is available
   on [Maven Central](https://search.maven.org/artifact/com.configcat/configcat-java-client).

## Update import examples in local README.md

## Update code examples in ConfigCat Dashboard projects

`Steps to connect your application`

1. Update Maven import examples.
2. Update Gradle import examples.

## Update import examples in Docs

## Update samples

Update and test sample apps with the new SDK version.
