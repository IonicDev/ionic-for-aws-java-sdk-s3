/*
 * S3SampleApp.java
 * The purpose of this project is to store an object in AWS S3 with client-side Ionic protection.
 * This code is an example of what clients would use programmatically to incorporate the Ionic platform
 * into their S3 use cases.
 * 
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 * Derived in part from AWS Sample S3 Project, S3Sample.java.
 */

package com.ionicsecurity.examples;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import com.ionicsecurity.ipcs.awss3.IonicEncryptionMaterialsProvider;
import com.ionicsecurity.ipcs.awss3.IonicS3EncryptionClientBuilder;
import com.ionicsecurity.ipcs.awss3.Version;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

public class S3SampleApp 
{

    enum Action {
        GETSTRING ("getString"),
        GETFILE ("getFile"),
        PUTSTRING("putString"),
        PUTFILE("putFile"),
        PUTMULTIPART ("putMultipart"),
        VERSION ("version"),
        ;
        
        final String str;
        
        Action (String name)
        {
            this.str = name;
        }
    }
    
    static void putString(String bucketName, String objectKey, String objectContent,
            AmazonS3Encryption s3, ObjectMetadata s3ObjectMetadata)
    {
        System.out.println("Putting object " + objectKey + " in bucket " + bucketName);
        // Treat as a string
        byte[] contentBytes = objectContent.getBytes(StringUtils.UTF8);
        InputStream is = new ByteArrayInputStream(contentBytes);
        s3ObjectMetadata.setContentLength(contentBytes.length);
        s3.putObject(bucketName, objectKey, is, s3ObjectMetadata);
    }
    
    static void putFile(String bucketName, String objectKey, String filePath, AmazonS3Encryption s3,
            ObjectMetadata s3ObjectMetadata) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            System.out.println("Putting object " + objectKey + " in bucket " + bucketName);
            PutObjectRequest req = new PutObjectRequest(bucketName, objectKey, file);
            req.setMetadata(s3ObjectMetadata);
            s3.putObject(req);
        } else {
            System.err.println("File " + filePath + " does not exist.");
            System.exit(1);
        }
    }
    
    static void getString(String bucketName, String objectKey, AmazonS3Encryption s3, boolean printingMetadata)
    {
        System.out.println("Getting object '" + objectKey + "' at '" + bucketName + "'");
        S3Object obj = s3.getObject(bucketName, objectKey);
        try {
            System.out.println(IOUtils.toString(obj.getObjectContent()));
        } catch (IOException e) {
            throw new SdkClientException("Error streaming content from S3 during download");
        }
        if (printingMetadata) {
            for (Map.Entry<String, String> entry : obj.getObjectMetadata().getUserMetadata().entrySet()) {
                System.out.println("Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        }
    }
    
    static void getFile(String bucketName, String objectKey, String destination, AmazonS3Encryption s3, boolean printingMetadata)
    {
        System.out.println("Getting object '" + objectKey + "' at '" + bucketName + "'");
        S3Object obj = s3.getObject(bucketName, objectKey);
        Path destinationPath = Paths.get(destination);
        if (destinationPath.toFile().exists())
        {
            destinationPath.toFile().delete();
        }
        try {
            Files.copy(obj.getObjectContent(), destinationPath);
        } catch (IOException e) {
            System.out.println("IOException with destination: " + destination);
            System.exit(1);
        }
        if (printingMetadata)
        {
            System.out.println("Metadata:");
            for(Map.Entry<String, String> entry : obj.getObjectMetadata().getUserMetadata().entrySet())
            {
                System.out.println("Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        }
    }
    
    static void putMultipart(String bucketName, String objectKey, String filePath, int partSize,
            AmazonS3Encryption s3, ObjectMetadata s3ObjectMetadata)
    {

        long partSizeBytes = partSize * 1024 * 1024; 

        if (partSizeBytes < 5 * 1024 *1024)
        {
            System.out.println(partSizeBytes + " < " + (5 * 1024 *1024));
            System.out.println("Part size must be 5(mb) or greater");
            System.exit(1);
        }

        File file = new File(filePath);

        List<PartETag> partETags = new ArrayList<PartETag>();

        long contentLength = file.length();
        long totalChunks = contentLength/partSizeBytes;
        if(contentLength % partSizeBytes != 0)
        {
            totalChunks++;
        }

        System.out.println("Initating Multipart Upload");
        System.out.println("With chunk size of " + partSizeBytes + "bytes");
        System.out.println(totalChunks + " parts to upload");

        // Step 1: Initiate multipart upload request
        InitiateMultipartUploadRequest req = 
                new InitiateMultipartUploadRequest(bucketName, objectKey, s3ObjectMetadata);
        InitiateMultipartUploadResult res = s3.initiateMultipartUpload(req);
        try {
            // Step 2: Upload parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) 
            {
                // Last part can be less than 5 MB. Adjust part size.
                partSizeBytes = Math.min(partSizeBytes, (contentLength - filePosition));

                boolean isLast = i == totalChunks;

                // Create request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName).withKey(objectKey)
                        .withUploadId(res.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSizeBytes)
                        .withLastPart(isLast);

                // Upload part and add response to our list.
                System.out.println("Uploading Part #" + i);
                partETags.add(
                        s3.uploadPart(uploadRequest).getPartETag());
                if (isLast)
                {
                    System.out.println("Uploaded last part");
                }

                filePosition += partSizeBytes;
            }

            // Step 3: Complete.
            CompleteMultipartUploadRequest compRequest = new 
                    CompleteMultipartUploadRequest(
                            bucketName, 
                            objectKey, 
                            res.getUploadId(), 
                            partETags);

            s3.completeMultipartUpload(compRequest);
            System.out.println("Upload Complete");
        }
        catch (Exception e) 
        {
            System.out.println("Exception Occured: " + e.getMessage());
            System.out.println("Aborting Upload");
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(
                    bucketName, objectKey, res.getUploadId()));
        }
    }
    
    static AmazonS3Encryption setUp()
    {
        // Set up IonicEncryptionMaterialsProvider and provide to constructor of IonicS3EncryptionClient
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        IonicEncryptionMaterialsProvider.setIonicMetadataMap(S3SampleApp.getMetadataMap());
        iemp.setEnabledMetadataCapture(true);
        iemp.setEnabledMetadataReturn(true);

        // Load a plain-text device profile (SEP) from disk 
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        try {
            ptPersistor.setFilePath(sProfilePath);
        } catch (IonicException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        iemp.setPersistor(ptPersistor);

        return IonicS3EncryptionClientBuilder.standard().withEncryptionMaterials(iemp).build();
    }
    
    public static void main(String[] args) throws IOException 
    {
        // Command Line Processing
        
        if (args.length == 0)
        {
            usage();
        }
        
        Action action = null;
        
        for ( Action a : Action.values())
        {
            if (a.str.equals(args[0]))
            {
                action = a;
                break;
            }
        }
        if(action == null)
        {
            usage();
        }
        
        boolean mFlag = false;
        
        if (args.length >= 2 && args[1].equals("-m"))
        {
            mFlag = true;
        }
        
        ObjectMetadata s3ObjectMetadata= new ObjectMetadata();
        AmazonS3Encryption s3;
        
        switch (action)
        {
            case PUTFILE:
                if (args.length >= 5)
                {
                    s3ObjectMetadata.setUserMetadata(mapFromString(args[4]));
                }
                if (args.length >= 4)
                {
                    s3 = setUp();
                    putFile(args[1], args[2], args[3], s3, s3ObjectMetadata);
                } 
                else
                {
                    usage();
                }
                break;
            case PUTSTRING:
                if (args.length >= 5)
                {
                    s3ObjectMetadata.setUserMetadata(mapFromString(args[4]));
                }
                if (args.length >= 4)
                {
                    s3 = setUp();
                    putString(args[1], args[2], args[3], s3, s3ObjectMetadata);
                } 
                else
                {
                    usage();
                }
                break;
                
            case PUTMULTIPART:
                if (args.length >= 6)
                {
                    s3ObjectMetadata.setUserMetadata(mapFromString(args[5]));
                }
                if (args.length >= 5)
                {
                    s3 = setUp();
                    putMultipart(args[1], args[2], args[3], Integer.parseInt(args[4]), s3, s3ObjectMetadata);
                } 
                else
                {
                    usage();
                }
                break;
            case GETSTRING:
                if (mFlag && args.length >= 4)
                {
                    s3 = setUp();
                    getString(args[2], args[3], s3, mFlag);
                }
                else if (!mFlag && args.length >= 3)
                {
                    s3 = setUp();
                    getString(args[1], args[2], s3, mFlag);
                }
                else
                {
                    usage();
                }
                break;
            case GETFILE:
                if (mFlag && args.length >= 5)
                {
                    s3 = setUp();
                    getFile(args[2], args[3], args[4], s3, mFlag);
                }
                else if (!mFlag && args.length >= 4)
                {
                    s3 = setUp();
                    getFile(args[1], args[2], args[3], s3, mFlag);
                }
                else
                {
                    usage();
                }
                break;
            case VERSION:
                System.out.println(Version.getFullVersion());
                break;
        }
    }

    private static void usage()
    {
	System.out.println("Usage: prog <put<x> command> | <get<x> command>");
	System.out.println("put<x> commands:");
	System.out.println("\tNOTE: <metadata> for these commands is a comma delimited list of string pairs");
        System.out.println("\t\tEx: \"Foo,Bar,Biz,Baz\" -> [Foo,Bar],[Biz,Biz]");
	System.out.println("");
        System.out.println("\tputString <bucketName> <objectKey> <objectContent> [<metadata>]");
        System.out.println("\tputFile <bucketName> <objectKey> <filePath> [<metadata>]");
        System.out.println("\tputMultipart <bucketName> <objectKey> <file> <partsize_mb> [<metadata>]");
	System.out.println("get<x> commands:");
	System.out.println("\tNOTE: The optional -m prints out metadata associated with the object");
	System.out.println("");
        System.out.println("\tgetFile [-m] <bucketName> <objectKey> <destinationPath>");
        System.out.println("\tgetString [-m] <bucketName> <objectKey>");
        System.exit(1);
    }

    private static Map<String, String> mapFromString(String str) {
        String[] tokens = str.split(",");
        Map<String, String> map = new HashMap<String, String>();
        if(tokens.length%2 != 0)
        {
            System.out.println("Odd number of tokens for metadata. Tokens must be provided in pairs.");
            System.out.println("Ex: \"Key1, Value1, Key2, Value2\"");
            System.exit(1);
        }
        for (int i=0; i<tokens.length-1; ){
            map.put(tokens[i++], tokens[i++]);
        }
        return map;
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
