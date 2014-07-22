package org.vaadin.addons.lazyquerycontainer.filters;

import java.util.List;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;

public class In<T> implements Filter {

    private final Object propertyId;
    private final List<T> values;
    private boolean caseSensitive;


    public In(Object propertyId, List<T> values, boolean caseSensitive) {
		super();
		this.propertyId = propertyId;
		this.values = values;
		this.caseSensitive = caseSensitive;
	}

	public Object getPropertyId() {
		return propertyId;
	}

	public List<T> getValues() {
		return values;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public boolean passesFilter(Object itemId, Item item)
			throws UnsupportedOperationException {
		return true;
	}

    @Override
    public boolean appliesToProperty(Object propertyId) {
        return getPropertyId() != null && getPropertyId().equals(propertyId);
    }


}
