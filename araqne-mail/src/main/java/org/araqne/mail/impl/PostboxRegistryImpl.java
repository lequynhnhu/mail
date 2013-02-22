/*
 * Copyright 2011 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.mail.impl;

import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.araqne.api.PrimitiveConverter;
import org.araqne.confdb.Config;
import org.araqne.confdb.ConfigCollection;
import org.araqne.confdb.ConfigDatabase;
import org.araqne.confdb.ConfigService;
import org.araqne.confdb.Predicates;
import org.araqne.mail.Postbox;
import org.araqne.mail.PostboxConfig;
import org.araqne.mail.PostboxRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "postbox-registry")
@Provides
public class PostboxRegistryImpl implements PostboxRegistry {
	private final Logger logger = LoggerFactory.getLogger(PostboxRegistryImpl.class.getName());

	@Requires
	private ConfigService conf;

	private ConfigCollection getCollection() {
		ConfigDatabase db = conf.ensureDatabase("araqne-mail");
		return db.ensureCollection(PostboxConfig.class);
	}

	@Override
	public Collection<PostboxConfig> getConfigs() {
		ConfigCollection col = getCollection();
		return col.findAll().getDocuments(PostboxConfig.class);
	}

	@Override
	public PostboxConfig getConfig(String name) {
		ConfigCollection col = getCollection();
		Config c = col.findOne(Predicates.field("name", name));
		if (c == null)
			throw new IllegalArgumentException("not found");
		return c.getDocument(PostboxConfig.class);
	}

	@Override
	public void register(PostboxConfig config) {
		ConfigCollection col = getCollection();
		if (col.findOne(Predicates.field("name", config.getName())) != null)
			throw new IllegalArgumentException("already exist");
		col.add(PrimitiveConverter.serialize(config));
	}

	@Override
	public void unregister(String name) {
		ConfigCollection col = getCollection();
		Config c = col.findOne(Predicates.field("name", name));
		if (c == null)
			throw new IllegalArgumentException("not exist");
		col.remove(c);
	}

	@Override
	public Postbox connect(PostboxConfig config) {
		Session session = Session.getInstance(config.getProperties());
		try {
			Store store = session.getStore("imaps");
			store.connect(config.getHost(), config.getUser(), config.getPassword());
			return new Postbox(store);
		} catch (NoSuchProviderException e) {
			logger.error("araqne-mail: cannot open imap", e);
		} catch (MessagingException e) {
			logger.error("araqne-mail: cannot open imap", e);
		}
		return null;
	}
}
