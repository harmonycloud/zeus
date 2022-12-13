package com.middleware.zeus.util;

/**
 * @author liyinlong
 * @since 2021/10/21 9:48 下午
 */
public class SortUtils {

    public static void quickSort(String[] arr, int left, int right) {
        String f, t;
        int rtemp, ltemp;

        ltemp = left;
        rtemp = right;
        f = arr[(left + right) / 2];                        //分界值
        while (ltemp < rtemp) {
            while (arr[ltemp].compareTo(f) < 0) {
                ++ltemp;
            }
            while (arr[rtemp].compareTo(f) > 0) {
                --rtemp;
            }
            if (ltemp <= rtemp) {
                t = arr[ltemp];
                arr[ltemp] = arr[rtemp];
                arr[rtemp] = t;
                --rtemp;
                ++ltemp;
            }
        }
        if (ltemp == rtemp) {
            ltemp++;
        }
        if (left < rtemp) {
            quickSort(arr, left, ltemp - 1);            //递归调用
        }
        if (ltemp < right) {
            quickSort(arr, rtemp + 1, right);            //递归调用
        }
    }
}
