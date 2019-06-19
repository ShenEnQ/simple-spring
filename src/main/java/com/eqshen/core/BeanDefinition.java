package com.eqshen.core;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BeanDefinition {
    private String id;
    private String clazz;
    private List<PropertyInfo> properties;
    private boolean inCreation;
}
