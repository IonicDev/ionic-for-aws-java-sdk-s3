/*
 * (c) 2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.amazonaws.services.s3.model.EncryptionMaterials;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IonicEncryptionMaterialsProviderTest extends TestCase {

    IonicEncryptionMaterialsProvider iemp;
    DeviceProfilePersistorPlainText ptPersistor;

    public IonicEncryptionMaterialsProviderTest(String testName) throws IonicException {
        super(testName);
        setUp();
    }

    public static Test suite() {
        return new TestSuite(IonicEncryptionMaterialsProviderTest.class);
    }

    public void setUp() throws IonicException {
        AgentSdk.initialize(null);
        ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        ptPersistor.setFilePath(sProfilePath);
        iemp = new IonicEncryptionMaterialsProvider(ptPersistor);
    }

    public void testGetEncryptionMaterials() throws IonicException {
        EncryptionMaterials encMat = iemp.getEncryptionMaterials(null);
        assertTrue(encMat instanceof EncryptionMaterials);

        String ionicKeyId = encMat.getMaterialsDescription().get(IonicEncryptionMaterialsProvider.KEYIDKEY);
        assertTrue(ionicKeyId != null);

        Agent agent = new Agent();
        GetKeysResponse.Key IonicKey = null;
        agent.initialize(ptPersistor);
        IonicKey = agent.getKey(ionicKeyId).getKeys().get(0);
        assertTrue(IonicKey != null);
    }

    public void testGetEncryptionMaterialsWithDesc() throws IonicException {
        Map<String, String> desc = new HashMap<String, String>();
        Agent agent = new Agent();
        agent.initialize(ptPersistor);

        CreateKeysResponse.Key IonicKey = null;
        ArrayList<String> collection = new ArrayList<String>();
        collection.add("ajp_test");
        KeyAttributesMap myKam = new KeyAttributesMap();
        myKam.put("ajp_test", collection);
        CreateKeysResponse response = agent.createKey(myKam);
        List<CreateKeysResponse.Key> list = response.getKeys();
        IonicKey = list.get(0);
        desc.put(IonicEncryptionMaterialsProvider.KEYIDKEY, IonicKey.getId());

        EncryptionMaterials encMat = iemp.getEncryptionMaterials(desc);
        assertTrue(encMat instanceof EncryptionMaterials);

        SecretKeySpec awsKey = new SecretKeySpec(IonicKey.getKey(), 0, IonicKey.getKey().length, "AES");
        assertTrue(encMat.getSymmetricKey().equals(awsKey));
    }

    public void testSetandGetDefaultAttributes() throws IonicException {
        KeyAttributesMap kam = new KeyAttributesMap();
        ArrayList<String> collection = new ArrayList<String>();
        collection.add("confidential");
        collection.add("secured");
        kam.put("privacy", collection);

        IonicEncryptionMaterialsProvider.setDefaultAttributes(kam);
        assertTrue(IonicEncryptionMaterialsProvider.getDefaultAttributes().equals(kam));

        EncryptionMaterials encMat = iemp.getEncryptionMaterials(null);
        Agent agent = new Agent();
        GetKeysResponse.Key IonicKey = null;
        agent.initialize(ptPersistor);
        iemp = new IonicEncryptionMaterialsProvider(ptPersistor);
        String ionicKeyId = encMat.getMaterialsDescription().get(IonicEncryptionMaterialsProvider.KEYIDKEY);
        IonicKey = agent.getKey(ionicKeyId).getKeys().get(0);
        KeyAttributesMap ionicKam = IonicKey.getAttributesMap();
        for (Iterator<Map.Entry<String, List<String>>> iter = kam.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, List<String>> entry = iter.next();
            assertTrue(entry.getValue().equals(ionicKam.get(entry.getKey())));
        }
    }
}
