package com.middleware.zeus.common.model;

import java.util.List;

public class PodDetail {

    private String name;

    private String namespace;

    private String status;

    private String ip;

    private String nodeIp;

    private String startTime;

    private String tag;

    private List<ContainerWithStatus> containers;

    private List<ContainerWithStatus> initContainers;

    private String deployment;

    private String deployVersion;

    private Boolean isTerminating;

    private String uid;

    //private String runtimeClassName;
    private boolean safeContainer;

    private boolean highPerformance;

    private String clusterId;

    private String clusterAliasName;

    public PodDetail() {

    }

    public String getClusterAliasName() {
        return clusterAliasName;
    }

    public void setClusterAliasName(String clusterAliasName) {
        this.clusterAliasName = clusterAliasName;
    }

    public PodDetail(String name, String namespace, String status, String ip, String nodeIp, String startTime) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.ip = ip;
        this.nodeIp = nodeIp;
        this.startTime = startTime;
    }

    public PodDetail(String name, String namespace, String status, String ip, String nodeIp, String startTime, String uid) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.ip = ip;
        this.nodeIp = nodeIp;
        this.startTime = startTime;
        this.uid = uid;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getDeployVersion() {
        return deployVersion;
    }

    public void setDeployVersion(String deployVersion) {
        this.deployVersion = deployVersion;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public List<ContainerWithStatus> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerWithStatus> containers) {
        this.containers = containers;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public Boolean getTerminating() {
        return isTerminating;
    }

    public void setTerminating(Boolean terminating) {
        isTerminating = terminating;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<ContainerWithStatus> getInitContainers() {
        return initContainers;
    }

    public void setInitContainers(List<ContainerWithStatus> initContainers) {
        this.initContainers = initContainers;
    }
/*
    public String getRuntimeClassName() {
        return runtimeClassName;
    }

    public void setRuntimeClassName(String runtimeClassName) {
        this.runtimeClassName = runtimeClassName;
    }*/

    public boolean isSafeContainer() {
        return safeContainer;
    }

    public void setSafeContainer(boolean safeContainer) {
        this.safeContainer = safeContainer;
    }

    public boolean isHighPerformance() {
        return highPerformance;
    }

    public void setHighPerformance(boolean highPerformance) {
        this.highPerformance = highPerformance;
    }
}
