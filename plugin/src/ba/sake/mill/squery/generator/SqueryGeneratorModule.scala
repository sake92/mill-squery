package ba.sake.mill.squery.generator

import mill.*
import mill.scalalib.*
import mill.api.BuildCtx
import mill.util.Jvm

trait SqueryGeneratorModule extends JavaModule {

  def squeryJdbcUrl: T[String]
  def squeryJdbcDeps: T[Seq[Dep]]

  /** List of (schema, basePackage) */
  def squerySchemaMappings: T[Seq[(String, String)]]

  def squeryColNameIdentifierMapper: T[String] = "camelcase"
  def squeryTypeNameMapper: T[String] = "camelcase"
  def squeryRowTypeSuffix: T[String] = "Row"
  def squeryDaoTypeSuffix: T[String] = "Dao"
  /** Ordered rules in `column-name-regex|declared-type-regex|Scala-type` format. */
  def squeryTypeMappingRules: T[Seq[String]] = Seq.empty
  /** Regexes matching `schema.table` names to generate; empty means all tables. */
  def squeryIncludeTables: T[Seq[String]] = Seq.empty
  /** Regexes matching `schema.table` names to exclude; exclusions take precedence. */
  def squeryExcludeTables: T[Seq[String]] = Seq.empty
  /** Key newtypes: `none` (default) or `iron`. `iron` requires the `squery-iron` dependency. */
  def squeryNewtypes: T[String] = "none"

  def squeryTargetDir: T[PathRef] = Task {
    BuildCtx.withFilesystemCheckerDisabled {
      PathRef(moduleDir / "src")
    }
  }

  def squeryVersion: T[String] = "0.13.0"

  def squeryClasspath: T[Seq[PathRef]] = Task {
    defaultResolver().classpath(
      squeryJdbcDeps() ++
        Seq(mvn"ba.sake:squery-cli_2.13:${squeryVersion()}")
    )
  }

  def squeryGenerateArgs: T[Seq[String]] = Task {
    val includeTables = squeryIncludeTables() match {
      case Seq()    => Seq(".*")
      case patterns => patterns
    }
    Seq(
      "--jdbcUrl",
      squeryJdbcUrl(),
      "--baseFolder",
      squeryTargetDir().path.wrapped.toString,
      "--colNameIdentifierMapper",
      squeryColNameIdentifierMapper(),
      "--typeNameMapper",
      squeryTypeNameMapper(),
      "--rowTypeSuffix",
      squeryRowTypeSuffix(),
      "--daoTypeSuffix",
      squeryDaoTypeSuffix(),
      "--newtypes",
      squeryNewtypes()
    ) ++ squerySchemaMappings().flatMap { case (schemaName, packageName) =>
      Seq("--schemaMappings", s"${schemaName}:${packageName}")
    } ++ squeryTypeMappingRules().flatMap(rule => Seq("--typeMappingRule", rule)) ++
      includeTables.flatMap(pattern => Seq("--includeTables", pattern)) ++
      squeryExcludeTables().flatMap(pattern => Seq("--excludeTables", pattern))
  }

  def squeryGenerate(): Command[Unit] = Task.Command {
    val args = squeryGenerateArgs()
    println("Starting to generate Squery sources...")
    Jvm.withClassLoader(classPath = squeryClasspath().map(_.path).toSeq) { classLoader =>
      classLoader
        .loadClass("ba.sake.squery.cli.SqueryMain")
        .getMethod("main", classOf[Array[String]])
        .invoke(null, args.toArray)
    }
    println("Finished generating Squery sources")
  }

}
