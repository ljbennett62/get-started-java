/*******************************************************************************
 * Copyright (c) 2017 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/ 
package wasdev.sample.store;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.JsonObject;

import redis.clients.jedis.JedisPool;

public class JedisPoolFactory {

    private static JedisPool pool;
    static {
        try {
            JedisPool jp = new JedisPool(getRedisURI());
            pool = jp;
        } catch (Exception e) {
            pool = null;
        }
    }
    public static JedisPool getInstance() {
    	  return pool;
    	}
    
    private static URI getRedisURI() {
    	  String url;
    	  URI uri;

    	  if (System.getenv("VCAP_SERVICES") != null) {
    	    // When running in Bluemix, the VCAP_SERVICES env var will have the credentials for all bound/connected services
    	    // Parse the VCAP JSON structure looking for redis.
    	    JsonObject redisCredentials = VCAPHelper.getCloudCredentials("redis");
    	    if(redisCredentials == null){
    	      System.out.println("No redis cache service bound to this application");
    	      return null;
    	    }
    	    url = redisCredentials.get("uri").getAsString();
    	  } else if (System.getenv("REDISURI") != null) {
    		 System.out.println("Using credentials in REDISURI");
             url = System.getenv("REDISURI");
             url = url.replace("\n", "").replace("\r", "");
          } else {
    	    System.out.println("Running locally. Looking for credentials in redis.properties");
    	    url = VCAPHelper.getLocalProperties("redis.properties").getProperty("redis_url");
    	    if(url == null || url.length()==0){
    	      System.out.println("To use a database, set the Redis url in src/main/resources/redis.properties");
    	      return null;
    	    }
    	  }
    	  try {
    	    uri = new URI(url);
    	      return uri;
    	  } catch (URISyntaxException e) {
    	    // TODO Auto-generated catch block
    	    e.printStackTrace();
    	    return null;
    	  }
     }

}
