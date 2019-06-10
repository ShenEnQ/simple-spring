package com.eqshen.test;

import com.eqshen.core.SpringContext;
import com.eqshen.demo.BrotherCompany;

public class IocTest {
    public static void main(String[] args) {
        String filePath = IocTest.class.getClassLoader()
                .getResource("beans.xml").getFile();
        SpringContext springContext = new SpringContext(filePath);

        BrotherCompany company = (BrotherCompany) springContext.getBean("brotherCompany");
        System.out.println(company);
    }
}
