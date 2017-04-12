package atlas.commands;

public @interface CommandCallback {

	String command();
	String[] aliases() default {};
	String[] permissions() default {};
	String[] tab() default {};
}
