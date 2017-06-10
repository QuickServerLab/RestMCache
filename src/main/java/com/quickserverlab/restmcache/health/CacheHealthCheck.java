package com.quickserverlab.restmcache.health;

import com.codahale.metrics.health.HealthCheck;
import com.quickserverlab.restmcache.client.RestMCacheClient;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheHealthCheck extends HealthCheck {
    private static final Logger logger =  Logger.getLogger(CacheHealthCheck.class.getName());
	
	private RestMCacheClient restMCacheClient;
	private String url;
    
    public CacheHealthCheck(String url, RestMCacheClient restMCacheClient) {
		this.url = url;
        this.restMCacheClient = restMCacheClient;
    }

    @Override
    protected Result check() throws Exception {
        if (restMCacheClient.isAlive()) {
            String version = null;
            try {
                version = restMCacheClient.getVersion();
				restMCacheClient.getStats();
				restMCacheClient.flushAll();
				restMCacheClient.flushAll(10);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "error :{0}"+e, e);
            }
            logger.log(Level.INFO, "version: {0}", version);

            if(version==null) {
                return Result.unhealthy("Cannot communicate to cache " + url);
            } else {
                return Result.healthy();
            }
        } else {
            return Result.unhealthy("Cannot connect to cache " + url);
        }
    }
    
}
