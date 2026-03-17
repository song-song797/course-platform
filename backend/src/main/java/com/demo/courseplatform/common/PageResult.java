package com.demo.courseplatform.common;

import java.util.List;

public record PageResult<T>(List<T> list, long total, int pageNo, int pageSize) {

    public static <T> PageResult<T> of(List<T> source, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, source.size());
        int toIndex = Math.min(fromIndex + safePageSize, source.size());
        return new PageResult<>(source.subList(fromIndex, toIndex), source.size(), safePageNo, safePageSize);
    }
}
