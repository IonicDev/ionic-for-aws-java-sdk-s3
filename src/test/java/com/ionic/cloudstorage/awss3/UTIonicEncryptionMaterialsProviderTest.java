/*
 * (c) 2019-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import com.ionic.sdk.error.IonicException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;


public class UTIonicEncryptionMaterialsProviderTest {

    @Test
    public void defaultIEMPConstructor() {
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
    }

    @Rule
     public ExpectedException thrown = ExpectedException.none();

    @Test
    public void persistorIEMPConstructor() throws IonicException {
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider(ptPersistor);
        assertNotNull(iemp.getAgent());
    }

    @Test
    public void setGetMetaDataCapture() {
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        assertFalse("MetadataCapture was not false by default.", iemp.isEnabledMetadataCapture());
        iemp.setEnabledMetadataCapture(true);
        assertTrue("MetadataCapture was not true after iemp.setEnabledMetadataCapture(true)", iemp.isEnabledMetadataCapture());
    }

    @Test
    public void setGetDefaultAttributes() {
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        assertTrue("DefaultAttributes were not empty by default.", iemp.getDefaultAttributes().isEmpty());

        KeyAttributesMap kam = new KeyAttributesMap();
        ArrayList<String> collection = new ArrayList<String>();
        collection.add("confidential");
        collection.add("secured");
        kam.put("privacy", collection);

        iemp.setDefaultAttributes(kam);
        assertEquals("getDefaultAttributes() did not equal map set with setDefaultAttributes()", iemp.getDefaultAttributes(), kam);
    }

    @Test
    public void setGetAgentMetadata() {
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        assertTrue("IonicMetadataMap was not empty by default.", iemp.getIonicMetadataMap().isEmpty());

        MetadataMap metaMap = new MetadataMap();
        metaMap.set("ionic-application-name", "Unit_Test");
        metaMap.set("ionic-application-version", "0.0.0");

        iemp.setIonicMetadataMap(metaMap);
        assertEquals("getIonicMetadataMap() did not equal map set with setIonicMetadataMap()", iemp.getIonicMetadataMap(), metaMap);
    }

    @Test
    public void refresh() {
        IonicEncryptionMaterialsProvider iemp = new IonicEncryptionMaterialsProvider();
        iemp.refresh();
    }

}
