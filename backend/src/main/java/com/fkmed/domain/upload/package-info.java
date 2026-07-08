/**
 * File-upload content-detection kernel (DL-0027, ADR-0022): the pure magic-byte sniffer every
 * module that accepts a JPG/PNG/PDF upload builds its own size/mandatory checks and domain
 * exceptions on. Mirrors {@code domain.error}'s minimal-kernel shape — no persistence, no Spring
 * beans, no business rules of its own (a module's size limit and required/optional matrix stay
 * module-specific).
 */
@org.springframework.modulith.ApplicationModule(displayName = "upload kernel")
package com.fkmed.domain.upload;
