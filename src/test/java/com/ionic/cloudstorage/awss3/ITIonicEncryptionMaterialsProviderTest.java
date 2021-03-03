/*
 * (c) 2019-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.IonicException;
import java.util.HashMap;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class ITIonicEncryptionMaterialsProviderTest {

    private static IonicEncryptionMaterialsProvider iemp = null;
    private static Agent agent = null;

    @BeforeClass
    public static void setup() {
        try {
            iemp = TestUtils.getIEMP();
            agent = TestUtils.getAgent();
        } catch (IonicException e) {
            // Catch any IonicExceptions thrown during setup and null related objects so
            // that dependent tests are each skipped during the preconditions check.
            iemp = null;
            agent = null;
        }
    }

    @Before
    public void preconditions() {
        assertNotNull(iemp);
        assertNotNull(agent);
    }

    @Test
    public void getEncryptionMaterials() throws IonicException {
        EncryptionMaterials encMat = iemp.getEncryptionMaterials(null);

        String ionicKeyId = encMat.getMaterialsDescription().get(IonicEncryptionMaterialsProvider.KEYIDKEY);
        assertNotNull("Generated EncryptionMaterials did not contain Ionic Key ID", ionicKeyId);

        GetKeysResponse.Key IonicKey = null;
        IonicKey = agent.getKey(ionicKeyId).getFirstKey();
    }

    @Test
    public void getEncryptionMaterialsWithDesc() throws IonicException {
        HashMap<String, String> desc = new HashMap<String, String>();

        CreateKeysResponse.Key IonicKey = agent.createKey().getFirstKey();
        desc.put(IonicEncryptionMaterialsProvider.KEYIDKEY, IonicKey.getId());

        EncryptionMaterials encMat = iemp.getEncryptionMaterials(desc);

        SecretKeySpec awsKey = new SecretKeySpec(IonicKey.getKey(), 0, IonicKey.getKey().length, "AES");
        assertTrue("SecretKey returned from getEncryptionMaterials() did not match SecretKey derived from original Ionic Key.",
            encMat.getSymmetricKey().equals(awsKey));
    }


}
