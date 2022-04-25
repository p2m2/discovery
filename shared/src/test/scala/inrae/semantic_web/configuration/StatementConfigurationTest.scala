package inrae.semantic_web.configuration

import utest._
import wvlet.log.LogLevel

import scala.util.{Failure, Success, Try}

object StatementConfigurationTest extends TestSuite {
  val configBase = """
            {
             "sources" : [{
               "id"  : "dbpedia",
               "url" : "https://dbpedia.org/sparql",
               "mimetype" : "application/sparql-query",
               "method" : "POST"
             }],
             "settings" : {
               "cache" : true,
               "logLevel" : "info",
               "sizeBatchProcessing" : 10,
               "pageSize" : 10
             }
            }
            """.stripMargin

  def tests = Tests {
    test("Create a simple source with string configuration") {
      SWDiscoveryConfiguration.setConfigString(configBase)
    }

    test("Get a unknown source") {
      val c = SWDiscoveryConfiguration.setConfigString(configBase)

      Try(c.source("something")) match {
        case Success(_) => assert(false)
        case Failure(_) => assert(true)
      }
    }

    test("Create a simple source") {

      val dbname = "dbpedia"
      val url = "http://test"
      val mimetype = "application/sparql-query"

      val configDbpediaBasic: SWDiscoveryConfiguration = SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, mimetype=mimetype)))
      val source = configDbpediaBasic.source("dbpedia")

      assert(source.id == dbname)
      assert(source.url == url)
      assert(source.mimetype == mimetype)
    }

    test("unknown mimetype") {

      val dbname = "dbpedia"
      val url = "http://test"
      val mimetype = " -- "

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, mimetype=mimetype)))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }
    }

    test("unknown method") {

      val dbname = "dbpedia"
      val url = "http://test"
      val method = " -- "

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, method=method)))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }
    }

    test("defined too much source") {

      val dbname = "dbpedia"
      val url = "http://test"

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, file="sss", content="sss")))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, file="sss", content="sss")))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, file="sss")))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }

      Try(SWDiscoveryConfiguration(
        Seq(Source(id=dbname, url=url, content="sss")))) match {
        case Success(s) => assert(false)
        case Failure(e) => assert(true)
      }
    }

    test("Create a config with a bad tag ") {
      Try(SWDiscoveryConfiguration
        .setConfigString(
          """
          {
           "hello" : [{
           }]}
          """.stripMargin)) match {
        case Success(_) => assert(false)
        case Failure(_) => assert(true)
      }
    }

    test("Create a request config with an unknown log level ") {
      assert(SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"hello.world\"")).settings.getLogLevel == LogLevel.WARN)
    }

    test("Create a request config log level debug ") {
      Try(SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"debug\"")).settings.getLogLevel == LogLevel.DEBUG) match {
        case Success(_) => assert(true)
        case Failure(_) => assert(false)
      }
    }

    test("Create a request config log level info ") {

      val c = SWDiscoveryConfiguration
        .setConfigString(configBase)
      assert(c.settings.getLogLevel == LogLevel.INFO)

    }
    test("Create a request config log level trace ") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"trace\""))
      assert(c.settings.getLogLevel == LogLevel.TRACE)
    }
    test("Create a request config log level warn ") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"warn\""))
      assert(c.settings.getLogLevel == LogLevel.WARN)
    }

    test("Create a request config log level error ") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"error\""))
      assert(c.settings.getLogLevel == LogLevel.ERROR)
    }

    test("Create a request config log level all ") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"all\""))
      assert(c.settings.getLogLevel == LogLevel.ALL)
    }

    test("Create a request config log level off ") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"info\"",
          "\"off\""))
      assert(c.settings.getLogLevel == LogLevel.OFF)
    }

    test("pageSize can not be negative") {
      Try(SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"pageSize\" : 10",
          "\"pageSize\" : -1"))) match {
        case Success(c) => assert(false)
        case Failure(e) => assert(true)
      }
    }
    test("pageSize can be equal to zero") {
      Try(SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"pageSize\" : 10",
          "\"pageSize\" : 0"))) match {
        case Success(c) => assert(false)
        case Failure(e) => assert(true)
      }
    }
    test("pageSize") {
      val c = SWDiscoveryConfiguration
        .setConfigString(configBase.replace("\"pageSize\" : 10",
          "\"pageSize\" : 5"))
      assert(c.settings.pageSize == 5)
    }
  }
}
