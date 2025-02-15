/*******************************************************************************
 * Copyright (c) 2019, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.UnnecessaryArrayCreationFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes unnecessary array creation for a varargs parameter of a method or super method invocation.
 */
public class UnnecessaryArrayCreationCleanUpCore extends AbstractMultiFix {

	public UnnecessaryArrayCreationCleanUpCore() {
		this(Collections.emptyMap());
	}

	public UnnecessaryArrayCreationCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION)) {
			return new String[] { MultiFixMessages.UnnecessaryArrayCreationCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION)) {
			return "List k= Arrays.asList(\"a\", \"b\", \"c\");\n"; //$NON-NLS-1$
		}

		return "List k= Arrays.asList(new String[] {\"a\", \"b\", \"c\"});\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		UnnecessaryArrayCreationFixCore.UnnecessaryArrayCreationFinder finder= new UnnecessaryArrayCreationFixCore.UnnecessaryArrayCreationFinder(true, rewriteOperations);
		unit.accept(finder);

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.UnnecessaryArrayCreationCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

}
