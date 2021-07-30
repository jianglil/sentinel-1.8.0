package com.alibaba.csp.sentinel.dashboard.persist;

import com.alibaba.csp.sentinel.command.CommandConstants;
import com.alibaba.csp.sentinel.dashboard.client.CommandFailedException;
import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.controller.AuthorityRuleController;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.util.AsyncUtils;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NacosSentinelApiClient extends SentinelApiClient {
    private final Logger logger = LoggerFactory.getLogger(AuthorityRuleController.class);

    private static final String remoteAddress = "localhost:8848";
    private static final String groupId = "Sentinel:Demo";

    @Override
    public List<FlowRuleEntity> fetchFlowRuleOfMachine(String app, String ip, int port) {

        try {
            return get(app, ip, port, RuleType.FLOW);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.fetchFlowRuleOfMachine(app, ip, port);
    }

    @Override
    public CompletableFuture<Void> setFlowRuleOfMachineAsync(String app, String ip, int port, List<FlowRuleEntity> rules) {
        try {
            save(app, ip, port, rules, RuleType.FLOW);
            return CompletableFuture.completedFuture(null);
        } catch (NacosException e) {
            logger.warn("Error when modifying cluster mode: " + e);
            return AsyncUtils.newFailedFuture(new RuntimeException(e));
        }
//        return super.setFlowRuleOfMachineAsync(app, ip, port, rules);
    }

    @Override
    public boolean setDegradeRuleOfMachine(String app, String ip, int port, List<DegradeRuleEntity> rules) {
        try {
            save(app, ip, port, rules, RuleType.DEGRADE);
            return true;
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<DegradeRuleEntity> fetchDegradeRuleOfMachine(String app, String ip, int port) {

        try {
            return get(app, ip, port, RuleType.DEGRADE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.fetchDegradeRuleOfMachine(app, ip, port);
    }

    private void save(String app, String ip, int port,
                      List<? extends RuleEntity> oldRules, RuleType ruleType) throws NacosException {
        final String dataId = app + getRuleType(ruleType);
        final String rule = JSON.toJSONString(oldRules.stream()
                .map(r->r.toRule()).collect(Collectors.toList()),true);
        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        logger.info("规则{}推送到{}配置结果：{}",
                ruleType.value,
                remoteAddress + "#" + groupId + "#" + dataId,
                configService.publishConfig(dataId, groupId, rule));
    }

    private List get(String app, String ip, int port, RuleType ruleType) throws NacosException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        final String dataId = app + getRuleType(ruleType);
        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
        String config = configService.getConfig(dataId, groupId, 5000);
        List ruleList = JSON.parseArray(config, ruleType.ruleClass);

        logger.info("规则{}从{}获取配置结果：{}",
                ruleType.value,
                remoteAddress + "#" + groupId + "#" + dataId,
                config);

        return (List) ruleList.stream().map(rule -> ruleType.toRuleEntity(app,ip,port,rule))
                .collect(Collectors.toList());
    }

    private String getRuleType(RuleType ruleType){
        return "-" + ruleType.value;
    }

    public enum RuleType{
        FLOW("flow",FlowRuleEntity.class, FlowRule.class),
        DEGRADE("degrade",DegradeRuleEntity.class, DegradeRule.class),
        ;

        private Class<? extends RuleEntity> ruleEntityClass;
        private Class<? extends Rule> ruleClass;
        private String value;
        RuleType(String value, Class<? extends RuleEntity> ruleEntityClass, Class<? extends Rule> ruleClass) {
            this.value = value;
            this.ruleClass = ruleClass;
            this.ruleEntityClass = ruleEntityClass;
        }

        public Object toRuleEntity(String app, String ip, int port, Object rule) {
            switch (this) {
                case FLOW:
                    return FlowRuleEntity.fromFlowRule(app, ip, port, (FlowRule) rule);
                case DEGRADE:
                    return DegradeRuleEntity.fromDegradeRule(app, ip, port, (DegradeRule) rule);
            }
            return null;
        }
    }
}
