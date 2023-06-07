package com.middleware.zeus.util.middleware;

import com.github.zafarkhaja.semver.Version;


/**
 * @author liyinlong
 * @since 2022/2/25 3:01 下午
 */
public class ChartVersionUtil {

    public static int compare(String chartVersion1, String chartVersion2) {
        Version v1 = Version.parse(chartVersion1);
        Version v2 = Version.parse(chartVersion2);
        return v2.compareTo(v1);
    }
}
