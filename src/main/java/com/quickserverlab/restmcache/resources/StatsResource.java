package com.quickserverlab.restmcache.resources;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.quickserverlab.restmcache.RestMCache;
import com.quickserverlab.restmcache.RestMCacheCfg;
import com.quickserverlab.restmcache.core.CacheCounter;
import com.quickserverlab.restmcache.core.CacheInterface;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author akshath
 */
@Path("/stats")
@Metered
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {
	private static final SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private CacheInterface cache;
	
	public StatsResource(RestMCacheCfg cfg, CacheInterface cache) {
		this.cache = cache;
    }
	
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}
	
	public static String getPID() {
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		int i = pid.indexOf("@");
		pid = pid.substring(0, i);
		return pid;
	}
	

	@Timed
    @GET
    public Map<String,String> stats() {		
		Map<String,String> stats = new LinkedHashMap<>(30);

		stats.put("pid", getPID());

		//uptime
		long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		stats.put("uptime", "" + uptimeSec);

		//time - current UNIX time according to the server 
		long timeMili = System.currentTimeMillis();
		stats.put("time", "" + (timeMili / 1000));
		//stats.put("current_time_millis", "" + timeMili);

		stats.put("datetime", sdfDateTime.format(new Date(timeMili)));

		//version
		stats.put("version", RestMCache.version);

		//curr_connections
		//stats.put("curr_connections", "" + server.getClientCount());

		//total_connections
		//stats.put("total_connections", "" + totalConnections);

		//bytes_read    Total number of bytes read by this server from network
		//stats.put("bytes_read", "" + bytesRead);

		//bytes_written     Total number of bytes sent by this server to network
		//stats.put("bytes_written", "" + bytesWritten);

		//bytes - Current number of bytes used by this server to store items
		long usedMemory = Runtime.getRuntime().totalMemory()
			- Runtime.getRuntime().freeMemory();
		stats.put("bytes", "" + usedMemory);

		//limit_maxbytes    Number of bytes this server is allowed to use for storage.
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		stats.put("limit_maxbytes", "" + heapMaxSize);

		long mem_percent_used = (long) (100.0 * usedMemory / heapMaxSize);
		stats.put("mem_percent_used", "" + mem_percent_used);

		//threads           Number of worker threads requested.
		//stats.put("threads", );

		cache.saveStats(stats);

		stats.put("incr_misses", "" + CacheCounter.incrMisses);
		stats.put("incr_hits", "" + CacheCounter.incrHits);
		stats.put("decr_misses", "" + CacheCounter.decrMisses);
		stats.put("decr_hits", "" + CacheCounter.decrHits);
		stats.put("cas_misses", "" + CacheCounter.casMisses);
		stats.put("cas_hits", "" + CacheCounter.casHits);
		stats.put("cas_badval", "" + CacheCounter.casBadval);
	
		stats.put("app_impl_used", cache.getName());

		stats.put("gc_calls", "" + CacheCounter.gcCalls);
				
		return stats;
	}
}
