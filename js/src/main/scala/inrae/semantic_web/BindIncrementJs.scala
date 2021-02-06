package inrae.semantic_web

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("BindIncrement")
case class BindIncrementJs(swf: SWDiscoveryJs,`var` : String) {

  @JSExport
  def subStr(startingLoc : Any,length : Any ) : SWDiscoveryJs =
    SWDiscoveryJs(swf.config,swf.sw.bind(`var`).subStr(startingLoc, length))

  @JSExport
  def regex(pattern : Any, flags : Any="") :  SWDiscoveryJs =
    SWDiscoveryJs(swf.config,swf.sw.bind(`var`).regex(pattern,flags))

  @JSExport
  def replace(pattern : Any, replacement : Any, flags : Any="") :  SWDiscoveryJs =
    SWDiscoveryJs(swf.config,swf.sw.bind(`var`).replace(pattern, replacement,flags))

  @JSExport
  def abs() :  SWDiscoveryJs = SWDiscoveryJs(swf.config,swf.sw.bind(`var`).abs())

  @JSExport
  def round() :  SWDiscoveryJs = SWDiscoveryJs(swf.config,swf.sw.bind(`var`).round())

  @JSExport
  def ceil() :  SWDiscoveryJs = SWDiscoveryJs(swf.config,swf.sw.bind(`var`).ceil())

  @JSExport
  def floor() :  SWDiscoveryJs = SWDiscoveryJs(swf.config,swf.sw.bind(`var`).floor())

  @JSExport
  def rand() :  SWDiscoveryJs = SWDiscoveryJs(swf.config,swf.sw.bind(`var`).rand())

}
