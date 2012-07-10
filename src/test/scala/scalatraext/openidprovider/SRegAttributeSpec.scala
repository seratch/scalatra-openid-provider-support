package scalatraext.openidprovider

import org.scalatest._
import org.scalatest.matchers._

class SRegAttributeSpec extends FlatSpec with ShouldMatchers {

  behavior of "SRegAttribute"

  it should "accept null values" in {
    val name: String = ""
    val value: String = ""
    val instance = new SRegAttribute(name, value)
    instance should not be null
  }

  it should "accept empty values" in {
    val name: String = ""
    val value: String = ""
    val instance = new SRegAttribute(name, value)
    instance should not be null
  }

  it should "accept values" in {
    val name: String = "nickname"
    val value: String = "Alice"
    val instance = new SRegAttribute(name, value)
    instance should not be null
  }

}
