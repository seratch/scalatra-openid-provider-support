package scalatraext.openidprovider

import org.scalatest.matchers._
import org.scalatra.test.scalatest.ScalatraFlatSpec

class OPSRegServlet extends OPServlet with SRegSupport {

  protected override def sreg(id: String): Seq[SRegAttribute] = Seq(
    SRegAttribute("nickname", "Alice")
  )

}

class OPSRegServletImmediateModeEnabled extends OPSRegServlet {

  protected override lazy val isImmediateModeAllowed = true

}

class SRegSupportSpec extends ScalatraFlatSpec with ShouldMatchers with Logging {

  addServlet(classOf[OPSRegServlet], "/*")

  behavior of "SRegSupport"

  // ---------------------
  // server discovery

  it should "accept discovery requests" in {
    get("/server.xrds") {
      status should equal(200)
      body should include("http://specs.openid.net/auth/2.0/server")
      body should include("http://openid.net/sreg/1.0")
    }
  }

  // ---------------------
  // checkid_setup

  val chechIdPath = """/auth?openid.assoc_handle=1341803681958-0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete"""

  val chechIdWithSRegPath = """/auth?openid.assoc_handle=1341803681958-0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete&openid.sreg.required=nickname,email"""

  it should "accept checkid_setup requests and show login form" in {
    get(chechIdWithSRegPath) {
      status should equal(200)
      body should equal("login form")
    }
    post(chechIdWithSRegPath) {
      status should equal(200)
      body should equal("login form")
    }
    put(chechIdWithSRegPath) {
      status should equal(405)
    }
  }

  it should "accept checkid_setup requests and show confirmation form" in {
    get(chechIdWithSRegPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    post(chechIdWithSRegPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    put(chechIdWithSRegPath + "&loggedin=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being confirmed" in {
    get(chechIdWithSRegPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    post(chechIdWithSRegPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    put(chechIdWithSRegPath + "&loggedin=true&confirmation=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being cancelled" in {
    get(chechIdWithSRegPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    post(chechIdWithSRegPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    put(chechIdWithSRegPath + "&loggedin=true&confirmation=false") {
      status should equal(405)
    }
  }

  it should "append atrributes when it's needed." in {
    get(chechIdWithSRegPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.sreg.nickname=Alice")
    }
    post(chechIdWithSRegPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.sreg.nickname=Alice")
    }
  }

  it should "never append atrributes when it's not needed." in {
    get(chechIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should not include ("openid.sreg.nickname=Alice")
    }
    post(chechIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should not include ("openid.sreg.nickname=Alice")
    }
  }

  // ---------------------
  // identity discovery

  it should "accept id discovery requests" in {
    get("/signon.xrds") {
      status should equal(200)
      body should include("http://specs.openid.net/auth/2.0/signon")
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

  addServlet(classOf[OPSRegServletImmediateModeEnabled], "/immediate/*")

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

}
