/*
 * (c) 2017 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;

public class IonicS3EncryptionClientBuilderTest {

    @Test
    public void testBuilderWithNonIonicProvider() {

        class NonIonicEncryptionMaterialsProvider implements EncryptionMaterialsProvider {
            @Override
            public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription) {
                return null;
            }

            @Override
            public EncryptionMaterials getEncryptionMaterials() {
                return null;
            }

            @Override
            public void refresh() {
            }
        }

        IonicS3EncryptionClientBuilder iscb = new IonicS3EncryptionClientBuilder();

        NonIonicEncryptionMaterialsProvider nIemp = new NonIonicEncryptionMaterialsProvider();
        iscb.setEncryptionMaterials(nIemp);

        try {
            AmazonS3Encryption ise2 = iscb.build();
            fail("Non-Ionic EncryptionMaterialsProvider cannot successfully be used to instantiate a client");
        } catch (Exception e) {
            // Successfully failed in try
        }
    }

    @Test
    public void testBuilderWithandWithoutProvider() {
        IonicS3EncryptionClientBuilder iscb = IonicS3EncryptionClientBuilder.standard();

        try {
            AmazonS3Encryption ise = iscb.build();
            fail("Expected an Illegal Argument Exception to be thrown");
        } catch (Exception e) {
            assertTrue(e instanceof java.lang.IllegalArgumentException);
        }

        try {
            iscb.withEncryptionMaterials(IonicEncryptionMaterialsProvider.standard());
        } catch (Exception e) {
            fail("No exception expected when IonicMaterialsProvider is supplied");
        }
    }
}
