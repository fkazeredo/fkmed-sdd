package com.fkmed.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as internal to its business module (DECISIONS-BASELINE §0016).
 *
 * <p>Annotated types must only be accessed from the same module (same flat package) or from the
 * centralized {@code com.fkmed.infra} layer. Enforced by an ArchUnit rule with a teeth test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInternal {}
