package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack){
                // 记录日志
                log.warn("消息没有到达交换机：" + cause);
            }
        });
        this.rabbitTemplate.setReturnCallback(
                (message, replyCode, replyText, exchange, routingKey) -> {
                    log.warn("消息没有到达消息队列。来自于交换机：{},路由键:{},消息内容:{}"
                            ,exchange,routingKey,new String(message.getBody()));
        });
    }

    @Bean
    public TopicExchange delayExchange(){
        return ExchangeBuilder.topicExchange("SPRING_DELAY_EXCHANGE").build();
    }

//    x-message-ttl：指定TTL时间
//    x-dead-letter-exchange：死信转发所需的死信交换机（DLX）
//    x-dead-letter-routing-key：转发死信时的routingKey（DLK）
    @Bean
    public Queue delayQueue(){
        return QueueBuilder.durable("SPRING_DELAY_QUEUE").withArgument("x-message-ttl",6000)
                .withArgument("x-dead-letter-exchange","SPRING_DEAD_EXCHANGE")
                .withArgument("x-dead-letter-routing-key","ab.dead").build();

    }

    @Bean
    public Binding delayBinding(TopicExchange delayExchange, Queue delayQueue){
        return BindingBuilder.bind(delayQueue).to(delayExchange).with("ab.delay");
    }

    @Bean
    public TopicExchange deadExchange(){
        return ExchangeBuilder.topicExchange("SPRING_DEAD_EXCHANGE").build();
    }

    @Bean
    public Queue deadQueue(){
        return QueueBuilder.durable("SPRING_DEAD_QUEUE").build();
    }

    @Bean
    public Binding deadBinding(TopicExchange deadExchange, Queue deadQueue){
        return BindingBuilder.bind(deadQueue).to(deadExchange).with("ab.dead");
    }
}
