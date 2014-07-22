// -- -----------------------------------------------------------------------------
// -- Copyright 1998 - 2011 Structure Consulting Group, LLC. All rights reserved.
// -- PROPRIETARY AND CONFIDENTIAL.  
// -- Intellectual Property of Structure Consulting Group
// -- -----------------------------------------------------------------------------
// -- -----------------------------------------------------------------------------
// -- User        Date        Comments
// -- ----------  ----------- -----------------------------------------------------
// -- FAdhami     Aug 28, 2012  Created.
// -- -----------------------------------------------------------------------------
package org.vaadin.addons.lazyquerycontainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.vaadin.addons.lazyquerycontainer.filters.In;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Between;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.IsNull;
import com.vaadin.data.util.filter.Like;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.Or;
import com.vaadin.data.util.filter.SimpleStringFilter;

/**
 * TODO: compare with the provided EntityQuery The implementation of
 * EntityProvider interface that saves entities directly to the entity manager.
 */
public abstract class AbstractEntityProviderBean<T> implements
		EntityProvider<T> {

	protected abstract EntityManager getEntityManager();

	@Override
	public int getQuerySize(Class<T> entityClass, List<Filter> filters) {
		final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		final Root<T> root = cq.from(entityClass);

		cq.select(cb.count(root));

		setWhereCriteria(cb, cq, root, filters);

		final javax.persistence.Query query = getEntityManager()
				.createQuery(cq);

		return ((Number) query.getSingleResult()).intValue();

	}

	/**
	 * Sets where criteria of JPA 2.0 Criteria API query according to Vaadin
	 * filters.
	 * 
	 * @param cb
	 *            the CriteriaBuilder
	 * @param cq
	 *            the CriteriaQuery
	 * @param root
	 *            the root
	 * @param <SE>
	 *            the selected entity
	 */
	private <SE> void setWhereCriteria(final CriteriaBuilder cb,
			final CriteriaQuery<SE> cq, final Root<T> root,
			final List<Container.Filter> filters) {

		final Object[] sortPropertyIds;
		final boolean[] sortPropertyAscendingStates;

		Container.Filter rootFilter;
		if (filters.size() > 0) {
			rootFilter = filters.remove(0);
		} else {
			rootFilter = null;
		}
		while (filters.size() > 0) {
			final Container.Filter filter = filters.remove(0);
			rootFilter = new And(rootFilter, filter);
		}

		if (rootFilter != null) {
			cq.where(setFilter(rootFilter, cb, cq, root));
		}
	}

	@Override
	public List<?> loadItems(Class<T> entityClass, int startIndex, int count,
			boolean detachedEntities,
			List<Filter> filters, Object[] sortPropertyIds,
			boolean[] sortPropertyAscendingState) {
		final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<T> cq = cb.createQuery(entityClass);
		final Root<T> root = cq.from(entityClass);

		cq.select(root);

		setWhereCriteria(cb, cq, root, filters);

		setOrderClause(cb, cq, root, sortPropertyIds,
				sortPropertyAscendingState);

		final javax.persistence.TypedQuery<T> query = getEntityManager()
				.createQuery(cq);

		query.setFirstResult(startIndex);
		query.setMaxResults(count);

		List<T> entities = query.getResultList();
		for (Object entity : entities) {
			if (detachedEntities) {
				getEntityManager().detach(entity);
			}
		}

		return entities;
	}

	@Override
	public void saveItems(List<T> addedEntities, List<T> modifiedEntities,
			List<T> removedEntities, boolean applicationTransactionManagement,
			boolean detachedEntities) {
		if (applicationTransactionManagement) {
			getEntityManager().getTransaction().begin();
		}
		try {
			for (T entity : addedEntities) {
				if (!removedEntities.contains(entity)) {
					getEntityManager().persist(entity);
				}
			}
			for (T entity : modifiedEntities) {
				if (!removedEntities.contains(entity)) {
					if (detachedEntities) {
						entity = getEntityManager().merge(entity);
					}
					getEntityManager().persist(entity);
				}
			}
			for (T entity : removedEntities) {
				// if the entity is not a new entity
				if (!addedEntities.contains(entity) && !isNewEntity(entity)) {
					if (detachedEntities) {
						entity = getEntityManager().merge(entity);
					}
					getEntityManager().remove(entity);
				}
			}
			if (applicationTransactionManagement) {
				getEntityManager().getTransaction().commit();
			}
		} catch (Exception e) {
			if (applicationTransactionManagement) {
				if (getEntityManager().getTransaction().isActive()) {
					getEntityManager().getTransaction().rollback();
				}
			}
			throw new RuntimeException(e);
		}
	}

	protected abstract boolean isNewEntity(T entity);// {return ((BaseEntity)
														// entity).getId() ==
														// null;}

	@Override
	public boolean deleteAllItems(boolean applicationTransactionManagement,
			Class<T> entityClass, List<Filter> filters,
			Object[] sortPropertyIds, boolean[] sortPropertyAscendingStates) {
		if (applicationTransactionManagement) {
			getEntityManager().getTransaction().begin();
		}
		try {
			final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			final CriteriaQuery<T> cq = cb.createQuery(entityClass);
			final Root<T> root = cq.from(entityClass);

			cq.select(root);

			setWhereCriteria(cb, cq, root, filters);

			setOrderClause(cb, cq, root, sortPropertyIds,
					sortPropertyAscendingStates);

			final javax.persistence.TypedQuery<T> query = getEntityManager()
					.createQuery(cq);

			final List<?> entities = query.getResultList();
			for (final Object entity : entities) {
				getEntityManager().remove(entity);
			}

			if (applicationTransactionManagement) {
				getEntityManager().getTransaction().commit();
			}
		} catch (final Exception e) {
			if (applicationTransactionManagement) {
				if (getEntityManager().getTransaction().isActive()) {
					getEntityManager().getTransaction().rollback();
				}
			}
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * Sets order clause of JPA 2.0 Criteria API query according to Vaadin sort
	 * states.
	 * 
	 * @param cb
	 *            the CriteriaBuilder
	 * @param cq
	 *            the CriteriaQuery
	 * @param root
	 *            the root
	 * @param <SE>
	 *            the selected entity
	 */
	private <SE> void setOrderClause(final CriteriaBuilder cb,
			final CriteriaQuery<SE> cq, final Root<T> root,
			final Object[] sortPropertyIds,
			final boolean[] sortPropertyAscendingStates) {
		if (sortPropertyIds.length > 0) {
			final List<Order> orders = new ArrayList<Order>();
			for (int i = 0; i < sortPropertyIds.length; i++) {
				final Expression property = (Expression) getPropertyPath(root,
						sortPropertyIds[i]);
				if (sortPropertyAscendingStates[i]) {
					orders.add(cb.asc(property));
				} else {
					orders.add(cb.desc(property));
				}
			}
			cq.orderBy(orders);
		}
	}

	/**
	 * Implements conversion of Vaadin filter to JPA 2.0 Criteria API based
	 * predicate. Supports the following operations:
	 * 
	 * And, Between, Compare, Compare.Equal, Compare.Greater,
	 * Compare.GreaterOrEqual, Compare.Less, Compare.LessOrEqual, IsNull, Like,
	 * Not, Or, SimpleStringFilter
	 * 
	 * @param filter
	 *            the Vaadin filter
	 * @param cb
	 *            the CriteriaBuilder
	 * @param cq
	 *            the CriteriaQuery
	 * @param root
	 *            the root
	 * @return the predicate
	 */
	private Predicate setFilter(final Container.Filter filter,
			final CriteriaBuilder cb, final CriteriaQuery<?> cq,
			final Root<?> root) {
		if (filter instanceof And) {
			final And and = (And) filter;
			final List<Container.Filter> filters = new ArrayList<Container.Filter>(
					and.getFilters());

			Predicate predicate = cb.and(
					setFilter(filters.remove(0), cb, cq, root),
					setFilter(filters.remove(0), cb, cq, root));

			while (filters.size() > 0) {
				predicate = cb.and(predicate,
						setFilter(filters.remove(0), cb, cq, root));
			}
			
			return predicate;
		}

		if (filter instanceof Or) {
			final Or or = (Or) filter;
			final List<Container.Filter> filters = new ArrayList<Container.Filter>(
					or.getFilters());

			Predicate predicate = cb.or(
					setFilter(filters.remove(0), cb, cq, root),
					setFilter(filters.remove(0), cb, cq, root));

			while (filters.size() > 0) {
				predicate = cb.or(predicate,
						setFilter(filters.remove(0), cb, cq, root));
			}

			return predicate;
		}

		if (filter instanceof Not) {
			final Not not = (Not) filter;
			return cb.not(setFilter(not.getFilter(), cb, cq, root));
		}

		if (filter instanceof Between) {
			final Between between = (Between) filter;
			final Expression property = (Expression) getPropertyPath(root,
					between.getPropertyId());
			return cb.between(property, (Comparable) between.getStartValue(),
					(Comparable) between.getEndValue());
		}

		if (filter instanceof Compare) {
			final Compare compare = (Compare) filter;
			final Expression<Comparable> property = (Expression) getPropertyPath(
					root, compare.getPropertyId());
			switch (compare.getOperation()) {
			case EQUAL:
				return cb.equal(property, compare.getValue());
			case GREATER:
				return cb
						.greaterThan(property, (Comparable) compare.getValue());
			case GREATER_OR_EQUAL:
				return cb.greaterThanOrEqualTo(property,
						(Comparable) compare.getValue());
			case LESS:
				return cb.lessThan(property, (Comparable) compare.getValue());
			case LESS_OR_EQUAL:
				return cb.lessThanOrEqualTo(property,
						(Comparable) compare.getValue());
			default:
			}
		}

		if (filter instanceof IsNull) {
			final IsNull isNull = (IsNull) filter;
			return cb.isNull((Expression) getPropertyPath(root,
					isNull.getPropertyId()));
		}

		if (filter instanceof Like) {
			final Like like = (Like) filter;
			if (like.isCaseSensitive()) {
				return cb
						.like((Expression) getPropertyPath(root,
								like.getPropertyId()), like.getValue());
			} else {
				return cb.like(cb.lower((Expression) getPropertyPath(root,
						like.getPropertyId())), like.getValue().toLowerCase());
			}
		}

		if (filter instanceof SimpleStringFilter) {
			final SimpleStringFilter simpleStringFilter = (SimpleStringFilter) filter;
			final Expression<String> property = (Expression) getPropertyPath(
					root, simpleStringFilter.getPropertyId());
			return cb.like(property, "%" + simpleStringFilter.getFilterString()
					+ "%");
		}
		
		if(filter instanceof In){
			//TODO: implement support for In
		}

		throw new UnsupportedOperationException("Vaadin filter: "
				+ filter.getClass().getName() + " is not supported.");
	}

	/**
	 * Gets property path.
	 * 
	 * @param root
	 *            the root where path starts form
	 * @param propertyId
	 *            the property ID
	 * @return the path to property
	 */
	private Path<Object> getPropertyPath(final Root<?> root,
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