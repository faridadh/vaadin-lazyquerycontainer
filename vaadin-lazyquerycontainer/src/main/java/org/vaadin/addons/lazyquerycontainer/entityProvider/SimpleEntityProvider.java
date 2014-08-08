package org.vaadin.addons.lazyquerycontainer.entityProvider;

import javax.persistence.EntityManager;

import org.vaadin.addons.lazyquerycontainer.AbstractEntityProviderBean;

public class SimpleEntityProvider<T> extends AbstractEntityProviderBean<T> {

	private EntityManager entityManager;
	
	public SimpleEntityProvider(EntityManager entityManager) {
		super();
		this.entityManager = entityManager;
	}

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	protected boolean isNewEntity(T entity) {
		return true;
	}

}
