package org.snomed.snowstorm.avelios;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AveliosConfiguration {
    @Bean
    RestHighLevelClient restHighLevelClient(
            @Value("${elasticsearch.host}") String host,
            @Value("${elasticsearch.port}") Integer port,
            @Value("${elasticsearch.scheme}") String scheme
    ) {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
    }

}
