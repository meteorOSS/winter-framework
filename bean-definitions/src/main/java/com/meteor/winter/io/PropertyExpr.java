package com.meteor.winter.io;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PropertyExpr {
    private String key;
    private String defaultValue;
}
