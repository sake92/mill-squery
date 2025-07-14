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
  def squeryJdbcUrl = s"jdbc:h2:..."
  def squeryUsername = ""
  def squeryPassword = ""
  // mapping from db schema to package name
  def squerySchemas = Seq("PUBLIC" -> "com.myapp.public")
```
