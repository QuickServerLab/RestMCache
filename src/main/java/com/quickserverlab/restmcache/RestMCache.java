package com.quickserverlab.restmcache;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.health.HealthCheck;
import com.quickserverlab.restmcache.client.RestMCacheClient;
import com.quickserverlab.restmcache.core.CacheCounter;
import com.quickserverlab.restmcache.core.CacheException;
import com.quickserverlab.restmcache.core.CacheInterface;
import com.quickserverlab.restmcache.health.CacheHealthCheck;
import com.quickserverlab.restmcache.resources.CacheResource;
import com.quickserverlab.restmcache.resources.FlushAllResource;
import com.quickserverlab.restmcache.resources.StatsResource;
import com.quickserverlab.restmcache.resources.VersionResource;
import com.quickserverlab.restmcache.util.HexUtil;
import com.quickserverlab.restmcache.util.MemoryWarningSystem;
import io.dropwizard.Application;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author akshath
 */
public class RestMCache extends Application<RestMCacheCfg> {
    private static final Logger logger =  Logger.getLogger(RestMCache.class.getName());	
	public static final String version = "0.1.0";
	
	private CacheInterface cache;
	private int httpPort;
    
    public static void main(String[] args) throws Exception {
        new RestMCache().run(new String[]{"server", System.getProperty("restmcache.config")});
    }

    @Override
    public void initialize(Bootstrap<RestMCacheCfg> bootstrap) {
    }

    @Override
    public void run(RestMCacheCfg cfg, Environment env) {
        JmxReporter.forRegistry(env.metrics()).build().start(); // Manually add JMX reporting (Dropwizard regression)
        
		DefaultServerFactory serverFactory = (DefaultServerFactory) cfg.getServerFactory();
		for (ConnectorFactory connector : serverFactory.getApplicationConnectors()) {
			if (connector.getClass().isAssignableFrom(HttpConnectorFactory.class)) {
				httpPort = ((HttpConnectorFactory) connector).getPort();
				break;
			}
		}
		
		HexUtil.setCharset(cfg.getCharSet());
		
		try {
			cache = (CacheInterface) Class.forName(cfg.getCacheImplClass()).newInstance();
			cache.setPort(httpPort);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error: "+ex, ex);
			System.exit(-1);
		}		
		
		env.jersey().register(new CacheResource(cfg, cache));
		env.jersey().register(new FlushAllResource(cfg, cache));
		env.jersey().register(new StatsResource(cfg, cache));
		env.jersey().register(new VersionResource(cfg, cache));	
		
		RestMCacheClient restMCacheClient = new RestMCacheClient();
		restMCacheClient.setUrl("http://localhost:"+httpPort);
		restMCacheClient.init();
		
        CacheHealthCheck cacheHealthCheck = new CacheHealthCheck(restMCacheClient.getUrl(), restMCacheClient);        
        env.healthChecks().register("cache", cacheHealthCheck);  
		
		
		if (cfg.getFlushOnLowMemoryPercent()>0) {
			final double fpercent = cfg.getFlushOnLowMemoryPercent()/100.0;
			MemoryWarningSystem.setPercentageUsageThreshold(fpercent);//.95=95%
			logger.log(Level.INFO, "MemoryWarningSystem set to {0}; will flush if reached!", fpercent);

			MemoryWarningSystem mws = new MemoryWarningSystem();
			mws.addListener(new MemoryWarningSystem.Listener() {
				public void memoryUsageHigh(long usedMemory, long maxMemory) {
					logger.log(Level.INFO,
						"Memory usage high!: UsedMemory: {0};maxMemory:{1}",
						new Object[]{usedMemory, maxMemory});
					double percentageUsed = (((double) usedMemory) / maxMemory) * 100;
					logger.log(Level.SEVERE,
						"Memory usage high! Percentage of memory used: {0}",
						percentageUsed);

					long memLimit = (long) (fpercent * 100);
					logger.warning("Calling GC to clear memory");
					System.gc();
					CacheCounter.gcCalls++;
					long memPercentAfterGC = MemoryWarningSystem.getMemUsedPercentage();
					logger.log(Level.WARNING, "After GC mem percent used: {0}", memPercentAfterGC);
					if (memPercentAfterGC < 0 || memPercentAfterGC > memLimit) {
						logger.warning("Flushing cache to save JVM.");
						try {
							cache.flush();
						} catch (CacheException ex) {
							logger.log(Level.SEVERE, "Error: " + ex, ex);
						}
						System.gc();
						CacheCounter.gcCalls++;
					}
					memPercentAfterGC = MemoryWarningSystem.getMemUsedPercentage();
					logger.log(Level.FINE, "Done. Mem percent used: {0}", memPercentAfterGC);
				}
			});			
		}
		
		if(cfg.isSaveCacheToDiskBwRestarts()) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					cache.saveToDisk();
				}
			});
			cache.readFromDisk();
		}
		/*
		HealthCheck.Result result = cacheHealthCheck.execute();
        if(result!=HealthCheck.Result.healthy()) {
            System.err.println("\nIs cache up? JVM will exit. Health Check Result: "+result.toString());
			System.exit(-1);
            return;
        }
				*/
    }
	
}
