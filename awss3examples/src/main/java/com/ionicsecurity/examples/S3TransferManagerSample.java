/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.examples;

import java.io.File;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.ionicsecurity.ipcs.awss3.IonicEncryptionMaterialsProvider;
import com.ionicsecurity.ipcs.awss3.IonicS3EncryptionClientBuilder;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;


public class S3TransferManagerSample {

    public static void main(String[] args) {

        if (args.length != 4)
        {
            usage();
        }
        
        int status = 0;
        
        String bucketName = args[1];
        String objectKey = args[2];
        File file = new File(args[3]);

        // Set up IonicEncryptionMaterialsProvider and provide to constructor of IonicS3EncryptionClient
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        IonicEncryptionMaterialsProvider.setIonicMetadataMap(S3SampleApp.getMetadataMap());

        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        try {
            ptPersistor.setFilePath(sProfilePath);
        } catch (IonicException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        iemp.setPersistor(ptPersistor);

        AmazonS3Encryption s3 = IonicS3EncryptionClientBuilder.standard().withEncryptionMaterials(iemp).build();

        TransferManager manager = TransferManagerBuilder.standard().withS3Client(s3).build();

        if (args[0].equals("put"))
        {
            Upload upload = manager.upload(new PutObjectRequest(bucketName, objectKey, file));
                try {
                    upload.waitForCompletion();
                } catch (AmazonClientException | InterruptedException e) {
                    status = 1;
                    e.printStackTrace();
                }
        }
        else if (args[0].equals("get"))
        {
            Download download = manager.download(bucketName, objectKey, file);
            try {
                download.waitForCompletion();
            } catch (AmazonClientException | InterruptedException e) {
                status = 1;
                e.printStackTrace();
            }
        }
        else
        {
            usage();
        }
        // Shut down transferManager and underlying S3 client
        manager.shutdownNow(true);
        System.exit(status);
    }

    private static void usage()
    {
        System.out.println("put <bucketName> <objectKey> <fileOriginPath>");
        System.out.println("get <bucketName> <objectKey> <fileDestinPath");
        System.exit(1);
    }

    public static MetadataMap getMetadataMap() 
    {
        MetadataMap mApplicationMetadata = new MetadataMap();
        mApplicationMetadata.set("ionic-application-name", "IonicS3Example");
        mApplicationMetadata.set("ionic-application-version", "0.0.1");
        mApplicationMetadata.set("ionic-client-type", "Java Application");
        mApplicationMetadata.set("ionic-client-version", "0.0");

        return mApplicationMetadata;
    }    
}
