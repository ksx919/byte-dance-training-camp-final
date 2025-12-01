package com.rednote.entity.dto;

import lombok.Data;

@Data
public class PostPublishDTO {

    private String title;

    private String content;

    private Integer imgWidth;

    private Integer imgHeight;
}
