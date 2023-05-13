package com.middleware.zeus.common.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author LanChao
 * @date 2021-01-11 10:58:15
 */
public class RelationWorkload implements Serializable {

    private String name;

    /**
     * Deployment / StatefulSet
     */
    private String type;

    private String namespace;

    private List<String> image;

    private String status;

    private Integer instance;

    private String createTime;

    private List<String> cpu;

    private List<String> memory;

    private Map<String, String> labels;

    private boolean notRelated;

    private String deployRevision;

    private String uid;

    public String getName() {
        return name;
    }

    public RelationWorkload setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public RelationWorkload setType(String type) {
        this.type = type;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public RelationWorkload setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public List<String> getImage() {
        return image;
    }

    public RelationWorkload setImage(List<String> image) {
        this.image = image;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public RelationWorkload setStatus(String status) {
        this.status = status;
        return this;
    }

    public Integer getInstance() {
        return instance;
    }

    public RelationWorkload setInstance(Integer instance) {
        this.instance = instance;
        return this;
    }

    public String getCreateTime() {
        return createTime;
    }

    public RelationWorkload setCreateTime(String createTime) {
        this.createTime = createTime;
        return this;
    }

    public List<String> getCpu() {
        return cpu;
    }

    public RelationWorkload setCpu(List<String> cpu) {
        this.cpu = cpu;
        return this;
    }

    public List<String> getMemory() {
        return memory;
    }

    public RelationWorkload setMemory(List<String> memory) {
        this.memory = memory;
        return this;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public RelationWorkload setLabels(Map<String, String> labels) {
        this.labels = labels;
        return this;
    }

    public boolean isNotRelated() {
        return notRelated;
    }

    public RelationWorkload setNotRelated(boolean notRelated) {
        this.notRelated = notRelated;
        return this;
    }

    public String getDeployRevision() {
        return deployRevision;
    }

    public RelationWorkload setDeployRevision(String deployRevision) {
        this.deployRevision = deployRevision;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public RelationWorkload setUid(String uid) {
        this.uid = uid;
        return this;
    }
}
