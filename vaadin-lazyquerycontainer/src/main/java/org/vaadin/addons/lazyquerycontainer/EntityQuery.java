/**
 * Copyright 2010 Tommi S.E. Laukkanen
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
package org.vaadin.addons.lazyquerycontainer;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;

/**
 * Entity query implementation which dynamically injects missing query
 * definition properties to CompositeItems.
 * 
 * @param <E>
 *            the entity type
 * 
 * @author Tommi S.E. Laukkanen
 */
public class EntityQuery<E> implements Query, Serializable {
	/**
	 * Java serialization version UID.
	 */
	private static final long serialVersionUID = 1L;
	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(EntityQuery.class);

	/**
	 * QueryDefinition contains definition of the query properties and batch
	 * size.
	 */
	private final EntityQueryDefinition queryDefinition;
	/**
	 * The size of the query.
	 */
	private int querySize = -1;
	private EntityProvider<E> entityProvider;

	/**
	 * Constructor for configuring the query.
	 * 
	 * @param entityQueryDefinition
	 *            The entity query definition.
	 */
	public EntityQuery(final EntityQueryDefinition entityQueryDefinition,
			final EntityProvider<E> entityProvider) {
		this.queryDefinition = entityQueryDefinition;
		this.entityProvider = entityProvider;
	}

	/**
	 * Constructs new item based on QueryDefinition.
	 * 
	 * @return new item.
	 */
	@Override
	public final Item constructItem() {
		Class entityClass = getEntityClass();
		try {
			final Object entity = entityClass.newInstance();
			final BeanInfo info = Introspector.getBeanInfo(entityClass);
			for (final PropertyDescriptor pd : info.getPropertyDescriptors()) {
				for (final Object propertyId : queryDefinition.getPropertyIds()) {
					if (pd.getName().equals(propertyId)) {
						try {
							pd.getWriteMethod()
									.invoke(entity,
											queryDefinition
													.getPropertyDefaultValue(propertyId));
						} catch (Exception e) {
							throw new RuntimeException(
									"Error in "
											+ propertyId
											+ " property population with default value "
											+ queryDefinition.getPropertyDefaultValue(propertyId)
											+ " in " + entityClass
											+ " entity class.", e);
						}
					}
				}
			}
			return toItem(entity);
		} catch (final Exception e) {
			// FA: added more details to the exception message.
			throw new RuntimeException(
					"Error in "
							+ entityClass
							+ " bean construction or property population with default values ",
					e);
		}
	}

	private Class<E> getEntityClass() {
		return queryDefinition.getEntityClass();
	}

	/**
	 * Number of beans returned by query.
	 * 
	 * @return number of beans.
	 */
	@Override
	public final int size() {

		if (querySize == -1) {
			if (queryDefinition.getBatchSize() == 0) {
				LOGGER.debug(getEntityClass().getName()
						+ " size skipped due to 0 bath size.");
				return 0;
			}

			querySize = getEntityProvider().getQuerySize(getEntityClass(),
					getFilters());
			LOGGER.debug(getEntityClass().getName() + " container size: "
					+ querySize);
		}
		return querySize;
	}

	private List<Filter> getFilters() {
		return queryDefinition.getFilters();
	}

	private EntityProvider<E> getEntityProvider() {
		return entityProvider;
	}

	/**
	 * Load batch of items.
	 * 
	 * @param startIndex
	 *            Starting index of the item list.
	 * @param count
	 *            Count of the items to be retrieved.
	 * @return List of items.
	 */
	@Override
	public final List<Item> loadItems(final int startIndex, final int count) {
		final Object[] sortPropertyIds;
		final boolean[] sortPropertyAscendingStates;

		if (queryDefinition.getSortPropertyIds().length == 0) {
			sortPropertyIds = queryDefinition.getDefaultSortPropertyIds();
			sortPropertyAscendingStates = queryDefinition
					.getDefaultSortPropertyAscendingStates();
		} else {
			sortPropertyIds = queryDefinition.getSortPropertyIds();
			sortPropertyAscendingStates = queryDefinition
					.getSortPropertyAscendingStates();
		}

		List<?> entities = entityProvider.loadItems(getEntityClass(),
				startIndex, count, 
				queryDefinition.isDetachedEntities(), 
				getFilters(), sortPropertyIds,
				sortPropertyAscendingStates);

		List<Item> items = new ArrayList<Item>();
		for (Object entity : entities) {
			items.add(toItem(entity));
		}

		return items;
	}

	/**
	 * Saves the modifications done by container to the query result. Query will
	 * be discarded after changes have been saved and new query loaded so that
	 * changed items are sorted appropriately.
	 * 
	 * @param addedItems
	 *            Items to be inserted.
	 * @param modifiedItems
	 *            Items to be updated.
	 * @param removedItems
	 *            Items to be deleted.
	 */
	public void saveItems(final List<Item> addedItems,
			final List<Item> modifiedItems, final List<Item> removedItems) {
		entityProvider.saveItems(fromItem(addedItems), fromItem(modifiedItems),
				fromItem(removedItems),
				queryDefinition.isApplicationManagedTransactions(),
				queryDefinition.isDetachedEntities());
	}

	private List<E> fromItem(List<Item> items) {
		List<E> entities = new ArrayList<E>();
		if (items != null) {
			for (Item item : items) {
				entities.add(fromItem(item));
			}
		}
		return entities;
	}

	/**
	 * Removes all items. Query will be discarded after delete all items has
	 * been called.
	 * 
	 * @return true if the operation succeeded or false in case of a failure.
	 */
	public boolean deleteAllItems() {
		return entityProvider.deleteAllItems(
				queryDefinition.isApplicationManagedTransactions(),
				getEntityClass(), getFilters(),
				queryDefinition.getSortPropertyIds(),
				queryDefinition.getSortPropertyAscendingStates());
	}

	/**
	 * Converts bean to Item. Implemented by encapsulating the Bean first to
	 * BeanItem and then to CompositeItem.
	 * 
	 * @param entity
	 *            bean to be converted.
	 * @return item converted from bean.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final Item toItem(final Object entity) {
		if (queryDefinition.isCompositeItems()) {
			final NestingBeanItem<?> beanItem = new NestingBeanItem<Object>(
					entity, queryDefinition.getMaxNestedPropertyDepth(),
					queryDefinition.getPropertyIds());

			final CompositeItem compositeItem = new CompositeItem();
			compositeItem.addItem("bean", beanItem);

			for (final Object propertyId : queryDefinition.getPropertyIds()) {
				if (compositeItem.getItemProperty(propertyId) == null) {
					compositeItem
							.addItemProperty(
									propertyId,
									new ObjectProperty(
											queryDefinition
													.getPropertyDefaultValue(propertyId),
											queryDefinition
													.getPropertyType(propertyId),
											queryDefinition
													.isPropertyReadOnly(propertyId)));
				}
			}

			return compositeItem;
		} else {
			return new NestingBeanItem<Object>(entity,
					queryDefinition.getMaxNestedPropertyDepth(),
					queryDefinition.getPropertyIds());
		}
	}

	/**
	 * Converts item back to bean.
	 * 
	 * @param item
	 *            Item to be converted to bean.
	 * @return Resulting bean.
	 */
	protected final E fromItem(final Item item) {
		if (queryDefinition.isCompositeItems()) {
			return (E) ((BeanItem<?>) (((CompositeItem) item).getItem("bean")))
					.getBean();
		} else {
			return (E) ((BeanItem<?>) item).getBean();
		}
	}

	/**
	 * @return the queryDefinition
	 */
	protected final EntityQueryDefinition getQueryDefinition() {
		return queryDefinition;
	}

}
