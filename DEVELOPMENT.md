# Development

## Building the project

### First-time set-up
Follow [GitHub's "Managing your personal access tokens" guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) to set up a GitHub personal access token.

Set up a `GITHUB_USERNAME` environment variable storing your GitHub username.

Set up a `GITHUB_TOKEN` environment variable storing your GitHub personal access token.

To build the project for the first time, run

```sh
./gradlew build
```

and validate that it completes successfully.

### Subsequent builds
In order to clean up stale build artifacts and rebuild the API models based on your latest changes, run

```sh
./gradlew clean build
```

If you do not clean before building, your local environment may continue to use stale, cached artifacts in builds.

## Helpful commands

* `./gradlew build` - build project, run checkstyle, and run unit tests
* `./gradlew clean build` - clear build artifacts, rebuild project, run checkstyle, and run unit tests
* `./gradlew tasks` - list available Gradle tasks
* `./gradlew test` - run unit tests
* `./gradlew test --tests TestClass --info` - run unit tests from a specific test class with info-level logging, helpful when debugging errors
* `./gradlew test --tests TestClass.TestMethod --info` - run a specific unit test with info-level logging, helpful when debugging errors

## Troubleshooting

#### My local tests failed but the output doesn't include logs or stack traces needed to debug

Run `./gradlew build --info` to rerun the tests with info logging enabled, which will include logs and stack traces for failed tests.

#### My local builds are not picking up Gradle dependency changes

Run `./gradlew clean build --refresh-dependencies` to ignore your Gradle environment's cached entries for modules and artifacts, and download new versions if they have different published SHA1 hashsums.
