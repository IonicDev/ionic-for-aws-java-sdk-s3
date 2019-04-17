
## Setup Instructions for Developers

## AWS Documentation:
* [AWS SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-install.html)
* [AWS Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)
* [AWS CLI](https://aws.amazon.com/cli/)

### Amazon Standard SDKs
* If you don't already have an AWS Account, go to [Sign In or Create an AWS Account](https://aws.amazon.com/). Select "Create an AWS Account" and follow the instructions to create and configure your AWS account.

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
  * `export AWS_REGION=your_default_region`

 Windows:
  * `set AWS_ACCESS_KEY_ID=your_access_key_id`
  * `set AWS_SECRET_ACCESS_KEY=your_secret_access_key`
  * `set AWS_REGION=your_default_region`


### Build the Project
To build and install the Ionic S3 sdk `mvn install`

To build the IonicS3SampleApp from ./examples `mvn package`

### Usage

#### Sample App

After the build, a fat JAR of the sample app is produced at `./examples/target/IonicS3SampleApp.jar`.

Ensure that your Ionic device credentials can be located at `${user.home}/.ionicsecurity/profiles.pt`.

The user can run the program from /awss3examples with either "put" or "get" commands with usage as follows:
* `./run.sh  putString <bucketName> <objectKey> <objectContent> [<metadata>]`
* `./run.sh  putFile <bucketName> <objectKey> <filePath> [<metadata>]`
* `./run.sh  putMultipart <bucketName> <objectKey> <file> <partsize_mb> [<metadata>]`
* `./run.sh  getFile <bucketName> <objectKey> <destinationPath>`
* `./run.sh  getString <bucketName> <objectKey>`
Windows users should use ./run.bat instead

Note: S3SampleApp does not protect against invalid entry of AWS S3 bucket names or Object Keys
      Current Rules for naming S3 buckets can be found at:
          https://docs.aws.amazon.com/AmazonS3/latest/dev//BucketRestrictions.html#bucketnamingrules
      Current Rules for specifying Object Keys can be found at:
          https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html#object-keys

#### Using the Library

Developer documentation for this library can be found at https://dev.ionic.com/integrations/aws-s3.
The JAR for use here is produced in `./target/`, such as `./target/ionic-for-aws-java-sdk-s3-1.0.0.jar`.
This JAR is thin, and needs the Ionic SDK and the AWS SDKs available during builds that use it.
