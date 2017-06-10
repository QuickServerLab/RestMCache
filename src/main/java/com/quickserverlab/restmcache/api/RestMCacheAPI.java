package com.quickserverlab.restmcache.api;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author akshath
 */
public interface RestMCacheAPI {
	@GET @Path("/version")
	@Produces(MediaType.TEXT_PLAIN)
    public String version();
	
	@GET @Path("/stats")
    Map<String,String> stats();
}
