package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.enums.middleware.PostgresqlAuthorityEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/13 2:30 下午
 */
public class PostgresqlAuthorityUtil {

    public static String checkAuthority(String acl, String username) {
        acl = acl.replace("{", "");
        acl = acl.replace("}", "");
        String[] acls = acl.split(",");
        String authority = "";
        for (String a : acls) {
            if (a.contains(username + "=")) {
                authority = a.split("/")[0].replace(username + "=", "");
                break;
            }
        }
        StringBuilder sb =new StringBuilder();
        // 根据*号判断是否可传递权限
        if (authority.contains("*")){
            sb.append("*");
        }
        if (StringUtils.isEmpty(authority)){
            return null;
        }
        if (authority.contains(PostgresqlAuthorityEnum.INSERT.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.UPDATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.DELETE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TRUNCATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.REFERENCES.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TRIGGER.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.CREATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TEMPORARY.getAuthority())) {
            sb.append("readWrite");
        } else {
            sb.append("readOnly");
        }
        return sb.toString();
    }

}
