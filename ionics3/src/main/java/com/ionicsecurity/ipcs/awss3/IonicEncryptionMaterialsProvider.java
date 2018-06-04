/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;


/**
 * IonicEncryptionMaterialsProvider class.
 */
public class IonicEncryptionMaterialsProvider implements EncryptionMaterialsProvider {
    // String constants
    static final String KEYIDKEY = "ionic-key-id";
    static final String IONICVERSIONKEY = "ionic-s3-version";

    private boolean enabledMetadataCapture = false;
    private boolean enabledMetadataReturn = false;

    private static KeyAttributesMap defaultAttributes = new KeyAttributesMap();
    private ISAgentPool agentPool = new ISAgentPool();

    /**
     * IonicEncryptionMaterialsProvider() default constructor for IonicEncryptionMaterialsProvider
     */
    public IonicEncryptionMaterialsProvider()
    {
        try {
            AgentSdk.initialize(null);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        } 
    }

    /**
     * IonicEncryptionMaterialsProvider() constructor for IonicEncryptionMaterialsProvider that
     * takes a persistor and set it on it's agentPool
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase} object.
     */
    public IonicEncryptionMaterialsProvider(DeviceProfilePersistorBase persistor)
    {
        try {
            AgentSdk.initialize(null);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        } 
        agentPool.setPersistor(persistor);
    }

    /**
     * standard() Create new instance of IonicEncryptionMaterialsProvider
     * with the default PlainText Persistor.
     *
     * @return a {@link com.ionicsecurity.ipcs.awss3.IonicEncryptionMaterialsProvider} object.
     */
    static public IonicEncryptionMaterialsProvider standard()
    {
        // Load a plain-text device profile (SEP) from disk 
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        try {
            ptPersistor.setFilePath(sProfilePath);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
        return new IonicEncryptionMaterialsProvider(ptPersistor);
    }

    /**
     * {@inheritDoc}
     *
     * refresh() no-op
     */
    @Override
    public void refresh() {}

    /**
     * getEncryptionMaterials() produces EncryptionMaterials by
     * creating a new encryption key via IDC
     *
     * @return a {@link com.amazonaws.services.s3.model.EncryptionMaterials} object.
     */
    public EncryptionMaterials getEncryptionMaterials()
    {
        return generateEncryptionMaterials(new HashMap<String, String>());
    }

    /**
     * {@inheritDoc}
     *
     * getEncryptionMaterials() produces EncryptionMaterials by either
     * creating a new encryption key via IDC if no Ionic_Key_ID key is
     * present in materialsDescription or by attempting to retrieve an
     * existing key from IDC with its KeyId
     */
    @Override
    public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription)
    {
        return getEncryptionMaterialsInternal(materialsDescription); 
    }

    private EncryptionMaterials getEncryptionMaterialsInternal(Map<String, String> desc)
    {
        if (desc == null || desc.get(KEYIDKEY) == null)
        {
            return generateEncryptionMaterials(desc);
        }
        String ionicKeyId = desc.get(KEYIDKEY);
        return retrieveEncryptionMaterials(ionicKeyId);
    }

    private EncryptionMaterials generateEncryptionMaterials(Map<String, String> desc)
    {
        KeyAttributesMap kam = new KeyAttributesMap();

        for (Iterator<Map.Entry<String, List<String>>> iter = defaultAttributes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, List<String>> entry = iter.next();
            kam.put(entry.getKey(), entry.getValue());
        }
        if (desc != null && enabledMetadataCapture)
        {
            for(Map.Entry<String, String> entry : desc.entrySet()){
                ArrayList<String> collection = new ArrayList<String>(); 
                collection.add(entry.getValue());
                kam.put(entry.getKey(), collection);
            }
        }
        Agent agent;
        try {
            agent = agentPool.getAgent();
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = agent.createKey(kam).getKeys().get(0);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
        agentPool.returnAgent(agent);        
        SecretKey awsKey;
        awsKey = new SecretKeySpec(ionicKey.getKey(), 0, ionicKey.getKey().length, "AES");
        EncryptionMaterials materials = new EncryptionMaterials(awsKey);
        materials.addDescription(KEYIDKEY, ionicKey.getId());
        materials.addDescription(IONICVERSIONKEY, "1.0.0");
        return materials;
    }

    private EncryptionMaterials retrieveEncryptionMaterials(String keyID)
    {
        Agent agent;
        try {
            agent = agentPool.getAgent();
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
        GetKeysResponse.Key ionicKey;
        try {
            ionicKey = agent.getKey(keyID).getKeys().get(0);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
        agentPool.returnAgent(agent);

        KeyAttributesMap kam = ionicKey.getAttributesMap();
        EncryptionMaterials materials;
        SecretKey awsKey;
        awsKey = new SecretKeySpec(ionicKey.getKey(), 0, ionicKey.getKey().length, "AES");
        materials = new EncryptionMaterials(awsKey);
        if (enabledMetadataReturn)
        {
            Iterator<Map.Entry<String,List<String>>> iterator = kam.entrySet().iterator();
            while (iterator.hasNext()) 
            {
                Map.Entry<String, List<String>> attributeEntry = iterator.next();
                materials.addDescription(attributeEntry.getKey(), attributeEntry.getValue().get(0));
            }
        }
        return materials;
    }

    /**
     * isEnabledMetadataCapture() returns enabledMetadataCapture
     *
     * @return a boolean.
     */
    public boolean isEnabledMetadataCapture() {
        return enabledMetadataCapture;
    }

    /**
     * setEnabledMetadataCapture() sets enabledMetadataCapture,
     * while true S3 requests to store objects with client side
     * Encryption will have their userMetadata parsed and passed
     * as ionic attributes when content encryption keys are
     * generated
     *
     * @param enabledMetadataCapture a boolean.
     */
    public void setEnabledMetadataCapture(boolean enabledMetadataCapture) {
        this.enabledMetadataCapture = enabledMetadataCapture;
    }

    /**
     * isEnabledMetadataReturn() returns isEnabledMetadataReturn
     * 
     * @return a boolean.
     */ 
    boolean isEnabledMetadataReturn() {
         return enabledMetadataReturn;
     }

     /**
      * setEnabledMetadataReturn() sets enabledMetadataReturn,
      * while true ionic attributes associated with the content encryption
      * key will be added to the encryption materials description associated
      * with the local instance of the s3object
      *
      * @param enabledMetadataReturn a boolean.
      */
     public void setEnabledMetadataReturn(boolean enabledMetadataReturn) {
         this.enabledMetadataReturn = enabledMetadataReturn;
     }

     /**
      * setDefaultAttributes() sets the default Attributes to
      * be applied to all Agent.keyCreate() requests
      *
      * @param defaultAttributes a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
      */
     public static void setDefaultAttributes(KeyAttributesMap defaultAttributes)
     {
         IonicEncryptionMaterialsProvider.defaultAttributes = defaultAttributes;
     }

     /**
      * getDefaultAttributes() gets defaultAttributes
      *
      * @return a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
      */
     public static KeyAttributesMap getDefaultAttributes()
     {
         return defaultAttributes;
     }

     /**
      * setIonicMetadataMap() sets the MetadataMap for IDC interactions
      *
      * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
      */
     public static void setIonicMetadataMap(MetadataMap map)
     {
         ISAgentPool.setMetadataMap(map);
     }

     /**
      * getIonicMetadataMap() gets the MetadataMap used for IDC interactions
      *
      * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
      */
     public static MetadataMap getIonicMetadataMap()
     {
         return ISAgentPool.getMetadataMap();
     }

     /**
      * setPersistor() sets the Persistor with which to
      * create Agents in the AgentPool
      *
      * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase} object.
      */
     public void setPersistor(DeviceProfilePersistorBase persistor)
     {
         agentPool.setPersistor(persistor);
     }
}

