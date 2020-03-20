/*
 * (c) 2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.samples;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClient;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClientBuilder;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPassword;
import com.ionic.sdk.error.IonicException;
import java.io.IOException;

/**
 * A hello world example using the IonicS3EncryptionClient client.
 */
public class HelloWorld {

    private static final String HOME = System.getProperty("user.home");

    public static void main(String... args) {
        // read persistor password from environment variable
        String persistorPassword = System.getenv("IONIC_PERSISTOR_PASSWORD");
        if (persistorPassword == null) {
            System.out.println("[!] Please provide the persistor password as env variable:"
                    + " IONIC_PERSISTOR_PASSWORD");
            System.exit(1);
        }

        // initialize agent
        Agent agent = new Agent();
        try {
            String persistorPath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pw";
            DeviceProfilePersistorPassword persistor =
                    new DeviceProfilePersistorPassword(persistorPath);
            persistor.setPassword(persistorPassword);
            agent.initialize(persistor);
        } catch (IonicException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }


        // Create a new instance of IonicS3EncryptionClient using agent. standard() will create
        // the underlying S3 client using the DefaultAWSCredentialsProviderChain. See
        // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        // for details.

        // The region for the client can be specified by using the .withRegion() method on the
        // builder. Otherwise the region will be resolved using the DefaultAwsRegionProviderChain.
        IonicS3EncryptionClient client =
                IonicS3EncryptionClientBuilder.standard().withIonicAgent(agent).buildIonic();

        // These values must be changed to the bucket and object key to be used.
        // Note that the credentials used in the previous step must have permissions to read and
        // write to the specified bucket in the region determined in the previous step.
        final String bucketName = "my-unique-bucket";
        final String objectKey = "my-object-key";

        // Upload a string to an object in the specified bucket.
        try {
            client.putObject(bucketName, objectKey, "Hello, World!");
        } catch (SdkClientException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // Download an object
        S3Object obj = null;
        try {
            obj = client.getObject(bucketName, objectKey);
        } catch (SdkClientException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // Get string from object
        String downloadedString = null;
        try {
            downloadedString = IOUtils.toString(obj.getObjectContent());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // print string
        System.out.println(downloadedString);

        // exit
        System.exit(0);
    }
}
