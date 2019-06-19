package com.eqshen.demo;

import lombok.Data;

@Data
public class TriangleLove {
    private String userName;
    private TriangleLove personInHert;

    public String toString(){
        return "(userName:" + userName+",personInHert:" + personInHert.getUserName()+")";
    }
}
