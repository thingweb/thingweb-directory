package org.eclipse.thingweb.directory.util;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.utils.Obj;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.thingweb.directory.utils.TDTransform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;


/**
 * org.eclipse.thingweb.digrectory.util
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

  public Object frame(RepositoryConnection connection, String frame) throws IOException {

    Object frameObject = JsonUtils.fromString(frame);

    //expanded frame object
    Object expandedframeObject = JsonLdProcessor.expand(frameObject);


    //Get subGraph from the repository by the frame object
    if (expandedframeObject instanceof  List){
      expandedframeObject = ((List) expandedframeObject).get(0);
    }

    Object subGraph = getSubGraph(connection, expandedframeObject);

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

    Object framed = JsonLdProcessor.frame(subGraph, frameObject, opts);
    return framed;
  }


  private Object getSubGraph(RepositoryConnection connection, Object frame) throws IOException {

    subGraphModel = new TreeModel();

    Model parentLayer = processParentLayer(connection, (Map<String, Object>) frame);

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
          processChildrenLayers(connection, value.toString(), seen);
        }
      }
    }

    subGraphModel.addAll(parentLayer);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Rio.write(subGraphModel, buffer, RDFFormat.JSONLD);

    return JsonUtils.fromString(buffer.toString());
  }


  private Model processParentLayer(RepositoryConnection connection, Map<String, Object> object){
    Object id = object.get("@id");
    Object type = object.get("@type");

    List<String> properties = new ArrayList<>();

    object.forEach((String key, Object value)->{
      if (!key.equals("@id")){
        if (!key.equals("@context")) {
          if (!key.equals("@type")) {
            properties.add(key);
          }
        }
      }
    });

    String query = generateDescribeQuery(id, properties, type);

    Model model = doDescribeQuery(connection, query);

    return model;
  }

  private void processChildrenLayers(RepositoryConnection connection, String resource, Set<String> seen){
    Model model__ = queryResource(connection, resource);
    seen.add(resource);

    for (Statement statement : model__) {
      Value value = statement.getObject();
      if (!value.equals(resource)) {
        if (value instanceof Resource) {
          if (!seen.contains(value.toString())) {
            processChildrenLayers(connection, value.toString(), seen);
          }
        }
      }
    }

    subGraphModel.addAll(model__);
  }


  private String generateDescribeQuery(Object resource, List<String> properties, Object type){
    String head = resource == null? "?x" : "<" + resource + ">";

    String query = "DESCRIBE " + head;
    query += " WHERE{ \n";

    int i = 0;
    for (String property:properties){
      query += head + " <" + property + "> " + "?o" + (++i) + ". \n";
    }

    if (type != null){
      query += generateType(head, type);
    }

    query += "}";

    return query;
  }

  private String generateType(String head, Object type){
    if (type instanceof Map){
      Map<String, Object> map = (Map<String, Object>) type;
      Object type_ = map.get("@id");
      return head + " a <" + type_ + ">.\n";
    }

    if (type instanceof List){
      List<Object> list = (List<Object>) type;

      String s = "";
      for (Object object:list){
        if (object instanceof String){
          s += head + " a <" + object + ">.\n";
        }
      }
      return s;
    }

    if (type instanceof String)
    {
      return head + " a <" + type + "> .\n";
    }

    return "";
  }

  private Model queryResourceWithFilter(RepositoryConnection connection, String subject, String type ){
    if (subject == null) { subject = "?x"; }
    else {subject = "<" + subject + ">"; }
    type = "<" + type + ">";
    String describeQuery = "DESCRIBE " + subject + " " + "WHERE { " + subject + " a " + type + ".}";

    return doDescribeQuery(connection, describeQuery);
  }

  private Model queryResource(RepositoryConnection connection, String subject){
    String describeQuery  = "DESCRIBE <" + subject + "> ";
    return doDescribeQuery(connection, describeQuery);
  }

  private Model doDescribeQuery(RepositoryConnection connection, String describeQuery){
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
  private void testFrame() throws IOException {
    String input =
        "{\n"
            + "  \"@context\": [\"https://w3c.github.io/wot/w3c-wot-td-context.jsonld\"],\n"
            + "  \"@type\": [\"Thing\"],\n"
            + "  \"name\": \"MyLampThing\",\n"
            + "  \"interaction\": [\n"
            + "      {\n"
            + "          \"@type\": [\"Property\"],\n"
            + "          \"name\": \"status\",\n"
            + "          \"schema\": {\"type\": \"string\"},\n"
            + "          \"writable\": false,\n"
            + "          \"observable\": true,\n"
            + "          \"form\": [{\n"
            + "              \"href\": \"coaps://mylamp.example.com:5683/status\",\n"
            + "              \"mediaType\": \"application/json\"\n"
            + "          }]\n"
            + "      },\n"
            + "      {\n"
            + "          \"@type\": [\"Action\"],\n"
            + "          \"name\": \"toggle\",\n"
            + "          \"form\": [{\n"
            + "              \"href\": \"coaps://mylamp.example.com:5683/toggle\",\n"
            + "              \"mediaType\": \"application/json\"\n"
            + "          }]\n"
            + "      },\n"
            + "      {\n"
            + "          \"@type\": [\"Event\"],\n"
            + "          \"name\": \"overheating\",\n"
            + "          \"schema\": {\"type\": \"string\"},\n"
            + "          \"form\": [{\n"
            + "              \"href\": \"coaps://mylamp.example.com:5683/oh\",\n"
            + "              \"mediaType\": \"application/json\"\n"
            + "          }]\n"
            + "      }\n"
            + "  ]\n"
            + "}";

    String frame=
        "{\"@context\": [\"https://w3c.github.io/wot/w3c-wot-td-context.jsonld\"],\n"
            + "  \"@type\": [\"Thing\"]}";


    RepositoryConnection connection = inputData(input).getConnection();
    Object object = frame(connection, frame);

    String td = JsonUtils.toPrettyString(object);
    TDTransform transform = new TDTransform(new ByteArrayInputStream(td.getBytes()));
                td        = transform.asJsonLd11();

    System.out.println(JsonUtils.toPrettyString(JsonUtils.fromString(td)));
  }

  public static void main(String[] args){
    try {
      (new Frame2SPARQL()).testFrame();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
