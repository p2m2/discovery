package inrae.semantic_web.sparql

import inrae.semantic_web.event.{DiscoveryRequestEvent, DiscoveryStateRequestEvent, Publisher}
import inrae.semantic_web.sparql.QueryResult
import wvlet.log.Logger.rootLogger.{debug, info}

import scala.collection.mutable
import scala.collection.mutable._
import scala.concurrent.Future


case class ConfigurationHttpRequest(
                                     url : String,
                                     login: String = "", // proxy, basic case
                                     password: String="", // proxy, basic case
                                     token: String = "", // bearer case
                                     auth: String = "", // basic, proxy, bearer
                                     mimetype : String ="json"
                                   )

final case class HttpRequestDriverException(private val message: String = "",
                                            private val cause: Throwable = None.orNull) extends Exception(message,cause)

abstract class HttpRequestDriver extends Publisher[DiscoveryRequestEvent] {

  def request(`type`: String , query: String, config : ConfigurationHttpRequest ): Future[QueryResult] = {
    publish(DiscoveryRequestEvent(DiscoveryStateRequestEvent.START_HTTP_REQUEST))
    debug(" -- HttpRequestDriver > " + this.getClass.getName )
    debug(s" ${this.getClass.getName} http request on ${config.url}")

    debug("\n\t"+config.url+"\n\n\t\t"+query.replace("\n","\n\t\t")+"\n\n")

    `type`.toLowerCase() match {
      case "post" => post( query, config  )
      case "get" => get( query, config  )
      case _ => {
        throw HttpRequestDriverException(s"Unknown http type request : ${`type`}")
      }
    }
  }

  protected[sparql] def get( query: String, config : ConfigurationHttpRequest ) : Future[QueryResult]

  protected[sparql] def post( query: String, config : ConfigurationHttpRequest ) : Future[QueryResult]

}