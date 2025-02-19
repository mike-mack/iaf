/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.http;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.pipes.JsonPipe;
import nl.nn.adapterframework.pipes.JsonPipe.Direction;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.stream.Message;

/**
 * Listener that allows a {@link Receiver} to receive messages as a REST webservice.
 * Prepends the configured URI pattern with <code>rest/</code>. When you are writing a new Frank config, you are recommended
 * to use an {@link ApiListener} instead. You can find all serviced URI patterns
 * in the Frank!Console: main menu item Webservice, heading Available REST Services.
 *
 * <p>
 * Note:
 * Servlets' multipart configuration expects a Content-Type of <code>multipart/form-data</code> (see http://docs.oracle.com/javaee/6/api/javax/servlet/annotation/MultipartConfig.html).
 * So do not use other multipart content types like <code>multipart/related</code>
 * </p>
 * @author  Niels Meijer
 * @author  Gerrit van Brakel
 */
public class RestListener extends PushingListenerAdapter implements HasPhysicalDestination, HasSpecialDefaultValues {

	private final @Getter(onMethod = @__(@Override)) String domain = "Http";
	private @Getter String uriPattern;
	private @Getter String method;
	private @Getter String etagSessionKey;
	private @Getter String contentTypeSessionKey;
	private @Getter String restPath = "/rest";
	private @Getter Boolean view = null;
	private @Getter String authRoles="IbisAdmin,IbisDataAdmin,IbisTester,IbisObserver,IbisWebService";
	private @Getter boolean writeToSecLog = false;
	private @Getter boolean writeSecLogMessage = false;
	private @Getter boolean retrieveMultipart = true;

	private @Getter MediaTypes consumes = MediaTypes.XML;
	private @Getter MediaTypes produces = MediaTypes.XML;

	private @Getter boolean validateEtag = false;
	private @Getter boolean generateEtag = false;

	public enum MediaTypes {
		XML, JSON, TEXT;
	}
	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (view==null) {
			if (StringUtils.isEmpty(getMethod()) || "GET".equalsIgnoreCase(getMethod())) {
				setView(true);
			} else {
				setView(false);
			}
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		try {
			RestServiceDispatcher.getInstance().registerServiceClient(this, getUriPattern(), getMethod(), getEtagSessionKey(), getContentTypeSessionKey(), isValidateEtag());
		} catch (ConfigurationException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void close() {
		super.close();
		RestServiceDispatcher.getInstance().unregisterServiceClient(getUriPattern(), getMethod());
	}

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		Message response;
		String contentType = (String) session.get("contentType");

		//Check if valid path
		String requestRestPath = (String) session.get("restPath");
		if (!getRestPath().equals(requestRestPath)) {
			throw new ListenerException("illegal restPath value [" + requestRestPath + "], must be [" + getRestPath() + "]");
		}

		//Check if consumes has been set or contentType is set to JSON
		if(getConsumes()== MediaTypes.JSON && "application/json".equalsIgnoreCase(httpServletRequest.getContentType())) {
			try {
				message = transformToXml(message);
			} catch (PipeRunException e) {
				throw new ListenerException("Failed to transform JSON to XML", e);
			}
		}
		int eTag = 0;

		//Check if contentType is not overwritten which disabled auto-converting and mediatype headers
		if(contentType == null || StringUtils.isEmpty(contentType) || contentType.equalsIgnoreCase("*/*")) {
			switch(getProduces()) {
				case XML:
					session.put("contentType", "application/xml");
					break;
				case JSON:
					session.put("contentType", "application/json");
					break;
				case TEXT:
					session.put("contentType", "text/plain");
					break;
				default:
					throw new IllegalStateException("Unknown mediatype ["+getProduces()+"]");
			}

			response = super.processRequest(message, session);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();

			if(getProduces()== MediaTypes.JSON) {
				try {
					response = transformToJson(response);
				} catch (PipeRunException e) {
					throw new ListenerException("Failed to transform XML to JSON", e);
				}
			}
		}
		else {
			response = super.processRequest(message, session);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();
		}

		if(!session.containsKey("etag") && isGenerateEtag() && eTag != 0) { //The etag can be a negative integer...
			session.put("etag", "rest_"+eTag);
		}

		return response;
	}

	public Message transformToJson(Message message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		pipe.setDirection(Direction.XML2JSON);
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSession());
		return pipeResult.getResult();
	}

	public Message transformToXml(Message message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSession());
		return pipeResult.getResult();
	}

	@Override
	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		if ("view".equals(attributeName)) { // if attribute view is present
			if (attributes.get("method") == null || "GET".equalsIgnoreCase(attributes.get("method"))) {// if view="true" AND no method has been supplied, or it's set to GET
				return true; //Then the default is TRUE
			}
			return false;
		}
		return defaultValue;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "uriPattern: "+(getUriPattern()==null?"-any-":getUriPattern())+"; method: "+(getMethod()==null?"all":getMethod());
	}

	public String getRestUriPattern() {
		return getRestPath().substring(1) + "/" + getUriPattern();
	}


	@IbisDoc({"Uri pattern to match, the {uri} part in https://mydomain.com/ibis4something/rest/{uri}, where mydomain.com and ibis4something refer to 'your ibis'. ", ""})
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	@IbisDoc({"Method (e.g. GET or POST) to match", ""})
	public void setMethod(String method) {
		this.method = method;
	}

	@IbisDoc({"Key of session variable to store etag", ""})
	public void setEtagSessionKey(String etagSessionKey) {
		this.etagSessionKey = etagSessionKey;
	}

	@IbisDoc({"Key of Session variable that determines requested content type, overrides {@link #setProduces(String) produces}", ""})
	public void setContentTypeSessionKey(String contentTypeSessionKey) {
		this.contentTypeSessionKey = contentTypeSessionKey;
	}

	public void setRestPath(String restPath) {
		this.restPath = restPath;
	}

	@IbisDoc({"Indicates whether this listener supports a view (and a link should be put in the ibis console)", "if <code>method=get</code> then <code>true</code>, else <code>false</code>"})
	public void setView(boolean b) {
		view = b;
	}
	public boolean isView() {
		if (view==null ) {
			log.warn("RestListener ["+getName()+"] appears to be not configured");
			return false;
		}
		return view;
	}

	@IbisDoc({"Comma separated list of authorization roles which are granted for this rest service", "IbisAdmin,IbisDataAdmin,IbisTester,IbisObserver,IbisWebService"})
	public void setAuthRoles(String string) {
		authRoles = string;
	}

	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}

	public void setWriteSecLogMessage(boolean b) {
		writeSecLogMessage = b;
	}

	@IbisDoc({"Indicates whether the parts of a multipart entity should be retrieved and put in session keys. This can only be done once!", "true"})
	public void setRetrieveMultipart(boolean b) {
		retrieveMultipart = b;
	}

	@IbisDoc({"Mediatype (e.g. XML, JSON, TEXT) the {@link RestServiceDispatcher} receives as input", "XML"})
	public void setConsumes(MediaTypes consumes) {
		this.consumes = consumes;
	}

	@IbisDoc({"Mediatype (e.g. XML, JSON, TEXT) the {@link RestServiceDispatcher} sends as output, if set to json the ibis will automatically try to convert the xml message", "XML"})
	public void setProduces(MediaTypes produces) {
		this.produces = produces;
	}

	@IbisDoc({"If set to true the ibis will automatically validate and process etags", "false"})
	public void setValidateEtag(boolean b) {
		this.validateEtag = b;
	}

	@IbisDoc({"If set to true the ibis will automatically create an etag", "false"})
	public void setGenerateEtag(boolean b) {
		this.generateEtag = b;
	}
}
