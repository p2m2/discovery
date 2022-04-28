package inrae.semantic_web.strategy

import com.github.p2m2.facade.Axios
import inrae.semantic_web.sparql.QueryResult
import inrae.semantic_web.exception._
import inrae.semantic_web._
import inrae.semantic_web.configuration.OptionPickler

import scala.concurrent.Future

case class ProxyStrategyRequest(urlProxy: String) extends StrategyRequest {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   * send serialized swtransaction at Url Proxy
   * @param sw
   * @return
   */
  def execute(swt: SWTransaction): Future[QueryResult] = {

    val header =  "headers" -> Map(
      "Content-Type" -> "application/json",
      "Content-Type" -> "text/plain")

    val request = Map(
      "url" -> urlProxy,
      "method" -> "POST",
      "type" -> "transaction",
      "object" -> OptionPickler.write(swt)
    )

    Axios.post(urlProxy)
      .toFuture.map(
        res => QueryResult(res.toString)
      )
      .recover(
        error => throw SWDiscoveryException(error.getMessage())
      )
  }

  def request(query: String): Future[QueryResult] = {
    throw SWDiscoveryException("request string is not implemented. Proxy")
  }

}
