package com.rednote.common;

import lombok.Data;
import java.util.List;

@Data
public class CursorResult<T> {
    private List<T> list; // 数据列表
    private String nextCursor; // 下一次请求用的游标
    private Boolean hasMore; // 是否还有更多数据

    public static <T> CursorResult<T> build(List<T> list, String nextCursor, Boolean hasMore) {
        CursorResult<T> result = new CursorResult<>();
        result.setList(list);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);
        return result;
    }
}