package com.ionic.cloudstorage.awss3;

public final class Version {

    private static final String BUILD = "${build}";
    private static final String VERSION = "${project.version}";
    private static final String GROUPID = "${project.groupId}";
    private static final String ARTIFACTID = "${project.artifactId}";

    public static String getMajorVersion() {
        return VERSION.split("\\.")[0];
    }

    public static String getMinorVersion() {
        return VERSION.split("\\.")[1];
    }

    public static String getPatchVersion() {
        return VERSION.split("\\.")[2];
    }

    public static String getBuild() {
        return BUILD;
    }

    public static String getGroupId() {
        return GROUPID;
    }

    public static String getArtifactId() {
        return ARTIFACTID;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getFullVersion() {
        return VERSION + "-" + BUILD;
    }
}
