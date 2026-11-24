package com.example.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object PinHashUtil {
  private const val SALT = "study_guard_accountability_pin_v1"

  fun isValidFourDigitPin(pin: String): Boolean =
    pin.length == 4 && pin.all { it.isDigit() }

  fun hashPin(pin: String): String {
    require(isValidFourDigitPin(pin)) { "PIN must be exactly 4 digits." }
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$SALT:$pin".toByteArray(StandardCharsets.UTF_8))
    return bytes.joinToString(separator = "") { "%02x".format(it) }
  }

  fun verifyPin(pin: String, storedHash: String): Boolean {
    if (!isValidFourDigitPin(pin) || storedHash.isBlank()) return false
    val candidate = hashPin(pin).toByteArray(StandardCharsets.UTF_8)
    val expected = storedHash.toByteArray(StandardCharsets.UTF_8)
    return MessageDigest.isEqual(candidate, expected)
  }
}
