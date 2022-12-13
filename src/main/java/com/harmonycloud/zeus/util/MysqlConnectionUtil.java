package com.harmonycloud.zeus.util;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.MysqlAccessInfo;
import com.middleware.caas.common.model.MysqlDbDTO;
import com.middleware.caas.common.model.MysqlUserDTO;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author liyinlong
 * @since 2022/3/25 3:09 下午
 */
public class MysqlConnectionUtil {

    private static final String DBDRIVER = "com.mysql.cj.jdbc.Driver";//mysql数据库驱动类

    /**
     * 获取mysql限定名
     *
     * @param dbDTO
     * @return
     */
    public static String getMysqlQualifiedName(MysqlDbDTO dbDTO) {
        return String.format("%s_%s_%s", dbDTO.getClusterId(), dbDTO.getNamespace(), dbDTO.getMiddlewareName());
    }

    public static String getMysqlQualifiedName(String clusterId,String namespace,String middlewareName) {
        return String.format("%s_%s_%s", clusterId, namespace, middlewareName);
    }

    /**
     * 获取mysql限定名
     *
     * @param userDTO
     * @return
     */
    public static String getMysqlQualifiedName(MysqlUserDTO userDTO) {
        return String.format("%s_%s_%s", userDTO.getClusterId(), userDTO.getNamespace(), userDTO.getMiddlewareName());
    }

    public static Connection getDBConnection(MysqlAccessInfo mysqlAccessInfo) {
        String host = mysqlAccessInfo.getHost();
        int port = Integer.parseInt(mysqlAccessInfo.getPort());
        String user = mysqlAccessInfo.getUsername();
        String password = mysqlAccessInfo.getPassword();
        // 获取一个mysql连接
        return MysqlConnectionUtil.getDbConn(host, port, user, password);
    }

    public static boolean passwordCheck(MysqlAccessInfo mysqlAccessInfo, String user, String password) {
        try {
            Class.forName(DBDRIVER);
            String dbUrl = "jdbc:mysql://" + mysqlAccessInfo.getHost() + ":" + mysqlAccessInfo.getPort() + "/?characterEncoding=UTF-8&useSSL=false";
            DriverManager.getConnection(dbUrl, user, password);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取一个数据库连接
     *
     * @param host     主机地址
     * @param port     端口号
     * @param user     用户名
     * @param password 密码
     * @return
     */
    public static Connection getDbConn(String host, int port, String user, String password) {
        try {
            Class.forName(DBDRIVER);
            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/?characterEncoding=UTF-8&useSSL=false";
            return DriverManager.getConnection(dbUrl, user, password);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorMessage.MYSQL_CONNECTION_FAILED);
        }
    }

    /**
     * 连接测试
     *
     * @param host     主机地址
     * @param port     端口号
     * @param user     用户名
     * @param password 密码
     * @return
     */
    public static boolean linkTest(String host, int port, String user, String password) {
        Connection dbConn = getDbConn(host, port, user, password);
        if (dbConn != null) {
            return true;
        }
        return false;
    }

}
