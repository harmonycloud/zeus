package com.middleware.zeus.service.middleware;

import java.util.List;
import java.util.Map;

/**
 * @author lfy
 * @date 2024/11/20 09:53:26 上午
 */
public interface PostgresqlService {

    /**
     * 查询字符集及其语言环境
     */
    public Map<String, List<String>> getChatSet(String version);
}
