/*
* Copyright MolGenie GmbH, 
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
*/
package com.molgenie.smiles2concepts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.molgenie.smiles2concepts.Main;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class AppPropertiesLoader {
	private static final Logger log = LoggerFactory.getLogger(AppPropertiesLoader.class);
	private static final String DEFAULT_PROPERTIES_FILE = "classify.default.properties";
	private static final String PROPERTIES_FILE = "config/classify.properties";

	public static AppProperties load() {
		var defaultProperties = new Properties();
		try (var input = Main.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
			if (input == null) {
				throw new RuntimeException("Cannot load default properties from resources: " + DEFAULT_PROPERTIES_FILE);
			}
			defaultProperties.load(input);
		} catch (IOException e) {
			throw new RuntimeException("Cannot load default properties from resources: " + DEFAULT_PROPERTIES_FILE, e);
		}

		Properties appProps = new Properties(defaultProperties);
		
		var absolutePropertyFilePath = Path.of(PROPERTIES_FILE).toAbsolutePath().toString();
		
		try {
			appProps.load(new FileInputStream(absolutePropertyFilePath));
		} catch (IOException e) {
			log.warn("Cannot load application properties from {}", absolutePropertyFilePath, e);
		}

		// Convert properties to AppProperties object
		int port = Integer.parseInt(appProps.getProperty("port"));
		int maxThreads = Integer.parseInt(appProps.getProperty("maxThreads"));
		
		String timeout = appProps.getProperty("timeout");
		String baseApiPath = appProps.getProperty("baseApiPath");
		String smiles =  appProps.getProperty("smiles");
		String module =  appProps.getProperty("module");
		String ontologyFilename = appProps.getProperty("ontologyFilename");
		boolean writeLeafsOnly = Boolean.parseBoolean( (String) appProps.get("writeLeafsOnly"));
		//System.out.println(appProps.get("writeLeafsOnly"));
		return new AppProperties( port, maxThreads, 
				baseApiPath, 
				timeout, module, ontologyFilename, smiles, writeLeafsOnly );
	}
}
