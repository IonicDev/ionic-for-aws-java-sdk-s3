/*
 * (c) 2019 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.error.AgentErrorModuleConstants;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.data.MetadataMap;

/**
 * AgentFactory provides a means of acquiring initialized Agents in a thread safe manner that
 * minimizes the overhead of constructing and initiating Agents on demand.
 */
public class IonicAgentFactory {

    private MetadataMap metadataMap = new MetadataMap();

    private DeviceProfile profile;

    /**
     * getAgent() creates an Agent and initializes it with the given active profile and metadataMap
     *
     * @return a {@link com.ionic.sdk.agent.Agent} object.
     * @throws com.ionic.sdk.error.IonicException if the
     *         {@link com.ionic.sdk.device.profile.DeviceProfile active profile}
     *         is unset.
     */
    public Agent getAgent() throws IonicException{
        if (profile == null) {
            throw new IonicException(AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE);
        }
        Agent agent = new Agent();
        agent.initializeWithoutProfiles();
        agent.addProfile(profile, true);
        agent.setMetadata(this.getMetadataMap());
        return agent;
    }

    /**
     * setProfile() sets the profile to use when making new agents based on the active profile in
     *         the given persistor.
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase}
     *        object.
     * @throws com.ionic.sdk.error.IonicException if the
     *         {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase Persistor}
     *         is unset or does not contain an active profile.
     */
    public void setActiveProfile(DeviceProfilePersistorBase persistor) throws IonicException {
        Agent agent = new Agent();
        agent.initialize(persistor);
        if (agent.hasActiveProfile() == false) {
            throw new IonicException(AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE);
        }
        profile = agent.getActiveProfile();
    }

    /**
     * setMetadataMap() sets the MetadataMap to be used when generating new Agents
     *
     * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    void setMetadataMap(MetadataMap map) {
        MetadataMap newMap = new MetadataMap();
        newMap.putAll(map);
        this.metadataMap = newMap;
    }

    /**
     * getMetadataMap() gets a copy of Agent.MetadataMap
     *
     * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    MetadataMap getMetadataMap() {
        MetadataMap map = new MetadataMap();
        map.putAll(this.metadataMap);
        return map;
    }

}
