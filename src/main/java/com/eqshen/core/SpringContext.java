package com.eqshen.core;

import com.eqshen.enums.PropertyTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SpringContext {

    private static Map<String,Object> beanContext = new HashMap<>();


    public SpringContext(String xmlConfigPath) {
        try {
            this.loadBeans(xmlConfigPath);
        } catch (Exception e) {
            log.error("SpringContext init error",e);
        }
    }

    public  Object getBean(String id){
        return beanContext.get(id);
    }

    private void loadBeans(String filePath) throws Exception {
        InputStream inputStream = new FileInputStream(filePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputStream);
        Element root = doc.getDocumentElement();
        NodeList nodes = root.getChildNodes();

        List<BeanDefinition> beanDefinitions = new ArrayList<BeanDefinition>();
        Node node = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if(!(node instanceof Element)){
               continue;
            }
            Element element = (Element) node;
            beanDefinitions.add(this.buildBeanDefinition(element));
        }
        doLoadBean(beanDefinitions);

    }

    private void doLoadBean(List<BeanDefinition> beanDefinitions){
        if(beanDefinitions == null) return;

        for (BeanDefinition beanDefinition : beanDefinitions) {
            try {
                Class beanClass = Class.forName(beanDefinition.getClazz());
                Object bean = beanClass.newInstance();
                List<PropertyInfo> propertyInfos = beanDefinition.getProperties();
                for (PropertyInfo propertyInfo: propertyInfos) {
                    Field declaredField = bean.getClass().getDeclaredField(propertyInfo.getName());
                    declaredField.setAccessible(true);

                    if(propertyInfo.getType() == PropertyTypeEnum.value){
                        Object value = this.typeConvert(declaredField.getType().getSimpleName(),propertyInfo.getValue());
                        declaredField.set(bean,value);
                    }else if(propertyInfo.getType() == PropertyTypeEnum.ref){
                        declaredField.set(bean,this.getBeanById(propertyInfo.getRef()));
                    }else{
                        log.warn("Unknown property type {}",propertyInfo);
                    }
                }
                //register
                registerBean(beanDefinition.getId(),bean);

            } catch (Exception e) {
                log.error("Create bean error",e);
            }
        }
    }

    private BeanDefinition buildBeanDefinition(Element element){
        String id = element.getAttribute("id");
        String className = element.getAttribute("class");
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setClazz(className);
        beanDefinition.setId(id);
        List<PropertyInfo> propertyInfos = new ArrayList<>();
        beanDefinition.setProperties(propertyInfos);


        NodeList propertyList = element.getElementsByTagName("property");

        for (int i = 0; i < propertyList.getLength(); i++) {
            Node property = propertyList.item(i);
            if(property instanceof Element){
                Element propertyElement = (Element) property;
                String name = propertyElement.getAttribute("name");
                String value = propertyElement.getAttribute("value");
                String ref = propertyElement.getAttribute("ref");
                PropertyInfo propertyInfo = new PropertyInfo();
                propertyInfo.setName(name);
                if(value != null && !"".equals(value)){
                    propertyInfo.setType(PropertyTypeEnum.value);
                    propertyInfo.setValue(value);
                }else{
                    propertyInfo.setType(PropertyTypeEnum.ref);
                    propertyInfo.setRef(ref);
                }
                propertyInfos.add(propertyInfo);
            }
        }
        return beanDefinition;
    }

    private Object getBeanById(String id){
        return beanContext.get(id);
    }

    private void registerBean(String id,Object object){
        beanContext.put(id,object);
    }

    /**
     * convert {value} to type of {type}
     * @param type
     * @param value
     */
    private Object typeConvert(String type,Object value){
        Object result = null;
        switch (type){
            case "int":
                result = Integer.parseInt(String.valueOf(value));
                break;
            case "byte":
                result = Byte.valueOf(String.valueOf(value));
                break;
            case "short":
                result = Short.valueOf(String.valueOf(value));
                break;
            case "long":
                result = Long.valueOf(String.valueOf(value));
                break;
            case "float":
                result = Float.valueOf(String.valueOf(value));
                break;
            case "double":
                result = Double.valueOf(String.valueOf(value));
                break;
            case "char":
                result = String.valueOf(value).charAt(0);
                break;
            case "boolean":
                result = Boolean.valueOf(String.valueOf(value));
                break;
            case "String":
                result = String.valueOf(value);
                break;
            default:
                log.error("unknow type:{},value:{}",type,value);
                break;

        }
        return result;
    }
}
