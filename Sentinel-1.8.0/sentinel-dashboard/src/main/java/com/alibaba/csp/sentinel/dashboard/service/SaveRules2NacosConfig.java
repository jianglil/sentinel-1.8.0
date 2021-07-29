package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaveRules2NacosConfig {


    @Autowired
    private InMemoryRuleRepositoryAdapter<FlowRuleEntity> repository;

    public void save(FlowRuleEntity entity) throws NacosException {
        final String remoteAddress = "localhost:8848";
        final String groupId = "Sentinel:Demo";
        final String dataId = entity.getApp() + "-flow";


        List<FlowRuleEntity> oldRules = repository.findAllByMachine(MachineInfo.of(entity.getApp(), entity.getIp(), entity.getPort()));


        final String rule = JSON.toJSONString(oldRules.stream().map(r->r.toRule()).collect(Collectors.toList()),true);
        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        System.out.println(configService.publishConfig(dataId, groupId, rule));
    }

    public <R> List<R> get(String app, String ip, int port, Class<R> r) throws NacosException {

        final String remoteAddress = "localhost:8848";
        final String groupId = "Sentinel:Demo";
        final String dataId = app + "-flow";


        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        String config = configService.getConfig(dataId, groupId, 5000);
        List<FlowRule> flowRuleList = JSON.parseArray(config, FlowRule.class);

        return (List<R>) flowRuleList.stream().map(rule -> FlowRuleEntity.fromFlowRule(app, ip, port, rule))
                .collect(Collectors.toList());
    }

}
