/*
 * (c) 2017-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.error.IonicException;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class UTIonicS3EncryptionClientBuilderTest {

    static Boolean awsCredsAvailable;

    @BeforeClass
    public static void setup() {
        awsCredsAvailable = TestUtils.awsCredsAvailable();
    }

    @Test
    public void builderWithNonIonicMaterialsProvider() {

        class NonIonicEncryptionMaterialsProvider implements EncryptionMaterialsProvider {
            @Override
            public EncryptionMaterials getEncryptionMaterials(
                    Map<String, String> materialsDescription) {
                return null;
            }

            @Override
            public EncryptionMaterials getEncryptionMaterials() {
                return null;
            }

            @Override
            public void refresh() {}
        }

        IonicS3EncryptionClientBuilder ionicBuilder = new IonicS3EncryptionClientBuilder();

        NonIonicEncryptionMaterialsProvider materialsProvider = new NonIonicEncryptionMaterialsProvider();
        ionicBuilder.setEncryptionMaterials(materialsProvider);

        try {
            AmazonS3Encryption ionicEncryptionClient = ionicBuilder.build();
            fail("Non-Ionic EncryptionMaterialsProvider was successfully used to instantiate an IonicS3EncryptionClient");
        } catch (IllegalArgumentException e) {
            assertTrue(e instanceof java.lang.IllegalArgumentException);
        } catch (Exception e) {
            fail("Unexpected Exception building IonicS3EncryptionClient with Non-Ionic EncryptionMaterialsProvider ");
        }
    }

    @Test
    public void builderWithoutProvider() {
        try {
            AmazonS3Encryption ionicEncryptionClient = IonicS3EncryptionClientBuilder.standard().build();
            fail("Expected an Illegal Argument Exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e instanceof java.lang.IllegalArgumentException);
        } catch (Exception e) {
            fail("Unexpected Exception building IonicS3EncryptionClient without EncryptionMaterialsProvider ");
        }
    }

    @Test
    public void builderWithProvider() {
        Assume.assumeTrue(awsCredsAvailable);
        AmazonS3Encryption ionicEncryptionClient = null;
        try {
            ionicEncryptionClient = IonicS3EncryptionClientBuilder.standard()
                .withEncryptionMaterials(new IonicEncryptionMaterialsProvider()).build();
        } catch (Exception e) {
            fail("Unexpected Exception building IonicS3EncryptionClient with IonicMaterialsProvider");
        }
        assertTrue(ionicEncryptionClient instanceof IonicS3EncryptionClient);
    }

    @Test
    public void defaultClient() throws IonicException, IOException {
        Assume.assumeTrue(awsCredsAvailable);
        AmazonS3Encryption ionicEncryptionClient = IonicS3EncryptionClientBuilder.defaultClient();
        assertTrue(ionicEncryptionClient instanceof IonicS3EncryptionClient);
    }

    @Test
    public void builderWithAgent() {
        Assume.assumeTrue(awsCredsAvailable);
        Agent agent = new Agent();
        AmazonS3Encryption ionicEncryptionClient = IonicS3EncryptionClientBuilder.standard().withIonicAgent(agent).build();
        assertTrue(ionicEncryptionClient instanceof IonicS3EncryptionClient);
    }

    @Test
    public void ionicBuilderWithProvider() {
        Assume.assumeTrue(awsCredsAvailable);
        IonicS3EncryptionClient ionicEncryptionClient = null;
        try {
            ionicEncryptionClient = IonicS3EncryptionClientBuilder.standard()
                .withEncryptionMaterials(new IonicEncryptionMaterialsProvider()).buildIonic();
        } catch (Exception e) {
            fail("Unexpected Exception building IonicS3EncryptionClient with IonicMaterialsProvider");
        }
        assertTrue(ionicEncryptionClient instanceof IonicS3EncryptionClient);
    }

}
