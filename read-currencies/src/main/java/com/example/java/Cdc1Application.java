/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.java;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class Cdc1Application {

	private static final Logger logger = LoggerFactory.getLogger(Cdc1Application.class);

	private final Map<String, Double> currencies = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		SpringApplication.run(Cdc1Application.class, args);
	}

	@KafkaListener(id = "currency1", topics = "currency")
	public void listen(@Payload(required = false) Double rate,
			@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {

		if (rate == null) {
			this.currencies.remove(key);
		}
		else {
			this.currencies.put(key, rate);
		}
		logger.info("Currencies now: " + this.currencies);
	}

	@Bean
	public NewTopic topic() {
		return TopicBuilder.name("currency")
				.compact()
				.partitions(1)
				.replicas(1)
				.build();
	}

}

@Component
class FactoryConfigurer {

	FactoryConfigurer(ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
		factory.getContainerProperties().setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {

			@Override
			public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
				consumer.seekToBeginning(partitions);
			}

		});
	}

}
