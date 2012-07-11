import org.openid4java.server.ServerAssociationStore
import org.openid4java.association.{ AssociationException, Association }
import util.Random
import org.apache.commons.codec.binary.Base64
import org.joda.time._
import com.weiglewilczek.slf4s.Logging

object DatabaseServerAssociationStore extends ServerAssociationStore with Logging {

  // 1 min in millis
  private val CLEANUP_INTERVAL = 60 * 1000;

  private var lastCleanup: Long = 0L

  private val random = new Random(System.currentTimeMillis)

  private def cleanupExpired(): Unit = {
    if (System.currentTimeMillis - lastCleanup >= CLEANUP_INTERVAL) {
      logger.debug("Cleanup expired associations...")
      SeverAssociation.deleteExpired()
      lastCleanup = System.currentTimeMillis
    }
  }

  override def generate(_type: String, expiryIn: Int): Association = {
    cleanupExpired()
    var attemptsLeft = 5
    while (attemptsLeft > 0) {
      try {
        val handle = java.lang.Long.toHexString(random.nextLong)
        val association = Association.generate(_type, handle, expiryIn)
        SeverAssociation.create(handle = association.getHandle,
          _type = association.getType,
          mackey = new String(Base64.encodeBase64(association.getMacKey.getEncoded)),
          datetimeToExpire = new DateTime(association.getExpiry)
        )
        return association
      } catch {
        case e =>
          logger.debug("Failed to create a new association..", e)
          attemptsLeft = attemptsLeft - 1
      }
    }
    throw new AssociationException("JDBCServerAssociationStore: Error generating association.")
  }

  override def load(handle: String): Association = {
    SeverAssociation.findByHandle(handle) match {
      case Some(as) => as._type match {
        case Association.TYPE_HMAC_SHA1 =>
          Association.createHmacSha1(handle, Base64.decodeBase64(as.macKey.getBytes()), as.datetimeToExpire.toDate)
        case Association.TYPE_HMAC_SHA256 =>
          Association.createHmacSha256(handle, Base64.decodeBase64(as.macKey.getBytes()), as.datetimeToExpire.toDate)
        case _ =>
          logger.info("Invalid association type retrieved from database: " + as._type)
          null
      }
      case _ =>
        logger.info("Invalid association handle retrieved from database: " + handle)
        null
    }
  }

  override def remove(handle: String): Unit = {
    SeverAssociation.deleteByHandle(handle)

  }
}


