package com.middleware.zeus.util.page;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.github.pagehelper.PageInfo;

/**
 * @author xutianhong
 * @Date 2024/10/24 8:50 PM
 */
public class PageUtil {

    public static <T> PageInfo<T> convertPage(List<T> list, Integer current, Integer size){
        List<T> res = new ArrayList<>();
        for (int i = (current - 1) * size ; i < list.size() && i < current * size; ++i) {
            res.add(list.get(i));
        }
        PageInfo<T> pageInfo = new PageInfo<>(res);
        pageInfo.setPageNum(current);
        pageInfo.setTotal(list.size());
        pageInfo.setSize(res.size());
        pageInfo.setPageSize(size);
        pageInfo.setPrePage(current - 1);
        pageInfo.setNextPage(pageInfo.getList().size() == size ?  current + 1 : 0);
        double endPageNum = Math.ceil((double) list.size() / size);
        // page 页码
        int[] pageNum = IntStream.rangeClosed(1, (int)endPageNum).boxed().mapToInt(Integer::intValue).toArray();
        pageInfo.setNavigatepageNums(pageNum);

        return pageInfo;
    }

    public static <T> List<T> splitPage(List<T> list, Integer current, Integer size){
        List<T> res = new ArrayList<>();
        for (int i = (current - 1) * size ; i < list.size() && i < current * size; ++i) {
            res.add(list.get(i));
        }
        return res;
    }
}
