package io.ceratech.fcm

import scala.concurrent.Future

/**
  * Interface that defines the operations the underlaying token storage should provide
  *
  * @author dries
  */
trait TokenRepository {
  /**
    * Called when the FCM API notifies us of an outdated/unused token
    *
    * @param token the token that should be deleted
    * @return the completion of the operation
    */
  def deleteToken(token: String): Future[Unit]
}
