/*
 * Copyright 2012 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalatraext.openidprovider

import scala.collection.JavaConverters._
import scala.collection.mutable
import org.openid4java.server._
import org.openid4java.message._
import org.scalatra.ScalatraBase
import scala.Option

/**
 * OpenID Authentication 2.0 Provider implementation
 *
 * @see http://openid.net/specs/openid-authentication-2_0.html
 */
trait OpenIDProviderSupport extends Logging {

  this: ScalatraBase =>

  protected lazy val htmlContentType: String = "text/html; charset=utf-8"

  protected lazy val xrdsContentType: String = "application/xrds+xml"

  protected lazy val confirmationParameterName: String = "confirmed"

  protected lazy val methodNotAllowedBody: String = "<html><body><h1>405 Method Not Allowed</h1></body></html>"

  protected lazy val badRequestBody: String = "<html><body><h1>400 Bad Request</h1></body></html>"

  // ------------------------
  // URL / Path Settings
  // ------------------------

  protected lazy val baseUrl: String = "http://localhost:8080"

  protected lazy val endpointPath: String = "/auth"

  protected lazy val serverXrdsPath: String = "/server.xrds"

  protected lazy val signonXrdsPath: String = "/signon.xrds"

  // ------------------------
  // OpenID4Java settings
  // ------------------------

  protected lazy val openid4java: ServerManager = {
    val manager = new ServerManager
    manager.setOPEndpointUrl(baseUrl + endpointPath)
    manager.setUserSetupUrl(baseUrl + endpointPath) // for immedate mode failure
    manager.setPrivateAssociations(privateAssociations)
    manager.setSharedAssociations(sharedAssociations)
    manager.setEnforceRpId(isRPDiscoveryEnabled)
    logger.debug("OpenID4Java ServerManager is initialized. (EndpointUrl: " + manager.getOPEndpointUrl +
      ", PrivateAssociations: " + privateAssociations + ", SharedAssociations: " + sharedAssociations +
      ", RPDiscoveryEnabled: " + isRPDiscoveryEnabled + ")"
    )
    manager
  }

  protected lazy val isRPDiscoveryEnabled: Boolean = false

  protected lazy val isImmediateModeAllowed: Boolean = false

  protected lazy val privateAssociations: ServerAssociationStore = new InMemoryServerAssociationStore

  protected lazy val sharedAssociations: ServerAssociationStore = new InMemoryServerAssociationStore

  // ------------------------
  // OpenID4Java ParameterList
  // ------------------------

  case class ParameterListHasAsJavaAndAsScala(l: ParameterList) {
    def asJava: java.util.List[Parameter] = l.getParameters.asInstanceOf[java.util.List[Parameter]]

    def asScala: Seq[Parameter] = l.getParameters.asInstanceOf[java.util.List[Parameter]].asScala
  }

  implicit def toParameterListHasAsJavaAndAsScala(l: ParameterList) = ParameterListHasAsJavaAndAsScala(l)

  protected def parameterList(): ParameterList = new ParameterList(params.toMap.asJava)

  // ------------------------
  // Setup Processing
  // ------------------------

  protected def authenticate(): Option[Identity]

  protected def authenticatedIdentity(): Option[Identity]

  protected def loginFormBodyOrRedirect(): Any

  protected def confirmationFormBody(): String

  // ------------------------
  // Identity Discovery
  // ------------------------

  protected lazy val identityPathPrefix: String = "/user/*"

  // ------------------------
  // Discovery
  // ------------------------

  protected lazy val serverXrdsTypes: mutable.ListBuffer[String] = mutable.ListBuffer("http://specs.openid.net/auth/2.0/server")

  protected lazy val serverXrdsBody: String = {
    """<?xml version="1.0" encoding="UTF-8"?>
      |<xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      |  <XRD>
      |    <Service priority="0">
      |{types}
      |      <URI>{endpoint}</URI>
      |    </Service>
      |  </XRD>
      |</xrds:XRDS>"""
      .stripMargin
      .replaceFirst("\\{endpoint\\}", baseUrl + endpointPath)
      .replaceFirst("\\{types\\}", serverXrdsTypes.map(t => "      <Type>" + t + "</Type>").mkString("\n"))
  }

  protected lazy val signonXrdsBody: String = {
    """<?xml version="1.0" encoding="UTF-8"?>
      |<xrds:XRDS
      |    xmlns:xrds="xri://$xrds"
      |    xmlns:openid="http://openid.net/xmlns/1.0"
      |    xmlns="xri://$xrd*($v*2.0)">
      |  <XRD>
      |    <Service priority="0">
      |      <Type>http://specs.openid.net/auth/2.0/signon</Type>
      |      <URI>{endpoint}</URI>
      |    </Service>
      |  </XRD>
      |</xrds:XRDS>""".stripMargin.replaceFirst("\\{endpoint\\}", baseUrl + endpointPath)
  }

  // ------------------------
  // Public API
  // ------------------------

  before() {
    response.setHeader("X-XRDS-Location", baseUrl + serverXrdsPath)
    response.setHeader("Connection", "close")
  }

  /**
   * Discovery
   *
   * @see http://openid.net/specs/openid-authentication-2_0.html#discovery
   */
  get(serverXrdsPath) {
    contentType = xrdsContentType
    logger.debug(serverXrdsPath + "\n" + serverXrdsBody)
    serverXrdsBody
  }

  get(signonXrdsPath) {
    contentType = xrdsContentType
    logger.debug(signonXrdsPath + "\n" + signonXrdsBody)
    signonXrdsBody
  }

  /**
   * 8.  Establishing Associations
   * 11.  Verifying Assertions
   */
  post(endpointPath) {
    params.get("openid.mode") map {
      case AssociationRequest.MODE_ASSOC => associationBody()
      case AuthRequest.MODE_SETUP => checkIdSetup()
      case AuthRequest.MODE_IMMEDIATE => checkIdImmediate()
      case VerifyRequest.MODE_CHKAUTH => verifyAuthBody()
      case _ => badRequest()
    } getOrElse badRequest()
  }

  /**
   * 9.  Requesting Authentication
   * 10.  Responding to Authentication Requests
   */
  get(endpointPath) {
    params.get("openid.mode") map {
      case AssociationRequest.MODE_ASSOC => methodNotAllowed()
      case AuthRequest.MODE_SETUP => acceptCheckIdSetup()
      case AuthRequest.MODE_IMMEDIATE => checkIdImmediate()
      case VerifyRequest.MODE_CHKAUTH => methodNotAllowed()
      case _ => badRequest()
    } getOrElse badRequest()
  }

  /**
   * 11.2.  Verifying Discovered Information
   */
  before(identityPathPrefix.r) {
    response.setHeader("X-XRDS-Location", baseUrl + signonXrdsPath)
  }

  // ------------------------
  // Handler
  // ------------------------

  protected def methodNotAllowed(): Unit = {
    contentType = htmlContentType
    halt(status = 405, body = methodNotAllowed)
  }

  protected def badRequest(): Unit = {
    contentType = htmlContentType
    halt(status = 400, body = badRequestBody)
  }

  protected def associationBody(): String = {
    val response = openid4java.associationResponse(parameterList).keyValueFormEncoding
    logger.debug("Association: " + response)
    response
  }

  protected def verifyAuthBody(): String = {
    val response = openid4java.verify(parameterList).keyValueFormEncoding
    logger.debug("Verify: " + response)
    response
  }

  protected def acceptCheckIdSetup(): Any = {
    checkIdSetup(false)
  }

  protected def checkIdSetup(loginSubmitted: Boolean = true): Any = {
    // already logged in
    authenticatedIdentity().orElse {
      logger.debug("Authenticated identity is not found.")
      loginSubmitted match {
        case true =>
          // login request
          val result = authenticate()
          logger.debug("Authentication result: " + result)
          result
        case false => None
      }
    }.map {
      case Identity(id) =>
        // user confirmation
        params.get(confirmationParameterName).map {
          confirmed =>
            // confirmed or cancelled
            logger.debug("Confirmation: (id: " + id + ", confirmed: " + confirmed + ")")
            val message = checkId(id, id, confirmed.toBoolean)
            redirect(message.getDestinationUrl(true))
        }.getOrElse(confirmationFormBody())
    }.getOrElse(loginFormBodyOrRedirect())
  }

  protected def checkIdImmediate(): Unit = {
    val message = authenticatedIdentity().map {
      case Identity(id) =>
        if (!isImmediateModeAllowed) {
          logger.debug("Immediate mode is not allowed. (id: " + id + ")")
        }
        checkId(id, id, isImmediateModeAllowed)
    }.getOrElse {
      val id = params.get("openid.claimed_id").get
      checkId(id, id, false) // setup_needed
    }
    redirect(message.getDestinationUrl(true))
  }

  protected def checkId(userSelectedId: String, userSelelctedClaimedId: String, authenticatedAndApproved: Boolean): Message = {
    checkIdFunctions.foldLeft(Option[Message](null)) {
      case (msg, checkIdFunction) =>
        val result = checkIdFunction.apply(userSelectedId, userSelelctedClaimedId, authenticatedAndApproved, msg)
        Option(result)
    }.getOrElse(null)
  }

  protected val checkIdFunctions: mutable.ListBuffer[(String, String, Boolean, Option[Message]) => Message] = new mutable.ListBuffer

  checkIdFunctions.append((id: String, claimedId: String, authed: Boolean, msg: Option[Message]) => {
    logger.debug("CheckId: (userSelectedId: " + id + ", userSelelctedClaimedId: " + claimedId +
      ", authenticatedAndApproved: " + authed + ")")
    msg.getOrElse(openid4java.authResponse(parameterList, id, claimedId, authed))
  })

}

