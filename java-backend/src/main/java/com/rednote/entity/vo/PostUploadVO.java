package com.rednote.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostUploadVO {

    private List<String> urls;

    private Integer imgWidth;

    private Integer imgHeight;
}
