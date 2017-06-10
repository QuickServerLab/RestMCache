package com.quickserverlab.restmcache.resources;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.quickserverlab.restmcache.RestMCacheCfg;
import com.quickserverlab.restmcache.core.CacheCounter;
import com.quickserverlab.restmcache.core.CacheException;
import com.quickserverlab.restmcache.core.CacheInterface;
import com.quickserverlab.restmcache.core.DataCarrier;
import com.quickserverlab.restmcache.util.HexUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author akshath
 */
@Path("/cache/{key}")
@Produces(MediaType.TEXT_PLAIN)
@Metered
public class CacheResource {
	private static final Logger logger = Logger.getLogger(CacheResource.class.getName());
	
	private CacheInterface cache;
	
	public CacheResource(RestMCacheCfg cfg, CacheInterface cache) {
		this.cache = cache;
    }
	
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}
	
	/*
	get		=>  GET /cache/key
	gat		=>  GET /cache/key?cmd=gat
	
	incr	=>  GET /cache/key?cmd=incr
	decr	=>  GET /cache/key?cmd=decr
	touch	=>  GET /cache/key?cmd=touch
	*/
	@Timed
    @GET
    public Response access(@PathParam("key") String key, @QueryParam("cmd") String cmd, 
			@HeaderParam("X-MCache-ExpTime") String expTime,
			@HeaderParam("X-MCache-By-Value") String byValue) throws InterruptedException {		
		try {
			boolean incGetCount = false;
			if(cmd==null || "get".equals(cmd)) {
				incGetCount = true;
			}
			
			int expInSec = 0;
			if("gat".equals(cmd) || "touch".equals(cmd)) {
				if(expTime!=null && expTime.isEmpty()==false) {
					try {
						expInSec = Integer.parseInt(expTime);
					} catch (NumberFormatException ex) {
						logger.log(Level.SEVERE, null, ex);
						
						throw ResourceUtil.makeError("CLIENT_ERROR bad X-MCache-ExpTime passed "+ex.getMessage(), 
								Response.Status.BAD_REQUEST);
						
					}
				} else {
					throw ResourceUtil.makeError("CLIENT_ERROR No X-MCache-ExpTime passed", Response.Status.BAD_REQUEST);
				}
			}
			
			if("touch".equals(cmd)) {
				boolean flag = cache.touch(key, expInSec);
				
				if(flag==false) {
					throw ResourceUtil.makeError("NOT FOUND", Response.Status.NOT_FOUND);	
				} else {
					return Response.ok("TOUCHED").type(MediaType.TEXT_PLAIN).build();	
				}
			}
			
			
			DataCarrier dc = (DataCarrier) cache.get(key, incGetCount);
			if (dc != null) {
				if("gat".equals(cmd)) {					
					cache.touch(key, expInSec);
					
					CacheCounter.gatHits++;
				}
				
				if("incr".equals(cmd) || "decr".equals(cmd)) {
					long value = 1;
					if(byValue!=null && byValue.isEmpty()==false) {
						try {
							value = Integer.parseInt(byValue);
						} catch (NumberFormatException ex) {
							logger.log(Level.SEVERE, null, ex);
							throw ResourceUtil.makeError("CLIENT_ERROR bad X-MCache-By-Value passed "+ex.getMessage(), Response.Status.BAD_REQUEST);
						}
					} else {
						throw ResourceUtil.makeError("CLIENT_ERROR No X-MCache-By-Value passed", Response.Status.BAD_REQUEST);
					}
					
					dc.writeLock.lock();
					try {			
						long oldvalue = Long.parseLong(new String(dc.getData(), 
								HexUtil.getCharset()));
						if (cmd.equals("incr")) {
							value = oldvalue + value;
						} else if (cmd.equals("decr")) {
							value = oldvalue - value;
							if (value < 0) {
								value = 0;
							}
						}

						dc.setData(new StringBuilder().append(value).toString().getBytes(HexUtil.getCharset()));
						cache.update(key, dc, dc.getSize());
					} catch(Exception e) {
						if (cmd.equals("incr")) {
							CacheCounter.incrMisses++;
						} else if (cmd.equals("decr")) {
							CacheCounter.decrMisses++;
						}
						
						throw ResourceUtil.makeError("SERVER_ERROR "+e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
					} finally {
						dc.writeLock.unlock();
					}
					
					if (cmd.equals("incr")) {
						CacheCounter.incrHits++;
					} else if (cmd.equals("decr")) {
						CacheCounter.decrHits++;
					}
				}
				
				String contentType = MediaType.APPLICATION_OCTET_STREAM; //default				
				if(dc.getContentType()!=null && dc.getContentType().isEmpty()==false) {
					contentType = dc.getContentType();
				}
				
				Response.ResponseBuilder responseBuilder = Response.ok(dc.getData(), contentType);			
				responseBuilder.header("X-MCache-Key", key);
				responseBuilder.header("X-MCache-Flags", dc.getFlags());
				responseBuilder.header("X-MCache-Cas-Unique", dc.getCas());

				return responseBuilder.build();
			} else {
				if(cmd!=null) {
					if (cmd.equals("incr")) {
						CacheCounter.incrMisses++;
					} else if (cmd.equals("decr")) {
						CacheCounter.decrMisses++;
					} else if (cmd.equals("gat")) {
						CacheCounter.gatMisses++;
					} 
				}
				
				throw ResourceUtil.makeError("NOT FOUND", Response.Status.NOT_FOUND);
			}
		} catch (CacheException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw ResourceUtil.makeError("SERVER_ERROR "+ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}			
    }
	
	/*
	delete	=>  DELETE /cache/key
	*/
	@Timed
    @DELETE
    public Response delete(@PathParam("key") String key) throws InterruptedException {		
		try {
			boolean flag = cache.delete(key);
			if (flag) {
				throw ResourceUtil.makeError("DELETED", Response.Status.OK);
			} else {
				throw ResourceUtil.makeError("NOT FOUND", Response.Status.NOT_FOUND);
			}
		} catch (CacheException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw ResourceUtil.makeError("SERVER_ERROR "+ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
    }
	
	/*
	set		=> PUT/POST /cache/key
	add		=> POST /cache/key?cmd=add
	replace	=> POST /cache/key?cmd=replace
	append	=> POST /cache/key?cmd=append
	prepend	=> POST /cache/key?cmd=prepend
	cas		=> POST /cache/key?cmd=cas
	*/
	@Timed
    @PUT
    public Response set(@PathParam("key") String key, @QueryParam("cmd") String cmd,
			@HeaderParam("X-MCache-Flags") String flags, @HeaderParam("X-MCache-ExpTime") String expTime, 
			@HeaderParam("X-MCache-Cas-Unique") String casUnique, @HeaderParam("Content-Type") String contentType, 
			byte[] payload) throws InterruptedException {
        return store(key, "set", flags, expTime, casUnique, contentType, payload);
    }
	
	@Metered
    @POST
    public Response store(@PathParam("key") String key, @QueryParam("cmd") String cmd, 
			@HeaderParam("X-MCache-Flags") String flags, @HeaderParam("X-MCache-ExpTime") String expTime, 
			@HeaderParam("X-MCache-Cas-Unique") String casUnique,  @HeaderParam("Content-Type") String contentType, 
			byte[] payload) throws InterruptedException {
		if(cmd==null) {
			cmd = "set";
		}
		
		DataCarrier dc = new DataCarrier(payload);
		dc.setFlags(flags);
		dc.setContentType(contentType);
		
		int expInSec = 0;
		if(expTime!=null && expTime.isEmpty()==false) {
			try {
				expInSec = Integer.parseInt(expTime);
			} catch (NumberFormatException ex) {
				logger.log(Level.SEVERE, null, ex);
				throw ResourceUtil.makeError("CLIENT_ERROR bad ExpTime passed "+ex.getMessage(), Response.Status.BAD_REQUEST);
			}
		}
		
		try {
			if("set".equals(cmd)) {
				DataCarrier olddata = (DataCarrier) cache.get(key, false);
				if(olddata==null) {
					cache.set(key, dc, dc.getSize(), expInSec);
				} else {
					olddata.writeLock.lock();
					try {
						olddata.setData(dc.getData());
						olddata.setFlags(dc.getFlags());

						cache.update(key, olddata, olddata.getSize(), expInSec);
					} finally {
						olddata.writeLock.unlock();
					}
				}
				return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();				
			} else if ("add".equals(cmd)) {
				Object olddata = cache.get(key, false);
				if (olddata == null) {
					cache.set(key, dc, dc.getSize(), expInSec);
					return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();					
				} else {
					return Response.ok("NOT_STORED").type(MediaType.TEXT_PLAIN).build();					
				}
			} else if ("replace".equals(cmd)) {
				DataCarrier olddata = (DataCarrier) cache.get(key, false);
				if (olddata != null) {
					olddata.writeLock.lock();
					try {
						olddata.setData(dc.getData());
						cache.update(key, olddata, olddata.getSize());
					} finally {
						olddata.writeLock.unlock();
					}

					dc.setData(null);
					dc = null;

					return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();
				} else {
					return Response.ok("NOT_STORED").type(MediaType.TEXT_PLAIN).build();					
				}
			} else if ("append".equals(cmd)) {
				DataCarrier olddata = (DataCarrier) cache.get(key, false);
				if (olddata != null) {
					olddata.writeLock.lock();
					try {
						olddata.append(dc.getData());
						cache.update(key, olddata, olddata.getSize());
					} finally {
						olddata.writeLock.unlock();
					}

					dc.setData(null);
					dc = null;

					return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();					
				} else {
					return Response.ok("NOT_STORED").type(MediaType.TEXT_PLAIN).build();					
				}
			} else if ("prepend".equals(cmd)) {
				DataCarrier olddata = (DataCarrier) cache.get(key, false);
				if (olddata != null) {
					olddata.writeLock.lock();
					try {
						olddata.prepend(dc.getData());
						cache.update(key, olddata, olddata.getSize());
					} finally {
						olddata.writeLock.unlock();
					}

					dc.setData(null);
					dc = null;

					return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();					
				} else {
					return Response.ok("NOT_STORED").type(MediaType.TEXT_PLAIN).build();					
				}
			} else if ("cas".equals(cmd)) {
				if(casUnique==null || casUnique.trim().isEmpty()) {
					throw ResourceUtil.makeError("CLIENT_ERROR bad X-MCache-Cas-Unique passed ", Response.Status.BAD_REQUEST);
				}
				DataCarrier olddata = (DataCarrier) cache.get(key, false);			
				if(olddata != null) {
					olddata.writeLock.lock();
					try {
						int oldcas = olddata.getCas();
						int passedcas = Integer.parseInt(casUnique);

						if (oldcas == passedcas) {
							olddata.setData(dc.getData());
							cache.update(key, olddata, olddata.getSize());

							dc.setData(null);
							dc = null;

							CacheCounter.casHits++;
							return Response.ok("STORED").type(MediaType.TEXT_PLAIN).build();							
						} else {
							CacheCounter.casBadval++;
							return Response.ok("EXISTS").type(MediaType.TEXT_PLAIN).build();
						}
					} finally {
						olddata.writeLock.unlock();
					}
				} else {
					CacheCounter.casMisses++;
					
					return Response.ok("NOT_FOUND").type(MediaType.TEXT_PLAIN).build();
				}
			} else {
				return Response.ok("ERROR").type(MediaType.TEXT_PLAIN).build();
			}
			
		} catch (CacheException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw ResourceUtil.makeError("SERVER_ERROR "+ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
    }
}
