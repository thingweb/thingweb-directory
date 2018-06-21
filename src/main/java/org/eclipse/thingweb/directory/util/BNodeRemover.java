package org.eclipse.thingweb.directory.util;


import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import org.eclipse.thingweb.directory.ThingDirectory;

import java.util.*;

/**
 * insight.thingweb.directory.util
 * <p>
 * This is the add-hoc solution for issue: TDs polluted with blank node `@id`s... #17
 * TODO: Require unit testing
 * <p>
 * Author:  Anh Le-Tuan
 * <p>
 * Email:   anh.letuan@insight-centre.org
 * <p>
 * Date:  20/06/18.
 */
public class BNodeRemover {

  public static Object removePollutedBnode(Object input){
    try {

      Set<String> unPollutedBNodes = new HashSet<>();
      Set<String> seen       = new HashSet<>();

      //--> compact Json-ld
      Object object = JsonLdProcessor.compact(input, new HashMap<>(), new JsonLdOptions());

      //get unpolluted BNodes
      unPollutedBNodes = getUnPollutedBNodes(object, seen, unPollutedBNodes);

      //remove polluted BNodes
      BNodeRemover.removePollutedBnode(object, unPollutedBNodes);

      return object;
    } catch (JsonLdError jsonLdError) {
      ThingDirectory.LOG.warn(jsonLdError.getMessage());
      return input;
    }
  }

  private static void removePollutedBnode(Object input, Set<String> unPollutedBNodes){
    if (input instanceof List){
      List<Object> list = (List<Object>) input;
      list.forEach((Object o)->removePollutedBnode(o, unPollutedBNodes));
    }

    if (input instanceof Map){
      Map<String, Object> map = (Map<String, Object>) input;

      Object value = map.get("@id");

      if (value != null){
        if (value instanceof String){
          if (((String) value).startsWith("_:")){
            if (!unPollutedBNodes.contains(value)){
              map.remove("@id");
            }
          }
        }
      }
      map.forEach((String id, Object o)->{removePollutedBnode(o, unPollutedBNodes);});
    }
  }

  //Find unpolluted Blank Node
  private static Set<String> getUnPollutedBNodes(Object input,
                                                 Set<String> seen,
                                                 Set<String> unPollutedBNodes){
    if (input instanceof List){
      List<Object> list = (List<Object>) input;

      for (Object object:list){
        getUnPollutedBNodes(object, seen, unPollutedBNodes);
      }
    }

    if (input instanceof Map){
      Map<String, Object> map = (Map<String, Object>) input;

      map.forEach((String s, Object o)->{
        if (s.equals("@id")){
          if (o != null)
          {
            if (o instanceof String){
              if (((String)o).startsWith("_:")){
                if (!seen.contains(o))
                {
                  seen.add((String) o);
                } else {
                  unPollutedBNodes.add((String) o);
                }
              }
            }
          }
        }else {
          getUnPollutedBNodes(o, seen, unPollutedBNodes);
        }
      });
    }
    return unPollutedBNodes;
  }
}
