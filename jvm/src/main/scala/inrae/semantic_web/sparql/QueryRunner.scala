package inrae.semantic_web.sparql
import inrae.semantic_web._
import org.apache.jena.query._

import scala.concurrent.Future

case class QueryRunner(source: ConfigurationObject.Source) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def infoLogger() = {
    println("----------------------------LOG INFO ----------------------------------")
    println("-------------------------- getInfoLogger ------------------------------------")
    println("RQ.getInfoLogger DEBUG : " + ARQ.getInfoLogger.isDebugEnabled())
    println("RQ.getInfoLogger INFO  : " + ARQ.getInfoLogger().isInfoEnabled())
    println("RQ.getInfoLogger WARN  : " + ARQ.getInfoLogger().isWarnEnabled())
    println("RQ.getInfoLogger ERROR : " + ARQ.getInfoLogger().isErrorEnabled())
    println("RQ.getInfoLogger TRACE : " + ARQ.getInfoLogger().isTraceEnabled())
    println("-------------------------- getHttpRequestLogger ------------------------------------")
    println("RQ.getInfoLogger DEBUG : " + ARQ.getHttpRequestLogger.isDebugEnabled())
    println("RQ.getInfoLogger INFO  : " + ARQ.getHttpRequestLogger().isInfoEnabled())
    println("RQ.getInfoLogger WARN  : " + ARQ.getHttpRequestLogger().isWarnEnabled())
    println("RQ.getInfoLogger ERROR : " + ARQ.getHttpRequestLogger().isErrorEnabled())
    println("RQ.getInfoLogger TRACE : " + ARQ.getHttpRequestLogger().isTraceEnabled())
    println("---------------------------- getExecLogger ----------------------------------")
    println("RQ.getExecLogger DEBUG : " + ARQ.getExecLogger().isDebugEnabled())
    println("RQ.getExecLogger INFO  : " + ARQ.getExecLogger().isInfoEnabled())
    println("RQ.getExecLogger WARN  : " + ARQ.getExecLogger().isWarnEnabled())
    println("RQ.getExecLogger ERROR : " + ARQ.getExecLogger().isErrorEnabled())
    println("RQ.getExecLogger TRACE : " + ARQ.getExecLogger().isTraceEnabled())
    println("----------------------------END LOG INFO----------------------------------")
  }

  def query(queryStr: String): Future[QueryResult] = {
    println("-------  query -----------")
    println(queryStr)
    
    Future {
      /* Graph equiv Model => defined in configuration */
      //val model = ModelFactory.createDefaultModel
      val query = QueryFactory.create(queryStr)

      //val authenticator = new Nothing("user", "password".toCharArray)
      val qexec: QueryExecution = QueryExecutionFactory.sparqlService(source.url, query)
      val results: ResultSet = qexec.execSelect()

      QueryResult(ResultSetFactory.copyResults(results))
    }
  }

  def ask(): Unit = {

  }

  def construct() : Unit = {

  }

  def describe() : Unit = {

  }
}
