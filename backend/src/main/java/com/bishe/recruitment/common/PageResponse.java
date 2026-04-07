package com.bishe.recruitment.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponse<T> {

    private long pageNum;
    private long pageSize;
    private long total;
    private List<T> records;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .total(page.getTotal())
                .records(page.getRecords())
                .build();
    }
}
