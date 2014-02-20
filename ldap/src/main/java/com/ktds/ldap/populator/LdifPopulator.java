package com.ktds.ldap.populator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.util.Assert;

import com.ktds.ldap.web.HomeController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Mattias Hellborg Arthursson
 * @since 2.0
 */
public class LdifPopulator implements InitializingBean {
	private static final Logger logger = LoggerFactory.getLogger("com.ktds.ldap");
	
	private Resource resource;
	private ContextSource contextSource;

	private String base = "";
	private boolean clean = false;
	private String defaultBase;

	public void setContextSource(ContextSource contextSource) {
		this.contextSource = contextSource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public void setClean(boolean clean) {
		this.clean = clean;
	}

	public void setDefaultBase(String defaultBase) {
		this.defaultBase = defaultBase;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(contextSource, "ContextSource must be specified");
		Assert.notNull(resource, "Resource must be specified");

		if (!LdapUtils.newLdapName(base).equals(LdapUtils.newLdapName(defaultBase))) {
			List<String> lines = IOUtils.readLines(resource.getInputStream());

			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			for (String line : lines) {
				writer.println(StringUtils.replace(line, defaultBase, base));
			}

			writer.flush();
			resource = new ByteArrayResource(sw.toString().getBytes("UTF8"));
		}

		logger.info("�덉쥌��===> afterProperties:{}", this.defaultBase);
		if (clean) {
			LdapTestUtils.clearSubContexts(contextSource, LdapUtils.emptyLdapName());
		}

		LdapTestUtils.loadLdif(contextSource, resource);
	}
}
