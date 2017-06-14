# thingweb-repository

Thingweb-Repository is an open source repository for Thing Descriptions. Thing Description (TD) is a proposal of W3C Web of Things interest group to describe Things.

Thingweb-Repository features an API to create, read, update and delete (CRUD) a TD. The repository can be used to *browse* and *discover* Things based on their TDs. This functionality includes but is not limited to following cases:

  - Searching for a Thing based on its metadata, properties, actions or events;
  - Creating a new Thing's TD or updating an existing one;
  - Deleting a Thing's TD;
  - CRUD operations are supported either over HTTP or CoAP;
  - Generating a servient based on a discovered Thing.

## Contents
1. [Building](#building)
2. [Running a Thingweb-Repository Server](#Running-a-Thingweb-Repository-Server)
3. [Interacting with a Thingweb-Repository Server](#Interacting-with-a-Thingweb-Repository-Server)
4. [Open API Specification of Thingweb-Repository](#Open-API-Specification-of-Thingweb-Repository)
5. [ToDos](#ToDos)

### Building

* We are using [Gradle](https://gradle.org/) as a build tool
* The application is built using the [Gradle Application Plugin](https://docs.gradle.org/current/userguide/application_plugin.html)

### Running a Thingweb-Repository Server

Download the project and build it (see [Building](#Building). Once it is finished, Thingweb-Repository server can be started:
```sh
$ java -jar thingweb-repository.jar <thingweb_repository_path>
```
<thingweb-repository_path> specifies the chosen location where Thingweb-Repository will be installed. After this step, a Thingweb-Repository server is running and can be accessed over HTTP from:
```sh
http://<thingweb_repository_ip>:8080/td
```
or over CoAP from:

    coap://<thingweb_repository_ip>:5683/td

### Interacting with a Thingweb-Repository Server

###### Creates (adds) a TD to a collection `/td` (e.g., a Thing registers itself).

```sh
Method: POST
URI Template: /td
Request Parameters:
  lt := Lifetime (optional). Lifetime of the registration in seconds. If not specified, a default value of 86400 (24 hours) is assumed.
Content-Type: application/ld+json
Payload: content of a TD.jsonld file
Success: 201 Created
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

If the response code is `201 Created`, the URI path of the created sub-resource is defined in the header field `Location` (for HTTP) or `Location-Path` (for CoAP). The path is relative to the root resource and follows the pattern `/td/{id}`, where `id` is an ID assigned by the repository for the uploaded Thing Description.

If the associated uris of the new TD are already in the dataset, the response code is `400 Bad Request`. This is done in order to avoid duplicated TDs in the dataset. To change an existing TD use PUT instead.

###### Returns a list of TDs based on a SPARQL query pattern (e.g., a client queries the repository for a TD with a specific Thing URI).

```sh
Method: GET
URI Template: /td
Request Parameters:
  query := SPARQL query encoded as URI.
  text := Boolean text search query.
Content-Type: application/ld+json
Success: 200 OK
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

Examples:

- SPARQL query pattern to return a TD with `coap://192.168.1.104/Fan` associated as its URI:
```sh
?Y <http://www.w3c.org/wot/td#hasMetadata> ?X . ?Z <http://www.w3c.org/wot/td#associatedUri> "coap://192.168.1.104/Fan" .
```
HTTP request with the SPARQL query encoded as URI:
```sh
http://localhost:8080/td?query=%3FY+<http%3A%2F%2Fwww.w3c.org%2Fwot%2Ftd%23hasMetadata>+%3FX+.%3FZ+<http%3A%2F%2Fwww.w3c.org%2Fwot%2Ftd%23associatedUri>++"coap%3A%2F%2F192.168.1.104%2FFan".
```

- SPARQL query pattern to return all TDs (not recommended if their is a large amount of TDs in the repository)
```sh
?X ?Y ?Z .
```

- Boolean text search query to return a TD with `location` in `room_4`:
```sh
"location AND room_4"
```

Other possible combinations: "word", "word1 AND word2", "word1 OR word2", etc.

The response is a JSON object (_but no valid JSON-LD document_). This JSON object should have the following form:
```sh
{
  "/td/{id}": {... Thing Description ...},
  "/td/{id}": {... Thing Description ...},
  ...
}
```

###### Returns a TD based on its `{id}` (e.g., a client queries the repository for a specific TD).

```sh
Method: GET
URI Template: /td/{id}
URI Template Parameter:   
  {id} := ID of a TD to fetch.
Content-Type: application/ld+json
Success: 200 OK
Failure: 404 Not Found
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

Example:
```sh
http://localhost:8080/td/0d134768
```


###### Updates an existing TD.
```sh
Method: PUT
URI Template: /td/{id}
URI Template Parameter:   
  {id} := ID of a TD to be updated.
  lt := Lifetime of the registration in seconds. If not specified, a default value of 86400 (24 hours) is assumed.
Payload: content of a TD.jsonld file
Content-Type: application/ld+json
Success: 200 OK
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

If the update changes the associated uris and at least one of them is registered with another TD, then the response code is `400 Bad Request`. This is done in order to avoid duplicated TDs in the dataset.

###### Deletes an existing TD.
```sh
Method: DELETE
URI Template: /td/{id}
URI Template Parameter:   
  {id} := ID of a TD to be deleted
Content-Type: application/ld+json
Success: 200 OK
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

###### Discovers a TD based on different type of lookups
```sh
Method: GET
URI Template: /td-lookup/{ep,sem}
URI Template Parameter:   
  lookup-type := {ep, sem, rdf} (Mandatory). Used to select the kind of lookup to perform (endpoint or semantic). The first type is used to lookup TD’s by endpoint. The second type is used to lookup based on SPARQL query or a full text search query. The third type is used to lookup the unit values of a given RDF property.
  ep := Endpoint name (Optional). Use for endpoint lookups. It is the name given to the TD on registration (see POST method), and returned in the response content. If not specified all TDs are listed, otherwise it is used as a filter. Ex.: /td/1a23bc.
  query := SPARQL query encoded as URI. Used for semantic lookups.
  text := Full text search query. Used for semantic lookups.
Content-Type: application/ld+json
Success: 200 OK
Failure: 400 Bad Request
Failure: 500 Internal Server Error
```

Examples:
- Lookup by endpoint name:
```sh
coap://localhost:8080/td-lookup/ep?ep=/td/1a23bc
```

- Lookup by SPARQL query (same value as in GET method):
```sh
coap://localhost:8080/td-lookup/sem?query=?X ?Y ?Z .
```

- Lookup by text query (same value as in GET method):
```sh
coap://localhost:8080/td-lookup/sem?text="word1 AND word2"
```

- Lookup by RDF property:
```sh
coap://localhost:8080/td-lookup/sem?rdf=http://example.org/lightBrightness
```

## Open API Specification of Thingweb-Repository

See `src/main/resources/api.json` for a formal specification of the Thingweb-Repository API. This file is exposed by the server at `/api.json`. It can e.g. be processed by the [Swagger UI](http://swagger.io/swagger-ui/) to render an online documentation. See the [Open API Initiative](https://www.openapis.org/) for more details.

## TODOs

 - proper configuration of log4j
 - stack trace in logs if 500 returned
 - full-fledged SPARQL endpoint?