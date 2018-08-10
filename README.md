# thingweb-directory

The Thingweb Directory is an open source directory for Thing Descriptions. The [Thing Description (TD) model](https://www.w3.org/TR/wot-thing-description/) is a recommendation of the W3C Web of Things working group to describe Things.
The directory features an API to create, read, update and delete (CRUD) a TD. The directory can be used to *browse* and *discover* Things based on their TDs. This functionality includes but is not limited to following cases:

  - Searching for a Thing based on its metadata, properties, actions or events;
  - Creating a new Thing's TD or updating an existing one;
  - Deleting a Thing's TD;
  - CRUD operations are supported either over HTTP or CoAP;
  - Generating a servient based on a discovered Thing.

## Getting Started

The Thingweb Directory requires Java 1.8.

### Running the Directory

Download the project and unzip it (see [releases](https://github.com/thingweb/thingweb-directory/releases)). Then, run:
```sh
$ bin/thingweb-directory
```
After this step, the Directory server is running and can be accessed over HTTP or CoAP:
 - http://localhost:8080
 - coap://localhost:5683

By default, the Directory runs an in-memory RDF store, whose content is deleted after the Directory is shut down. To persist Thing Descriptions, provide an external SPARQL endpoint as argument. Run the command above with arg `-help` for more details.

#### Known issues

On Windows, the execution will fail with a Batch error message. If [Mingw](http://mingw.org/) is installed on your machine, try to run `bin/thingweb-directory` inside an emulated Bash shell. Alternatively, try `java -cp lib\* org.eclipse.thingweb.directory.ThingDirectory`.

### Interacting with the Directory

The HTTP endpoint provides an HTML client to register and discover Thing Descriptions. This client accesses a REST API to manage Thing Descriptions that complies to the [IETF Resource Directory specification](https://tools.ietf.org/html/draft-ietf-core-resource-directory-12). Registration is done by POSTing on [`/td`](http://localhost:8080/td) and discovery can be performed on [`td-lookup/ep`](http://localhost:8080/td-lookup/ep), [`td-lookup/res`](http://localhost:8080/td-lookup/res) and [`td-lookup/sem`](http://localhost:8080/td-lookup/sem) (which expects a SPARQL graph pattern as query parameter).

See `src/main/resources/api.json` for a formal specification of the Thing Directory API. This file is exposed by the server at [`/api.json`](http://localhost:8080/api.json). It can e.g. be processed by the [Swagger UI](http://swagger.io/swagger-ui/) to render an online documentation. See the [Open API Initiative](https://www.openapis.org/) for more details.

### Building from sources

We are using [Gradle](https://gradle.org/) as a build tool.

The following command will produce a distribution of the Thingweb Directory in the `/build/distributions/` folder:
```sh
$ cd thingweb-directory
$ gradle build
```