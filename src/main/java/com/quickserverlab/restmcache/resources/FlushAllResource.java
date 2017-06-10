package com.quickserverlab.restmcache.resources;

import com.codahale.metrics.annotation.Metered;
import com.quickserverlab.restmcache.RestMCacheCfg;
import com.quickserverlab.restmcache.core.CacheException;
import com.quickserverlab.restmcache.core.CacheInterface;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author akshath
 */
@Path("/flush/all")
@Produces(MediaType.TEXT_PLAIN)
@Metered
public class FlushAllResource {
	private static final Logger logger = Logger.getLogger(FlushAllResource.class.getName());
	
	private CacheInterface cache;
	
	public FlushAllResource(RestMCacheCfg cfg, CacheInterface cache) {
		this.cache = cache;
    }
	
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}
	
	@Metered
    @POST
    public Response flushall(@HeaderParam("X-MCache-ExpTime") String expTime) {
		if (expTime == null) {			
			try {
				cache.flush();
			} catch (CacheException ex) {
				logger.log(Level.SEVERE, "Error: "+ex, ex);
				throw ResourceUtil.makeError("SERVER_ERROR "+ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
			}		
		} else {
			final int sleeptime = Integer.parseInt(expTime);
			Thread t = new Thread() {
				public void run() {
					try {
						sleep(1000 * sleeptime);
					} catch (InterruptedException ex) {
						logger.log(Level.WARNING, "Error: "+ex, ex);
					}
					try {
						cache.flush();
					} catch (CacheException ex) {
						logger.log(Level.SEVERE, "Error: "+ex, ex);
						throw ResourceUtil.makeError("SERVER_ERROR "+ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
					}
				}
			};
			t.start();
		}	
		
		return Response.ok("OK").type(MediaType.TEXT_PLAIN).build();		
	}
}
