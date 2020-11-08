package one.edee.oss.pmptt.dao;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * Extends {@link HierarchyStorage} adding method to access used transaction manager.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public interface DbHierarchyStorage extends HierarchyStorage {

	/**
	 * Returns transaction manager used by this hierarchy storage.
	 * @return
	 */
	PlatformTransactionManager getTransactionManager();

}
