/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.ui.tests.refactoring.all;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import org.eclipse.jdt.ui.tests.refactoring.reorg.AllReorgPerformanceTests;
import org.eclipse.jdt.ui.tests.refactoring.type.AllTypeConstraintsPerformanceTests;

@Suite
@SelectClasses({
	AllReorgPerformanceTests.class,
	AllTypeConstraintsPerformanceTests.class
})
public class AllRefactoringPerformanceTests {
}
