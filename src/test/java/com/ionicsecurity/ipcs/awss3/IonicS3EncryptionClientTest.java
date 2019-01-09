/*
 * (c) 2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.UploadPartRequest;

import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;

@RunWith(Parameterized.class)
public class IonicS3EncryptionClientTest {

    IonicS3EncryptionClient isec;
    static String bucketName;
    File uploadFile;
    File largeUpload;

    static {
        AmazonIdentityManagementClient idtyClient = (AmazonIdentityManagementClient) AmazonIdentityManagementClientBuilder
                .defaultClient();
        bucketName = idtyClient.getUser().getUser().getUserName().toLowerCase() + "-ipcstrials";
    }

    @Parameters
    public static List<IonicS3EncryptionClient> getEncryptionClients() throws IonicException {
        AgentSdk.initialize(null);
        IonicS3EncryptionClient isec;

        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";

        ptPersistor.setFilePath(sProfilePath);

        isec = (IonicS3EncryptionClient) IonicS3EncryptionClientBuilder.standard()
                .withEncryptionMaterials(new IonicEncryptionMaterialsProvider(ptPersistor)).build();

        List<IonicS3EncryptionClient> isecl = new ArrayList<IonicS3EncryptionClient>();

        isecl.add(isec);
        return isecl;
    }

    public IonicS3EncryptionClientTest(IonicS3EncryptionClient inputIemp) throws URISyntaxException {
        URL url1 = this.getClass().getResource("/testFile.txt");
        URI uri1 = new URI(url1.toString());
        this.uploadFile = new File(uri1.getPath());

        URL url2 = this.getClass().getResource("/largeFile.txt");
        URI uri2 = new URI(url2.toString());
        this.largeUpload = new File(uri2.getPath());

        isec = inputIemp;
    }

    @Test
    public void testPutAndGetObject() throws IOException {
        try {
            if (!(isec.doesBucketExist(bucketName))) {
                isec.createBucket(new CreateBucketRequest(bucketName));
            }
        } catch (AmazonServiceException ase) {
            fail("AmazonServiceException while creating bucket: " + ase.getErrorMessage());
        } catch (AmazonClientException ace) {
            fail("AmazonClientException while creating bucket: " + ace.getMessage());
        }

        String key = "firstTest";
        PutObjectRequest por = new PutObjectRequest(bucketName, key, uploadFile);

        try {
            isec.putObject(por);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while putting: " + ase.getErrorMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);
        S3Object obj = null;

        try {
            obj = isec.getObject(gor);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting: " + ase.getErrorMessage());
        }

        assertEquals(obj.getKey(), key);

        S3ObjectInputStream content = obj.getObjectContent();
        FileInputStream originalStream = new FileInputStream(uploadFile);

        assertTrue(IOUtils.contentEquals(content, originalStream));
        originalStream.close();
    }

    @Test
    public void testPutandGetViaRegularClient() throws IOException {
        String key = "secondTest";
        S3Object theObject = null;
        PutObjectRequest por = new PutObjectRequest(bucketName, key, uploadFile);

        try {
            isec.putObject(por);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while putting for use with regular client: " + ase.getErrorMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);
        AmazonS3 regularClient = AmazonS3ClientBuilder.defaultClient();

        try {
            theObject = regularClient.getObject(gor);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting: " + ase.getErrorMessage());
        }

        InputStream originalFileStream = new FileInputStream(uploadFile);
        assertFalse(IOUtils.contentEquals(originalFileStream, theObject.getObjectContent()));
    }

    @Test
    public void testPutAndGetObjectMetadata() throws URISyntaxException {
        URL url = this.getClass().getResource("/metaDataDest.txt");
        URI uri = new URI(url.toString());
        this.uploadFile = new File(uri.getPath());

        File metaDest = new File(uri.getPath());
        String key = "secondTest";
        PutObjectRequest por = new PutObjectRequest(bucketName, key, uploadFile);

        Map<String, String> inputMap = new HashMap<String, String>();
        inputMap.put("first", "a");
        inputMap.put("second", "b");
        ObjectMetadata s3ObjectMetadata = new ObjectMetadata();
        s3ObjectMetadata.setUserMetadata(inputMap);
        por.withMetadata(s3ObjectMetadata);

        try {
            isec.putObject(por);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while putting with metadata: " + ase.getErrorMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);
        ObjectMetadata meta = null;

        try {
            meta = isec.getObject(gor, metaDest);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting with metadata: " + ase.getErrorMessage());
        }

        Map<String, String> resultMap = meta.getUserMetadata();
        inputMap.forEach((k, v) -> assertTrue(v.equals(resultMap.get(k))));
    }

    @Test
    public void testInitiateMultipartUpload() {
        InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName, "secondTest");

        try {
            InitiateMultipartUploadResult imur = isec.initiateMultipartUpload(req);
        } catch (Exception e) {
            fail("No exception is expected, but got: " + e.getMessage());
        }
    }

    @Test
    public void testMultipartRoundTripViaRegularClient() throws IOException {
        String key = "multipartForRegularClientTest";
        S3Object theObject = null;

        try {
            uploadMultipart(key);
        } catch (AmazonS3Exception ase) {
            fail("failed to upload multipart from regular client test: " + ase.getMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);
        AmazonS3 regularClient = AmazonS3ClientBuilder.defaultClient();

        try {
            theObject = regularClient.getObject(gor);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting: " + ase.getErrorMessage());
        }

        InputStream originalFileStream = new FileInputStream(largeUpload);
        assertFalse(IOUtils.contentEquals(originalFileStream, theObject.getObjectContent()));

        PrintWriter writer = new PrintWriter(largeUpload);
        writer.print("");
        writer.close();
    }

    @Test
    public void testMultipartRoundTripViaIonicClient() throws IOException {
        String key = "multipartForIonicClientTest";
        S3Object theObject = null;

        try {
            uploadMultipart(key);
        } catch (AmazonS3Exception ase) {
            fail("failed to upload multipart from ionic client test: " + ase.getMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);

        try {
            theObject = isec.getObject(gor);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting: " + ase.getErrorMessage());
        }

        InputStream originalFileStream = new FileInputStream(largeUpload);
        assertTrue(IOUtils.contentEquals(originalFileStream, theObject.getObjectContent()));

        PrintWriter writer = new PrintWriter(largeUpload);
        writer.print("");
        writer.close();
    }

    public void uploadMultipart(String key) throws FileNotFoundException, IOException {
        float totalChunks;
        long partSizeBytes = 6 * 1024 * 1024;
        List<PartETag> partETags = new ArrayList<PartETag>();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(largeUpload)))) {
            for (int i = 0; i < 1500000; i++) {
                writer.write("Repeating ");
            }
        }

        long contentLength = largeUpload.length();
        if (contentLength % partSizeBytes != 0) {
            totalChunks = contentLength / partSizeBytes + 1;
        } else {
            totalChunks = contentLength / partSizeBytes;
        }

        ObjectMetadata s3ObjectMetadata = new ObjectMetadata();

        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1","Val2","Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1","Val2","Val3"));

        CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName, key, s3ObjectMetadata);
        InitiateMultipartUploadResult res = isec.initiateMultipartUpload(req, reqKey);

        try {
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                partSizeBytes = Math.min(partSizeBytes, (contentLength - filePosition));
                boolean isLast = i == totalChunks;

                UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName).withKey(key)
                        .withUploadId(res.getUploadId()).withPartNumber(i).withFileOffset(filePosition)
                        .withFile(largeUpload).withPartSize(partSizeBytes).withLastPart(isLast);

                partETags.add(isec.uploadPart(uploadRequest).getPartETag());

                filePosition += partSizeBytes;
            }

            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, key,
                    res.getUploadId(), partETags);

            isec.completeMultipartUpload(compRequest);
        } catch (AmazonS3Exception ase) {
            isec.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, res.getUploadId()));
            fail("AmazonS3Exception during Multipart upload: " + ase.getErrorMessage());
        }
    }

    @Test
    public void testPutAndGetWithCreatRequestKey() throws IOException {
        try {
            if (!(isec.doesBucketExist(bucketName))) {
                isec.createBucket(new CreateBucketRequest(bucketName));
            }
        } catch (AmazonServiceException ase) {
            fail("AmazonServiceException while creating bucket: " + ase.getErrorMessage());
        } catch (AmazonClientException ace) {
            fail("AmazonClientException while creating bucket: " + ace.getMessage());
        }

        String key = "attributesTest";
        PutObjectRequest por = new PutObjectRequest(bucketName, key, uploadFile);
        KeyAttributesMap attributes = new KeyAttributesMap();
        KeyAttributesMap mutableAttributes = new KeyAttributesMap();
        attributes.put("Attribute", Arrays.asList("Val1","Val2","Val3"));
        mutableAttributes.put("Mutable-Attribute", Arrays.asList("Val1","Val2","Val3"));

        CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);

        try {
            isec.putObject(por, reqKey);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while putting: " + ase.getErrorMessage());
        }

        GetObjectRequest gor = new GetObjectRequest(bucketName, key);
        IonicS3EncryptionClient.IonicKeyS3ObjectPair pair = null;

        try {
            pair = isec.getObjectAndKey(gor);
        } catch (AmazonS3Exception ase) {
            fail("AmazonS3Exception while getting: " + ase.getErrorMessage());
        }

        assertEquals(pair.getKey().getAttributesMap().get("Attribute"), attributes.get("Attribute"));
        assertTrue(pair.getKey().getMutableAttributesMap().equals(mutableAttributes));
    }
}
