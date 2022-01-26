package com.cb.fts.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

@Configuration
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    @Value("${spring.couchbase.bootstrap-hosts}")
    private String connectionString;

    @Value("${spring.couchbase.bucket.name}")
    private String bucket;

    @Value("${spring.couchbase.bucket.user}")
    private String username;

    @Value("${spring.couchbase.bucket.password}")
    private String password;

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getBucketName() {
        return bucket;
    }




    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}