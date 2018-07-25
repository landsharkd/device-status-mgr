package com.phicomm.smarthome.statusmgr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.client.RestTemplate;

import com.phicomm.smarthome.statusmgr.listener.MqttBrokerMsgListener;
import com.phicomm.smarthome.statusmgr.model.RocketmqConfig;

/**
 * 应用启动入口
 * package: com.phicomm.smarthome.statusmgr
 * class: StatusMgrMain.java
 * date: 2018年6月28日 上午11:15:36
 * author: wen.xia
 * description:
 */
@SpringBootApplication(scanBasePackages = { "com.phicomm.smarthome.**" })
@PropertySources({ @PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true) })
@MapperScan("com.phicomm.smarthome.command.dao.**")
@EnableConfigurationProperties({ RocketmqConfig.class })
@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableEurekaClient
//@EnableFeignClients
public class StatusMgrMain {
    private static Logger logger = LogManager.getLogger(StatusMgrMain.class);

    /**
     * rest接口.
     * @return rest模板
     */
    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 项目启动方法入口.
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplication.run(StatusMgrMain.class, args);

        //初始化mqtt消息回调，初始化读取mqtt服务器消息插件的消息队列
        MqttBrokerMsgListener.instance.start();
        logger.info("startup success!");
    }
}
