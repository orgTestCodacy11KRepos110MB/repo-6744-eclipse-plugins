/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package ilg.gnumcueclipse.packs.core.tree;

public class TreePreOrderIterator extends AbstractTreePreOrderIterator {

	public TreePreOrderIterator() {
		super();
	}

	@Override
	public boolean isIterable(Leaf node) {
		return true;
	}

	@Override
	public boolean isLeaf(Leaf node) {
		return false;
	}
}