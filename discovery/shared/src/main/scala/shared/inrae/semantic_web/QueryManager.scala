package inrae.semantic_web
import wvlet.log.Logger.rootLogger._

import inrae.semantic_web.internal._
import inrae.semantic_web.rdf._
import inrae.semantic_web.sparql.{QueryResult, _}


import scala.concurrent.{Future, Promise}
import scala.util._


object QueryManager {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   * Find source available in configuration for this node
   * @param n
   */

  def sparql_string(root: Root, n: Node): String = {
    val (refToIdentifier,_) = pm.SparqlGenerator.correspondenceVariablesIdentifier(n)
    SparqlQueryBuilder.queryString(root,refToIdentifier,refToIdentifier.values.toSeq,root.prefixes)
  }

  def queryPropertyNode(rootRequest : Node, n: Node, config : StatementConfiguration) : Future[Seq[URI]] = {
    Future {
      Seq[URI]()
    }
  }
  def queryAll(rootRequest : Root,
               config : StatementConfiguration) : Future[QueryResult] = {
    debug("queryAll")
    queryVariables(rootRequest,Node.references(rootRequest),config)
  }

  def countNbSolutions(root : Root,  config : StatementConfiguration) : Future[Int] = {
    debug("countNbSolutions")

    if (config.sources().length == 0) {
      throw new Exception(" ** None sources available ** ")
    } else if (config.sources().length == 1) {
      val source = config.sources()(0)
      val (refToIdentifier, _) = pm.SparqlGenerator.correspondenceVariablesIdentifier(root)
      val varCount = "count"
     // val prolog = pm.SparqlGenerator.prologCountSelection(varCount,refToIdentifier(Node.references(n)))
     val prolog = pm.SparqlGenerator.prologCountSelection(varCount)


      val query =
        pm.SparqlGenerator.prefixes(root.prefixes) +
        prolog +
        pm.SparqlGenerator.body(root, refToIdentifier) +
        pm.SparqlGenerator.solutionModifier()

      val res: Future[QueryResult] = QueryRunner(source).query(query)
      res.map(v => {
        SparqlBuilder.createLiteral(v.json("results")("bindings")(0)(varCount)).toInt()
      })
    } else {
      // todo query planner
      error("QueryPlanner is not available .")
      throw new Exception("not manage.......")
    }
  }

  def queryVariables(root: Root,
                     listVariables : Seq[String],
                     config : StatementConfiguration) : Future[QueryResult] = {
    debug("queryVariables")

    config.sources().length match {
      case 0 => {
        throw new Exception(" ** None sources available ** ")
      }
      case 1 => {
        val (refToIdentifier, _) = pm.SparqlGenerator.correspondenceVariablesIdentifier(root)
        info(refToIdentifier.toString())
        val query: String = SparqlQueryBuilder.queryString(root, refToIdentifier, listVariables, root.prefixes)
        QueryRunner(config.sources()(0)).query(query).map( qr => {
          println("FIN queryVariables ********************************************************************************")
          qr
        })
      }
      case _ => {
        val plan = QueryPlanner.buildPlanning(root) //,listVariables,config)
        val plan_results_set = QueryPlanner.ordonnanceBySource(plan, root)
        QueryPlannerExecutor.executePlanning(root, plan_results_set, listVariables, config, root.prefixes)
      }
    }
  }

  /**
   * Assign a list of source (existence of a remote persistence) if possible
   * @param n
   * @param source
   * @return
   */
  def setUpSourcesNode(n: Node,
                       config : StatementConfiguration,
                       prefixes : Map[String,IRI]): Future[Option[SourcesNode]] = {

    n match {
      case s : Something =>
        /* Something is Everywhere !! */
        Future {
          Some(SourcesNode(s.reference(),config.sources().map( _.id )))
        }
      case r : RdfNode =>
        val (refToIdentifier,_) = pm.SparqlGenerator.correspondenceVariablesIdentifier(n)
        val query = pm.SparqlGenerator.prefixes(prefixes) + "\n" +
          pm.SparqlGenerator.prologSourcesSelection() + "\n" +
          pm.SparqlGenerator.sparqlNode(r,refToIdentifier,"varUp","varCur") +
          pm.SparqlGenerator.solutionModifierSourcesSelection()

        debug(query)

        val nbRowResultsBySource : Seq[Future[Boolean]] = {
          config.sources().map(
              source => QueryRunner(source).query(query)
            ).map( {
              ( _.map(rr => rr.json("results")("bindings").arr.length>0))
            })
          }

        val y2 = Future.sequence(nbRowResultsBySource)
        val y3 = Promise[Option[SourcesNode]]()

        y2.onComplete {
          case Success(lCheck) => {
            y3 success Some(SourcesNode(r.reference(), lCheck.zip(config.sources()).filter( _._1).map( _._2.id)))
          }
          case msg => {
            System.err.println(msg)
            y3 success (None)
          }
        }

        y3.future

      case _ => Future(None)
    }
  }

  def process_datatypes(qr : QueryResult,
                        datatypeNode : DatatypeNode,
                        lUris : Seq[SparqlDefinition],
                        config : StatementConfiguration) = {
    println("lllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll")
    info(" -- process_datatypes --")
    val labelProperty = datatypeNode.property.reference()

    lUris.grouped(config.getInt("datatype_batch_processing")).toList.map(
      lSubUris => {
        info( " datatypes:"+lSubUris.toString )
        /* request using api */
        SW(config).something("val_uri")
          .setList(lSubUris.map(_ match { case uri: URI => uri }))
          .setupnode(datatypeNode.property, false, false)
          .select(List("val_uri", labelProperty))
          .map(json => {
            println("=====================================resultats datatype")
            println(json("results")("bindings").toString)
            qr.setDatatype(labelProperty, json("results")("bindings").arr.map(rec => {
              rec("val_uri")("value").value.toString -> rec(labelProperty)
            }).toMap)
          })
      })
  }

}