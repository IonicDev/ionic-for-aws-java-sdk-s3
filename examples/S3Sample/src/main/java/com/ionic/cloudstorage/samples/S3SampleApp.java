/*
 * S3SampleApp.java The purpose of this project is to store an object in AWS S3 with client-side
 * Ionic protection. This code is an example of what clients would use programmatically to
 * incorporate the Ionic platform into their S3 use cases.
 *
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/). Derived in part from AWS Sample S3 Project,
 * S3Sample.java.
 */

package com.ionic.cloudstorage.samples;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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
import com.ionic.cloudstorage.awss3.IonicEncryptionMaterialsProvider;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClient;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClientBuilder;
import com.ionic.cloudstorage.awss3.Version;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3SampleApp {

    enum Action {
        GETSTRING("getString"),
        GETFILE("getFile"),
        PUTSTRING("putString"),
        PUTFILE("putFile"),
        PUTMULTIPART("putMultipart"),
        VERSION("version"),;

        final String str;

        Action(String name) {
            this.str = name;
        }
    }

    // AWS S3 limits multipart uploads to 10000 parts
    public static final int MAX_AWS_SDK_CHUNKS = 10000;
    private static final String HOME = System.getProperty("user.home");

    static void putString(String bucketName, String objectKey, String objectContent,
            IonicS3EncryptionClient s3, KeyAttributesMap attributes) {
        System.out.println("Putting object as string in specified S3 bucket");
        if (attributes != null) {
            s3.putObject(bucketName, objectKey, objectContent,
                    new CreateKeysRequest.Key("", 1, attributes));
        } else {
            s3.putObject(bucketName, objectKey, objectContent);
        }
    }

    static void putFile(String bucketName, String objectKey, String filePath,
            IonicS3EncryptionClient s3, KeyAttributesMap attributes) {

        String srcFilePathStr = getCanonicalPathString(filePath);

        if ((srcFilePathStr == null) || (srcFilePathStr.isEmpty())) {
            System.err.println("No filepath specified");
            return;
        }

        Path srcFilePath = Paths.get(srcFilePathStr);

        if (!Files.exists(srcFilePath)) {
            System.err.println("File " + srcFilePathStr + " does not exist.");
            return;
        }
        if (!Files.isRegularFile(srcFilePath)) {
            System.err.println("File " + srcFilePathStr + " not a file.");
            return;
        }

        System.out.println("Putting object file in S3 bucket ");
        PutObjectRequest req = new PutObjectRequest(bucketName, objectKey, srcFilePath.toFile());
        if (attributes != null) {
            s3.putObject(req, new CreateKeysRequest.Key("", 1, attributes));
        } else {
            s3.putObject(req);
        }
    }

    static void getString(String bucketName, String objectKey, AmazonS3Encryption s3) {
        System.out.println("Getting object as string from specified S3 bucket");
        S3Object obj = s3.getObject(bucketName, objectKey);
        try {
            System.out.println(IOUtils.toString(obj.getObjectContent()));
        } catch (IOException e) {
            System.err.println("IOException reading content from S3 object");
        }
    }

    static void getFile(String bucketName, String objectKey, String destination,
            AmazonS3Encryption s3) {
        String destFilePathStr = getCanonicalPathString(destination);

        if ((destFilePathStr == null) || (destFilePathStr.isEmpty())) {
            System.err.println("No filepath specified");
            return;
        }

        Path destFilePath = Paths.get(destFilePathStr);

        // Check if file already exists but is not a file (e.g. don't try to overwrite a directory)
        if ((Files.exists(destFilePath)) && (!Files.isRegularFile(destFilePath))) {
            System.err.println("File " + destFilePathStr + " not a file");
            return;
        }

        try {
            // Safe to delete existing file
            Files.deleteIfExists(destFilePath);
        } catch (IOException e) {
            System.err.println("Exception deleting file " + destFilePathStr + "\n:"
                    + e.getMessage());
            return;
        }

        System.out.println("Getting object from S3 bucket");
        S3Object obj = s3.getObject(bucketName, objectKey);
        try {
            Files.copy(obj.getObjectContent(), destFilePath);
        } catch (IOException e) {
            System.err.println("Exception writing to " + destFilePathStr + "\n:" + e.getMessage());
            return;
        }
    }

    static void putMultipart(String bucketName, String objectKey, String filePath, int partSize,
            IonicS3EncryptionClient s3, KeyAttributesMap attributes) throws SdkClientException {

        long partSizeBytes = ((long) partSize * 1024 * 1024);

        if (partSizeBytes < (5L * 1024 * 1024)) {
            System.out.println("Part size must be 5(mb) or greater");
            return;
        }

        String srcFilePathStr = getCanonicalPathString(filePath);

        if ((srcFilePathStr == null) || (srcFilePathStr.isEmpty())) {
            System.err.println("No filepath specified");
            return;
        }

        Path srcFilePath = Paths.get(srcFilePathStr);

        if (!Files.exists(srcFilePath)) {
            System.err.println("Source file does not exist");
            return;
        }
        if (!Files.isRegularFile(srcFilePath)) {
            System.err.println("Source file is not a file");
            return;
        }

        File file = srcFilePath.toFile();

        List<PartETag> partETags = new ArrayList<PartETag>();

        long contentLength = file.length();
        if (partSizeBytes <= 0) {
            System.out.println("Invalid partition size");
            return;
        }
        long totalChunks = contentLength / partSizeBytes;

        if (contentLength % partSizeBytes != 0) {
            totalChunks++;
        }

        if (totalChunks == 0L) {
            System.out.println("No Multipart upload of an empty file");
            return;
        }
        if (totalChunks == 1L) {
            putFile(bucketName, objectKey, srcFilePathStr, s3, attributes);
            return;
        }
        // Note: Number of parts for Amazon S3 must be between 1 and 10000
        // https://docs.aws.amazon.com/AmazonS3/latest/API/mpUploadUploadPart.html
        if (totalChunks > (long) MAX_AWS_SDK_CHUNKS) {
            System.out.println("File must be limited to " + MAX_AWS_SDK_CHUNKS
                    + " partitions for multipart upload");
            return;
        }
        int totalChunksInt = (int) totalChunks;

        System.out.println("Initating Multipart Upload");
        System.out.println("With chunk size of " + partSizeBytes + "bytes");
        System.out.println(totalChunksInt + " parts to upload");

        // Step 1: Initiate multipart upload request
        InitiateMultipartUploadRequest req =
                new InitiateMultipartUploadRequest(bucketName, objectKey, null);
        InitiateMultipartUploadResult res;
        if (attributes != null) {
            res = s3.initiateMultipartUpload(req, new CreateKeysRequest.Key("", 1, attributes));
        } else {
            res = s3.initiateMultipartUpload(req);
        }

        // Step 2: Upload parts.
        long filePosition = 0L;
        for (int i = 1; i <= Math.min(MAX_AWS_SDK_CHUNKS, totalChunksInt); i++) {
            if (filePosition >= contentLength) {
                break;
            }
            // Last part can be less than 5 MB. Adjust part size.
            partSizeBytes = Math.min(partSizeBytes, (contentLength - filePosition));

            boolean isLast = i == totalChunksInt;

            // Create request to upload a part.
            UploadPartRequest uploadRequest =
                    new UploadPartRequest()
                    .withBucketName(bucketName)
                    .withKey(objectKey)
                    .withUploadId(res.getUploadId())
                    .withPartNumber(i)
                    .withFileOffset(filePosition)
                    .withFile(file)
                    .withPartSize(partSizeBytes)
                    .withLastPart(isLast);

            // Upload part and add response to our list.
            System.out.println("Uploading Part #" + i + " of " + totalChunksInt);
            partETags.add(s3.uploadPart(uploadRequest).getPartETag());
            if (isLast) {
                System.out.println("Uploaded last part");
            }

            filePosition += partSizeBytes;
        }

        // Step 3: Complete.
        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
                bucketName, objectKey, res.getUploadId(), partETags);

        s3.completeMultipartUpload(compRequest);
        System.out.println("Upload Complete");
    }

    static IonicS3EncryptionClient setUp()
            throws IOException, IllegalArgumentException, IonicException {
        // Load a plain-text device profile (SEP) from disk
        String profilePath = Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile()
                .getCanonicalPath();
        Agent agent = new Agent(new DeviceProfilePersistorPlainText(profilePath));
        agent.setMetadata(getMetadataMap());
        return IonicS3EncryptionClientBuilder.standard().withIonicAgent(agent).buildIonic();
    }

    public static void main(String[] args) {

        Action action = null;
        final int actionArg = 0;
        final int bucketNameArg = 1;
        final int objectKeyArg = 2;
        final int objectContentArg = 3;
        final int filePathArg = 3;
        final int partSizeArg = 4;
        int attributesArg = 4;

        // Command Line Processing
        if (args.length > actionArg) {
            // Determine Action (e.g. getString)
            for (Action a : Action.values()) {
                if (a.str.equals(args[actionArg])) {
                    action = a;
                    break;
                }
            }
            if (action == null) {
                usage();
                return;
            } else if (action == Action.VERSION) {
                System.out.println(Version.getFullVersion());
                System.exit(0);
            }

        } else {
            usage();
            return;
        }

        String bucketName = null;
        String objectKey = null;
        String objectContent = null;
        String filePath = null;
        KeyAttributesMap attributes = null;

        if (args.length > objectKeyArg) {
            bucketName = new String(args[bucketNameArg].getBytes(StringUtils.UTF8));
            objectKey = new String(args[objectKeyArg].getBytes(StringUtils.UTF8));
        }

        if (args.length > objectContentArg) {
            objectContent = new String(args[objectContentArg].getBytes(StringUtils.UTF8));
        }

        if (args.length > filePathArg) {
            filePath = new String(args[filePathArg].getBytes(StringUtils.UTF8));
        }

        if (action == Action.PUTMULTIPART) {
            // increment to account for addition of partSize arg
            attributesArg++;
        }

        if (args.length > attributesArg) {
            attributes = parseAttributes(args[attributesArg]);
            if (attributes == null) {
                return;
            }
        }

        try {
            IonicS3EncryptionClient s3 = setUp();
            switch (action) {
                case PUTFILE:
                    if (args.length > filePathArg) {
                        putFile(bucketName, objectKey, filePath, s3, attributes);
                    } else {
                        usage();
                    }
                    break;
                case PUTSTRING:
                    if (args.length > filePathArg) {
                        putString(bucketName, objectKey, objectContent, s3, attributes);
                    } else {
                        usage();
                    }
                    break;
                case PUTMULTIPART:
                    if (args.length > partSizeArg) {
                        // Parse the size of each part being uploaded in MB
                        // Parts must be at least 5MB (except for last part which may be smaller)
                        // https://docs.aws.amazon.com/AmazonS3/latest/API/mpUploadUploadPart.html
                        int partSizeMB = Math.max(Integer.parseInt(args[partSizeArg]), 0);
                        if ((partSizeMB == 0) || (partSizeMB < 5)) {
                            System.out.println(
                                    "partsize_mb must be a positive integer value 5 or greater");
                            usage();
                            System.exit(1);
                        }
                        putMultipart(bucketName, objectKey, filePath, partSizeMB, s3, attributes);
                    } else {
                        usage();
                    }
                    break;
                case GETSTRING:
                    if (args.length > objectKeyArg) {
                        getString(bucketName, objectKey, s3);
                    } else {
                        usage();
                    }
                    break;
                case GETFILE:
                    if (args.length > filePathArg) {
                        getFile(bucketName, objectKey, filePath, s3);
                    } else {
                        usage();
                    }
                    break;
                default:
                    usage();
            }
        } catch (AmazonS3Exception e) {
            int statusCode = e.getStatusCode();
            if (statusCode == 404) {
                String errorCode = e.getErrorCode();
                if (errorCode.equals("NoSuchKey")) {
                    System.out.println("Key: " + objectKey + " does not exist in Bucket: "
                            + bucketName);
                } else if (errorCode.equals("NoSuchBucket")) {
                    System.out.println("Bucket: " + bucketName + " does not exist");
                } else {
                    System.out.println(e.getClass() + ": " + e.getMessage());
                }
            } else if (statusCode == 40024) {
                System.out.println("Permission Denied by Ionic Policy.");
            } else {
                System.out.println(e.getMessage());
            }
            System.exit(1);
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
            System.exit(1);
        }
    }


    private static void usage() {
        System.out.println("Usage: prog <put<x> command> | <get<x> command> | version");
        System.out.println("put<x> commands:");
        System.out.println("\tNOTE: <attributes> for these commands is a list of comma delimited "
                + "tuples  with each tuple composed of a key followed by a colon delimited list of "
                + "values");
        System.out.println("\t\t<key>:<value>[:<value>]…[,<key>:<value>[:<value>]…]…");
        System.out.println("\t\tExample: attribute1:value1:value2,attribute2:value3");
        System.out.println("\tputString <bucketName> <objectKey> <objectContent> [<attributes>]");
        System.out.println("\tputFile <bucketName> <objectKey> <filePath> [<attributes>]");
        System.out.println(
                "\tputMultipart <bucketName> <objectKey> <file> <partsize_mb> [<attributes>]");
        System.out.println("get<x> commands:");
        System.out.println("\tgetFile <bucketName> <objectKey> <destinationPath>");
        System.out.println("\tgetString <bucketName> <objectKey>");
    }

    public static KeyAttributesMap parseAttributes(String str) {
        KeyAttributesMap ret = new KeyAttributesMap();
        String[] pairs = str.split(",");
        for (String pair : pairs) {
            String[] tuples = pair.split(":");
            ArrayList<String> values = new ArrayList<String>();
            for (int i = 1; i < tuples.length; i++) {
                values.add(tuples[i]);
            }
            ret.put(tuples[0], values);
        }
        return ret;
    }

    public static MetadataMap getMetadataMap() {
        MetadataMap applicationMetadata = new MetadataMap();
        applicationMetadata.set("ionic-application-name", "MachinaS3Example");
        applicationMetadata.set("ionic-application-version", Version.getFullVersion());
        applicationMetadata.set("ionic-client-type", "Machina Tools for Cloud Storage (S3 Java)");
        applicationMetadata.set("ionic-client-version", Version.getFullVersion());

        return applicationMetadata;
    }

    public static String getCanonicalPathString(String originalPath) {
        String canonicalPathStr = null;

        try {
            canonicalPathStr = Paths.get(originalPath).toFile().getCanonicalPath();
        } catch (NullPointerException e) {
            System.err.println("Missing original pathname");
        } catch (IOException e) {
            System.err.println("Path IOError");
        }
        return canonicalPathStr;
    }
}
