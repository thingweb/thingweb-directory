package de.thingweb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.QueryExecUtils;

public class ThingDescriptionUtils {
	
  private static final URL TD_CONTEXT_URL = ClassLoader.getSystemResource("td-context.jsonld");

  public static String withLocalJsonLdContext(String data) {
	  // FIXME proper context substitution
	  return data.replace("http://w3c.github.io/wot/w3c-wot-td-context.jsonld", TD_CONTEXT_URL.toString());
  }

  public static List<String> listThingDescriptions(String query) {
	List<String> tds = new ArrayList<>();
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);

	try {
	  String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { " + query + " FILTER NOT EXISTS { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> } } }";
	  try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
		ResultSet result = qexec.execSelect();
		while (result.hasNext()) {
		  tds.add(result.next().get("g").asResource().getURI());
		}
	  }
	catch (Exception e) {
	  throw e;
	}
	} finally {
	  dataset.end();
	}

	return tds;
  }
  
  public static String streamToString(InputStream s) throws IOException {
	StringWriter w = new StringWriter();
	InputStreamReader r = new InputStreamReader(s, "UTF-8");
	char[] buf = new char [512];
	int len;
	
	while ((len = r.read(buf)) > 0) {
	  w.write(buf, 0, len);
	}
	s.close();
	
	return w.toString();
  }

  /**
   * Returns the ID of a thing description stored in the database given its URI.
   * @param uri URI of the thing description we want to return.
   * @return the ID of the thing description.
   */
  public static String getThingDescriptionIdFromUri(String uri) {
	
	String query = "?td <http://iot.linkeddata.es/def/wot#baseURI> <" + uri + ">";
	String id = "NOT FOUND";
	  
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);

	try {
	  String q = "SELECT ?g_id WHERE { GRAPH ?g_id { " + query + " }}";
	  QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
	  ResultSet result = qexec.execSelect();
	  while (result.hasNext()) { 
	    id = result.next().get("g_id").toString();
	  }
	} catch (Exception e) {
	  e.printStackTrace();
	  throw e;
	} finally {
	  dataset.end();
	}
	
	return id;
  }

  /**
   * Returns a list of the thing descriptions URIs.
   * @return a list of URIs stored in the database.
   */
  public static List<String> listThingDescriptionsUri() {
	
	List<String> tds = new ArrayList<>();
	String query = "?td <http://iot.linkeddata.es/def/wot#baseURI> ?uri";
	  
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);

	try {
	  String q = "SELECT ?uri WHERE { GRAPH ?g_id { " + query + " }}";
	  try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
		ResultSet result = qexec.execSelect();
		while (result.hasNext()) { 
		tds.add(result.next().get("uri").toString());
		}
	  }
	catch (Exception e) {
	  throw e;
	}
	} finally {
	  dataset.end();
	}
	
	return tds;
  }
  
  /**
   * Returns true if td's uris are already registered
   * in the database, false otherwise.
   * Unless td is in the dataset with tdId.
   * 
   * @return true or false.
   */
  public static boolean hasInvalidURI(String td, String tdId) {
	  
	  String uris_re = "(\"uris\")[ ]*:[ ]*[-a-zA-Z0-9+&@#/%? \"=~_|!:,.;\\[\\]]*,";
	  String url_re = "(coap?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	  
	  // Extract list of uris
	  Matcher m = Pattern.compile(uris_re).matcher(td);
	  String uri_ar = "";
	  while (m.find()) {
		  uri_ar += m.group();
	  }
	  
	  // Check each uri
	  Matcher m2 = Pattern.compile(url_re).matcher(uri_ar);
	  while (m2.find()) {
		  String thing_uri = m2.group();
		  String id = getThingDescriptionIdFromUri(thing_uri);
		  if (!id.equalsIgnoreCase("NOT FOUND") && !id.equalsIgnoreCase(tdId)) {
			  return true;
		  }
	  }

	  return false;
  }
  
  /**
   * Returns true if td's uris are already registered
   * in the database, false otherwise.
   * @return true or false.
   */
  public static boolean hasInvalidURI(String td) {
	  return hasInvalidURI(td, "NOT FOUND");
  }

  
  /**
   * Returns a list of type values for the given property.
   * @param propertyURI Complete URI of the property (baseUri + propertyName).
   * @return List of values for the given property.
   */
  public static List<String> listRDFTypeValues(String propertyURI) {
	  
	  List<String> vals = new ArrayList<>();
	  Dataset dataset = Repository.get().dataset;
	  String prefix = StrUtils.strjoinNL
			  ( "PREFIX td: <http://w3c.github.io/wot/w3c-wot-td-ontology.owl#>"
			  , "PREFIX qu: <http://purl.oclc.org/NET/ssnx/qu/qu#>"
			  , "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			  , "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
			  , "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
			  , "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
			  , "PREFIX dim: <http://purl.oclc.org/NET/ssnx/qu/dim#>"
			  , "PREFIX quantity: <http://purl.oclc.org/NET/ssnx/qu/quantity#>");
	  String query = prefix + "SELECT ?unit WHERE { "
			  + "GRAPH ?g { "
			  + " ?td td:hasProperty ?property . "
			  + "?property a ?propertytype . "
			  + "} "
			  + "?propertytype a ?class . "
			  + "?class rdfs:subClassOf ?s . "
			  + "?s owl:onProperty qu:unitKind ; "
			  + "owl:allValuesFrom ?unitKind . "
			  + "?unit rdf:type ?unitKind . "
			  + "FILTER (?property = <" + propertyURI + ">)"
			  + "}";
	  
	  dataset.begin(ReadWrite.READ);
	  try {
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			  ResultSet result = qexec.execSelect();
			  while (result.hasNext()) {
				  vals.add(result.next().get("unit").toString());
			  }
		  }
		  
	  } finally {
		  dataset.end();
	  }
	  
	  return vals;
  }
  
  /**
   * Loads an ontology to the triple store, in the
   * default graph.
   * @param fileName File name with the ontology context.
   */
  public static void loadOntology(InputStream fileName) {
	  
	  List<String> ont = new ArrayList<>();
	  
	  // Check if the ontology is already there
	  Dataset dataset = Repository.get().dataset;
	  dataset.begin(ReadWrite.READ);
	  try {
		  String prefix = StrUtils.strjoinNL
				  ( "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				  , "PREFIX owl: <http://www.w3.org/2002/07/owl#>");
		  String query = prefix + "SELECT ?s WHERE {?s rdf:type owl:Ontology}";
		  
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			  ResultSet result = qexec.execSelect();
			  while (result.hasNext()) {
				  ont.add(result.next().get("s").toString());
			  }
		  }
		  
	  } finally {
		  dataset.end();
	  }
	  
	  // Load QUDT ontology
	  if (ont.isEmpty()) {
		  dataset = Repository.get().dataset;
	      dataset.begin( ReadWrite.WRITE );
	      try {
	    	  Model m = dataset.getDefaultModel();
	    	  //RDFDataMgr.read(m, fileName);
	    	  RDFDataMgr.read(m, fileName, Lang.TURTLE);
	    	  dataset.commit();
	      } finally {
	    	  dataset.end();
	      }
	  }
  }


  // *********** FOR JENA-TEXT ************

  /**
  * Extracts the key words from the model's statements.
  * This includes the Literals and the localNames of Resources and Predicates.
  * 
  * @param model Model to extract key words
  * @return List of key word strings
  */
  public List<String> getModelKeyWords(Model model) {
	
	List<String> keyWords = new ArrayList<String>();
	StmtIterator statementIter = model.listStatements();
	Statement s;
	Property predicate;
	RDFNode object;
	
	while (statementIter.hasNext()) {
	  s = statementIter.nextStatement();
	  predicate = s.getPredicate();
	  object = s.getObject();
		
	  keyWords.add(predicate.getLocalName());

	  // local name of (non blank nodes) Resources
	  if (object instanceof Resource && object.toString().contains("/")) {
		keyWords.add(object.asResource().getLocalName());
		  
	  } else if (object instanceof Literal) {
		// object is a Literal
		keyWords.add(object.toString());
	  }
  }
  return keyWords;
 }
  

 /**
   * Does a full text searc using jena-text.
   * Returns the uris of the TDs that matched with the given key words.
   * @param keyWords Words to find in a TD content.
   * @return List of uris
   */
  public static List<String> listThingDescriptionsFromTextSearch(String keyWords) {
	
	List<String> tds = new ArrayList<>();

	// Construct query
	String qMatch = "";
	String predicate, property;
	predicate = " text:query ";
	property = "rdfs:comment";     
	qMatch += " ?s " + predicate + "(" + property + " " + keyWords + ") . ";
	
	String prefix = StrUtils.strjoinNL
			  ( "PREFIX text: <http://jena.apache.org/text#>"
			  , "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
			  , "PREFIX td: <http://www.w3c.org/wot/td#>"
			  , "PREFIX qu: <http://purl.oclc.org/NET/ssnx/qu/qu#>"
			  , "PREFIX unit: <http://purl.oclc.org/NET/ssnx/qu/unit#>"
			  , "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>");
	
	// Run the query
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	
	try {
	  String query = "SELECT DISTINCT ?g WHERE { GRAPH ?g { " + qMatch + " FILTER NOT EXISTS { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> } } }";
	  Query q = QueryFactory.create(prefix + "\n" + query);
	  
	  try {
		QueryExecution qexec = QueryExecutionFactory.create(q , dataset);
		ResultSet result = qexec.execSelect();
		while (result.hasNext()) {
		  tds.add(result.next().get("g").asResource().getURI());
		}
	  } catch (Exception e) {
		throw e;
	  }
	} finally {
	  dataset.end();
	}
	
	return tds;
  }


  // *********** FOR RESOURCE DIRECTORY ************

  /**
   * Returns a list of registered end points
   * @return
   */
  public static List<String> listEndpoints() {
	
	List<String> eps = new ArrayList<>();
	
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	
	String prefix = "PREFIX rdf-schema: <http://www.w3.org/2000/01/rdf-schema#>";

	try {
	  String q = prefix + " SELECT ?endpoint WHERE { ?s rdf-schema:isDefinedBy ?endpoint . }";
	  try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
		ResultSet result = qexec.execSelect();
		while (result.hasNext()) { 
		eps.add(result.next().get("endpoint").toString());
		}
	  }
	catch (Exception e) {
	  throw e;
	}
	} finally {
	  dataset.end();
	}
	
	return eps;
  }

  /**
   * Checks if lifetime is still valid (at least 10 seconds)
   * @param uri Uri of the resource to check its lifetime
   * @return true if time > 10 seconds, false otherwise
   */
  public static Boolean checkLifeTime(URI uri) {
  
	List<String> dates = new ArrayList<String>();
	Boolean hasTime = true;
	
	Dataset dataset = Repository.get().dataset;
	dataset.begin(ReadWrite.READ);
	
	String prefix = "PREFIX purl: <http://purl.org/dc/terms/> ";
	String query = "SELECT ?modified ?lifetime WHERE { " +
			" <" + uri.toString() + "> purl:modified ?modified. " +
			" <" + uri.toString() + "> purl:dateAccepted ?lifetime. }";
	
	String dateMod = "";
	String dateLife = "";
	
	try {
	  
	  try (QueryExecution qexec = QueryExecutionFactory.create(prefix + query, dataset)) {
		ResultSet result = qexec.execSelect();
		QuerySolution sol;
		  while (result.hasNext()) {
			sol = result.next();
			dateMod = sol.get("modified").toString();
			dateLife = sol.get("lifetime").toString();
		  }
	  }
	  
	} catch (Exception e) {
	  throw e;
	} finally {
	  dataset.end();
	}
	
	if ( dateMod != null && !dateMod.isEmpty() && dateLife != null && !dateLife.isEmpty() ) {
	  DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	  Calendar modCal = Calendar.getInstance();
	  Calendar lifeCal = Calendar.getInstance();
	  long diff;
	  try {
		lifeCal.setTime(dateFormat.parse(dateLife));
		diff =  lifeCal.getTimeInMillis() - modCal.getTimeInMillis();
		//System.out.println("Remaining time " + Long.toString(diff));
		if (diff <= 0) {
		  hasTime = false;
		}
		  
	  } catch (ParseException e) {
		e.printStackTrace();
	  }
	}
	
	return hasTime;
  }
  
  
  public String getCurrentDateTime(int plusTime) {
	// TODO static?
	
	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	Calendar cal = Calendar.getInstance();
	cal.add(Calendar.SECOND, plusTime); // for the life time, else adds 0 sec
	return dateFormat.format(cal.getTime());
  }
  
  
  public static void printTDQueue() {
	  
	  Iterator<ThingDescription> iter = Repository.get().tdQueue.iterator();
	  while (iter.hasNext()) {
		  System.out.println(iter.next().getId());
	  }
  }
  
  
  public static List<Entry<String,String>> listThingDescriptionsLifetime() {
	  
	  List<Entry<String, String>> tds = new ArrayList<>();
	  Dataset dataset = Repository.get().dataset;
	  
	  String prefix = "PREFIX purl: <http://purl.org/dc/terms/> ";
	  String query = prefix + "SELECT ?id ?lifetime WHERE {?id purl:dateAccepted ?lifetime.}";
	  
	  dataset.begin(ReadWrite.READ);
	  try {
		  try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet result = qexec.execSelect();
			QuerySolution sol;
			while (result.hasNext()) {
				sol = result.next();
				tds.add( new SimpleEntry<String,String>(sol.get("id").toString(), sol.get("lifetime").toString()) );
			}
		  } catch (Exception e) {
			  throw e;
		  }
		  
	  } finally {
		  dataset.end();
	  }
	  
	  return tds;
  }

}