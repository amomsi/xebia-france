/**
 * Workaround overlap between Spring 2.5 
 * org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
 * and Jsr250BeanPostProcessor that causes double invocation of 
 * JSR250 life cycle methods if both post processors are registered.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.bus.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

public class Jsr250BeanPostProcessor 
    implements DestructionAwareBeanPostProcessor, Ordered, ApplicationContextAware, InitializingBean {

    protected boolean disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered = false;
    
    private ResourceManager resourceManager;
    private List<ResourceResolver> resolvers;
    
    private ApplicationContext context;

    Jsr250BeanPostProcessor() {
    }
    
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;    
    }
    
    public int getOrder() {
        return 1010;
    }
        
    public Object postProcessAfterInitialization(Object bean, String beanId) throws BeansException {
        if(disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered) {
            return bean;
        }
        if (bean != null) {
            new ResourceInjector(resourceManager, resolvers).construct(bean);
        }
        if (bean instanceof ResourceManager) {
            resourceManager = (ResourceManager)bean;

            Map<String, Object> mp = CastUtils.cast(context.getBeansOfType(ResourceResolver.class));
            Collection<ResourceResolver> resolvs = CastUtils.cast(mp.values());
            resolvers = new ArrayList<ResourceResolver>(resourceManager.getResourceResolvers());
            resolvers.addAll(resolvs);
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanId) throws BeansException {
        if(disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered) {
            return bean;
        }
        if (bean != null) {
            new ResourceInjector(resourceManager, resolvers).inject(bean);
        }
        return bean;
    }

    public void postProcessBeforeDestruction(Object bean, String beanId) {
        if(disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered) {
            return;
        }
        if (bean != null) {
            new ResourceInjector(resourceManager, resolvers).destroy(bean);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // CommonAnnotationBeanPostProcessor was introduced in Spring 2.5
            Class<?> commonAnnotationBeanPostProcessorClass = Class
                .forName("org.springframework.context.annotation.CommonAnnotationBeanPostProcessor");
            String[] beanNamesForCommonAnnotationBeanPostProcessor = context.getBeanNamesForType(commonAnnotationBeanPostProcessorClass);
            if (beanNamesForCommonAnnotationBeanPostProcessor.length > 0) {
                disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered = true;
                System.out.println("disabledBecauseCommonAnnotationBeanPostProcessorIsAlreadyRegistered");
            }
        } catch (ClassNotFoundException e) {
            // ignore, CommonAnnotationBeanPostProcessor is not in he classpath
        }        
    }
}
