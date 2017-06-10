package com.quickserverlab.restmcache.core.impl;

import com.quickserverlab.restmcache.core.CacheCounter;
import com.quickserverlab.restmcache.core.CacheException;
import com.quickserverlab.restmcache.core.CacheInterface;
import com.quickserverlab.restmcache.core.DataCarrier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BaseCacheImpl implementation
 * @author akshath
 */
public abstract class BaseCacheImpl implements CacheInterface {
	private static final Logger logger = Logger.getLogger(BaseCacheImpl.class.getName());	
	
	public abstract String getName();
	
	public abstract long getSize();
	
	public abstract void setToCache(String key, Object value, int objectSize, int expInSec) throws Exception;
	public abstract void updateToCache(String key, Object value, int objectSize, int expInSec) throws Exception;
	public abstract void updateToCache(String key, Object value, int objectSize) throws Exception;
	
	public abstract Object getFromCache(String key) throws Exception;
	public abstract boolean deleteFromCache(String key) throws Exception;
	public abstract void flushCache() throws Exception;

	private long totalItems;
	private long cmdGets;
	private long cmdSets;
	private long cmdDeletes;
	private long cmdFlushs;
	private long cmdTouchs;
	private long getHits;
	private long getMisses;
	private long deleteMisses;
	private long deleteHits;	
	private long touchMisses;
	private long touchHits;	
	
	private int port;
	
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public void saveStats(Map stats) {
		if(stats==null) stats = new LinkedHashMap();

		//curr_items - Current number of items stored by the server
		stats.put("curr_items", "" + getSize());

		//total_items - Total number of items stored by this server ever since it started
		stats.put("total_items", "" + totalItems);

		//cmd_get           Cumulative number of retrieval reqs
		stats.put("cmd_get", "" + cmdGets);

		//cmd_set           Cumulative number of storage reqs
		stats.put("cmd_set", "" + cmdSets);

		//cmd_delete
		stats.put("cmd_delete", "" + cmdDeletes);

		//cmd_touch
		stats.put("cmd_touch", "" + cmdTouchs);
		
		//cmd_flush
		stats.put("cmd_flush", "" + cmdFlushs);

		//get_hits          Number of keys that have been requested and found present
		stats.put("get_hits", "" + getHits);
						  
		//get_misses        Number of items that have been requested and not found
		stats.put("get_misses", "" + getMisses);
		
		//delete_misses     Number of deletions reqs for missing keys
		stats.put("delete_misses", "" + deleteMisses);
		
		//delete_hits       Number of deletion reqs resulting in
		stats.put("delete_hits", "" + deleteHits);
		
		//touch_hits	Numer of keys that have been touched with a new expiration time 
		stats.put("touch_hits", "" + touchHits);
		
		//touch_misses	Numer of items that have been touched and not found 
		stats.put("touch_misses", "" + touchMisses);
		
	}

	public void set(String key, Object value, int objectSize, int expInSec) throws CacheException {
		logger.log(Level.FINEST, "set key: {0}; objectsize: {1};", 
				new Object[]{key, objectSize});
		
		cmdSets++;
		try {
			setToCache(key, value, objectSize, expInSec);
			
			totalItems++;
		
			
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}		
	}
	
	
	public boolean touch(String key, int expInSec) throws CacheException {
		return touch(key, expInSec, true);
	}
	
	public boolean touch(String key, int expInSec, boolean incrementCount) throws CacheException {
		logger.log(Level.FINEST, "touch key: {0}", key);
		if(incrementCount) {
			cmdTouchs++;
		}
		
		DataCarrier dc = (DataCarrier) get_(key);
		if (dc == null) {
			if(incrementCount) {
				touchMisses++;
			}
			return false;
		} else {		
			set(key, dc, dc.getSize(), expInSec);
			if(incrementCount) {
				touchHits++;
			}
			return true;
		}
	}
	
	public void update(String key, Object value, int objectSize) throws CacheException {
		logger.log(Level.FINEST, "update key: {0}; objectsize: {1};", 
				new Object[]{key, objectSize});
		try {
			updateToCache(key, value, objectSize);
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}
	}
	
	public void update(String key, Object value, int objectSize, int expInSec) throws CacheException {
		logger.log(Level.FINEST, "update key: {0}; objectsize: {1};", 
				new Object[]{key, objectSize});
		try {
			updateToCache(key, value, objectSize, expInSec);
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}
	}
	
	public Object get(String key) throws CacheException {
		return get(key, true);
	}

	public Object get(String key, boolean incrementCount) throws CacheException {
		logger.log(Level.FINEST, "get key: {0}", key);
		if(incrementCount) {
			cmdGets++;
		}
		
		Object obj = get_(key);
		
		if(incrementCount) {
			if(obj!=null) {
				getHits++;
			} else {
				logger.log(Level.FINEST, "no value for key: {0}", key);
				getMisses++;
			}
		}
		return obj;
	}
	
	private Object get_(String key) throws CacheException {
		Object obj = null;
		try {
			obj = getFromCache(key);			
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}
		return obj;
	}

	public boolean delete(String key) throws CacheException {
		logger.log(Level.FINEST, "delete key: {0};", key);
		cmdDeletes++;
		
		boolean flag = false;
		try {
			flag = deleteFromCache(key);
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}
		if(flag) {
			deleteHits++;
		} else {
			deleteMisses++;
		}
		return flag;
	}	

	public void flush() throws CacheException {
		logger.log(Level.FINEST, "flush");
		try {
			flushCache();
			cmdFlushs++;
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
			throw new CacheException(ex.toString());
		}		
	}
}
