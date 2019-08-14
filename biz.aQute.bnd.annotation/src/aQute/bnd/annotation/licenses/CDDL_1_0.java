package aQute.bnd.annotation.licenses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the Common Development and
 * Distribution license. Applying this annotation will add a Bundle-License
 * clause.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "CDDL-1.0", link = "https://opensource.org/licenses/CDDL-1.0", description = "Common Development and Distribution License 1.0")
public @interface CDDL_1_0 {}
