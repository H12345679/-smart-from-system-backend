package com.edu.smartfarm.common;

import lombok.Data;
import java.util.List;

/**
 * 分页响应体
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Long current;
    private Long size;

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }
}
