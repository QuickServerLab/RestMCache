package com.quickserverlab.restmcache.client;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

/**
 *
 * @author akshath
 */
public class RestMCacheClient {
	private static final Logger logger =  Logger.getLogger(RestMCacheClient.class.getName());
	
	private String url = "http://localhost:9200";	
	
	private OkHttpClient client;
	private Gson gson; 
	
	private String versionEndpoint;
	private String statsEndpoint;
	private String flushAllEndpoint;
	
	public RestMCacheClient() {
		
	}
	
	public void init() {
		client = new OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();
		
		gson = new Gson(); 

		versionEndpoint = url + "/version";
		statsEndpoint = url + "/stats";
		flushAllEndpoint = url + "/flush/all";
	}
	
	public boolean isAlive() {
		try {
			if(getVersion()!=null && getVersion().isEmpty()==false) {
				return true;
			}
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error: "+e, e);
		}
		return false;
	}
	
	public String getVersion() throws IOException {
		Request request = new Request.Builder()
			.url(versionEndpoint)
			.get()
			.addHeader("cache-control", "no-cache")
			.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}

		return response.body().string();
	}
	
	public Map<String,String> getStats() throws IOException {
		Request request = new Request.Builder()
			.url(statsEndpoint)
			.get()
			.addHeader("cache-control", "no-cache")
			.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}
		
		String responseBody = response.body().string();
		
		Type type = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> myMap = gson.fromJson(responseBody, type);

		return myMap;
	}
	
	public boolean flushAll() throws IOException {
		return flushAll(0);
	}
	
	public boolean flushAll(int sleepTimeSec) throws IOException {
		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType, "a=b");

		Request.Builder requestB = new Request.Builder()
			.url(flushAllEndpoint)
			.post(body)
			.addHeader("content-type", "application/x-www-form-urlencoded")
			.addHeader("cache-control", "no-cache");
		
		if(sleepTimeSec>0) {
			requestB.addHeader("X-MCache-ExpTime", ""+sleepTimeSec);
		}
		
		Request request = requestB.build();
		
		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}

		return "OK".equals(response.body().string());
	}
	
	private boolean store(String key, String flags, String cmd, int expTimeSec,
			String casUnique, byte[] payload, String mediaTypeString) throws IOException {
		MediaType mediaType;		
		if(mediaTypeString==null) {
			mediaTypeString = "application/octet-stream";
		}
		mediaType = MediaType.parse(mediaTypeString);
		
		if(cmd==null) {
			cmd = "set";
		}
		
		RequestBody body = RequestBody.create(mediaType, payload);

		Request.Builder requestB = new Request.Builder()
			.url(url+"/cache/"+key+"?cmd"+cmd)
			.post(body)
			.addHeader("content-type", mediaTypeString)
			.addHeader("cache-control", "no-cache");
		
		if(expTimeSec>0) {
			requestB.addHeader("X-MCache-ExpTime", ""+expTimeSec);
		}
		if(casUnique!=null) {
			requestB.addHeader("X-MCache-Cas-Unique", casUnique);
		}
		if(flags!=null) {
			requestB.addHeader("X-MCache-Flags", flags);
		}
		
		Request request = requestB.build();
		
		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}

		return "STORED".equals(response.body().string());
	}
	
	private Response access(String key, String cmd, int expTimeSec, int byValue) throws IOException {
		if(cmd==null) {
			cmd = "get";
		}

		Request.Builder requestB = new Request.Builder()
			.url(url+"/cache/"+key+"?cmd"+cmd)
			.get()
			.addHeader("cache-control", "no-cache");
		
		if(expTimeSec>0) {
			requestB.addHeader("X-MCache-ExpTime", ""+expTimeSec);
		}
		if(byValue > 0) {
			requestB.addHeader("X-MCache-By-Value", ""+byValue);
		}
		
		Request request = requestB.build();
		
		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Unexpected code " + response);
		}

		return response;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
