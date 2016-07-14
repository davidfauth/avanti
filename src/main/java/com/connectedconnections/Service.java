package com.connectedconnections;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cypher.internal.frontend.v3_0.ast.functions.Rels;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;



@Path("/service")
public class Service {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-M-d-H-m");

    @Context
    private final GraphDatabaseService database;

    public Service( @Context GraphDatabaseService database )
    {
        this.database = database;
    }
    
    public enum RelTypes implements RelationshipType
    {
    	ASSERTS_ID_MAP
}
    
    public enum Labels implements Label {
        Issue,
		PerspectiveEntity
    }
    
    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }
 
    
    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        try ( Transaction tx = db.beginTx()) {
            for ( Node n : db.getAllNodes()) {
                n.getPropertyKeys();
                for ( Relationship relationship : n.getRelationships()) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }
        }
        return "Warmed up and ready to go!";
    }
    
    @GET
    @Path("/findCohorts")
    @Produces({"application/json"})
    
    public Response findCliques(
            @Context final GraphDatabaseService db) throws IOException {
  	  
  	  final HashMap<Integer, Object> targetNodes = new HashMap<>();
  	  final HashMap<Integer, Object> removedNodes = new HashMap<>();
  	  final HashMap<Integer, Integer> countOfCliques = new HashMap<>();
 	  
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException
            {
          	  int uniqueNodes = 0;
          	  int cliqueID = 0;
          	  Node startFeed = null;
          	  try ( Transaction tx = db.beginTx() )
                {
          		  
                	for ( Node n : db.getAllNodes() ) {
                		 if (n.hasLabel(Labels.PerspectiveEntity)){
                			 targetNodes.put((int)n.getId(),(int)n.getId());
                		 }
                  	}

          		TraversalDescription friendsTraversal = db.traversalDescription()
          		        .depthFirst()
          		        .relationships(RelTypes.ASSERTS_ID_MAP)
          		        .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
          		  
          		for (Integer key : targetNodes.keySet()) {
          		
          		  startFeed = findNodeById(key);
          		  if (!removedNodes.containsKey(key)){
          		
        			cliqueID++;
         		  
          			for ( Node currentNodes : friendsTraversal
          					.traverse( startFeed )
          					.nodes() )
          			{
          				if (currentNodes.hasLabel(Labels.PerspectiveEntity)){
//          					output += currentNodes.getId() + "\n";
          					currentNodes.setProperty("cliqueID", cliqueID);
          					removedNodes.put((int)currentNodes.getId(),(int)currentNodes.getId());
          				}
          			}
          		  }
          		}


//          			System.out.println(output);
          		  // loop over nextLevelNodes
          		  
          	  tx.success();
                }
          	  
          	  try ( Transaction tx = db.beginTx() )
              {
          		targetNodes.clear();
          		for ( Node n : db.getAllNodes() ) {
           		 if (n.hasLabel(Labels.PerspectiveEntity)){
           			 targetNodes.put((int)n.getId(),(int)n.getId());
           		 }
             	} 
          		
          		for (Integer key : targetNodes.keySet()) {
          			Node issueNode = findNodeById(key);
          			cliqueID = (int) issueNode.getProperty("cliqueID");
          			if (!countOfCliques.containsKey(cliqueID)){
          				countOfCliques.put(cliqueID, 1);
          			}else{
          				countOfCliques.put(cliqueID, countOfCliques.get(cliqueID)+1);
          			}
          		}
          		for (Integer key : targetNodes.keySet()) {
          			Node issueNode = findNodeById(key);
          			issueNode.setProperty("numberInClique", countOfCliques.get(issueNode.getProperty("cliqueID")));
          		}
          		tx.success();
              }
          	  
          	  
          	  
          	  // Need to build a hashmap of nodes
          	  // Need to build a hashmap of relationships
          	  // Need to build out the hashmap of systems
          	  // Need to build out the controls
          	  
          	  // Write out JSON
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator( os, JsonEncoding.UTF8 );
                jg.writeStartObject();
                jg.writeArrayFieldStart("nodes");
                
          	  jg.writeEndArray();
                
                jg.writeArrayFieldStart("links");
               
                 jg.writeEndArray();

                
                jg.writeEndObject();
                jg.flush();
                jg.close();
           }
        };
        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
    }
    
    
    
 
     
        private Node findServiceNode(int id) {
            final Node node = database.findNode(Labels.Issue, "key", id);
            if (node == null) {
                return null;
            } else {
                return node;
            }
        }
        
        private Node findNodeById(int id) {
//          final Node node = database.findNode(Labels.PDE, "id", id);
          final Node node = database.getNodeById(id);
          
          if (node == null) {
              return null;
          } else {
              return node;
          }
      }

    @XmlRootElement
    public class MyJaxBean {
        @XmlElement public String param1;
    }
    
    class ValueComparator implements Comparator<String> {

        Map<String, Double> base;
        public ValueComparator(Map<String, Double> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.    
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }
    

    
}