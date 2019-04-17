/*
 * (c) 2019 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.error.IonicException;
import java.io.IOException;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

public class IonicS3EncryptionClientDeniedTest {

    private static IonicEncryptionMaterialsProvider iemp = null;
    private static IonicS3EncryptionClient ionicS3Client = null;
    private static String testBucket = null;
    private static String testString = null;

    @BeforeClass
    public static void setup() {
        if (TestUtils.awsCredsAvailable()) {
            try {
                iemp = TestUtils.getIEMP();
                ionicS3Client = (IonicS3EncryptionClient)IonicS3EncryptionClientBuilder.standard()
                    .withEncryptionMaterials(iemp).build();
            } catch (IonicException e) {
                // Catch any IonicExceptions thrown during setup and null related objects so
                // that dependent tests are each skipped during the preconditions check.
                iemp = null;
                ionicS3Client = null;
            }
        }
        testBucket = TestUtils.getTestBucket();
        testString = TestUtils.getTestPayload();
    }

    @Before
    public void preconditions() {
        Assume.assumeTrue(TestUtils.isProfilePolicyDenied());
        Assume.assumeNotNull(ionicS3Client);
        Assume.assumeNotNull(testBucket);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void putAndGetObject() throws IOException, AmazonS3Exception {

        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testPutAndGetObject";
        }

        ionicS3Client.putObject(testBucket, key, testString);

        thrown.expect(AmazonS3Exception.class);
        thrown.expectMessage("40024 - Key fetch or creation was denied by the server " +
            "(Service: null; Status Code: 0; Error Code: null; Request ID: null; S3 Extended Request ID: null");

        ionicS3Client.getObject(testBucket, key);
    }


}
