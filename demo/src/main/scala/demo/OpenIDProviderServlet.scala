package demo

import org.scalatra._
import org.openid4java.message._
import scalatraext.openidprovider._
import scalatraext.thymeleaf.ThymeleafSupport
import inputvalidator._

class OpenIDProviderServlet
    extends ScalatraServlet
    with OpenIDProviderSupport
    with AXSupport
    with ThymeleafSupport
    with Logging {

  protected override lazy val baseUrl: String = "http://localhost:8080"

  protected override lazy val identityPathPrefix: String = "/user/*"

  protected override lazy val isImmediateModeAllowed = true

  get("/user/Alice") {
    status = 200
  }

  get("/") {
    render("index")
  }

  protected override def authenticatedIdentity(): Option[Identity] = {
    session.get("identity").map(id => Identity(id.toString))
  }

  protected override def authenticate(): Option[Identity] = {
    // validation
    Validator(params)(
      inputKey("username") is required,
      inputKey("password") is required
    ).failure { (inputs, errors) =>
        // shows login form
        halt(400, render("auth/login",
          "username" -> inputs.getOrElse("username", ""),
          "password" -> inputs.getOrElse("password", ""),
          "openidParams" -> parameterList.asJava,
          "errors" -> inputs.toMap.flatMap {
            case (key, _) => errors.get(key).headOption.map { error =>
              val msg = Messages.get(key = error.name, params = key :: error.messageParams.toList)
                .getOrElse(error.name)
              (key, msg)
            }
          }
        ))
      }.success { inputs =>
        val username = inputs.getOrElse("username", "")
        val password = inputs.getOrElse("password", "")
        if ((username.equals("Alice") && password.equals("Bob"))) {
          session.setAttribute("identity", baseUrl + "/user/Alice")
          Some(Identity(baseUrl + "/user/Alice"))
        } else {
          // shows login form
          halt(400, render("auth/login",
            "username" -> username,
            "password" -> password,
            "openidParams" -> parameterList.asJava,
            "errors" -> Map("global" -> "username or password is invalid.")))
        }
      }.apply()
  }

  protected override def loginFormBodyOrRedirect(): Any = {
    render("auth/login",
      "username" -> "",
      "password" -> "",
      "errors" -> Map(),
      "openidParams" -> parameterList.asJava
    )
  }

  protected override def confirmationFormBody(): String = {
    render("auth/confirm",
      "relam" -> parameterList.getParameter("openid.realm").getValue,
      "openidParams" -> parameterList.asJava
    )
  }

  // provides AX 
  protected override def ax(identity: String): Seq[AXAttribute] = Seq(
    AXAttribute("username", "https://www.example.com/opeind/ax/username", "Alice"),
    AXAttribute("gender", "https://www.example.com/opeind/ax/gender", "Female")
  )

}
