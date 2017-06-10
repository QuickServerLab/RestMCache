package com.quickserverlab.restmcache.core;

/**
 *
 * @author akshath
 */
public class CacheCounter {
	
	public volatile static long gcCalls;
	public volatile static long incrMisses;
	public volatile static long incrHits;
	public volatile static long decrMisses;
	public volatile static long decrHits;
	public volatile static long casMisses;
	public volatile static long casHits;
	public volatile static long casBadval;
	public volatile static long gatHits;
	public volatile static long gatMisses;

}
