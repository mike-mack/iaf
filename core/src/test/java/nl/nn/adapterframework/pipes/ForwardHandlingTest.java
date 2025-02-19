package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.PipeForward;

public class ForwardHandlingTest extends ConfiguredTestBase {


	@Override
	public void setUp() throws Exception {
		super.setUp();
		ApplicationWarnings.removeInstance();
	}

	@Test
	public void testFindForwardToPipeExplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipe1.registerForward(new PipeForward("fakeForward", "pipe3"));
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe1.findForward("fakeForward");
		assertEquals("pipe3", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

	@Test
	public void testFindForwardToPipeImplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe1.findForward("pipe2");
		assertEquals("pipe2", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

	@Test
	public void testFindForwardToNextPipeImplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe2.findForward("success");
		assertEquals("pipe3", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}


	@Test
	public void testFindForwardToExitExplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipe1.registerForward(new PipeForward("ready", "READY"));
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		configureAdapter();

		PipeForward forward = pipe1.findForward("ready");
		assertEquals("READY", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}


	@Test
	public void testFindForwardToExitImplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		configureAdapter();

		PipeForward forward = pipe2.findForward("success");
		assertEquals("READY", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

}
