package com.xenotask.xeno.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${sync.rabbit.dlx-exchange:sync.dlx}")
    private String dlxExchange;

    @Value("${sync.rabbit.dlq-name:sync.jobs.dlq}")
    private String dlqName;

    @Value("${sync.rabbit.dlq-routing-key:dlq}")
    private String dlqRoutingKey;

    @Value("${sync.rabbit.listener.concurrent-consumers:2}")
    private int concurrentConsumers;

    @Value("${sync.rabbit.listener.max-concurrent-consumers:8}")
    private int maxConcurrentConsumers;

    @Value("${sync.rabbit.listener.default-requeue-rejected:false}")
    private boolean defaultRequeueRejected;

    @Bean
    public Exchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(dlxExchange).durable(true).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, Exchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to((DirectExchange) deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(defaultRequeueRejected);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        return factory;
    }
}
