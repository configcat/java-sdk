# Steps to deploy

## Preparation

1. Run tests
3. Increase the version in the `gradle.properties` file.
4. Commit & Push

## Publish

Use the **same version** for the git tag as in the properties file.

- Via git tag
    1. Create a new version tag.
       ```bash
       git tag [MAJOR].[MINOR].[PATCH]
       ```
       > Example: `git tag 2.5.5`
    2. Push the tag.
       ```bash
       git push origin --tags
       ```
- Via Github release

  Create a new [Github release](https://github.com/configcat/java-sdk/releases) with a new version tag and release
  notes.

## Sync

1. Log in to [bintray.com](https://bintray.com/configcat/releases/configcat-java-client#central) and sync the new
   package to Maven Central.
2. Make sure the new version is available on [jcenter](https://bintray.com/configcat/releases/configcat-java-client).
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
