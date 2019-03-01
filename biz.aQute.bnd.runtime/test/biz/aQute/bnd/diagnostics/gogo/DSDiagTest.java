package biz.aQute.bnd.diagnostics.gogo;

import org.junit.Test;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Launchpad;

public class DSDiagTest {

	LaunchpadBuilder	builder	= new LaunchpadBuilder().bndrun("run.bndrun");
	Launchpad			fw		= builder.create();

	interface IA {}

	interface IB {}

	interface IC {}

	interface ID {}

	interface IE {}

	@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
	public static class A implements IA {}

	@Component
	public static class B implements IB {}

	@Component
	public static class C implements IC {
		@Reference
		IA	a;
		@Reference
		IB	b;
	}

	@Component
	public static class D implements ID {
		@Reference
		IC c;
	}

	@Component
	public static class E implements IE {
		@Reference
		IA	a;
		@Reference
		IB	b;
		@Reference
		IC	c;
	}

	@Test
	public void testSimple() throws InterruptedException {
		fw.bundle()
			.addResource(A.class)
			.addResource(B.class)
			.addResource(C.class)
			.addResource(D.class)
			.addResource(E.class)
			.start();
	}
}
