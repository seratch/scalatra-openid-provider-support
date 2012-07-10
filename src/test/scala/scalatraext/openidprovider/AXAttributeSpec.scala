package scalatraext.openidprovider

import org.scalatest._
import org.scalatest.matchers._

class AXAttributeSpec extends FlatSpec with ShouldMatchers {

  behavior of "AXAttribute"

  it should "accept null values" in {
    val alias: String = null
    val typeUri: String = null
    val value: String = null
    val instance = new AXAttribute(alias, typeUri, value)
    instance should not be null
  }

  it should "accept empty values" in {
    val alias: String = ""
    val typeUri: String = ""
    val value: String = ""
    val instance = new AXAttribute(alias, typeUri, value)
    instance should not be null
  }

  it should "accept normal values" in {
    val alias: String = "name"
    val typeUri: String = "http://www.example.com/openid/ax/name"
    val value: String = "Alice"
    val instance = new AXAttribute(alias, typeUri, value)
    instance should not be null
  }

}
