
## README.md for Ionic Developers

### Install Ionic SDK JAR 

1. [Download](https://dev-dashboard.ionic.com/#/downloads?tenant=5640bb430ea2684423e0655c) the "Java 2.0 SDK" 
2. Extract SDK zip
3. Add Ionic SDK JAR to Maven Local Repository with the appropriate package information:

~~~bash
mvn install:install-file -Dfile=ionic-sdk-2.0.0.jar  -DpomFile=pom.xml
~~~

> NOTE: ionic-sdk-2.0.0.jar is only compatible with Java 8

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
* Open the IAM console.
* From the navigation menu, click Users.
* Select your IAM user name.
* Click User Actions, and then click Manage Access Keys.
* Click Create Access Key.
* Your keys will look something like this: 
* Access key ID example: AKIAIOSFODNN7EXAMPLE 
* Secret access key example: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

#### Verify Location of AWS Credentials
Create .aws/ folder and credentials file if needed
* `~/.aws/credentials` on Linux, macOS, or Unix
* `C:\Users\USERNAME\.aws\credentials` on Windows

In the credentials file, enter the following:
```bash
[default]
aws_access_key_id = your_access_key_id
aws_secret_access_key = your_secret_access_key
```

Replacing `your_access_key_id` and `your_secret_access_key` with the keys created for the IAM user.

Then add the keys to your PATH:

 Mac:
  * `export AWS_ACCESS_KEY_ID=your_access_key_id`
  * `export AWS_SECRET_ACCESS_KEY=your_secret_access_key`
  * `export AWS_DEFAULT_REGION=your_region`

 Windows:
  * `set AWS_ACCESS_KEY_ID=your_access_key_id`
  * `set AWS_SECRET_ACCESS_KEY=your_secret_access_key`
  * `set AWS_DEFAULT_REGION=your_region`


#### Add default AWS region
Create .aws/config and enter the following:
```bash
[default]
region=your_region
```

### Build the Project
#### Establish IONIC_SDK_PATH
Note that you will need to set the environment variable $IONIC_SDK_PATH to point wherever you extracted the Ionic SDK. This directory should include the Lib and Include directories.
* Example: `export IONIC_SDK_PATH=/Users/jdoe/Desktop/{Folder containing ISAgentSDKJava}/`
#### Install Ionic SDK into Maven Local Cache
mvn install:install-file -Dfile=ionic-sdk-2.0.0.jar -DpomFile=pom.xml
#### Install Java Cryptography Extension
If you don't already have this extension: [JCE Unlimited Strength Jurisdiction Policy Files 8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html), download it and store it in `${java.home}/jre/lib/security/`.
#### Load Ionic Security Credentials
Ensure that your Ionic device credentials can be located at `${user.home}/.ionicsecurity/profile.pt`.
#### Build
`./build.sh`

### Usage

#### Sample App

After the build, a fat JAR of the sample app is produced at `awss3examples/target/S3SampleApp.jar`.

The user can run the program with either "put" or "get" commands with usage as follows:
* `./run.sh put <bucketname> <objectKey> <string>`
* `./run.sh put <bucketname> <objectKey> <file>`
* `./run.sh get <bucketname> <objectKey>`

#### Using the Library

Using the library as a developer is documented in the `docs/content/` directories.
The JAR for use here is produced in `ionics3/target/`, such as `ionics3/target/ionics3-0.6.0.jar`.
This JAR is thin, and needs the Ionic SDK and the AWS SDKs available during builds that use it.

