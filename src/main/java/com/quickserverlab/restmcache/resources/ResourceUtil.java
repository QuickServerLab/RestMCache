package com.quickserverlab.restmcache.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author akshath
 */
public class ResourceUtil {
	public static WebApplicationException makeError(String msg, Response.Status status) {
		Response response = Response.status(status).entity(msg).type(MediaType.TEXT_PLAIN).build();
		return new WebApplicationException(response);
	}
}
