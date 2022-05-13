package fr.inrae.metabohub.semantic_web.exception

final case class SWDiscoveryException(private val message: String = "",
                                      private val cause: Throwable = None.orNull) extends Exception(message,cause)