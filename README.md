# Spring Cloud Skipper Maven plugin

This plugin adds the ability to create a [Skipper package](https://docs.spring.io/spring-cloud-skipper/docs/current/reference/htmlsingle/#using-packages)
as part of the Maven build process.

It contains [templates](https://github.com/donovanmuller/spring-cloud-skipper-maven-plugin/tree/master/src/main/resources/skipper) 
for the common package files and uses the Maven project values to populate much of the template properties 
(`project.artifactId`, `project.version`, etc.). It also provides the ability to override any of these template 
files in the project if you need to tweak or add properties.

See this blog post for more context:

https://blog.switchbit.io/spring-cloud-skipper-as-a-service-broker

## Default Skipper package templates

The default templates are located at [`src/main/resources/skipper`](src/main/resources/skipper).

```text
src/main/resources/skipper $ tree
├── package.yml
├── templates
│   └── template.yml
└── values.yml
```

## Goals Overview

The Spring Cloud Skipper Maven Plugin has the following goals.

* [skipper-package](#creating-a-skipper-package) creates a Spring Cloud Skipper compliant package
* [skipper-upload](#) uploads Spring Cloud Skipper package to a Skipper repository

## Usage

```xml
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>io.switchbit</groupId>
                <artifactId>spring-cloud-skipper-maven-plugin</artifactId>
                <version>0.1.0.BUILD-SNAPSHOT</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>skipper-package</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin> 
            ...       
        </plugins>    
    </build>
```

### Creating a Skipper package

The configuration above will create the Skipper package on the `package` phase.
In short it will copy the template package files into your build directory, overwrite any templates
in the current project, allowing you to override templates when you need too and then filter
the templates, replacing the respective values with their project POM values. Once that's done it zip's them all
up into a Skipper package.

### Deploying the Skipper package

This uploads the Skipper package built with the `skipper-package` goal to the repository specified (default: `local`).
See the below [attributes](#attributes) to customise which Skipper server to target etc.

### Attributes

* Requires a Maven project to be executed.
* Binds by default to the lifecycle phase: `package`.

#### Optional Parameters

| Name | Type | Description | Default |
| --- | --- | --- | --- |
| `overrideDirectory` | `String` | The directory where template files that will override the default tempaltes are located. | `${project.build.directory}/classes/META-INF/skipper` |
| `skipper.workDir` | `String` | The directory where the template files will be copied too during packaging | `${project.build.directory}/skipper` |
| `skipper.server.uri` | `String` | The URI of the Skipper server to target | `http://localhost:7577/api` |
| `skipper.repo.name` | `String` | The repository to upload the Skipper package too | `local` |


## Thanks

The [Fabric8 Maven plugin](https://github.com/fabric8io/fabric8-maven-plugin/blob/master/plugin/src/main/java/io/fabric8/maven/plugin/mojo/build/ResourceMojo.java)
was a great reference when creating this plugin.
Specifically how to load and process resources.
