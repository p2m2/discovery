package EasySparql

import java.util.UUID.randomUUID

class SW( var config: SWConfig = null ) {
  
  /* root node */
  private var rootNode   : Root = new Root()
  /* focus node */
  private var focusNode  : Node = rootNode

  def print() : Unit = {
    println(" - SW -");
    println(" -- root --");
    pprint.pprintln(rootNode.children)
    println(" -- focusNode --");
    pprint.pprintln(focusNode.children)
  }

  /* manage the creation of an unique ref */
  def getUniqueRef() : String = randomUUID.toString

  /* set the current focus on the select node */
  def focus(ref : String) : SW = {
    var sn = new SelectNode();
    focusNode = sn.setFocus(ref, rootNode)(0)
   
    return this
  }

  def focusManagement(n : Node) : SW = {
    focusNode.addChildren(n) 
    /* current node is the focusNode */
    focusNode = n 
    return this
  }

  /* start a request */
  def something( ref : String = getUniqueRef() ) : SW = {
    val lastNode = new Something(ref)
    /* special case when "somthing" is used. become the focus */
    focusManagement(lastNode) 
  }

  /* create node which focus is the subject : ?focusId <uri> ?target */
  def isSubjectOf( uri : URI , ref : String = getUniqueRef() ) : SW = {
    val lastNode = new SubjectOf(ref,uri)
    focusManagement(lastNode)
  }


  /* create node which focus is the subject : ?focusId <uri> ?target */
  def isObjectOf( uri : URI , ref : String = getUniqueRef() ) : SW = {
    val lastNode = new ObjectOf(ref,uri)
    focusManagement(lastNode)
  }

  /* set */
  def set( uri : URI ) : SW = {
    val lastNode = new Value(uri)
    focusManagement(lastNode)
  }

  def debug() : SW = {
    var sc = new SimpleConsole();
    //println( pprint.tokenize(rootNode).mkString )
    //pprint.pprintln(rootNode.children)
    println("--focus--")
    pprint.pprintln(focusNode)
    pprint.pprintln(focusNode.children)
    //rootNode.accept(sc)
    println(sc.get(rootNode))
    return this
  }

  def sparql() : String = {
    var sg = new SparqlGenerator();
    return sg.body(config, rootNode)
  }

  def select() : Option[String] = {
    val sg = new SparqlGenerator()
    val query = sg.prolog(config, rootNode ) + sg.body(config, rootNode ) +sg.solutionModifier(config, rootNode)
    println(" ------------------------------- SPARQL ----------------------------- ")
    println(query)
    println(" ------------------------------- RESULT ----------------------------- ")
    
    
    val dbpediaRunner = QueryRunner("http://dbpedia.org/sparql")
    val result = dbpediaRunner
      .query(query)
    result.asJson()
    None : Option[String]
  }

}