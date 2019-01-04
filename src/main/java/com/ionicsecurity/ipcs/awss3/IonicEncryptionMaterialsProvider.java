/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.AgentSdk;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
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
    static final String IONICKEYREQUUID = "ionic-kam-uuid";

    private static final String HOME = System.getProperty("user.home");


    private boolean enabledMetadataCapture = false;

    private static KeyAttributesMap defaultAttributes = new KeyAttributesMap();
    private ISAgentPool agentPool = new ISAgentPool();

    private HashMap<String, CreateKeysRequest.Key> requestKeyMap = new HashMap<String, CreateKeysRequest.Key>();
    private HashMap<String, GetKeysResponse.Key> responseKeyMap = new HashMap<String, GetKeysResponse.Key>();

    /**
     * IonicEncryptionMaterialsProvider() default constructor for IonicEncryptionMaterialsProvider
     */
    public IonicEncryptionMaterialsProvider() {
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
    public IonicEncryptionMaterialsProvider(DeviceProfilePersistorBase persistor) {
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
     * Note: PlainText persistors should be used for development only.
     * See Ionic devportal for more on profile persistor types
     * https://dev.in.ionicsecurity.com/devportal/develop/platform/concepts/profile-persistor
     *
     * @return a {@link com.ionicsecurity.ipcs.awss3.IonicEncryptionMaterialsProvider} object.
     */
    static public IonicEncryptionMaterialsProvider standard() {
        // Load a plain-text device profile (SEP) from disk
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();

        try {
            String sProfilePath = Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile().getCanonicalPath();
            ptPersistor.setFilePath(sProfilePath);
        } catch (InvalidPathException e) {
            System.err.println("Error: Invalid Path" );
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            System.err.println("Error: IO Error" );
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
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
    public EncryptionMaterials getEncryptionMaterials() {
        try {
            return generateEncryptionMaterials(new HashMap<String, String>());
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
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
    public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription) {
        return getEncryptionMaterialsInternal(materialsDescription);
    }

    private EncryptionMaterials getEncryptionMaterialsInternal(Map<String, String> desc) {
        try {
            if (desc == null || desc.get(KEYIDKEY) == null) {
                return generateEncryptionMaterials(desc);
            }
            return retrieveEncryptionMaterials(desc);
        } catch (IonicException e) {
            throw new AmazonS3Exception(e.getLocalizedMessage(), e);
        }
    }

    private EncryptionMaterials generateEncryptionMaterials(Map<String, String> desc) throws IonicException {
        KeyAttributesMap kam = new KeyAttributesMap();
        if (desc != null && enabledMetadataCapture) {
            for (Map.Entry<String, String> entry : desc.entrySet()) {
                if (entry.getValue() != IONICKEYREQUUID) {
                    ArrayList<String> collection = new ArrayList<String>();
                    collection.add(entry.getValue());
                    kam.put(entry.getKey(), collection);
                }
            }
        }

        kam.putAll(defaultAttributes);
        CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("");
        if (desc != null) {
            String uuid = desc.get(IONICKEYREQUUID);
            if (uuid != null) {
                reqKey = retrieveRequestKey(uuid);
                kam.putAll(reqKey.getAttributesMap());
            }
        }
        Agent agent = agentPool.getAgent();
        CreateKeysResponse.Key ionicKey = null;
        try {
            ionicKey = agent.createKey(kam, reqKey.getMutableAttributesMap()).getFirstKey();
        } finally {
            agentPool.returnAgent(agent);
        }
        SecretKey awsKey;
        awsKey = new SecretKeySpec(ionicKey.getKey(), 0, ionicKey.getKey().length, "AES");
        EncryptionMaterials materials = new EncryptionMaterials(awsKey);
        materials.addDescription(KEYIDKEY, ionicKey.getId());
        materials.addDescription(IONICVERSIONKEY, "1.0.0");
        return materials;
    }

    private EncryptionMaterials retrieveEncryptionMaterials(Map<String, String> desc) throws IonicException {
        String ionicKeyId = desc.get(KEYIDKEY);
        Agent agent = agentPool.getAgent();
        GetKeysResponse.Key ionicKey;
        try {
            ionicKey = agent.getKey(ionicKeyId).getFirstKey();
        } finally {
            agentPool.returnAgent(agent);
        }
        storeResponseKey(ionicKey);
        SecretKey awsKey = new SecretKeySpec(ionicKey.getKey(), 0, ionicKey.getKey().length, "AES");
        EncryptionMaterials materials = new EncryptionMaterials(awsKey);
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
      * setDefaultAttributes() sets the default Attributes to
      * be applied to all Agent.keyCreate() requests
      *
      * @param defaultAttributes a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
      */
    public static void setDefaultAttributes(KeyAttributesMap defaultAttributes) {
        IonicEncryptionMaterialsProvider.defaultAttributes = defaultAttributes;
    }

     /**
      * getDefaultAttributes() gets defaultAttributes
      *
      * @return a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
      */
    public static KeyAttributesMap getDefaultAttributes() {
        return defaultAttributes;
    }

     /**
      * setIonicMetadataMap() sets the MetadataMap for IDC interactions
      *
      * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
      */
    public static void setIonicMetadataMap(MetadataMap map) {
        ISAgentPool.setMetadataMap(map);
    }

     /**
      * getIonicMetadataMap() gets the MetadataMap used for IDC interactions
      *
      * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
      */
    public static MetadataMap getIonicMetadataMap() {
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

    protected String storeRequestKey(CreateKeysRequest.Key key) {
        String uuid = UUID.randomUUID().toString();
        this.requestKeyMap.put(uuid, key);
        return uuid;
    }

    private CreateKeysRequest.Key retrieveRequestKey(String uuid) {
        return this.requestKeyMap.remove(uuid);
    }

    private void storeResponseKey(GetKeysResponse.Key key) {
    {}
        this.responseKeyMap.put(key.getId(), key);
    }

    protected GetKeysResponse.Key retrieveResponseKey(String keyId) {
        return this.responseKeyMap.remove(keyId);
    }
}
