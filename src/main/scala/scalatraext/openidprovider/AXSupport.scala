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

import org.scalatra.ScalatraBase
import org.openid4java.message._
import org.openid4java.message.ax.FetchResponse

/**
 * Attribute Exchange Support
 *
 * @see http://openid.net/specs/openid-attribute-exchange-1_0.html
 */
trait AXSupport {

  this: ScalatraBase with OpenIDProviderSupport =>

  logger.debug("AXSupport is enabled.")

  serverXrdsTypes.append("http://openid.net/srv/ax/1.0")

  protected def ax(identity: String): Seq[AXAttribute] = Seq()

  checkIdFunctions.append((id: String, claimedId: String, authed: Boolean, msg: Option[Message]) => {
    msg.getOrElse(openid4java.authResponse(parameterList, id, claimedId, authed)) match {
      case successMessage: AuthSuccess =>
        params.get("openid.ax.mode") match {
          case Some("fetch_request") =>
            val fetchResponse = FetchResponse.createFetchResponse()
            ax(successMessage.getIdentity) foreach {
              attr => fetchResponse.addAttribute(attr.alias, attr.typeUri, attr.value)
            }
            successMessage.addExtension(fetchResponse)
            openid4java.sign(successMessage)
            successMessage
          case _ => successMessage
        }
      case negativeMessage => negativeMessage
    }
  })

}

