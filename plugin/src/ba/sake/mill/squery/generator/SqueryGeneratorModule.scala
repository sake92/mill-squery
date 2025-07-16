package ba.sake.mill.squery.generator

import mill.*
import mill.scalalib.*
import mill.api.BuildCtx
import mill.util.Jvm
import upickle.default.{ReadWriter, macroRW}
import ba.sake.squery.generator.{NameMapper, SchemaConfig, SqueryGenerator, SqueryGeneratorConfig}

trait SqueryGeneratorModule extends JavaModule {

  def squeryJdbcUrl: T[String]
  def squeryJdbcDeps: T[Seq[Dep]]

  /** List of (schema, basePackage) */
  def squerySchemaMappings: T[Seq[(String, String)]]

  def squeryColNameIdentifierMapper: T[String] = Task("camelcase")
  def squeryTypeNameMapper: T[String] = Task("camelcase")
  def squeryRowTypeSuffix: T[String] = Task("Row")
  def squeryDaoTypeSuffix: T[String] = Task("Dao")

  def squeryTargetDir: T[PathRef] = Task {
    BuildCtx.withFilesystemCheckerDisabled {
      PathRef(moduleDir / "src")
    }
  }

  def squeryVersion: T[String] = Task("0.8.1")

  def squeryClasspath: T[Seq[PathRef]] = Task {
    defaultResolver().classpath(
      squeryJdbcDeps() ++
        Seq(mvn"ba.sake:squery-cli_2.13:${squeryVersion()}")
    )
  }

  def squeryGenerate(): Command[Unit] = Task.Command {
    println("Starting to generate Squery sources...")
    Jvm.withClassLoader(classPath = squeryClasspath().map(_.path).toSeq) { classLoader =>
      classLoader
        .loadClass("ba.sake.squery.cli.SqueryMain")
        .getMethod("main", classOf[Array[String]])
        .invoke(
          null,
          Array[String](
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
            squeryDaoTypeSuffix()
          ) ++ squerySchemaMappings().flatMap { case (schemaName, packageName) =>
            Array("--schemaMappings", s"${schemaName}:${packageName}")
          }
        )
    }
    println("Finished generating Squery sources")
  }

}
