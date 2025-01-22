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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

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
	}

	@After
	public void tearDown() throws CoreException {
		JavaProjectHelper.delete(jProject);
	}

	private List<IRegion> getProjectionRangesOfFile(String str) throws JavaModelException, PartInitException {
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
	public void testClassWithJavadocAsHeaderComment() throws Exception {
		String str= """
				package org.example.test;
				/**									//here should not be an annotation
				 * Javadoc
				 */
				public class HeaderCommentTest {
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testImportsFolding() throws Exception {
		String str= """
				package org.example.test;

				import java.util.List;				//here should not be an annotation
				import java.util.ArrayList;

				public class ImportsTest {
				}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testSingleMethodWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				public class SingleMethodTest {
				    /**									//here should not be an annotation
				     * Javadoc
				     */
				    public void foo() {					//here should not be an annotation
				        System.out.println("Hello");
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testMultipleMethodsWithoutComments() throws Exception {
		String str= """
				package org.example.test;
				public class MultipleMethodTest {
				    public void foo() {					//here should not be an annotation

				    }
				    public void bar() {					//here should not be an annotation

				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testInnerClassFolding() throws Exception {
		String str= """
				package org.example.test;
				public class OuterClass {
				    class InnerClass {				//here should not be an annotation
				        void bar() {				//here should not be an annotation

				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 2);
	}

	@Test
	public void testInnerClassWithJavadoc() throws Exception {
		String str= """
				package org.example.test;
				public class OuterWithDocs {
				    /**										//here should not be an annotation
				     * Javadoc
				     */
				    class InnerWithDocs {					//here should not be an annotation
				        /**									//here should not be an annotation
				         * Javadoc
				         */
				        void bar() {						//here should not be an annotation

				        }
				    }
				}
				""";
		assertCodeHasRegions(str, 4);
	}

	@Test
	public void testDetailedRegionValuesInnerClass() throws Exception {
		String code= """
				package org.example.test;
				public class Example {
				    /**
				     * Javadoc
				     */
				    public class Example {
				        void foo() {
				        }
				    }
				}
				""";

		ICompilationUnit cu= packageFragment.createCompilationUnit("TestFolding.java", code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		ISourceViewer sourceViewer= editor.getViewer();
		if (sourceViewer instanceof ProjectionViewer) {
			((ProjectionViewer) sourceViewer).doOperation(ProjectionViewer.COLLAPSE_ALL);
		}
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

		assertEquals(3, regions.size(),
				String.format("Expected 3 regions but saw %d.", regions.size()));
		regions.sort(Comparator.comparingInt(IRegion::getOffset));

		IRegion javadocRegion= regions.get(0);
		IRegion innerClassRegion= regions.get(1);
		IRegion methodRegion= regions.get(2);
		int docIndex= code.indexOf("Javadoc");
		int docLength= "Javadoc".length();
		int innerClassIndex= code.indexOf("public class Example {", docIndex);
		int innerClassEndIndex= code.indexOf('}', innerClassIndex);
		int methodIndex= code.indexOf("void foo()");
		int methodLength= "void foo()".length();

		assertTrue(
				javadocRegion.getOffset() <= docIndex,
				() -> String.format(
						"Javadoc region starts at %d but should start before the comment text at %d.",
						javadocRegion.getOffset(), docIndex));
		assertTrue(
				javadocRegion.getOffset() + javadocRegion.getLength() >= docIndex + docLength,
				() -> String.format(
						"Javadoc region ends at %d but should include the end of the comment text at %d.",
						javadocRegion.getOffset() + javadocRegion.getLength(), docIndex + docLength));
		assertTrue(
				innerClassRegion.getOffset() <= innerClassIndex,
				() -> String.format(
						"Inner-class region starts at %d but should start before 'public class Example {' at %d.",
						innerClassRegion.getOffset(), innerClassIndex));
		assertTrue(
				innerClassRegion.getOffset() + innerClassRegion.getLength() >= innerClassEndIndex,
				() -> String.format(
						"Inner-class region ends at %d but should include the closing brace at %d.",
						innerClassRegion.getOffset() + innerClassRegion.getLength(), innerClassEndIndex));
		assertTrue(
				methodRegion.getOffset() <= methodIndex,
				() -> String.format(
						"Method region starts at %d but should start before 'void foo()' at %d.",
						methodRegion.getOffset(), methodIndex));
		assertTrue(
				methodRegion.getOffset() + methodRegion.getLength() >= methodIndex + methodLength,
				() -> String.format(
						"Method region ends at %d but should include the end of 'void foo()' at %d.",
						methodRegion.getOffset() + methodRegion.getLength(), methodIndex + methodLength));
	}

	@Test
	public void testJavadocs() throws Exception {
		String str= """
				package org.example.test;
				   /**										//here should be an annotation
				    * Javadoc
				    */
				    /**										//here should be an annotation
				    * Another Javadoc
				    */
				    /**										//here should be an annotation
				    * Yet another Javadoc
				    */
				    public class Example {}
				""";
		assertCodeHasRegions(str, 3);
	}

	@Test
	public void testCommentBlocks() throws Exception {
		String str= """
				package org.example.test;
				   /* 						//here should be an annotation
				 *
				 */
				/* 							//here should be an annotation
				 *
				 */
				/* 							//here should be an annotation
				 *
				 */
				class h {

					/* 						//here should be an annotation
					 *
					 */
					void b() { 				//here should be an annotation
						/* 					//here should NOT be an annotation
						 *
						 */
						int a;
					}
				}
				""";
		assertCodeHasRegions(str, 5);
	}

	@Test
	public void testCopyrightHeader() throws Exception {
		String str= """
				/**							//here should be an annotation
				* This is some copyright header
				*/
				package org.example.test;

				class SomeClass {}
				""";
		assertCodeHasRegions(str, 1);
	}

	@Test
	public void testMethodDeclarationFoldingWithSameLineStart() throws JavaModelException, PartInitException {
		String str= """
				package org.example.test;
				public class Q {
				    void a() {
				  int i = 0;
				    }void b() {
				    }
				}
				""";
		List<IRegion> regions= getProjectionRangesOfFile(str);
		assertTrue(regions.size() == 2, String.format("Expected %d regions but saw %d.", 2, regions.size()));

		regions.sort((r1, r2) -> Integer.compare(r1.getOffset(), r2.getOffset()));
		IRegion methodARegion= regions.get(0);
		IRegion methodBRegion= regions.get(1);
		int methodAEnd= methodARegion.getOffset() + methodARegion.getLength();
		int methodBStart= methodBRegion.getOffset();
		assertTrue(methodBStart >= methodAEnd, String.format("void b() should start void a(){}, but it wasnt. void b() started at %d and void a() ended at %d", methodBStart, methodAEnd));
	}

	private void assertCodeHasRegions(String code, int regionsCount) throws Exception {
		List<IRegion> regions= getProjectionRangesOfFile(code);
		assertEquals(regionsCount, regions.size(), String.format("Expected %d regions but saw %d.", regionsCount, regions.size()));
	}
}