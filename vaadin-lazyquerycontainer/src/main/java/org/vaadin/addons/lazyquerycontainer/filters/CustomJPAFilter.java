package org.vaadin.addons.lazyquerycontainer.filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;

public abstract class CustomJPAFilter<T> implements Filter {

    public CustomJPAFilter() {
		super();
	}

	@Override
	public boolean passesFilter(Object itemId, Item item)
			throws UnsupportedOperationException {
		return true;
	}

    @Override
    public boolean appliesToProperty(Object propertyId) {
    	return true;
    }

	public abstract Predicate prepareFilter(CriteriaBuilder cb, Root root);
}
