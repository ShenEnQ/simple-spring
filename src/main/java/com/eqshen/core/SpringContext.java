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
import java.util.*;

@Slf4j
public class SpringContext {

    /**
     * first level cache
     */
    private  Map<String,Object> beanContext = new HashMap<>();

    private  Map<String,Object> earlySingletonBeanMap = new HashMap<>();

    private Map<String,BeanDefinition> beanDefinitions = new HashMap<String,BeanDefinition>();


    public SpringContext(String xmlConfigPath) {
        try {
            this.loadBeans(xmlConfigPath);
        } catch (Exception e) {
            log.error("SpringContext init error",e);
        }
    }

    public  Object getBean(String beanName){
        return this.getBeanById(beanName);
    }

    private Object getSingleton(String beanName){
        Object targetObject = beanContext.get(beanName);
        if(targetObject == null && isSingletonInCreation(beanName)){
            targetObject = this.earlySingletonBeanMap.get(beanName);
        }
        return targetObject;
    }

    private boolean isSingletonInCreation(String beanName){
        BeanDefinition beanDefinition = beanDefinitions.get(beanName);
        return beanDefinition != null && beanDefinition.isInCreation();
    }

    private void loadBeans(String filePath) throws Exception {
        InputStream inputStream = new FileInputStream(filePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputStream);
        Element root = doc.getDocumentElement();
        NodeList nodes = root.getChildNodes();


        Node node = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if(!(node instanceof Element)){
               continue;
            }
            Element element = (Element) node;
            BeanDefinition beanDefinition = this.buildBeanDefinition(element);
            beanDefinitions.put(beanDefinition.getId(),beanDefinition);
        }
        doLoadBean();

    }

    private void createBeanByDefinition(BeanDefinition beanDefinition){
        Class beanClass = null;
        try {
            beanClass = Class.forName(beanDefinition.getClazz());
            Object bean = beanClass.newInstance();
            this.earlySingletonBeanMap.put(beanDefinition.getId(),bean);
            beanDefinition.setInCreation(true);
        } catch (Exception e) {
            log.error("create bean failed:{}",beanDefinition);
        }

    }

    private void doLoadBean(){
        if(beanDefinitions == null) return;
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            this.createBeanByDefinition(entry.getValue());
        }

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            this.initBeanByDefinition(entry.getValue());
        }
    }

    private void initBeanByDefinition(BeanDefinition beanDefinition){
        try{
            Object bean = this.earlySingletonBeanMap.get(beanDefinition.getId());
            if(bean == null){
                return;
            }
            List<PropertyInfo> propertyInfos = beanDefinition.getProperties();
            for (PropertyInfo propertyInfo: propertyInfos) {
                Field declaredField = bean.getClass().getDeclaredField(propertyInfo.getName());
                declaredField.setAccessible(true);

                if(propertyInfo.getType() == PropertyTypeEnum.value){
                    Object value = this.typeConvert(declaredField.getType().getSimpleName(),propertyInfo.getValue());
                    declaredField.set(bean,value);
                }else if(propertyInfo.getType() == PropertyTypeEnum.ref){
                    Object refObj = this.getSingleton(propertyInfo.getRef());
                    declaredField.set(bean,refObj);
                }else{
                    log.warn("Unknown property type {}",propertyInfo);
                }
            }
            //register
            beanDefinition.setInCreation(false);
            registerBean(beanDefinition,bean);
        }catch (Exception e){
            log.error("Create bean failed {}",beanDefinition);
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
        return  this.getSingleton(id);
    }

    private void registerBean(BeanDefinition beanDefinition,Object object){
        if(beanDefinition.isInCreation()){
            this.earlySingletonBeanMap.put(beanDefinition.getId(),object);
        }else{
            beanContext.put(beanDefinition.getId(),object);
        }
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
