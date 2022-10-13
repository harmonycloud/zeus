package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.enums.middleware.PostgresqlAuthorityEnum;

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
        for (String s : acls) {
            if (s.contains(username + "=")) {
                authority = s.split("/")[0].replace(username + "=", "");
                break;
            }
        }
        // todo 根据*号确认是否可传递权限
        if (authority.contains(PostgresqlAuthorityEnum.INSERT.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.UPDATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.DELETE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TRUNCATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.REFERENCES.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TRIGGER.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.CREATE.getAuthority())
            || authority.contains(PostgresqlAuthorityEnum.TEMPORARY.getAuthority())) {
            return "rw";
        } else {
            return "r";
        }
    }

}
