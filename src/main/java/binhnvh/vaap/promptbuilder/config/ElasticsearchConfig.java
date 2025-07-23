package binhnvh.vaap.promptbuilder.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username:elastic}")
    private String username;

    @Value("${elasticsearch.password:changeme}")
    private String password;

    @Value("${elasticsearch.scheme:http}")
    private String scheme;

    private ElasticsearchTransport transport;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Create credentials provider
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
        );

        // Create the low-level client
        RestClient restClient = RestClient.builder(
                        new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        // Create the transport with a Jackson mapper
        transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        // Create and return the API client
        return new ElasticsearchClient(transport);
    }

    @PreDestroy
    public void cleanup() {
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception e) {
                System.err.println("Error closing Elasticsearch transport: " + e.getMessage());
            }
        }
    }
}
