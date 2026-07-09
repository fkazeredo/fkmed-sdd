/**
 * File-upload kernel: the shared magic-byte sniffer (DL-0027, ADR-0022) and provider-neutral
 * storage port (SPEC-0019, ADR-0023). Upload-owning modules keep their size, mandatory-category and
 * domain-error rules.
 */
@org.springframework.modulith.ApplicationModule(displayName = "upload kernel")
package com.fkmed.domain.upload;
