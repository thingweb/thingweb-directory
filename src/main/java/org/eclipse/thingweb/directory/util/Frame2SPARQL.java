package org.eclipse.thingweb.directory.util;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;


/**
 * org.eclipse.thingweb.directory.util
 * <p>
 * TODO: Add class description
 * <p>
 * Author:  Anh Le-Tuan
 * <p>
 * Email:   anh.letuan@insight-centre.org
 * <p>
 * Date:  22/06/18.
 */
public class Frame2SPARQL
{
  private Model subGraphModel;

  private Object frame(Repository repo, Object frame) throws IOException {

    //Get subGraph from the repository by the frame object
    Object subGraph = getSubGraph(repo, frame);

    //If subGraph is empty return empty sub Graph
    if (subGraph instanceof List){
      List<Object> list = (List<Object>) subGraph;
      if (list.size() == 0){
        return subGraph;
      }
    }

    //Perform framing to from the framed json-ld
    JsonLdOptions opts = new JsonLdOptions();
    opts.setPruneBlankNodeIdentifiers(true);
    opts.setUseNativeTypes(true);
    opts.setCompactArrays(true);

    Object framed = JsonLdProcessor.frame(subGraph, frame, opts);
    return framed;
  }


  private Object getSubGraph(Repository repo, Object frame) throws IOException {
    subGraphModel = new TreeModel();

    Model parentLayer = processParentLayer(repo, (Map<String, Object>) frame);

    //seen set to prevent describing duplicated resource
    Set<String> seen = new HashSet<>();

    for (Statement statement:parentLayer){

      seen.add(statement.getSubject().toString());

      Value value = statement.getObject();

      //if the object is an resource
      if (value instanceof Resource)
      {
        //if the resource has not been described yet.
        if (!seen.contains(value.toString()))
        {
          //describe resource in further
          processChildrenLayers(repo, value.toString(), seen);
        }
      }
    }

    subGraphModel.addAll(parentLayer);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Rio.write(subGraphModel, buffer, RDFFormat.JSONLD);
    System.out.println(buffer.toString());
    return JsonUtils.fromString(buffer.toString());
  }


  private Model processParentLayer(Repository repository, Map<String, Object> object){
    Object id = object.get("@id");

    List<String> properties = new ArrayList<>();

    object.forEach((String key, Object value)->{
      if (!key.equals("@id")){
        properties.add(key);
      }
    });

    String query = generateDescribeQuery(id, properties);

    Model model = doDescribeQuery(repository, query);

    return model;
  }

  private void processChildrenLayers(Repository repository, String resource, Set<String> seen){
    Model model__ = queryResource(repository, resource);
    seen.add(resource);

    for (Statement statement : model__) {
      Value value = statement.getObject();
      if (!value.equals(resource)) {
        if (value instanceof Resource) {
          if (!seen.contains(value.toString())) {
            processChildrenLayers(repository, value.toString(), seen);
          }
        }
      }
    }

    subGraphModel.addAll(model__);
  }


  private String generateDescribeQuery(Object resource, List<String> properties){
    String head = resource == null? "?x" : "<" + resource + ">";

    String query = "DESCRIBE " + head;
    query += " WHERE{ \n";

    int i = 0;
    for (String property:properties){
      query += head + " <" + property + "> " + "?o" + (++i) + ". \n";
    }

    query += "}";

    return query;
  }

  private Model queryResourceWithFilter(Repository repository, String subject, String type ){
    if (subject == null) { subject = "?x"; }
    else {subject = "<" + subject + ">"; }
    type = "<" + type + ">";
    String describeQuery = "DESCRIBE " + subject + " " + "WHERE { " + subject + " a " + type + ".}";

    return doDescribeQuery(repository, describeQuery);
  }

  private Model queryResource(Repository repository, String subject){
    String describeQuery  = "DESCRIBE <" + subject + "> ";

    return doDescribeQuery(repository, describeQuery);
  }

  private Model doDescribeQuery(Repository repository, String describeQuery){
    RepositoryConnection connection   = repository.getConnection();
    GraphQueryResult graphQueryResult = connection.prepareGraphQuery(describeQuery).evaluate();
    Model model = QueryResults.asModel(graphQueryResult);

    return model;
  }




//=====================================================================================================================
//TEST CODE
//=====================================================================================================================

  private Repository inputData(String input) throws IOException {

    Repository repo = new SailRepository(new MemoryStore());
    repo.initialize();
    RepositoryConnection connection = repo.getConnection();

    connection.add(new StringReader(input), "", RDFFormat.JSONLD);
    connection.commit();

    return repo;
  }

  private void printJsonLd(Model model) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Rio.write(model, byteArrayOutputStream, RDFFormat.JSONLD);

    Object object = JsonUtils.fromString(byteArrayOutputStream.toString());
           object = JsonLdProcessor.compact(object, new HashMap<>(), new JsonLdOptions());

    System.out.println(JsonUtils.toPrettyString(object));
  }

  //Test describe query without Filter



  private void queryTest1(){
    String input =
        "[{\"@id\":\"http://personA\",\n"
            + "  \"@type\":\"http://person.org\",\n"
            + "  \"http://name.org\":\"Person A\",\n"
            + "  \"http://id.org\":\"1\"},\n"
            + " {\"@id\":\"http://deviceA\",\n"
            + "  \"@type\":\"http://device.org\",\n"
            + "  \"http://name.org\":\"Device A\",\n"
            + "  \"http://id.org\":\"2\"}]";

    try {
      Repository repo = inputData(input);

      Model model = queryResourceWithFilter(repo, null, "http://device.org");
      printJsonLd(model);


    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void testOtherLevel() throws IOException {
    String input =
        "[{\n"
            + "  \"@id\":\"http://personA\",\n"
            + "  \"http://name\":\"Anh\",\n"
            + "  \"http://knows\":{\n"
            + "    \"@id\":\"http://personB\",\n"
            + "    \"http://name\":\"B\"\n"
            + "  }},\n"
            + " {\n"
            + "  \"@id\":\"http://personC\",\n"
            + "  \"http://name\":\"Cnh\",\n"
            + "  \"http://knows\":{\n"
            + "  \"@id\":\"http://personD\",\n"
            + "    \"http://name\":\"C\"\n"
            + "  }\n"
            + " }\n"
            + "]";
    String frame="";

    subGraphModel = new TreeModel();

    Repository repository = inputData(input);

    processChildrenLayers(repository, "http://personA", new HashSet<>());

    printJsonLd(subGraphModel);
  }

  private void testFirstLevel() throws IOException {
    String input =
        "[{\n"
            + "  \"@id\":\"http://personA\",\n"
            + "  \"http://name\":\"Anh\",\n"
            + "  \"http://knows\":{\n"
            + "    \"@id\":\"http://personB\",\n"
            + "    \"http://name\":\"B\"\n"
            + "  }},\n"
            + " {\n"
            + "  \"@id\":\"http://personC\",\n"
            + "  \"http://name\":\"Cnh\",\n"
            + "  \"http://knows\":{\n"
            + "  \"@id\":\"http://personD\",\n"
            + "    \"http://name\":\"C\"\n"
            + "  }\n"
            + " }\n"
            + "]";
    String frame =
        "{\n"
            + "  \"@id\":\"http://personA\",\n"
            + "  \"http://knows\":{},\n"
            + "  \"http://name\":\"\"\n"
            + "}";

    Repository repo = inputData(input);
    Model model = processParentLayer(repo, (Map<String, Object>) JsonUtils.fromString(frame));
    printJsonLd(model);
  }

  private void testFrame() throws IOException {
    String input =
        "[{\n"
            + "  \"@id\":\"http://personA\",\n"
            + "  \"http://name\":\"Anh\",\n"
            + "  \"http://knows\":{\n"
            + "    \"@id\":\"http://personB\",\n"
            + "    \"http://name\":\"B\"\n"
            + "  }},\n"
            + " {\n"
            + "  \"@id\":\"http://personC\",\n"
            + "  \"http://name\":\"Cnh\",\n"
            + "  \"http://knows\":{\n"
            + "  \"@id\":\"http://personD\",\n"
            + "    \"http://name\":\"C\"\n"
            + "  }\n"
            + " }\n"
            + "]";

    String frame__ =
        "{\n"
            + "  \"@id\":\"http://personA\",\n"
            + "  \"http://knowss\":{},\n"
            + "  \"http://name\":\"\"\n"
            + "}";

    Repository repo = inputData(input);
    Object frameObj = JsonUtils.fromString(frame__);
    Object framed = frame(repo, frameObj);
  }



}
