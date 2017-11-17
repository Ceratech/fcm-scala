package io.ceratech.fcm.helpers

import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.argThat
import play.api.libs.json.JsObject

class JsObjectMatcher(check: JsObject ⇒ Unit) extends ArgumentMatcher[JsObject] {
  override def matches(argument: JsObject): Boolean = {
    check(argument)
    true // The check should fail with assertions
  }
}

object JsObjectMatcher {
  def jsMatches(check: JsObject ⇒ Unit): JsObject = argThat(new JsObjectMatcher(check))
}