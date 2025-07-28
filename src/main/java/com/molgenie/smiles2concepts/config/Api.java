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

import com.molgenie.smiles2concepts.models.common.ErrorResponse;

import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class Api {
	
	public static Javalin create( IJettySettings settings ) {
		// Configure Jetty ThreadPool
		var app = Javalin.create(config -> {
			config.server(() -> createJettyServer(settings));
			var objectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
					.serializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
					.build();
			config.jsonMapper(new JavalinJackson(objectMapper));
		});

		app.exception( RuntimeException.class, (e, ctx) -> {
			ctx.status(500);
			ctx.json(new ErrorResponse( e.getMessage(), 500 ) );
		});
		return app;
	}
	
	private static Server createJettyServer(IJettySettings settings) {
		// Configure Jetty ThreadPool
		QueuedThreadPool threadPool = new QueuedThreadPool(settings.maxThreadCount());
		threadPool.setName("server-thread-pool");

		// Create and configure Jetty server
		Server server = new Server(threadPool);
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(settings.listeningPort());
		server.addConnector(connector);
		return server;
	}
}
