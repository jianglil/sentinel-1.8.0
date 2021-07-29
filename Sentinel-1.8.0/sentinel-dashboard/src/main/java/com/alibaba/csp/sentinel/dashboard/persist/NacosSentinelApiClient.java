package com.alibaba.csp.sentinel.dashboard.persist;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NacosSentinelApiClient extends SentinelApiClient {

    @Override
    public boolean setDegradeRuleOfMachine(String app, String ip, int port, List<DegradeRuleEntity> rules) {
        try {
            save(app,ip,port,rules,DegradeRuleEntity.class);
            return true;
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<DegradeRuleEntity> fetchDegradeRuleOfMachine(String app, String ip, int port) {

        try {
            return get(app, ip, port, DegradeRuleEntity.class,DegradeRule.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.fetchDegradeRuleOfMachine(app, ip, port);
    }

    private void save(String app, String ip, int port,
                      List<? extends RuleEntity> oldRules, Class<? extends RuleEntity> rClazz) throws NacosException {
        final String remoteAddress = "localhost:8848";
        final String groupId = "Sentinel:Demo";
        final String dataId = app + getRuleType(rClazz);
        final String rule = JSON.toJSONString(oldRules.stream()
                .map(r->r.toRule()).collect(Collectors.toList()),true);
        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        System.out.println(configService.publishConfig(dataId, groupId, rule));
    }

    private <R,Q> List<R> get(String app, String ip, int port, Class<R> r, Class<Q> q) throws NacosException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        final String remoteAddress = "localhost:8848";
        final String groupId = "Sentinel:Demo";
        final String dataId = app + getRuleType(r);


        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        String config = configService.getConfig(dataId, groupId, 5000);
        List<Q> ruleList = JSON.parseArray(config, q);

        return ruleList.stream().map(rule -> this.ruleEntityFromRule(app, ip, port, rule, r))
                .collect(Collectors.toList());
    }

    private <R,Q> R ruleEntityFromRule(String app, String ip, int port, Q rule, Class<R> r) {

        if (rule instanceof FlowRule) {
            return (R) FlowRuleEntity.fromFlowRule(app, ip, port, (FlowRule) rule);
        }
        if (rule instanceof DegradeRule) {
            return (R) DegradeRuleEntity.fromDegradeRule(app, ip, port, (DegradeRule) rule);
        }
        return null;
    }

    private String getRuleType(Class r){

        if (r.equals(DegradeRuleEntity.class)) {
            return "-degrade";
        }

        if (r.equals(FlowRuleEntity.class)) {
            return "-flow";
        }

        return null;
    }
}
