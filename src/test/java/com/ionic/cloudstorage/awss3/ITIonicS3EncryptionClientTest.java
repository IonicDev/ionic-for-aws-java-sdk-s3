/*
 * (c) 2019-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.error.IonicException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


// TODO: See TODO in ITTransferManagerTest.java
public class ITIonicS3EncryptionClientTest {

    static Logger log = LogManager.getLogger();

    private static IonicEncryptionMaterialsProvider iemp = null;
    private static IonicS3EncryptionClient ionicS3Client = null;
    private static Agent agent = null;
    private static AmazonS3 s3Client = null;
    private static String testBucket = null;
    private static String testString = null;

    @BeforeClass
    public static void setup() {
        if (TestUtils.awsCredsAvailable()) {
            try {
                iemp = TestUtils.getIEMP();
                ionicS3Client = (IonicS3EncryptionClient)IonicS3EncryptionClientBuilder.standard()
                    .withEncryptionMaterials(iemp).build();
                agent = TestUtils.getAgent();
            } catch (IonicException e) {
                // Catch any IonicExceptions thrown during setup and null related objects so
                // that dependent tests are each skipped during the preconditions check.
                iemp = null;
                ionicS3Client = null;
                agent = null;
                log.warn("setup() was unsucessful: " + e.getMessage());
            }
            s3Client = AmazonS3ClientBuilder.defaultClient();
        }
        testBucket = TestUtils.getTestBucket();
        testString = TestUtils.getTestPayload();
    }

    @Before
    public void preconditions() {
        assertNotNull(ionicS3Client);
        assertNotNull(testBucket);
        assertNotNull(agent);
        assertNotNull(iemp);
    }

    @Test
    public void putAndGetObject() throws IOException, TimeoutException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testPutAndGetObject";
        }

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        ionicS3Client.putObject(testBucket, key, testString);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting Object " + key + " from bucket " + testBucket + " with Ionic Encryption Client");
        S3Object obj = ionicS3Client.getObject(testBucket, key);
        assertTrue("Decrypted object content does not match original String",
            IOUtils.toString(obj.getObjectContent(), "UTF8").equals(testString));
    }

    @Test
    public void atRestEncryption() throws IOException, TimeoutException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testAtRestEncryption";
        }

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        ionicS3Client.putObject(testBucket, key, testString);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting Object " + key + " from bucket " + testBucket + " with S3 Client");
        S3Object obj = s3Client.getObject(testBucket, key);
        assertFalse("Uploaded object content matches original String",
            IOUtils.toString(obj.getObjectContent(), "UTF8").equals(testString));
    }

    @Test
    public void metadataCaptureOn() throws IOException, IonicException, TimeoutException, InterruptedException {
        String testMetaKey = "TestMetadataKey";
        String testMetaValue = "TestMetadataValue";
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testMetadataCaptureOn";
        }

        iemp.setEnabledMetadataCapture(true);

        InputStream iStream = IOUtils.toInputStream(testString);

        ObjectMetadata requestMetadata = new ObjectMetadata();
        requestMetadata.addUserMetadata(testMetaKey, testMetaValue);
        requestMetadata.setContentLength(testString.length());

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        PutObjectRequest request = new PutObjectRequest(testBucket, key, iStream, requestMetadata);
        ionicS3Client.putObject(request);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting S3Object Metadata for " + key + " from bucket " + testBucket);
        ObjectMetadata uploadedMetadata = s3Client.getObjectMetadata(testBucket, key);
        String ionicKeyId = extractIonicKeyIDFromMatdesc(uploadedMetadata.getUserMetaDataOf("x-amz-matdesc"));

        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue( "Captured metadata value is not equal to \"" + testMetaValue + '"',
            ionicKey.getAttributesMap().get(testMetaKey).get(0).equals(testMetaValue));
    }

    @Test
    public void metadataCaptureOff() throws IOException, IonicException, TimeoutException, InterruptedException {
        String testMetaKey = "TestMetadataKey";
        String testMetaValue = "TestMetadataValue";
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testMetadataCaptureOff";
        }

        iemp.setEnabledMetadataCapture(false);

        InputStream iStream = IOUtils.toInputStream(testString);

        ObjectMetadata requestMetadata = new ObjectMetadata();
        requestMetadata.addUserMetadata(testMetaKey, testMetaValue);
        requestMetadata.setContentLength(testString.length());

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        PutObjectRequest request = new PutObjectRequest(testBucket, key, iStream, requestMetadata);
        ionicS3Client.putObject(request);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting S3Object Metadata for " + key + " from bucket " + testBucket);
        ObjectMetadata uploadedMetadata = s3Client.getObjectMetadata(testBucket, key);
        String ionicKeyId = extractIonicKeyIDFromMatdesc(uploadedMetadata.getUserMetaDataOf("x-amz-matdesc"));

        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue( "Metadata key \"" + testMetaKey + "\" is present in IonicKey Attributes",
            ionicKey.getAttributesMap().get(testMetaKey) == null);
    }

    @Test
    public void putObjectWithAttributes() throws IOException, IonicException, TimeoutException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testPutObjectWithAttributes";
        }

        iemp.setEnabledMetadataCapture(true);

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        CreateKeysRequest.Key ionicRequestKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        ionicS3Client.putObject(testBucket, key, testString, ionicRequestKey);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting S3Object Metadata for " + key + " from bucket " + testBucket);
        ObjectMetadata uploadedMetadata = s3Client.getObjectMetadata(testBucket, key);
        String ionicKeyId = extractIonicKeyIDFromMatdesc(uploadedMetadata.getUserMetaDataOf("x-amz-matdesc"));

        log.info("Getting Ionic Key " + ionicKeyId + " with Ionic Agent");
        GetKeysResponse.Key ionicKey = agent.getKey(ionicKeyId).getFirstKey();

        assertTrue("Response Key Attributes do not match specified Attributes",
            ionicKey.getAttributesMap().equals(attributes));

        assertTrue("Response Key Mutable Attributes do not match specified Mutable Attributes",
            ionicKey.getMutableAttributesMap().equals(mutableAttributes));
    }

    @Test
    public void putAndGetObjectWithAttributes() throws IOException, TimeoutException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testPutAndGetObjectWithAttributes";
        }

        iemp.setEnabledMetadataCapture(true);

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        CreateKeysRequest.Key ionicRequestKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        log.info("Putting Object " + key + " on to bucket " + testBucket + " with Ionic Encryption Client");
        ionicS3Client.putObject(testBucket, key, testString, ionicRequestKey);
        waitUntilObjectExists(testBucket, key);
        log.info("Getting Object " + key + " from bucket " + testBucket + " with Ionic Encryption Client");
        IonicS3EncryptionClient.IonicKeyS3ObjectPair pair = ionicS3Client.getObjectAndKey(testBucket, key);
        S3Object obj = pair.getS3Object();
        GetKeysResponse.Key ionicKey = pair.getKey();

        assertTrue("Response Key Attributes do not match specified Attributes",
            ionicKey.getAttributesMap().equals(attributes));

        assertTrue("Response Key Mutable Attributes do not match specified Mutable Attributes",
            ionicKey.getMutableAttributesMap().equals(mutableAttributes));

        assertTrue("Decrypted object content does not match original String",
            IOUtils.toString(obj.getObjectContent(), "UTF8").equals(testString));
    }

    @Test
    public void unencryptedDownload() throws IOException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testUnencryptedDownload";
        }
        log.info("Putting Object " + key + " on to bucket " + testBucket + " with S3 Client");
        s3Client.putObject(testBucket, key, testString);

        log.info("Getting Object " + key + " from bucket " + testBucket + " with Ionic Encryption Client");
        S3Object object = ionicS3Client.getObject(testBucket, key);
        String contents = IOUtils.toString(object.getObjectContent());
        assertTrue("Unencrypted download did not match source", testString.equals(contents));
    }

    @Test
    public void multipartUpload() throws IOException, AmazonS3Exception {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testMultipartUpload";
        }

        iemp.setEnabledMetadataCapture(true);

        InputStream inStream = null;
        long sourceSize = 0;
        String testDirectory = TestUtils.getTestDirectoryString(key);
        // Generate a source file larger than the partSizeBytes value.
        File sourceFile = TestUtils.generateTestFile(testDirectory, key + ".source", 7);
        File destFile = TestUtils.generateTestFile(testDirectory, key + ".dest", 0);
        assertNotNull(sourceFile);
        assertNotNull(destFile);

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1", "Val2", "Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1", "Val2", "Val3"));

        CreateKeysRequest.Key reqKey =
                new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        uploadMultipart(key, sourceFile, reqKey);
        inStream = new FileInputStream(sourceFile);

        log.info("Getting Object " + key + " from bucket " + testBucket + " with Ionic Encryption Client");
        IonicS3EncryptionClient.IonicKeyS3ObjectPair pair;
        pair = ionicS3Client.getObjectAndKey(testBucket, key);

        S3Object s3Object = pair.getS3Object();
        FileUtils.copyInputStreamToFile(s3Object.getObjectContent(), destFile);

        assertTrue("Response Key Attributes do not match specified Attributes",
            pair.getKey().getAttributesMap().equals(attributes));

        assertTrue("Response Key Mutable Attributes do not match specified Mutable Attributes",
            pair.getKey().getMutableAttributesMap().equals(mutableAttributes));

        assertTrue("Downloaded File did not match original File",
            FileUtils.contentEquals(sourceFile, destFile));
    }

    public void uploadMultipart(String key, File sourceFile, CreateKeysRequest.Key ionicKey)
        throws FileNotFoundException, IOException {
        float totalChunks;
        long partSizeBytes = 6 * 1024 * 1024;
        long contentLength = sourceFile.length();
        ArrayList<PartETag> partETags = new ArrayList<PartETag>();

        if (contentLength % partSizeBytes != 0) {
            totalChunks = contentLength / partSizeBytes + 1;
        } else {
            totalChunks = contentLength / partSizeBytes;
        }

        ObjectMetadata s3ObjectMetadata = new ObjectMetadata();

        InitiateMultipartUploadRequest req =
                new InitiateMultipartUploadRequest(testBucket, key, s3ObjectMetadata);
        log.info("Initiaing multipartUpload for " + key + " to bucket " + testBucket + " with Ionic Encryption Client");
        InitiateMultipartUploadResult res = ionicS3Client.initiateMultipartUpload(req, ionicKey);

        try {
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                partSizeBytes = Math.min(partSizeBytes, (contentLength - filePosition));
                boolean isLast = i == totalChunks;

                log.info("Uploading part " + i + " for " + key + " to bucket " + testBucket + " with Ionic Encryption Client");
                UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(testBucket)
                        .withKey(key).withUploadId(res.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition).withFile(sourceFile)
                        .withPartSize(partSizeBytes).withLastPart(isLast);

                partETags.add(ionicS3Client.uploadPart(uploadRequest).getPartETag());

                filePosition += partSizeBytes;
            }

            log.info("Completing multipartUpload for " + key + " to bucket " + testBucket + " with Ionic Encryption Client");
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
                    testBucket, key, res.getUploadId(), partETags);

            ionicS3Client.completeMultipartUpload(compRequest);
        } catch (AmazonS3Exception ase) {
            ionicS3Client.abortMultipartUpload(
                    new AbortMultipartUploadRequest(testBucket, key, res.getUploadId()));
            fail("AmazonS3Exception during Multipart upload: " + ase.getErrorMessage());
        }
    }

    private String extractIonicKeyIDFromMatdesc(String str) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(str, HashMap.class);
        return map.get(IonicEncryptionMaterialsProvider.KEYIDKEY);
    }

    private void waitUntilObjectExists(String bucket, String objectKey) throws TimeoutException, InterruptedException {
        int attempts = 0;
        while(s3Client.doesObjectExist(bucket, objectKey) == false && attempts < 5) {
            attempts++;
            log.info("Waiting for availability of " + objectKey + " on bucket " + testBucket + " attempt " + attempts);
            if (attempts >= 5) {
                throw new TimeoutException("Timed out waiting for uploaded object " +
                    objectKey + " to become available.");
            }
            Thread.sleep(1000);
        }
    }

}
