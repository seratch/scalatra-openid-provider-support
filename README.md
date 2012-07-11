# Scalatra OpenID Provider Support

This is an extension to Scalatra to build OpenID Provider application with OpenID4Java.

## Scalatra 

Scalatra is a tiny, Sinatra-like web framework for Scala. Here are some contributed extensions to the library.

https://github.com/scalatra/scalatra

## OpenID4Java

OpenID4Java allows you to OpenID-enable your Java webapp.

http://code.google.com/p/openid4java/


## How to use

Added library dependency to sbt settings.

```scala
"com.github.seratch" %% "scalatra-openid-provider-support" % "2.0.0"
```

And then mix OpenIDProviderSupport to ScalatraServlet/ScalatraFilter.

The following is not complete code, just an image.

```scala
import scalatraext.openidprovider._

class MyServlet extends ScalatraServlet
  with OpenIDProviderSupport
  with AXSupport
  with Logging {

  protected override lazy val baseUrl = "http://localhost:8080"
  protected override lazy val endpointPath = "/auth"
  protected override lazy val identityPathPrefix = "/user/*"
  protected override lazy val confirmationParameterName = "confirmed"
  protected override lazy val isImmediateModeAllowed = true

  protected override def authenticatedIdentity(): Option[Identity] = {
    session.get("identity").map(id => Identity(id.toString))
  }

  protected override def authenticate(): Option[Identity] = {
    UserService.authenticate(params).map { case (id) =>
      Some("http://www.example.com/openid/" + id)
    }.getOrElse { halt(400, render("loginForm")) }
  }

  protected override def loginFormBodyOrRedirect(): Any = {
    render("login", "openidParams" -> parameterList.asJava)
    // or redirect to the login page with openid params
  }

  protected override def confirmationFormBody(): String = {
    render("confirm",
      "relam" -> parameterList.getParameter("openid.realm").getValue,
      "returnTo" -> parameterList.getParameter("openid.return_to").getValue,
      "openidParams" -> parameterList.asJava
    )
  }

  protected override def ax(identity: String): Seq[AXAttribute] = Seq(
    AXAttribute("username", "https://www.example.com/opeind/ax/username", "Alice"),
    AXAttribute("gender", "https://www.example.com/opeind/ax/gender", "Female")
  )

  get("/user/Alice") { status(200) } // identity url

}
```

## License

Apache License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.html

