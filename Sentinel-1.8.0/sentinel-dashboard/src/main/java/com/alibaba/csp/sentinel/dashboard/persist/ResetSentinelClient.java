package com.alibaba.csp.sentinel.dashboard.persist;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class ResetSentinelClient implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {

        String beanName = "sentinelApiClient";
        if (beanDefinitionRegistry
                .getBeanDefinition(beanName) != null) {
            beanDefinitionRegistry
                    .registerBeanDefinition(beanName,new RootBeanDefinition(NacosSentinelApiClient.class));
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }
}
