package org.vaadin.addons.lazyquerycontainer.entityProvider;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

public class EntityProviderHelper {
	/**
	 * Gets property path.
	 * 
	 * @param root
	 *            the root where path starts form
	 * @param propertyId
	 *            the property ID
	 * @return the path to property
	 */
	public static Path<Object> getPropertyPath(final Root<?> root,
			final Object propertyId) {
		final String[] propertyIdParts = ((String) propertyId).split("\\.");

		Path<Object> path = null;
		for (final String part : propertyIdParts) {
			if (path == null) {
				path = root.get(part);
			} else {
				path = path.get(part);
			}
		}
		return path;
	}


}
