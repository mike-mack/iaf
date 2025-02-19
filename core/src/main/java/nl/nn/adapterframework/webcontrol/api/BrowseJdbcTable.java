/*
Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;

@Path("/")
public final class BrowseJdbcTable extends FrankApiBase {

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/browse")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(Map<String, Object> json) throws ApiException {
		int minRow = 1;
		int maxRow = 100;

		String datasource = getValue(json, "datasource");
		String tableName = getValue(json, "table");
		String where = getValue(json, "where");
		String order = getValue(json, "order");
		Boolean numberOfRowsOnly = Boolean.parseBoolean(getValue(json, "numberOfRowsOnly"));

		String minRowStr = getValue(json, "minRow");
		if(StringUtils.isNotEmpty(minRowStr)) {
			minRow = Integer.parseInt(minRowStr);
		}
		String maxRowStr = getValue(json, "maxRow");
		if(StringUtils.isNotEmpty(maxRowStr)) {
			maxRow = Integer.parseInt(maxRowStr);
		}

		if(tableName == null) {
			throw new ApiException("tableName not defined.", 400);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.FIND);
		if(StringUtils.isNotEmpty(datasource)) {
			builder.addHeader(HEADER_DATASOURCE_NAME_KEY, datasource);
		}
		builder.addHeader("table", tableName);
		builder.addHeader("where", where);
		builder.addHeader("order", order);
		builder.addHeader("numberOfRowsOnly", numberOfRowsOnly);
		builder.addHeader("minRow", minRow);
		builder.addHeader("maxRow", maxRow);
		return callSyncGateway(builder);
	}
}
