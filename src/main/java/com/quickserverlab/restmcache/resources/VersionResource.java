package com.quickserverlab.restmcache.resources;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.quickserverlab.restmcache.RestMCache;
import com.quickserverlab.restmcache.RestMCacheCfg;
import com.quickserverlab.restmcache.core.CacheInterface;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author akshath
 */
@Path("/version")
@Produces(MediaType.TEXT_PLAIN)
@Metered
public class VersionResource {
	private CacheInterface cache;
	
	public VersionResource(RestMCacheCfg cfg, CacheInterface cache) {
		this.cache = cache;
    }
	
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}
	
	@Timed
    @GET
    public String version() {		
		return RestMCache.version;
	}
}
