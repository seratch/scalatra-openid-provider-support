package scalatraext.openidprovider

import org.scalatest.matchers._

import org.scalatra.ScalatraServlet
import org.scalatra.test.scalatest.ScalatraFlatSpec

import com.weiglewilczek.slf4s.Logging

class OPServlet extends ScalatraServlet with OpenIDProviderSupport with Logging {

  protected override lazy val baseUrl: String = "http://localhost:8080"

  protected override lazy val identityPathPrefix: String = "/user/*"

  protected override lazy val confirmationParameterName = "confirmation"

  protected override def authenticatedIdentity(): Option[Identity] = params.get("loggedin") match {
    case Some("true") => Some(Identity("http://localhost:8080/user/Alice"))
    case _ => None
  }

  protected override def authenticate(): Option[Identity] = params.get("username") match {
    case Some(username) => Some(Identity("http://localhost:8080/user/" + username))
    case _ => None
  }

  protected override def loginFormBodyOrRedirect(): Any = "login form"

  protected override def confirmationFormBody(): String = "confirmation form"

}

class OPServletImmediateModeEnabled extends OPServlet {

  protected override lazy val isImmediateModeAllowed = true

}

class OpenIDProviderSupportSpec extends ScalatraFlatSpec with ShouldMatchers with Logging {

  addServlet(classOf[OPServlet], "/*")

  behavior of "OpenIDProviderSupport"

  // ---------------------
  // server discovery

  it should "accept discovery requests" in {
    get("/server.xrds") {
      status should equal(200)
      body should include("http://specs.openid.net/auth/2.0/server")
    }
  }

  // ---------------------
  // checkid_setup

  val checkIdPath = """/auth?openid.assoc_handle=1341803681958-0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete"""

  it should "accept checkid_setup requests and show login form" in {
    get(checkIdPath) {
      status should equal(200)
      body should equal("login form")
    }
    post(checkIdPath) {
      status should equal(200)
      body should equal("login form")
    }
    put(checkIdPath) {
      status should equal(405)
    }
  }

  it should "accept checkid_setup requests and show confirmation form" in {
    get(checkIdPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    post(checkIdPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    put(checkIdPath + "&loggedin=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being confirmed" in {
    get(checkIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    post(checkIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    put(checkIdPath + "&loggedin=true&confirmation=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being cancelled" in {
    get(checkIdPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    post(checkIdPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    put(checkIdPath + "&loggedin=true&confirmation=false") {
      status should equal(405)
    }
  }

  // ---------------------
  // checkid_immediate

  val checkIdImmedatePath = """/auth?openid.assoc_handle=1341803681958-0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_immediate&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete"""

  it should "reject checkid_immediate by default" in {
    get(checkIdImmedatePath + "&loggedin=true&confirmation=false") {
      // by default, immediate mode is disabled
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=setup_needed")
    }
  }

  addServlet(classOf[OPServletImmediateModeEnabled], "/immediate/*")

  it should "respond checkid_immediate" in {
    get("/immediate" + checkIdImmedatePath + "&loggedin=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=setup_needed")
    }
    get("/immediate" + checkIdImmedatePath + "&loggedin=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
  }

  // ---------------------
  // identity verifying

  it should "accept id discovery requests" in {
    get("/signon.xrds") {
      status should equal(200)
      body should include("http://specs.openid.net/auth/2.0/signon")
    }
  }

}

