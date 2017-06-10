package com.quickserverlab.restmcache.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Length;

/**
 *
 * @author ashetty
 */
public class Saying {
    private long id;
    private @Length(max = 10) String content;

    public Saying(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public Saying() {} // required for deserialization

    @JsonProperty public long getId() { return id; }
    @JsonProperty public String getContent() { return content; }
}
