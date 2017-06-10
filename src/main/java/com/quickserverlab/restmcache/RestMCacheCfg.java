package com.quickserverlab.restmcache;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author akshath
 */
public class RestMCacheCfg extends Configuration {
    @JsonProperty private @NotEmpty String cacheImplClass;
	@JsonProperty private @NotEmpty String charSet;	
	@JsonProperty private @Min(0) @Max(99) int flushOnLowMemoryPercent;
	@JsonProperty private boolean saveCacheToDiskBwRestarts;

	public String getCacheImplClass() {
		return cacheImplClass;
	}
	public void setCacheImplClass(String cacheImplClass) {
		this.cacheImplClass = cacheImplClass;
	}

	public String getCharSet() {
		return charSet;
	}
	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}	

	public int getFlushOnLowMemoryPercent() {
		return flushOnLowMemoryPercent;
	}
	public void setFlushOnLowMemoryPercent(int flushOnLowMemoryPercent) {
		this.flushOnLowMemoryPercent = flushOnLowMemoryPercent;
	}

	public boolean isSaveCacheToDiskBwRestarts() {
		return saveCacheToDiskBwRestarts;
	}

	public void setSaveCacheToDiskBwRestarts(boolean saveCacheToDiskBwRestarts) {
		this.saveCacheToDiskBwRestarts = saveCacheToDiskBwRestarts;
	}
}
