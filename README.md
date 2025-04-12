# consent-expiry-processor
Service code for the Consent Auto Expiry Processor, which updates the status of active consents past their scheduled expiry time.

## Technologies
[AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide) is used to write Java application code integrating with AWS services such as DynamoDB.

[GitHub Actions](https://docs.github.com/en/actions) are used to automatically build and run unit tests against service code changes pushed or submitted through pull requests.

[Gradle](https://docs.gradle.org) is used to build the project and manage package dependencies.

## License
The code in this project is released under the [GPL-3.0 License](LICENSE).
