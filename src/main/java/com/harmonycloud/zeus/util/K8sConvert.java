package com.harmonycloud.zeus.util;

import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.harmonycloud.caas.common.model.AffinityDTO;
import io.fabric8.kubernetes.api.model.*;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public static NodeAffinity convertNodeAffinity(String label, boolean isRequired, String operator) {
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
        nsr.setOperator(operator);
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

    /**
     * 将labels转为NodeAffinity
     * @param labels
     * @return
     */
    public static NodeAffinity convertNodeAffinity(List<String> labels, Boolean required) {
        NodeAffinity nf = new NodeAffinity();
        List<PreferredSchedulingTerm> pstList = new ArrayList<>(1);
        List<NodeSelectorTerm> nss = new ArrayList<>(1);

        for (int i = 0; i < labels.size(); i++) {
            String[] labelArr = labels.get(i).split("=");
            NodeSelectorTerm nst;
            if (labelArr.length == 2) {
                nst = convertNodeSelectorTerm(labelArr[0], labelArr[1]);
            } else if (labelArr.length == 1) {
                nst = convertNodeSelectorTerm(labelArr[0]);
            } else {
                continue;
            }

            if (required) {
                nss.add(nst);
            } else {
                PreferredSchedulingTerm p = new PreferredSchedulingTerm();
                p.setPreference(nst);
                // 权重
                p.setWeight(labels.size() - i);
                pstList.add(p);
            }
        }
        if (required) {
            NodeSelector ns = new NodeSelector();
            ns.setNodeSelectorTerms(nss);
            nf.setRequiredDuringSchedulingIgnoredDuringExecution(ns);
        } else {
            nf.setPreferredDuringSchedulingIgnoredDuringExecution(pstList);
        }
        return nf;
    }

    /**
     * 将label转为NodeSelectorTerm
     *
     * @param key   label的key
     * @param value label的value
     * @return
     */
    public static NodeSelectorTerm convertNodeSelectorTerm(String key, String value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return null;
        }
        List<NodeSelectorRequirement> nsrList = new ArrayList<>(1);
        NodeSelectorRequirement nsr = new NodeSelectorRequirement();
        nsr.setKey(key);
        nsr.setOperator("In");
        nsr.setValues(Collections.singletonList(value));
        nsrList.add(nsr);

        NodeSelectorTerm nst = new NodeSelectorTerm();
        nst.setMatchExpressions(nsrList);
        return nst;
    }

    public static NodeSelectorTerm convertNodeSelectorTerm(String key, String value, String operator) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return null;
        }
        List<NodeSelectorRequirement> nsrList = new ArrayList<>(1);
        NodeSelectorRequirement nsr = new NodeSelectorRequirement();
        nsr.setKey(key);
        nsr.setOperator(operator);
        nsr.setValues(Collections.singletonList(value));
        nsrList.add(nsr);

        NodeSelectorTerm nst = new NodeSelectorTerm();
        nst.setMatchExpressions(nsrList);
        return nst;
    }

    public static NodeSelectorTerm convertNodeSelectorTerm(String key) {
        List<NodeSelectorRequirement> nsrList = new ArrayList<>(1);
        NodeSelectorRequirement nsr = new NodeSelectorRequirement();
        nsr.setKey(key);
        nsr.setOperator("Exists");
        nsrList.add(nsr);

        NodeSelectorTerm nst = new NodeSelectorTerm();
        nst.setMatchExpressions(nsrList);
        return nst;
    }

    public static JSONObject convertNodeAffinity2Json(List<AffinityDTO> dtoLis) {
        if (CollectionUtils.isEmpty(dtoLis)) {
            return null;
        }
        NodeAffinity nodeAffinity;
        if (dtoLis.get(0) != null && dtoLis.get(0).isRequired()) {
            nodeAffinity = convertNodeAffinity(dtoLis.stream().map(AffinityDTO::getLabel).collect(Collectors.toList()), true);
        } else {
            nodeAffinity = convertNodeAffinity(dtoLis.stream().map(AffinityDTO::getLabel).collect(Collectors.toList()), false);
        }
        return convertNodeAffinity2Json(nodeAffinity);
    }

    public static JSONObject convertNodeAffinity2Json(AffinityDTO affinityDTO, String operator) {
        if (affinityDTO == null) {
            return null;
        }
        NodeAffinity nodeAffinity = convertNodeAffinity(affinityDTO.getLabel(), affinityDTO.isRequired(), operator);
        return convertNodeAffinity2Json(nodeAffinity);
    }

    public static JSONObject convertNodeAffinity2Json(NodeAffinity nodeAffinity) {
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
                        json.put("label", nsq.getKey() + "=" + (CollectionUtils.isEmpty(nsq.getValues()) ? "" : nsq.getValues().get(0)));
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

    public static Toleration convertToleration(String tolerationStr) {
        Toleration toleration = new Toleration();
        String[] tolerationAry = tolerationStr.split(":");
        String[] pair = tolerationAry[0].split("=");
        toleration.setKey(pair[0]);
        if (tolerationStr.contains("Exists")) {
            toleration.setOperator("Exists");
            toleration.setEffect(tolerationAry[2]);
        } else {
            toleration.setOperator("Equal");
            toleration.setValue(pair[1]);
            toleration.setEffect(tolerationAry[1]);
        }
        return toleration;
    }

    public static JSONObject convertObject2Json(Object obj) {
        if (obj == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String na;
        try {
            na = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("jackson转换错误，将使用fastjson转换", e);
            SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
            filter.getExcludes().add("additionalProperties");
            na = JSON.toJSONString(obj, filter);
        }
        if (StringUtils.isEmpty(na)) {
            return null;
        }
        return JSON.parseObject(na);
    }

    public static JSONArray convertToleration2Json(List<String> tolerationList){
        JSONArray jsonArray = new JSONArray();
        tolerationList.forEach(tolerationStr ->{
            Toleration toleration = convertToleration(tolerationStr);
            JSONObject jsonObject = convertObject2Json(toleration);
            jsonArray.add(jsonObject);
        });
        return jsonArray;
    }

}
