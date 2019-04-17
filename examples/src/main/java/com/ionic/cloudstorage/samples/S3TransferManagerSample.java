/*
 * (c) 2017-2019 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.samples;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.ionic.cloudstorage.awss3.IonicEncryptionMaterialsProvider;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClientBuilder;
import com.ionic.cloudstorage.awss3.Version;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;


public class S3TransferManagerSample {

    public static final boolean useSandbox = true; // if true limit file paths to within user's
                                                   // home dir
    private static String HOME = System.getProperty("user.home");

    public static void main(String[] args) {

        if (args.length != 4) {
            usage();
            return;
        }

        String bucketName = args[1];
        String objectKey = args[2];

        Path xfrFilePath = null;
        try {
            xfrFilePath = Paths.get(Paths.get(args[3]).toFile().getCanonicalPath());
        } catch (NullPointerException e) {
            System.err.println("Missing source file pathname");
            return;
        } catch (IOException e) {
            System.err.println("Path IOError");
            return;
        }

        // Sandbox within user home
        if ((useSandbox) && (!xfrFilePath.startsWith(HOME))) {
            System.err.println("Filepath outside of user home");
            return;
        }
        if (!Files.exists(xfrFilePath)) {
            System.err.println("Transfer file does not exist.");
            return;
        }
        if (!Files.isRegularFile(xfrFilePath)) {
            System.err.println("Transfer file not a file.");
            return;
        }

        // Set up IonicEncryptionMaterialsProvider and provide to constructor of
        // IonicS3EncryptionClient
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        iemp.setIonicMetadataMap(S3SampleApp.getMetadataMap());

        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        try {
            String sProfilePath =
                    Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile().getCanonicalPath();
            ptPersistor.setFilePath(sProfilePath);
            iemp.setPersistor(ptPersistor);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        } catch (IonicException e) {
            System.err.println(e.getMessage());
            return;
        }

        AmazonS3Encryption s3 = null;

        try {
            s3 = IonicS3EncryptionClientBuilder.standard().withEncryptionMaterials(iemp).build();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        TransferManager manager = TransferManagerBuilder.standard().withS3Client(s3).build();

        if (args[0].equals("put")) {
            Upload upload = manager
                    .upload(new PutObjectRequest(bucketName, objectKey, xfrFilePath.toFile()));
            try {
                upload.waitForCompletion();
            } catch (AmazonClientException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        } else if (args[0].equals("get")) {
            Download download = manager.download(bucketName, objectKey, xfrFilePath.toFile());
            try {
                download.waitForCompletion();
            } catch (AmazonClientException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        } else {
            usage();
        }
        // Shut down transferManager and underlying S3 client
        manager.shutdownNow(true);
        return;
    }

    private static void usage() {
        System.out.println("put <bucketName> <objectKey> <fileOriginPath>");
        System.out.println("get <bucketName> <objectKey> <fileDestinPath");
    }

    public static MetadataMap getMetadataMap() {
        MetadataMap mApplicationMetadata = new MetadataMap();
        mApplicationMetadata.set("ionic-application-name", "IonicS3TransferManagerExample");
        mApplicationMetadata.set("ionic-application-version", Version.getFullVersion());
        mApplicationMetadata.set("ionic-client-type", "IPCS S3 Java");
        mApplicationMetadata.set("ionic-client-version", Version.getFullVersion());

        return mApplicationMetadata;
    }
}
