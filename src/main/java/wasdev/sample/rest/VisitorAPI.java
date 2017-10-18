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
package wasdev.sample.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import wasdev.sample.Visitor;
import wasdev.sample.store.VCAPHelper;
import wasdev.sample.store.VisitorStore;
import wasdev.sample.store.VisitorStoreFactory;

@ApplicationPath("api")
@Path("/visitors")
public class VisitorAPI extends Application {
	
	//Our database store
	VisitorStore store = VisitorStoreFactory.getInstance();
	
	// add a redis connection pool
	JedisPool pool = new JedisPool(new JedisPoolConfig(), getRedisURI());
	
	private URI getRedisURI() {
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
		} else {
			// System.out.println("Running locally. Looking for credentials in redis.properties");
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

  /**
   * Gets all Visitors.
   * REST API example:
   * <code>
   * GET http://localhost:9080/GetStartedJava/api/visitors
   * </code>
   * 
   * Response:
   * <code>
   * [ "Bob", "Jane" ]
   * </code>
   * @return A collection of all the Visitors
   */
    @GET
    @Path("/")
    @Produces({"application/json"})
    public String getVisitors() {
		
		if (store == null) {
			return "[]";
		}
		
		List<String> names = new ArrayList<String>();
		for (Visitor doc : store.getAll()) {
			String name = doc.getName();
			if (name != null){
				names.add(name);
			}
		}
		return new Gson().toJson(names);
    }
    
    
    /**
     * Creates a new Visitor.
     * 
     * REST API example:
     * <code>
     * POST http://localhost:9080/GetStartedJava/api/visitors
     * <code>
     * POST Body:
     * <code>
     * {
     *   "name":"Bob"
     * }
     * </code>
     * Response:
     * <code>
     * {
     *   "id":"123",
     *   "name":"Bob"
     * }
     * </code>
     * @param visitor The new Visitor to create.
     * @return The Visitor after it has been stored.  This will include a unique ID for the Visitor.
     */
    @POST
    @Produces("application/text")
    @Consumes("application/json")
    public String newToDo(Visitor visitor) {
      if(store == null) {
    	  return String.format("Hello %s!", visitor.getName());
      }
      try (Jedis jedis = pool.getResource()) {
    	  /// check to see if this user is already in the cache
    	  String foo = jedis.get(visitor.getName());
    	  if ( foo == null) {
    	      store.persist(visitor);
    	      jedis.set(visitor.getName(),"persisted");
    	      return String.format("Hello %s! I've added you to the database.", visitor.getName());
    	  }
      }
      return String.format("Hello %s! It's nice to see you again.", visitor.getName());
    } 
}