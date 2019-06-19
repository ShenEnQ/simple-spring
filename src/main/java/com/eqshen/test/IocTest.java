package com.eqshen.test;

import com.eqshen.core.SpringContext;
import com.eqshen.demo.BrotherCompany;
import com.eqshen.demo.TriangleLove;

public class IocTest {
    public static void main(String[] args) {
        String filePath = IocTest.class.getClassLoader()
                .getResource("beans.xml").getFile();
        SpringContext springContext = new SpringContext(filePath);

        BrotherCompany company = (BrotherCompany) springContext.getBean("brotherCompany");
        System.out.println(company);

        TriangleLove a = (TriangleLove) springContext.getBean("peopleA");
        TriangleLove b = (TriangleLove) springContext.getBean("peopleB");
        TriangleLove c = (TriangleLove) springContext.getBean("peopleC");
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println("do");
    }
}
