package de.cgawron.go.gtp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an GTP command
 * 
 * @author Christian Gawron
 * 
 */
public @Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface GtpCmd {
}
