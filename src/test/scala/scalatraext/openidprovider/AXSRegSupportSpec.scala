package scalatraext.openidprovider

import org.scalatest.matchers._

import org.scalatra.test.scalatest.ScalatraFlatSpec

class OPAXSRegServlet extends OPServlet with AXSupport with SRegSupport {

  protected override def ax(id: String): Seq[AXAttribute] = Seq(
    AXAttribute("username", "https://www.m3.com/opeind/ax/username", "Alice")
  )

  protected override def sreg(id: String): Seq[SRegAttribute] = Seq(
    SRegAttribute("nickname", "Alice")
  )

}

class OPAXSRegServletImmediateModeEnabled extends OPAXSRegServlet {

  protected override lazy val isImmediateModeAllowed = true

}

class AXSRegSupportSpec extends ScalatraFlatSpec with ShouldMatchers with Logging {

  addServlet(classOf[OPAXSRegServlet], "/*")

  behavior of "OPAXSRegServlet"

  // ---------------------
  // server discovery

  it should "accept discovery requests" in {
    get("/server.xrds") {
      status should equal(200)
      body should include("http://specs.openid.net/auth/2.0/server")
      body should include("http://openid.net/srv/ax/1.0")
      body should include("http://openid.net/sreg/1.0")
    }
  }

  // ---------------------
  // checkid_setup

  val chechIdPath = """/auth?openid.assoc_handle=1341803681958-0&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete"""

  val chechIdWithSRegAXPath = """/auth?openid.assoc_handle=1341803681958-1&openid.ax.count.website=2&openid.ax.if_available=fullname%2Cwebsite%2Cpostcode%2Cgender%2Cbirth_date%2Ccountry%2Clanguage%2Ctimezone&openid.ax.mode=fetch_request&openid.ax.required=uid%2Cnickname%2Cemail&openid.ax.type.birth_date=http%3A%2F%2Faxschema.org%2FbirthDate&openid.ax.type.country=http%3A%2F%2Faxschema.org%2Fcontact%2Fcountry%2Fhome&openid.ax.type.email=http%3A%2F%2Faxschema.org%2Fcontact%2Femail&openid.ax.type.fullname=http%3A%2F%2Faxschema.org%2FnamePerson&openid.ax.type.gender=http%3A%2F%2Faxschema.org%2Fperson%2Fgender&openid.ax.type.language=http%3A%2F%2Faxschema.org%2Fpref%2Flanguage&openid.ax.type.nickname=http%3A%2F%2Faxschema.org%2FnamePerson%2Ffriendly&openid.ax.type.postcode=http%3A%2F%2Faxschema.org%2Fcontact%2FpostalCode%2Fhome&openid.ax.type.timezone=http%3A%2F%2Faxschema.org%2Fpref%2Ftimezone&openid.ax.type.uid=https%3A%2F%2Fopenid.tzi.de%2Fspec%2Fschema&openid.ax.type.website=http%3A%2F%2Faxschema.org%2Fcontact%2Fweb%2Fdefault&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.ns.ax=http%3A%2F%2Fopenid.net%2Fsrv%2Fax%2F1.0&openid.realm=http%3A%2F%2Flocalhost%3A3000%2Fconsumer&openid.return_to=http%3A%2F%2Flocalhost%3A3000%2Fconsumer%2Fcomplete%3Fdid_ax_fetch%3Dy&openid.sreg.required=nickname,email"""

  it should "accept checkid_setup requests and show login form" in {
    get(chechIdWithSRegAXPath) {
      status should equal(200)
      body should equal("login form")
    }
    post(chechIdWithSRegAXPath) {
      status should equal(200)
      body should equal("login form")
    }
    put(chechIdWithSRegAXPath) {
      status should equal(405)
    }
  }

  it should "accept checkid_setup requests and show confirmation form" in {
    get(chechIdWithSRegAXPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    post(chechIdWithSRegAXPath + "&loggedin=true") {
      status should equal(200)
      body should equal("confirmation form")
    }
    put(chechIdWithSRegAXPath + "&loggedin=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being confirmed" in {
    get(chechIdWithSRegAXPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    post(chechIdWithSRegAXPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=id_res")
    }
    put(chechIdWithSRegAXPath + "&loggedin=true&confirmation=true") {
      status should equal(405)
    }
  }

  it should "respond checkid_setup when being cancelled" in {
    get(chechIdWithSRegAXPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    post(chechIdWithSRegAXPath + "&loggedin=true&confirmation=false") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.mode=cancel")
    }
    put(chechIdWithSRegAXPath + "&loggedin=true&confirmation=false") {
      status should equal(405)
    }
  }

  it should "append atrributes when it's needed." in {
    get(chechIdWithSRegAXPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.ext1")
    }
    post(chechIdWithSRegAXPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should include("openid.ext1")
    }
  }

  it should "never append atrributes when it's not needed." in {
    get(chechIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should not include ("openid.ext1")
    }
    post(chechIdPath + "&loggedin=true&confirmation=true") {
      status should equal(302)
      logger.info("Location: " + header("Location"))
      header("Location") should startWith("http://localhost:3000/consumer/complete?")
      header("Location") should not include ("openid.ext1")
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

  addServlet(classOf[OPAXServletImmediateModeEnabled], "/immediate/*")

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
