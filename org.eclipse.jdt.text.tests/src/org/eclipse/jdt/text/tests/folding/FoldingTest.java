/*******************************************************************************
 * Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.folding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;

public class FoldingTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject jProject;

	private IPackageFragmentRoot sourceFolder;

	private IPackageFragment packageFragment;

	@Before
	public void setUp() throws CoreException {
		jProject= projectSetup.getProject();
		sourceFolder= jProject.findPackageFragmentRoot(jProject.getResource().getFullPath().append("src"));
		if (sourceFolder == null) {
			sourceFolder= JavaProjectHelper.addSourceContainer(jProject, "src");
		}
		packageFragment= sourceFolder.createPackageFragment("org.example.test", false, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, true);
	}

	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(jProject);
	}

	@Test
	public void testMethodDeclarationFoldingWithSameLineStart() throws Exception {
		String str= """
				package org.example.test;
				public class Q {
				    void a() {			//here should be an annotation
				  		int i = 0;
				    }void b() {			//here should be an annotation
				    }
				}
				""";
		List<IRegion> regions= getProjectionRangesOfFile(str);
		assertCodeHasRegions(str, 2);
		regions.sort((r1, r2) -> Integer.compare(r1.getOffset(), r2.getOffset()));
		IRegion methodARegion= regions.get(0);
		IRegion methodBRegion= regions.get(1);
		int methodAEnd= methodARegion.getOffset() + methodARegion.getLength();
		int methodBStart= methodBRegion.getOffset();
		assertTrue(methodBStart >= methodAEnd, String.format("void b() should start void a(){}, but it wasnt. void b() started at %d and void a() ended at %d", methodBStart, methodAEnd));
	}

	private List<IRegion> getProjectionRangesOfFile(String str) throws Exception {
		ICompilationUnit cu= packageFragment.createCompilationUnit("TestFolding.java", str, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);
		List<IRegion> regions= new ArrayList<>();
		Iterator<Annotation> it= model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation a= it.next();
			if (a instanceof ProjectionAnnotation) {
				Position p= model.getPosition(a);
				regions.add(new Region(p.getOffset(), p.getLength()));
			}
		}
		return regions;
	}

	@Test
	public void testCompilationUnitFolding() throws Exception {
		String str= """
				package org.example.test;
				public class A {		//here should not be an annotation
				}
				""";
		assertCodeHasRegions(str, 0);
	}

	@Test
	public void testTypeDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				public class B {
				    class Inner {		//here should be an annotation
				    }
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testMethodDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				public class C {
				    void m() {		//here should be an annotation
				    }
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testIfStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class D {
				    void x() {			//here should be an annotation
				        if (true) {		//here should be an annotation
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testTryStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class E {
				    void x() {						//here should be an annotation
				        try {						//here should be an annotation

				        } catch (Exception e) {		//here should be an annotation

				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 3);
	}

	@Test
	public void testWhileStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class F {
				    void x() {				//here should be an annotation
				        while (true) {		//here should be an annotation
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testForStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class G {
				    void x() {					//here should be an annotation
				        for(int i=0;i<1;i++){	//here should be an annotation
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testEnhancedForStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class H {
				    void x() {							//here should be an annotation
				        for(String s: new String[0]){	//here should be an annotation
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testDoStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class I {
				    void x() {				//here should be an annotation
				        do {				//here should be an annotation
				        } while(false);
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testSynchronizedStatementFolding() throws Exception {
		String str= """
				package org.example.test;
				public class K {
				    void x() {					//here should be an annotation
				        synchronized(this) {	//here should be an annotation
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testLambdaExpressionFolding() throws Exception {
		String str= """
				package org.example.test;
				import java.util.function.Supplier;
				public class L {
				    void x() {							//here should be an annotation
				        Supplier<String> s = () -> {	//here should be an annotation
				            return "";
				        };
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testAnonymousClassDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				public class M {
				    Object o = new Object(){		//here should be an annotation
				        void y() {					//here should be an annotation

				        }
				    };
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testEnumDeclarationFolding() throws Exception {
		String str= """
				package org.example.test;
				public enum N {					//here should be an annotation
				    A,
				    B
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testInitializerFolding() throws Exception {
		String str= """
				package org.example.test;
				public class O {
				    static {					//here should be an annotation
				    }
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testNestedFolding() throws Exception {
		String str= """
				package org.example.test;
				public class P {
				    void x() {							//here should be an annotation
				        if (true) {						//here should be an annotation
				            for(int i=0;i<1;i++){		//here should be an annotation
				                while(true) {			//here should be an annotation
				                    do {				//here should be an annotation
				                    } while(false);
				                }
				            }
				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 5);
	}

	@Test
	public void testCollapsed() throws Exception {
		String code= """
				package org.example.test;
				public class P {
				    void x() {				//here should be an annotation
				        if (true) {			//here should be an annotation
				        }
				    }
				}
				""";
		ICompilationUnit cu= packageFragment.createCompilationUnit("TestFolding.java", code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		JavaSourceViewer viewer= (JavaSourceViewer) editor.getViewer();
		viewer.doOperation(ProjectionViewer.COLLAPSE_ALL);
		ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);
		int foundCollapsed= 0;

		for (Iterator<Annotation> it= model.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation= it.next();
			if (annotation instanceof ProjectionAnnotation pa) {
				if (pa.isCollapsed()) {
					foundCollapsed+= 1;
				}
			}
		}
		assertTrue(foundCollapsed == 2, String.format("There should be 2 collapsed methods but it was %d", foundCollapsed));
	}

	private void assertCodeHasRegions(String code, int regionsCount) throws Exception {
		List<IRegion> regions= getProjectionRangesOfFile(code);
		assertEquals(regionsCount, regions.size(), String.format("Expected %d regions but saw %d.", regionsCount, regions.size()));
	}
}
