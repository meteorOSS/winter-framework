package com.meteor.winter.ioc.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Resource {
    private String path;
    private String name;
}
