package atlas.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandCallback {

	String command();
	String[] aliases() default {};
	String[] permissions() default {};
	String[] tab() default {};
}
