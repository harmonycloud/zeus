package com.middleware.zeus.service.k8s.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.model.StorageDto;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.integration.cluster.NamespaceWrapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.k8s.impl.NamespaceServiceImpl;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.skyview.client.Skyview2NamespaceServiceClient;
import com.middleware.zeus.util.ZeusCurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/21 2:19 下午
 */
@Slf4j
@Service
public class Skyview2NamespaceServiceImpl {
}
