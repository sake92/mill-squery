package ba.sake.mill.squery.generator

import scala.concurrent.duration.Duration
import mill.*, scalalib.*
import mill.api.PathRef
import mill.api.Discover
import mill.contrib.flyway.FlywayModule
import mill.util.TokenReaders.*
import mill.testkit.{TestRootModule, UnitTester}

class UnitTests extends munit.FunSuite {

  override val munitTimeout = Duration(5, "m")

  test("integration") {
    object build extends TestRootModule, ScalaModule, SqueryGeneratorModule, FlywayModule {
      lazy val millDiscover = Discover[this.type]

      def scalaVersion = "3.7.1"

      override def mvnDeps = Seq(
        mvn"com.zaxxer:HikariCP:4.0.3",
        mvn"com.h2database:h2:2.3.232",
        mvn"ba.sake::squery:0.7.0"
      )

      // cant just ./h2_pagila because of Mill task sandboxing
      def h2DbFile = Task(os.pwd / "h2_pagila")

      override def forkEnv = Map("JDBC_URL" -> s"jdbc:h2:${h2DbFile()}")

      def flywayDriverDeps = Seq(mvn"com.h2database:h2:2.3.232")
      def flywayUrl = s"jdbc:h2:${h2DbFile()}"

      def squeryJdbcUrl = s"jdbc:h2:${h2DbFile()}"
      def squerySchemaMappings = Seq("PUBLIC" -> "public")
      def squeryJdbcDeps = Seq(mvn"com.h2database:h2:2.3.232")

      object test extends ScalaTests with TestModule.Munit {
        override def forkEnv = Map("JDBC_URL" -> s"jdbc:h2:${h2DbFile()}")

        override def mvnDeps = Seq(
          mvn"org.scalameta::munit:1.0.2"
        )
      }
    }

    val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
    UnitTester(build, resourceFolder / "h2").scoped { eval =>
      eval(build.flywayMigrate())
      val squeryGenerateResult = eval(build.squeryGenerate())
      assert(squeryGenerateResult.isRight, squeryGenerateResult.toString)
      val Right(squeryTargetDir) = eval(build.squeryTargetDir): @unchecked

      val generatedModels = os.walk(squeryTargetDir.value.path / "public/models").filter(os.isFile)
      assertEquals(generatedModels.size, 17) // 16 + flyway table..
      assert(generatedModels.map(_.last).contains("ActorRow.scala"), "ActorRow was not generated")

      val generatedDaos = os.walk(squeryTargetDir.value.path / "public/daos").filter(os.isFile)
      assertEquals(generatedDaos.size, 17)
      assert(generatedDaos.map(_.last).contains("ActorDao.scala"), "ActorDao was not generated")

      assert(eval(build.test.testForked()).isRight)
    }
  }
}
