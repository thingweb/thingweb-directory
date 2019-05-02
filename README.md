## Getting Started

The Thingweb Directory requires Java 1.8.

### Building from sources

We are using [Gradle](https://gradle.org/) as a build tool.

The following command will produce a distribution of the Thingweb Directory in `directory-app/build/distributions/`:
```sh
$ cd thingweb-directory
$ gradle build
```
The following command will run the application locally on http://localhost:8080.
```sh
$ cd thingweb-directory
$ gradle runApp
```