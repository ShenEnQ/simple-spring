package com.eqshen.core;

import com.eqshen.enums.PropertyTypeEnum;
import lombok.Data;

@Data
public class PropertyInfo {
    private PropertyTypeEnum type;
    private String name;
    private Object value;
    private String ref;
}
