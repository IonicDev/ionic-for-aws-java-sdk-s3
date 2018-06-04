
## README.md for Ionic Developers

### Install Ionic SDK JAR

1. [Download](https://dev-dashboard.ionic.com/#/downloads?tenant=5640bb430ea2684423e0655c) the "Java 2.1.0 SDK"
2. Extract SDK zip
3. Add Ionic SDK JAR to Maven Local Repository with the appropriate package information:

~~~bash
mvn install:install-file -Dfile=ionic-sdk-2.1.0.jar  -DpomFile=pom.xml
~~~

> NOTE: ionic-sdk-2.1.0.jar is only compatible with Java 7 & 8

> NOTE: Because Ionic uses strong 256-bit keys for encryption, the standard cryptography library built into Java will
> require that you have installed the [Unlimited Strength Java Cryptography Extension](https://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).

## AWS Documentation:
* [AWS SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-install.html)
* [AWS Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)
* [AWS CLI](https://aws.amazon.com/cli/)

### Amazon Standard SDKs
* If you don't already have an AWS Account, go to [Sign In or Create an AWS Account](https://aws.amazon.com/). Select "Create an AWS Account" and follow the instructions to create and configure your AWS account.

### Install AWS SDK
* `git clone https://github.com/aws/aws-sdk-java.git`
* `cd aws-sdk-java/`
* If you have a gpg signing key `mvn clean install` will attempt to sign the SDK jar file using the key.
* If you want to skip the signing process use `mvn clean install -Dgpg.skip=true`


### Setup AWS Credentials
#### Create IAM User
The IAM user will be used to put and get objects from the AWS Bucket
* Open the [IAM console](https://console.aws.amazon.com/console/home).
* From the navigation menu, select your username and select My Security Credentials
* Select the Access keys tab
* Click the Create New Access Key button to generate Access key ID and Secret access key
* Access key ID example: AKIAIOSFODNN7EXAMPLE
* Secret access key example: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

## Add AWS Credentials and Default Region to your machine

Either add your credentials and default region to your environment or set them in the `.aws` directory in your HOME.

#### Adding the keys and region to your .aws directory

Create .aws/ folder and credentials file if needed
* `~/.aws/credentials` on Linux, macOS, or Unix
* `C:\Users\USERNAME\.aws\credentials` on Windows

In the credentials file, enter the following:
```bash
[default]
aws_access_key_id = your_access_key_id
aws_secret_access_key = your_secret_access_key
```
Create .aws/config and enter the following replacing `your_default_region` with the region you wish to use:

* [AWS REGIONS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html)

```bash
[default]
region=your-default-region
```

#### Adding the keys and region to your env

Replace `your_access_key_id` and `your_secret_access_key` with the keys created for the IAM user and `your_default_region` with the region you wish to use for IPCS S3.

* [AWS REGIONS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html)

 Mac:
  * `export AWS_ACCESS_KEY_ID=your_access_key_id`
  * `export AWS_SECRET_ACCESS_KEY=your_secret_access_key`
  * `export AWS_DEFAULT_REGION=your_default_region`

 Windows:
  * `set AWS_ACCESS_KEY_ID=your_access_key_id`
  * `set AWS_SECRET_ACCESS_KEY=your_secret_access_key`
  * `set AWS_DEFAULT_REGION=your_default_region`


### Build the Project

`./build.sh` or `build.bat`

### Usage

#### Sample App

After the build, a fat JAR of the sample app is produced at `awss3examples/target/S3SampleApp.jar`.

Ensure that your Ionic device credentials can be located at `${user.home}/.ionicsecurity/profiles.pt`.

The user can run the program with either "put" or "get" commands with usage as follows:
* `./run.sh  putString <bucketName> <objectKey> <objectContent> [<metadata>]`
* `./run.sh  putFile <bucketName> <objectKey> <filePath> [<metadata>]`
* `./run.sh  putMultipart <bucketName> <objectKey> <file> <partsize_mb> [<metadata>]`
* `./run.sh  getFile [-m] <bucketName> <objectKey> <destinationPath>`
* `./run.sh  getString [-m] <bucketName> <objectKey>`

#### Using the Library

Using the library as a developer is documented in the `docs/content/` directories.
The JAR for use here is produced in `ionics3/target/`, such as `ionics3/target/ionics3-0.7.0.jar`.
This JAR is thin, and needs the Ionic SDK and the AWS SDKs available during builds that use it.
