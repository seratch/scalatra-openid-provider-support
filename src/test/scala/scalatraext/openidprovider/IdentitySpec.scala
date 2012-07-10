package scalatraext.openidprovider

import org.scalatest._
import org.scalatest.matchers._

class IdentitySpec extends FlatSpec with ShouldMatchers {

  behavior of "Identity"

  it should "be available" in {
    Identity(null) should not be null
    Identity("") should not be null
    Identity("http://www.example.com/openid/ax/alice") should not be null
  }

}
