// -- -----------------------------------------------------------------------------
// -- Copyright 1998 - 2011 Structure Consulting Group, LLC. All rights reserved.
// -- PROPRIETARY AND CONFIDENTIAL.  
// -- Intellectual Property of Structure Consulting Group
// -- -----------------------------------------------------------------------------
// -- -----------------------------------------------------------------------------
// -- User        Date        Comments
// -- ----------  ----------- -----------------------------------------------------
// -- FAdhami     Dec 6, 2012  Created.
// -- -----------------------------------------------------------------------------
//


package org.vaadin.addons.lazyquerycontainer;

import java.util.List;
import java.util.Map;

import com.vaadin.data.Container.Filter;

public interface EntityProvider<T> {

	int getQuerySize(Class<T> entityClass, List<Filter> filters);

	List<?> loadItems(Class<T> entityClass, int startIndex, int count,
			boolean detachedEntities, 
			List<Filter> filters, Object[] sortPropertyIds,
			boolean[] sortPropertyAscendingState);

	void saveItems(List<T> addedEntities, List<T> modifiedEntities, List<T> removedEntities, boolean applicationTransactionManagement, boolean detachedEntities);

	boolean deleteAllItems(boolean applicationTransactionManagement,
			Class<T> entityClass, List<Filter> filters,
			Object[] sortPropertyIds, boolean[] sortPropertyAscendingStates);
	
}
