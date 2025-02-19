package nl.nn.adapterframework.validation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.soap.WsdlGeneratorUtils;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.util.StreamUtil;

public class XSDTest {

	private IScopeProvider scopeProvider = new TestScopeProvider();

	@Test
	public void xsdName() throws Exception {
		XSD xsd = new XSD();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("XSDTest/v1 test.xsd", xsd.getResourceTarget());
	}

	@Test
	public void xsdNamespace() throws Exception {
		XSD xsd = new XSD();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/v1 test.xsd");
		assertEquals("http://test", xsd.getNamespace());
		assertEquals("http://www.ing.com/pim", xsd.getTargetNamespace());
	}

	@Test
	public void writeXSD() throws Exception {
		XSD xsd = new XSD();
		xsd.initNamespace("http://test", scopeProvider, "XSDTest/test.xsd");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlGeneratorUtils.getWriter(out, false);
		SchemaUtils.xsdToXmlStreamWriter(xsd, writer);

		String result = new String(out.toByteArray());
		String expected = TestFileUtils.getTestFile("/XSDTest/test.xsd");

		MatchUtils.assertXmlEquals("expected xml (XSDTest/test_expected.xsd) not similar to result xml:\n" + new String(out.toByteArray()), expected, result);
	}

	public void testAddNamespacesToSchema(String schemaLocation, String expectedSchemaLocation) throws ConfigurationException, IOException {

		String expectedSchema = TestFileUtils.getTestFile(expectedSchemaLocation.trim().split("\\s+")[1]);

		XSD xsd = getXSD(schemaLocation);
		xsd.setAddNamespaceToSchema(true);

		xsd.addTargetNamespace();
		String actual = StreamUtil.streamToString(xsd.getInputStream(), null, null);
		MatchUtils.assertXmlEquals(expectedSchema, actual);
	}

	@Test
	public void testAddNamespacesToSchemaNoop() throws ConfigurationException, IOException {
		testAddNamespacesToSchema(ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK+"-after-adding-namespace.xsd");
	}

	@Test
	public void testAddNamespacesToSchema() throws ConfigurationException, IOException {
		testAddNamespacesToSchema(ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE+"-after-adding-namespace.xsd");
	}


	public XSD getXSD(String schemaLocation) throws ConfigurationException {
		String[] split =  schemaLocation.trim().split("\\s+");
		XSD xsd = new XSD();
		xsd.initNamespace(split[0], scopeProvider, split[1]);
		return xsd;
	}

}
