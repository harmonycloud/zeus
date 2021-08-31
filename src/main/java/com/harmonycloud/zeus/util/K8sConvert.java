package com.harmonycloud.zeus.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
@Slf4j
public class K8sConvert {

    /**
     * 把label转为节点亲和
     *
     * @param label      label，形如a=b,c=d
     * @param isRequired 是否强制亲和
     * @return
     */
    public static NodeAffinity convertNodeAffinity(String label, boolean isRequired) {
        if (StringUtils.isBlank(label) || !label.contains("=")) {
            return null;
        }
        String[] labelArr = label.split("=");
        if (labelArr.length != 2) {
            return null;
        }
        NodeAffinity nf = new NodeAffinity();
        List<NodeSelectorRequirement> nsrList = new ArrayList<>(1);
        NodeSelectorRequirement nsr = new NodeSelectorRequirement();
        nsr.setKey(labelArr[0]);
        nsr.setOperator("In");
        nsr.setValues(Collections.singletonList(labelArr[1]));
        nsrList.add(nsr);

        NodeSelectorTerm nst = new NodeSelectorTerm();
        nst.setMatchExpressions(nsrList);

        if (isRequired) {
            List<NodeSelectorTerm> nss = new ArrayList<>(1);
            nss.add(nst);
            NodeSelector ns = new NodeSelector();
            ns.setNodeSelectorTerms(nss);
            nf.setRequiredDuringSchedulingIgnoredDuringExecution(ns);
        } else {
            PreferredSchedulingTerm p = new PreferredSchedulingTerm();
            // 权重
            p.setWeight(50);
            p.setPreference(nst);
            List<PreferredSchedulingTerm> pstList = new ArrayList<>(1);
            pstList.add(p);
            nf.setPreferredDuringSchedulingIgnoredDuringExecution(pstList);
        }
        return nf;
    }
    
    public static JSONObject convertNodeAffinity2Json(String label, boolean isRequired) {
        NodeAffinity nodeAffinity = convertNodeAffinity(label, isRequired);
        if (nodeAffinity == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String na;
        try {
            na = objectMapper.writeValueAsString(nodeAffinity);
        } catch (JsonProcessingException e) {
            log.error("jackson转换NodeAffinity错误，将使用fastjson转换", e);
            SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
            filter.getExcludes().add("additionalProperties");
            na = JSON.toJSONString(nodeAffinity, filter);
        }
        if (StringUtils.isEmpty(na)) {
            return null;
        }
        return JSON.parseObject(na);
    }

    /**
     * 将k8s的节点亲和转为实体类
     * 
     * @param nodeAffinity 节点亲和
     * @param tClass       待转的class类
     * @param <T>          泛型
     * @return
     */
    public static <T> List<T> convertNodeAffinity(NodeAffinity nodeAffinity, Class<T> tClass) {
        List<T> list = new ArrayList<>();
        JSONObject json = new JSONObject();
        // 非强制亲和
        if (CollectionUtils.isNotEmpty(nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution())) {
            List<PreferredSchedulingTerm> pstList = nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution();
            for (PreferredSchedulingTerm pst : pstList) {
                List<NodeSelectorRequirement> nsqList = pst.getPreference().getMatchExpressions();
                if (nsqList != null && nsqList.size() > 0) {
                    for (NodeSelectorRequirement nsq : nsqList) {
                        json.put("required", false);
                        json.put("label", nsq.getKey() + "=" + nsq.getValues().get(0));
                        list.add(JSONObject.toJavaObject(json, tClass));
                    }
                }
            }
        }

        // 强制亲和
        if (Objects.nonNull(nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution())) {
            NodeSelector r = nodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution();
            if (CollectionUtils.isNotEmpty(r.getNodeSelectorTerms())) {
                for (NodeSelectorTerm nst : r.getNodeSelectorTerms()) {
                    if (Objects.nonNull(nst) && CollectionUtils.isNotEmpty(nst.getMatchExpressions())) {
                        List<NodeSelectorRequirement> nsr = nst.getMatchExpressions();
                        for (NodeSelectorRequirement ns : nsr) {
                            json.put("required", true);
                            json.put("label", ns.getKey() + "=" + ns.getValues().get(0));
                            list.add(JSONObject.toJavaObject(json, tClass));
                        }
                    }
                }
            }
        }
        return list;
    }

}
