# mill-squery

Mill plugin for Squery.

Generates models and DAOs automatically from db.
And incrementally refactors them via regenesca.



```scala
//| mill-version: 1.0.0
//| mvnDeps:
//| - "ba.sake::mill-squery-generator::0.8.0"
//| - "com.lihaoyi::mill-contrib-flyway:1.0.0"
package build

object myapp extends ScalaModule, SqueryGeneratorModule {
  def squeryJdbcUrl = "jdbc:h2:..."
  def squeryJdbcDeps = Seq(mvn"com.h2database:h2:...")
  // mappings from db schema to package name
  def squerySchemaMappings = Seq("PUBLIC" -> "com.myapp.public")

  // optional config
  // def squeryColNameIdentifierMapper = "camelcase" // or "noop"
  // def squeryTypeNameMapper = "camelcase" // or "noop"
  // def squeryRowTypeSuffix = "Row"
  // def squeryDaoTypeSuffix = "Dao"

  // def squeryTargetDir: T[PathRef] = Task {
  //   BuildCtx.withFilesystemCheckerDisabled(PathRef(moduleDir / "src"))
  // }

  // def squeryVersion = "0.8.1" // squery version used to generate sources
```

Generate source files whenever you change the openapi.json file:
```shell
./mill -i myapp.squeryGenerate
```






