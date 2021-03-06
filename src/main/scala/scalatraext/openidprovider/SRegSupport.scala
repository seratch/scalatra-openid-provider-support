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

import org.scalatra.ScalatraKernel
import org.openid4java.message._
import com.weiglewilczek.slf4s.Logging
import sreg.SRegResponse

/**
 * SReg Support
 *
 * @see http://openid.net/specs/openid-simple-registration-extension-1_0.html
 */
trait SRegSupport {

  self: ScalatraKernel with OpenIDProviderSupport with Logging =>

  logger.debug("SRegSupport is enabled.")

  serverXrdsTypes.append("http://openid.net/sreg/1.0")

  protected def sreg(identity: String): Seq[SRegAttribute] = Seq()

  checkIdFunctions.append((id: String, claimedId: String, authed: Boolean, msg: Option[Message]) => {
    msg.getOrElse(openid4java.authResponse(parameterList, id, claimedId, authed)) match {
      case successMessage: AuthSuccess =>
        params.get("openid.sreg.required") match {
          case Some(requiredCsv) =>
            val fetchResponse = SRegResponse.createFetchResponse()
            sreg(successMessage.getIdentity) foreach {
              attr => fetchResponse.addAttribute(attr.name, attr.value)
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

