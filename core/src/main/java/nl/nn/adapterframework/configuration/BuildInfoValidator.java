/*
   Copyright 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

/**
 * Validates if the BuildInfo.properties file is present in the configuration (jar), and if the name and version properties are set correctly
 * 
 * @author Niels Meijer
 */
public class BuildInfoValidator {
	private String name = null;
	private String version = null;
	private byte[] jar = null;
	private String buildInfoFilename = null;
	protected static String ADDITIONAL_PROPERTIES_FILE_SUFFIX = AppConstants.getInstance().getString(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY, null);

	protected BuildInfoValidator(InputStream stream) throws ConfigurationException {
		String buildInfo = "BuildInfo";
		if(StringUtils.isNotEmpty(ADDITIONAL_PROPERTIES_FILE_SUFFIX)) {
			buildInfo += ADDITIONAL_PROPERTIES_FILE_SUFFIX;
		}
		this.buildInfoFilename = buildInfo + ".properties";

		try {
			jar = Misc.streamToBytes(stream);

			read();
			validate();
		} catch(IOException e) {
			throw new ConfigurationException("unable to read jarfile", e);
		}
	}

	private void read() throws IOException, ConfigurationException {
		boolean isBuildInfoPresent = false;
		try (JarInputStream zipInputStream = new JarInputStream(getJar())) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextJarEntry()) != null) {
				if (!zipEntry.isDirectory()) {
					String entryName = zipEntry.getName();
					String fileName = FilenameUtils.getName(entryName);

					if(buildInfoFilename.equals(fileName)) {
						name = FilenameUtils.getPathNoEndSeparator(entryName);
						Properties props = new Properties();
						props.load(zipInputStream);
						version = ConfigurationUtils.getConfigurationVersion(props);

						isBuildInfoPresent = true;
						break;
					}
				}
			}
		}
		if(!isBuildInfoPresent) {
			throw new ConfigurationException("no ["+buildInfoFilename+"] present in configuration");
		}
	}

	private void validate() throws ConfigurationException {
		if(StringUtils.isEmpty(name))
			throw new ConfigurationException("unknown configuration name");
		if(StringUtils.isEmpty(version))
			throw new ConfigurationException("unknown configuration version");
	}

	public InputStream getJar() {
		return new ByteArrayInputStream(jar);
	}
	public String getName() {
		return name;
	}
	public String getVersion() {
		return version;
	}

}
