/* 
 * JSweet - http://www.jsweet.org
 * Copyright (C) 2015 CINCHEO SAS <renaud.pawlak@cincheo.fr>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsweet.transpiler.typescript;

import static org.jsweet.JSweetConfig.ANNOTATION_ERASED;
import static org.jsweet.JSweetConfig.ANNOTATION_FUNCTIONAL_INTERFACE;
import static org.jsweet.JSweetConfig.ANNOTATION_OBJECT_TYPE;
import static org.jsweet.JSweetConfig.ANNOTATION_ROOT;
import static org.jsweet.JSweetConfig.ANNOTATION_STRING_TYPE;
import static org.jsweet.JSweetConfig.GLOBALS_CLASS_NAME;
import static org.jsweet.JSweetConfig.GLOBALS_PACKAGE_NAME;
import static org.jsweet.JSweetConfig.INDEXED_DELETE_FUCTION_NAME;
import static org.jsweet.JSweetConfig.INDEXED_DELETE_STATIC_FUCTION_NAME;
import static org.jsweet.JSweetConfig.INDEXED_GET_FUCTION_NAME;
import static org.jsweet.JSweetConfig.INDEXED_GET_STATIC_FUCTION_NAME;
import static org.jsweet.JSweetConfig.INDEXED_SET_FUCTION_NAME;
import static org.jsweet.JSweetConfig.INDEXED_SET_STATIC_FUCTION_NAME;
import static org.jsweet.JSweetConfig.LANG_PACKAGE;
import static org.jsweet.JSweetConfig.TUPLE_CLASSES_PACKAGE;
import static org.jsweet.JSweetConfig.UNION_CLASS_NAME;
import static org.jsweet.JSweetConfig.UTIL_CLASSNAME;
import static org.jsweet.JSweetConfig.UTIL_PACKAGE;
import static org.jsweet.JSweetConfig.isJDKPath;
import static org.jsweet.JSweetConfig.isJSweetPath;
import static org.jsweet.transpiler.util.Util.getFirstAnnotationValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;

import org.apache.commons.lang3.StringUtils;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.JSweetProblem;
import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.TypeChecker;
import org.jsweet.transpiler.util.AbstractPrinterAdapter;
import org.jsweet.transpiler.util.AbstractTreePrinter;
import org.jsweet.transpiler.util.Util;

import com.sun.codemodel.internal.JJavaName;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Log;

/**
 * This is an adapter for the TypeScript code generator. It overrides the
 * default adapter's behavior.
 * 
 * @author Renaud Pawlak
 */
public class Java2TypeScriptAdapter extends AbstractPrinterAdapter {

	private Map<String, String> typesMapping = new HashMap<String, String>();
	private Map<String, String> langTypesMapping = new HashMap<String, String>();
	private Set<String> langTypesSimpleNames = new HashSet<String>();
	private Set<String> baseThrowables = new HashSet<String>();

	public Java2TypeScriptAdapter(JSweetContext context) {
		typesMapping.put(Object.class.getName(), "any");
		typesMapping.put(Runnable.class.getName(), "() => void");

		typesMapping.put(DoubleConsumer.class.getName(), "(number) => void");
		typesMapping.put(DoublePredicate.class.getName(), "(number) => boolean");
		typesMapping.put(DoubleSupplier.class.getName(), "() => number");
		typesMapping.put(DoubleBinaryOperator.class.getName(), "(number, number) => number");
		typesMapping.put(DoubleUnaryOperator.class.getName(), "(number) => number");
		typesMapping.put(DoubleToIntFunction.class.getName(), "(number) => number");
		typesMapping.put(DoubleToLongFunction.class.getName(), "(number) => number");

		typesMapping.put(IntConsumer.class.getName(), "(number) => void");
		typesMapping.put(IntPredicate.class.getName(), "(number) => boolean");
		typesMapping.put(IntSupplier.class.getName(), "() => number");
		typesMapping.put(IntBinaryOperator.class.getName(), "(number, number) => number");
		typesMapping.put(IntUnaryOperator.class.getName(), "(number) => number");
		typesMapping.put(IntToDoubleFunction.class.getName(), "(number) => number");
		typesMapping.put(IntToLongFunction.class.getName(), "(number) => number");

		typesMapping.put(LongConsumer.class.getName(), "(number) => void");
		typesMapping.put(LongPredicate.class.getName(), "(number) => boolean");
		typesMapping.put(LongSupplier.class.getName(), "() => number");
		typesMapping.put(LongBinaryOperator.class.getName(), "(number, number) => number");
		typesMapping.put(LongUnaryOperator.class.getName(), "(number) => number");
		typesMapping.put(LongToDoubleFunction.class.getName(), "(number) => number");
		typesMapping.put(LongToIntFunction.class.getName(), "(number) => number");

		typesMapping.put(BooleanSupplier.class.getName(), "() => boolean");

		typesMapping.put(String.class.getName(), "string");
		typesMapping.put(Number.class.getName(), "number");
		typesMapping.put(Integer.class.getName(), "number");
		typesMapping.put(Short.class.getName(), "number");
		typesMapping.put(Float.class.getName(), "number");
		typesMapping.put(Long.class.getName(), "number");
		typesMapping.put(Byte.class.getName(), "number");
		typesMapping.put(Double.class.getName(), "number");
		typesMapping.put(Boolean.class.getName(), "boolean");
		typesMapping.put(Character.class.getName(), "string");
		typesMapping.put(CharSequence.class.getName(), "string");
		typesMapping.put(Void.class.getName(), "void");
		typesMapping.put("double", "number");
		typesMapping.put("int", "number");
		typesMapping.put("float", "number");
		typesMapping.put("long", "number");
		typesMapping.put("byte", "number");
		typesMapping.put("short", "number");
		typesMapping.put("char", "string");
		typesMapping.put("Class", "Function");
		typesMapping.put(LANG_PACKAGE + ".Object", "Object");
		typesMapping.put(LANG_PACKAGE + ".Boolean", "boolean");
		typesMapping.put(LANG_PACKAGE + ".String", "string");
		typesMapping.put(LANG_PACKAGE + ".Number", "number");

		langTypesMapping.put("java.lang.Object", "Object");
		langTypesMapping.put("java.lang.String", "String");
		langTypesMapping.put("java.lang.Boolean", "Boolean");
		langTypesMapping.put("java.lang.Number", "Number");
		langTypesMapping.put("java.lang.Integer", "Number");
		langTypesMapping.put("java.lang.Long", "Number");
		langTypesMapping.put("java.lang.Short", "Number");
		langTypesMapping.put("java.lang.Float", "Number");
		langTypesMapping.put("java.lang.Double", "Number");
		langTypesMapping.put("java.lang.Byte", "Number");
		langTypesMapping.put("java.lang.Character", "String");
		langTypesMapping.put("java.lang.Math", "Math");
		langTypesMapping.put("java.lang.Exception", "Error");
		langTypesMapping.put("java.lang.RuntimeException", "Error");
		langTypesMapping.put("java.lang.Throwable", "Error");
		langTypesMapping.put("java.lang.Error", "Error");
		// langTypesMapping.put("java.util.Date", "Date");

		for (String s : langTypesMapping.keySet()) {
			langTypesSimpleNames.add(s.substring(s.lastIndexOf('.') + 1));
		}

		baseThrowables.add(Throwable.class.getName());
		baseThrowables.add(RuntimeException.class.getName());
		baseThrowables.add(Error.class.getName());
		baseThrowables.add(Exception.class.getName());
	}

	@Override
	public String needsImport(JCImport importDecl, String qualifiedName) {
		if (isJSweetPath(qualifiedName) || typesMapping.containsKey(qualifiedName) || langTypesMapping.containsKey(qualifiedName)
				|| qualifiedName.startsWith("java.util.function.") || (isJDKPath(qualifiedName) && !getPrinter().getContext().options.isJDKAllowed())
				|| qualifiedName.endsWith(GLOBALS_PACKAGE_NAME + "." + GLOBALS_CLASS_NAME)) {
			return null;
		}
		if (importDecl.qualid.type != null && (Util.hasAnnotationType(importDecl.qualid.type.tsym, ANNOTATION_ERASED, ANNOTATION_OBJECT_TYPE)
				|| importDecl.qualid.type.tsym.getKind() == ElementKind.ANNOTATION_TYPE)) {
			return null;
		}
		if (importDecl.isStatic()) {
			if (importDecl.getQualifiedIdentifier() instanceof JCFieldAccess) {
				JCFieldAccess fa = (JCFieldAccess) importDecl.getQualifiedIdentifier();
				String name = getPrinter().getRootRelativeName(fa.selected.type.tsym, getPrinter().getContext().useModules);
				String methodName = fa.name.toString();

				// function is a top-level global function (no need to import)
				if (GLOBALS_CLASS_NAME.equals(name)) {
					return null;
				}
				if (!getPrinter().getContext().useModules && name.endsWith(GLOBALS_PACKAGE_NAME + "." + GLOBALS_CLASS_NAME)) {
					return null;
				}

				if (JSweetConfig.TS_STRICT_MODE_KEYWORDS.contains(methodName.toLowerCase())) {
					// if method name is a reserved ts keyword, we have to fully
					// qualify calls to it (hence discarding any import)
					return null;
				}
				boolean globals = name.endsWith("." + JSweetConfig.GLOBALS_CLASS_NAME);
				if (globals) {
					name = name.substring(0, name.length() - JSweetConfig.GLOBALS_CLASS_NAME.length() - 1);
				}
				// function belong to the current package (no need to
				// import)
				String current = getPrinter().getRootRelativeName(getPrinter().getCompilationUnit().packge, getPrinter().getContext().useModules);
				if (getPrinter().getContext().useModules) {
					if (current.equals(name)) {
						return null;
					}
				} else {
					if (current.startsWith(name)) {
						return null;
					}
				}
				return StringUtils.isBlank(name) ? null : name + "." + getIdentifier(methodName);
			} else {
				return null;
			}
		}
		return super.needsImport(importDecl, qualifiedName);
	}

	private boolean isWithinGlobals(String targetClassName) {
		if (targetClassName == null || targetClassName.endsWith("." + GLOBALS_CLASS_NAME)) {
			JCClassDecl c = getPrinter().getParent(JCClassDecl.class);
			return c != null && c.sym.getQualifiedName().toString().endsWith("." + GLOBALS_CLASS_NAME);
		} else {
			return false;
		}
	}

	@Override
	public boolean substituteMethodInvocation(JCMethodInvocation invocation) {
		if ("System.out.println".equals(invocation.meth.toString())) {
			getPrinter().print("console.info(").print(invocation.args.head).print(")");
			return true;
		}
		if ("System.err.println".equals(invocation.meth.toString())) {
			getPrinter().print("console.error(").print(invocation.args.head).print(")");
			return true;
		}
		if ("super".equals(invocation.meth.toString())) {
			// we omit call to super if class extends nothing or if parent is an
			// interface
			if (getPrinter().getParent(JCClassDecl.class).extending == null //
					|| Util.isInterface(getPrinter().getParent(JCClassDecl.class).extending.type.tsym)) {
				return true;
			}
			// special case when subclassing a Java exception type
			if (invocation.meth instanceof JCIdent) {
				String superClassName = ((JCIdent) invocation.meth).sym.getEnclosingElement().getQualifiedName().toString();
				if (baseThrowables.contains(superClassName)) {
					// ES6 would take the cause, but we ignore it so far for
					// backward compatibility
					getPrinter().print("super(").print(invocation.getArguments().head).print(")");
					// PATCH:
					// https://github.com/Microsoft/TypeScript/issues/5069
					if (!invocation.getArguments().isEmpty()) {
						getPrinter().print("; this.message=").print(invocation.getArguments().head);
					}
					return true;
				}
			}
		}
		JCFieldAccess fieldAccess = null;
		TypeSymbol targetType = null;
		String targetClassName = null;
		String targetMethodName = null;
		if (invocation.getMethodSelect() instanceof JCFieldAccess) {
			fieldAccess = (JCFieldAccess) invocation.getMethodSelect();
			targetType = fieldAccess.selected.type.tsym;
			targetClassName = targetType.getQualifiedName().toString();
			targetMethodName = fieldAccess.name.toString();
		} else {
			targetMethodName = invocation.getMethodSelect().toString();
		}

		// System.out.println(invocation+" ===> "+fieldAccess+" :
		// targetClassName="+targetClassName+"
		// targetMethodName="+targetMethodName+
		// " ownerClassName="+ownerClassName);

		if (targetType != null && targetType.getKind() == ElementKind.ENUM) {
			// TODO: enum type simple name will not be valid when uses as fully
			// qualified name (not imported)
			String relTarget = getPrinter().getContext().useModules ? targetType.getSimpleName().toString() : getPrinter().getRootRelativeName(targetType);
			if (targetMethodName.equals("name")) {
				getPrinter().print(relTarget).print("[").print(fieldAccess.selected).print("]");
				return true;
			}
			if (targetMethodName.equals("ordinal")) {
				getPrinter().print(relTarget).print("[").print(relTarget).print("[").print(fieldAccess.selected).print("]").print("]");
				return true;
			}
			if (targetMethodName.equals("valueOf") && invocation.getArguments().size() == 1) {
				getPrinter().print(fieldAccess.selected).print("[").print(invocation.getArguments().head).print("]");
				return true;
			}
			if (targetMethodName.equals("values")) {
				getPrinter().print("function() { var result: number[] = []; for(var val in ").print(relTarget)
						.print(") { if(!isNaN(<any>val)) { result.push(parseInt(val,10)); } } return result; }()");
				return true;
			}
		}

		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "$export")) {
			if (invocation.args.head.getKind() != Kind.STRING_LITERAL) {
				getPrinter().report(invocation.args.head, JSweetProblem.STRING_LITERAL_EXPECTED);
			}
			String varName = "_exportedVar_" + StringUtils.strip(invocation.args.head.toString(), "\"");
			getPrinter().footer.append("var " + varName + ";\n");
			if (invocation.args.size() == 1) {
				getPrinter().print(varName);
			} else {
				getPrinter().print(varName + " = ").print(invocation.args.tail.head).print("; ");
				getPrinter().print("console.log('" + JSweetTranspiler.EXPORTED_VAR_BEGIN + StringUtils.strip(invocation.args.head.toString(), "\"") + "='+")
						.print(varName).print("+'" + JSweetTranspiler.EXPORTED_VAR_END + "')");
			}
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "array")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "function")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "string")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "bool")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "number")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "integer")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "any")) {
			getPrinter().print("(<any>");
			printCastMethodInvocation(invocation);
			getPrinter().print(")");
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "object")) {
			printCastMethodInvocation(invocation);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "union")) {
			getPrinter().typeChecker.checkUnionTypeAssignment(getPrinter().getContext().types, getPrinter().getParent(), invocation);
			getPrinter().print("(<any>");
			printCastMethodInvocation(invocation);
			getPrinter().print(")");
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "typeof")) {
			getPrinter().print("typeof ").print(invocation.getArguments().head);
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "equalsStrict")) {
			getPrinter().print("(").print(invocation.getArguments().head).print(" === ").print(invocation.getArguments().tail.head).print(")");
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "$map")) {
			if (invocation.args.size() % 2 != 0) {
				report(invocation, JSweetProblem.UNTYPED_OBJECT_ODD_PARAMETER_COUNT);
			}
			getPrinter().print("{");
			com.sun.tools.javac.util.List<JCExpression> args = invocation.args;
			while (args != null && args.head != null) {
				String key = args.head.toString();
				if (args.head.getTag() == Tag.LITERAL && key.startsWith("\"")) {
					key = key.substring(1, key.length() - 1);
					if (JJavaName.isJavaIdentifier(key)) {
						getPrinter().print(key);
					} else {
						getPrinter().print("\"" + key + "\"");
					}
				} else {
					report(args.head, JSweetProblem.UNTYPED_OBJECT_WRONG_KEY, args.head.toString());
				}
				getPrinter().print(": ");
				getPrinter().print(args.tail.head);
				args = args.tail.tail;
				if (args != null && args.head != null) {
					getPrinter().print(",");
				}
			}
			getPrinter().print("}");
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "$apply")) {
			getPrinter().print("(<any>").print(invocation.args.head).print(")(").printArgList(invocation.args.tail).print(")");
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, UTIL_CLASSNAME, "$new")) {
			getPrinter().print("new (<any>").print(invocation.args.head).print(")(").printArgList(invocation.args.tail).print(")");
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_GET_FUCTION_NAME)) {
			if (isWithinGlobals(targetClassName)) {
				if (invocation.getArguments().size() == 1) {
					report(invocation, JSweetProblem.GLOBAL_INDEXER_GET);
					return true;
				} else {
					if (invocation.args.head.toString().endsWith(GLOBALS_CLASS_NAME + ".class")) {
						report(invocation, JSweetProblem.GLOBAL_INDEXER_GET);
						return true;
					}
				}
			}

			if (fieldAccess != null && !fieldAccess.toString().equals(UTIL_CLASSNAME + "." + INDEXED_GET_FUCTION_NAME)) {
				getPrinter().print(fieldAccess.selected).print("[").print(invocation.args.head).print("]");
			} else {
				if (invocation.args.length() == 1) {
					getPrinter().print("this[").print(invocation.args.head).print("]");
				} else {
					getPrinter().print(invocation.args.head).print("[").print(invocation.args.tail.head).print("]");
				}
			}
			return true;
		}
		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_GET_STATIC_FUCTION_NAME)) {
			if (invocation.getArguments().size() == 1 && isWithinGlobals(targetClassName)) {
				report(invocation, JSweetProblem.GLOBAL_INDEXER_GET);
				return true;
			}

			getPrinter().print(fieldAccess.selected).print("[").print(invocation.args.head).print("]");
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_SET_FUCTION_NAME)) {

			if (isWithinGlobals(targetClassName)) {
				if (invocation.getArguments().size() == 2) {
					report(invocation, JSweetProblem.GLOBAL_INDEXER_SET);
					return true;
				} else {
					if (invocation.args.head.toString().endsWith(GLOBALS_CLASS_NAME + ".class")) {
						report(invocation, JSweetProblem.GLOBAL_INDEXER_SET);
						return true;
					}
				}
			}

			if (fieldAccess != null && !fieldAccess.toString().equals(UTIL_CLASSNAME + "." + INDEXED_SET_FUCTION_NAME)) {
				// check the type through the getter
				for (Symbol e : fieldAccess.selected.type.tsym.getEnclosedElements()) {
					if (e instanceof MethodSymbol && INDEXED_GET_FUCTION_NAME.equals(e.getSimpleName().toString())) {
						MethodSymbol getMethod = (MethodSymbol) e;
						TypeSymbol getterType = getMethod.getReturnType().tsym;
						TypeSymbol getterIndexType = getMethod.getParameters().get(0).type.tsym;

						TypeSymbol invokedIndexType = invocation.args.head.type.tsym;
						TypeSymbol invokedValueType = invocation.args.tail.head.type.tsym;

						boolean sameIndexType = getterIndexType.equals(invokedIndexType);

						if (sameIndexType && !Util.isAssignable(getPrinter().getContext().types, getterType, invokedValueType)) {
							report(invocation.args.tail.head, JSweetProblem.INDEXED_SET_TYPE_MISMATCH, getterType);
						}
					}
				}

				getPrinter().print(fieldAccess.selected).print("[").print(invocation.args.head).print("] = ").print(invocation.args.tail.head);
			} else {
				if (invocation.args.length() == 2) {
					getPrinter().print("this[").print(invocation.args.head).print("] = <any>").print(invocation.args.tail.head);
				} else {
					getPrinter().print(invocation.args.head).print("[").print(invocation.args.tail.head).print("] = <any>")
							.print(invocation.args.tail.tail.head);
				}
			}
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_SET_STATIC_FUCTION_NAME)) {

			if (invocation.getArguments().size() == 2 && isWithinGlobals(targetClassName)) {
				report(invocation, JSweetProblem.GLOBAL_INDEXER_SET);
				return true;
			}

			getPrinter().print(fieldAccess.selected).print("[").print(invocation.args.head).print("] = ").print(invocation.args.tail.head);
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_DELETE_FUCTION_NAME)) {
			if (isWithinGlobals(targetClassName)) {
				if (invocation.getArguments().size() == 1) {
					report(invocation, JSweetProblem.GLOBAL_DELETE);
					return true;
				} else {
					if (invocation.args.head.toString().endsWith(GLOBALS_CLASS_NAME + ".class")) {
						report(invocation, JSweetProblem.GLOBAL_DELETE);
						return true;
					}
				}
			}

			if (fieldAccess != null && !fieldAccess.toString().equals(UTIL_CLASSNAME + "." + INDEXED_DELETE_FUCTION_NAME)) {
				getPrinter().print("delete ").print(fieldAccess.selected).print("[").print(invocation.args.head).print("]");
			} else {
				if (invocation.args.length() == 1) {
					getPrinter().print("delete this[").print(invocation.args.head).print("]");
				} else {
					getPrinter().print("delete ").print(invocation.args.head).print("[").print(invocation.args.tail.head).print("]");
				}
			}
			return true;
		}

		if (matchesMethod(targetClassName, targetMethodName, null, INDEXED_DELETE_STATIC_FUCTION_NAME)) {
			if (invocation.getArguments().size() == 1 && isWithinGlobals(targetClassName)) {
				report(invocation, JSweetProblem.GLOBAL_DELETE);
				return true;
			}

			if (fieldAccess != null && !fieldAccess.toString().equals(UTIL_CLASSNAME + "." + INDEXED_GET_FUCTION_NAME)) {
				getPrinter().print("delete ").print(fieldAccess.selected).print("[").print(invocation.args.head).print("]");
			} else {
				if (invocation.args.length() == 1) {
					getPrinter().print("delete ").print("this[").print(invocation.args.head).print("]");
				} else {
					getPrinter().print("delete ").print(invocation.args.head).print("[").print(invocation.args.tail.head).print("]");
				}
			}
			return true;
		}

		if (targetClassName != null && targetClassName.endsWith(GLOBALS_CLASS_NAME)) {
			if (getPrinter().getContext().useModules) {
				if (JSweetConfig.GLOBALS_PACKAGE_NAME.equals(targetType.getEnclosingElement().getSimpleName().toString())) {
					getPrinter().print(JSweetConfig.GLOBALS_PACKAGE_NAME).print(".");
				}
			}
			Map<String, VarSymbol> vars = new HashMap<>();
			Util.fillAllVariablesInScope(vars, getPrinter().getStack(), invocation, getPrinter().getParent(JCMethodDecl.class));
			if (vars.containsKey(targetMethodName)) {
				report(invocation, JSweetProblem.HIDDEN_INVOCATION, targetMethodName);
			}
			getPrinter().printIdentifier(targetMethodName).print("(").printArgList(invocation.args).print(")");
			return true;
		}
		if (fieldAccess == null && matchesMethod(targetClassName, targetMethodName, null, "$super")) {
			getPrinter().print("super(").printArgList(invocation.args).print(")");
			return true;
		}
		if (fieldAccess != null && targetClassName != null
				&& (targetClassName.startsWith(UTIL_PACKAGE + ".function.") || targetClassName.startsWith(Function.class.getPackage().getName()))) {
			if (!getPrinter().getContext().options.isJDKAllowed() && targetClassName.startsWith(Function.class.getPackage().getName())
					&& TypeChecker.FORBIDDEN_JDK_FUNCTIONAL_METHODS.contains(targetMethodName)) {
				getPrinter().report(invocation, JSweetProblem.JDK_METHOD, targetMethodName);
			}
			getPrinter().print(fieldAccess.getExpression()).print("(").printArgList(invocation.args).print(")");
			return true;
		}
		if (fieldAccess != null && targetClassName != null && targetClassName.equals(java.lang.Runnable.class.getName())) {
			getPrinter().print(fieldAccess.getExpression()).print("(").printArgList(invocation.args).print(")");
			return true;
		}

		// built-in Java support

		if (targetClassName != null) {

			// expand macros
			switch (targetMethodName) {
			case "getMessage":
				if (targetType instanceof ClassSymbol) {
					if (Util.hasParent((ClassSymbol) targetType, "java.lang.Throwable")) {
						getPrinter().print(fieldAccess.getExpression()).print(".message");
						return true;
					}
				}
				break;
			case "getCause":
				if (targetType instanceof ClassSymbol) {
					if (Util.hasParent((ClassSymbol) targetType, "java.lang.Throwable")) {
						getPrinter().print("(<Error>null)");
						return true;
					}
				}
				break;
			}

			switch (targetClassName) {
			case "java.lang.String":
			case "java.lang.CharSequence":
				switch (targetMethodName) {
				case "valueOf":
					printMacroName(targetMethodName);
					if (invocation.args.length() == 3) {
						getPrinter().print("((str, index, len) => str.join('').substring(index, index + len))(").printArgList(invocation.args).print(")");
					} else {
						getPrinter().print("new String(").printArgList(invocation.args).print(").toString()");
					}
					return true;
				case "subSequence":
					printMacroName(targetMethodName);
					getPrinter().print(fieldAccess.getExpression()).print(".substring(").printArgList(invocation.args).print(")");
					return true;
				case "length":
					getPrinter().print(fieldAccess.getExpression()).print(".length");
					return true;
				// this macro is not needed in ES6
				case "startsWith":
					printMacroName(targetMethodName);
					getPrinter().print("((str, searchString, position = 0) => str.substr(position, searchString.length) === searchString)(")
							.print(fieldAccess.getExpression()).print(", ").printArgList(invocation.args).print(")");
					return true;
				// this macro is not needed in ES6
				case "codePointAt":
					printMacroName(targetMethodName);
					getPrinter().print(fieldAccess.getExpression()).print(".charCodeAt(").printArgList(invocation.args).print(")");
					return true;
				case "isEmpty":
					printMacroName(targetMethodName);
					getPrinter().print("(").print(fieldAccess.getExpression()).print(".length === 0)");
					return true;
				case "compareToIgnoreCase":
					printMacroName(targetMethodName);
					getPrinter().print(fieldAccess.getExpression()).print(".toUpperCase().localeCompare(").printArgList(invocation.args)
							.print(".toUpperCase())");
					return true;
				case "compareTo":
					printMacroName(targetMethodName);
					getPrinter().print(fieldAccess.getExpression()).print(".localeCompare(").printArgList(invocation.args).print(")");
					return true;
				case "equalsIgnoreCase":
					printMacroName(targetMethodName);
					getPrinter().print("((o1, o2) => o1.toUpperCase() === (o2===null?o2:o2.toUpperCase()))(").print(fieldAccess.getExpression()).print(", ")
							.printArgList(invocation.args).print(")");
					return true;
				case "toChars":
					printMacroName(targetMethodName);
					getPrinter().print("String.fromCharCode(").printArgList(invocation.args).print(")");
					return true;
				// In ES6, we can use the Array.from method
				case "getBytes":
					printMacroName(targetMethodName);
					getPrinter().print("(").print(fieldAccess.getExpression()).print(").split('').map(s => s.charCodeAt(0))");
					return true;
				// In ES6, we can use the Array.from method
				case "toCharArray":
					printMacroName(targetMethodName);
					getPrinter().print("(").print(fieldAccess.getExpression()).print(").split('')");
					return true;
				}
				break;
			case "java.lang.Character":
				switch (targetMethodName) {
				case "toChars":
					printMacroName(targetMethodName);
					getPrinter().print("String.fromCharCode(").printArgList(invocation.args).print(")");
					return true;
				}
				break;
			case "java.lang.Float":
			case "java.lang.Double":
			case "java.lang.Integer":
			case "java.lang.Byte":
			case "java.lang.Long":
			case "java.lang.Short":
				switch (targetMethodName) {
				case "isNaN":
					printMacroName(targetMethodName);
					if (!invocation.args.isEmpty()) {
						getPrinter().print("(Number.NaN === ").printArgList(invocation.args).print(")");
						return true;
					} else {
						getPrinter().print("(Number.NaN === ").print(fieldAccess.getExpression()).print(")");
						return true;
					}
				case "isInfinite":
					printMacroName(targetMethodName);
					if (!invocation.args.isEmpty()) {
						getPrinter().print("((value) => Number.NEGATIVE_INFINITY === value || Number.POSITIVE_INFINITY === value)(")
								.printArgList(invocation.args).print(")");
						return true;
					} else {
						getPrinter().print("((value) => Number.NEGATIVE_INFINITY === value || Number.POSITIVE_INFINITY === value)(")
								.print(fieldAccess.getExpression()).print(")");
						return true;
					}
				case "toString":
					if (!invocation.args.isEmpty()) {
						printMacroName(targetMethodName);
						getPrinter().print("(''+").print(invocation.args.head).print(")");
						return true;
					}
				}
				break;
			case "java.lang.Math":
				switch (targetMethodName) {
				case "cbrt":
					printMacroName(targetMethodName);
					getPrinter().print("Math.pow(").printArgList(invocation.args).print(", 1/3)");
					return true;
				case "copySign":
					printMacroName(targetMethodName);
					getPrinter()
							.print("((magnitude, sign) => { if (sign < 0) { return (magnitude < 0) ? magnitude : -magnitude; } else { return (magnitude > 0) ? magnitude : -magnitude; } })(")
							.printArgList(invocation.args).print(")");
					return true;
				case "cosh":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => (Math.exp(x) + Math.exp(-x)) / 2)(").printArgList(invocation.args).print(")");
					return true;
				case "expm1":
					printMacroName(targetMethodName);
					getPrinter()
							.print("((d) => { if (d == 0.0 || d === Number.NaN) { return d; } else if (!Number.POSITIVE_INFINITY === d && !Number.NEGATIVE_INFINITY === d) { if (d < 0) { return -1; } else { return Number.POSITIVE_INFINITY; } } })(")
							.printArgList(invocation.args).print(")");
					return true;
				case "hypot":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => Math.sqrt(x * x + y * y))(").printArgList(invocation.args).print(")");
					return true;
				case "log10":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => Math.log(x) * Math.LOG10E)(").printArgList(invocation.args).print(")");
					return true;
				case "log1p":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => Math.log(x + 1))(").printArgList(invocation.args).print(")");
					return true;
				case "rint":
					printMacroName(targetMethodName);
					getPrinter()
							.print("((d) => { if (d === Number.NaN) { return d; } else if (Number.POSITIVE_INFINITY === d || Number.NEGATIVE_INFINITY === d) { return d; } else if(d == 0) { return d; } else { return Math.round(d); } })(")
							.printArgList(invocation.args).print(")");
					return true;
				case "scalb":
					printMacroName(targetMethodName);
					getPrinter()
							.print("((d, scaleFactor) => { if (scaleFactor >= 31 || scaleFactor <= -31) { return d * Math.pow(2, scaleFactor); } else if (scaleFactor > 0) { return d * (1 << scaleFactor); } else if (scaleFactor == 0) { return d; } else { return d * 1 / (1 << -scaleFactor); } })(")
							.printArgList(invocation.args).print(")");
					return true;
				case "signum":
					printMacroName(targetMethodName);
					getPrinter().print("((f) => { if (f > 0) { return 1; } else if (f < 0) { return -1; } else { return 0; } })(").printArgList(invocation.args)
							.print(")");
					return true;
				case "sinh":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => (Math.exp(x) - Math.exp(-x)) / 2)(").printArgList(invocation.args).print(")");
					return true;
				case "tanh":
					printMacroName(targetMethodName);
					getPrinter()
							.print("((x) => { if (x == Number.POSITIVE_INFINITY) { return 1; } else if (x == Number.NEGATIVE_INFINITY) { return -1; } double e2x = Math.exp(2 * x); return (e2x - 1) / (e2x + 1); })(")
							.printArgList(invocation.args).print(")");
					return true;
				case "toDegrees":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => x * 180 / Math.PI; )(").printArgList(invocation.args).print(")");
					return true;
				case "toRadians":
					printMacroName(targetMethodName);
					getPrinter().print("((x) => x * Math.PI / 180; )(").printArgList(invocation.args).print(")");
					return true;
				}
				break;

			case "java.lang.Class":
				switch (targetMethodName) {
				case "getName":
					if (fieldAccess != null && fieldAccess.selected.toString().endsWith(".class")) {
						printMacroName(targetMethodName);
						getPrinter().print("\"").print(fieldAccess.selected.type.getTypeArguments().get(0).tsym.getQualifiedName().toString()).print("\"");
						return true;
					}
					break;
				case "getSimpleName":
					if (fieldAccess != null && fieldAccess.selected.toString().endsWith(".class")) {
						printMacroName(targetMethodName);
						getPrinter().print("\"").print(fieldAccess.selected.type.getTypeArguments().get(0).tsym.getSimpleName().toString()).print("\"");
						return true;
					}
					break;
				}
				break;
			// case "java.util.Date":
			// switch (targetMethodName) {
			// case "setFullYear":
			// printMacroName(targetMethodName);
			// getPrinter().print(fieldAccess.getExpression()).print(".setYear(").printArgList(invocation.args).print(")");
			//
			// }
			// break;
			}

			if (fieldAccess != null && typesMapping.containsKey(targetClassName) && targetClassName.startsWith("java.lang.")) {
				if (fieldAccess.sym.isStatic()) {
					// delegation to javaemul
					delegateToEmulLayer(targetClassName, targetMethodName, invocation);
					return true;
				} else {
					switch (targetMethodName) {
					case "equals":
						getPrinter().print("(").print(fieldAccess.getExpression()).print(" === ").printArgList(invocation.args).print(")");
						return true;
					case "hashCode":
						getPrinter().print("(<any>").print(fieldAccess.getExpression()).print(".toString())");
						return true;
					}
				}
			}
		}

		if (!JSweetConfig.isJDKReplacementMode())

		{
			Log log = Log.instance(getPrinter().getContext());
			if (String.class.getName().equals(targetClassName)) {
				log.rawError(invocation.pos, "Invalid use of native Java class. Use string(a_java_string) to convert to JSweet String first.");
			}
		}

		return super.substituteMethodInvocation(invocation);

	}

	private void delegateToEmulLayer(String targetClassName, String targetMethodName, JCMethodInvocation invocation) {
		getPrinter().print("javaemul.internal." + targetClassName.substring(10) + "Helper.").print(targetMethodName).print("(")
				.printArgList(invocation.getArguments()).print(")");
	}

	private void printMacroName(String macroName) {
		getPrinter().print("/* " + macroName + " */");
	}

	private void printCastMethodInvocation(JCMethodInvocation invocation) {
		if (getPrinter().getParent() instanceof JCMethodInvocation) {
			getPrinter().print("(");
		}
		getPrinter().print(invocation.args.head);
		if (getPrinter().getParent() instanceof JCMethodInvocation) {
			getPrinter().print(")");
		}
	}

	@Override
	public boolean substituteFieldAccess(JCFieldAccess fieldAccess) {
		String name = fieldAccess.name.toString();

		// translate tuple accesses
		if (name.startsWith("$") && name.length() > 1 && Character.isDigit(name.charAt(1))) {
			try {
				int i = Integer.parseInt(name.substring(1));
				getPrinter().print(fieldAccess.selected);
				getPrinter().print("[" + i + "]");
				return true;
			} catch (NumberFormatException e) {
				// swallow
			}
		}

		AnnotationMirror annotation;
		if ((annotation = Util.getAnnotation(fieldAccess.sym, ANNOTATION_STRING_TYPE)) != null) {
			getPrinter().print("\"");
			getPrinter().printIdentifier(getFirstAnnotationValue(annotation, fieldAccess.name).toString());
			getPrinter().print("\"");
			return true;
		}
		String selected = fieldAccess.selected.toString();
		if (selected.equals(GLOBALS_CLASS_NAME)) {
			getPrinter().printIdentifier(fieldAccess.name.toString());
			return true;
		}

		if (fieldAccess.selected.type.tsym instanceof PackageSymbol) {
			if (Util.hasAnnotationType(fieldAccess.selected.type.tsym, ANNOTATION_ROOT)) {
				if (fieldAccess.type != null && fieldAccess.type.tsym != null) {
					getPrinter().printIdentifier(Util.getActualName(fieldAccess.type.tsym));
				} else {
					getPrinter().printIdentifier(name);
				}
				return true;
			}
		}

		if (fieldAccess.selected.toString().equals("this")) {
			if (fieldAccess.sym.isStatic()) {
				report(fieldAccess, JSweetProblem.CANNOT_ACCESS_STATIC_MEMBER_ON_THIS, fieldAccess.name);
			}
		}

		// built-in Java support
		String accessedType = fieldAccess.selected.type.tsym.getQualifiedName().toString();
		if (fieldAccess.sym.isStatic() && typesMapping.containsKey(accessedType) && accessedType.startsWith("java.lang.")
				&& !"class".equals(fieldAccess.name.toString())) {
			delegateToEmulLayer(accessedType, fieldAccess);
			return true;
		}

		return super.substituteFieldAccess(fieldAccess);
	}

	private void delegateToEmulLayer(String targetClassName, JCFieldAccess fieldAccess) {
		getPrinter().print("javaemul.internal." + targetClassName.substring(10) + "Helper.").print(fieldAccess.name.toString());
	}

	private AbstractTreePrinter printArguments(List<JCExpression> arguments) {
		int i = 1;
		for (JCExpression argument : arguments) {
			printArgument(argument, i++).print(", ");
		}
		if (arguments.size() > 0) {
			getPrinter().removeLastChars(2);
		}
		return getPrinter();
	}

	private AbstractTreePrinter printArgument(JCExpression argument, int i) {
		getPrinter().print("p" + i + ": ");
		substituteAndPrintType(argument, false, false, true, false);
		return getPrinter();
	}

	@Override
	public boolean substituteNewClass(JCNewClass newClass) {
		String fullType = newClass.type.tsym.toString();
		if (fullType.startsWith(JSweetConfig.TUPLE_CLASSES_PACKAGE + ".")) {
			getPrinter().print("[").printArgList(newClass.args).print("]");
			return true;
		}
		if (typesMapping.containsKey(fullType)) {
			getPrinter().print("<").print(typesMapping.get(fullType)).print(">");
		}
		// macros
		if (newClass.clazz.type.equals(getPrinter().getContext().symtab.stringType)) {
			if (newClass.args.length() >= 3) {
				getPrinter().print("((str, index, len) => ").print("str.substring(index, index + len))((").print(newClass.args.head).print(")");
				if ("byte[]".equals(newClass.args.get(0).type.toString())) {
					getPrinter().print(".map(s => String.fromCharCode(s))");
				}
				getPrinter().print(".join(''), ");
				getPrinter().print(newClass.args.tail.head).print(", ").print(newClass.args.tail.tail.head).print(")");
				return true;
			}
		}
		return super.substituteNewClass(newClass);
	}

	@Override
	public AbstractTreePrinter substituteAndPrintType(JCTree typeTree, boolean arrayComponent, boolean inTypeParameters, boolean completeRawTypes,
			boolean disableSubstitution) {
		if (typeTree.type.tsym instanceof TypeVariableSymbol) {
			if (typeVariablesToErase.contains(typeTree.type.tsym)) {
				return getPrinter().print("any");
			}
		}
		if (!disableSubstitution) {
			if (Util.hasAnnotationType(typeTree.type.tsym, ANNOTATION_ERASED)) {
				return getPrinter().print("any");
			}
			if (Util.hasAnnotationType(typeTree.type.tsym, ANNOTATION_OBJECT_TYPE)) {
				// TODO: in case of object types, we should replace with the org
				// object type...
				return getPrinter().print("any");
			}
			String typeFullName = typeTree.type.getModelType().toString(); // typeTree.type.tsym.getQualifiedName().toString();
			if (Runnable.class.getName().equals(typeFullName)) {
				if (arrayComponent) {
					getPrinter().print("(");
				}
				getPrinter().print("() => void");
				if (arrayComponent) {
					getPrinter().print(")");
				}
				return getPrinter();
			}
			if (typeTree instanceof JCTypeApply) {
				JCTypeApply typeApply = ((JCTypeApply) typeTree);
				String typeName = typeApply.clazz.toString();
				if (typeFullName.startsWith(TUPLE_CLASSES_PACKAGE + ".")) {
					getPrinter().print("[");
					for (JCExpression argument : typeApply.arguments) {
						substituteAndPrintType(argument, arrayComponent, inTypeParameters, completeRawTypes, false).print(",");
					}
					if (typeApply.arguments.length() > 0) {
						getPrinter().removeLastChar();
					}
					getPrinter().print("]");
					return getPrinter();
				}
				if (typeFullName.startsWith(UNION_CLASS_NAME)) {
					getPrinter().print("(");
					for (JCExpression argument : typeApply.arguments) {
						substituteAndPrintType(argument, arrayComponent, inTypeParameters, completeRawTypes, false).print("|");
					}
					if (typeApply.arguments.length() > 0) {
						getPrinter().removeLastChar();
					}
					getPrinter().print(")");
					return getPrinter();
				}
				if (typeFullName.startsWith(UTIL_PACKAGE + ".") || typeFullName.startsWith("java.util.function.")) {
					if (typeName.endsWith("Consumer")) {
						if (arrayComponent) {
							getPrinter().print("(");
						}
						getPrinter().print("(");
						printArguments(typeApply.arguments);
						getPrinter().print(") => void");
						if (arrayComponent) {
							getPrinter().print(")");
						}
						return getPrinter();
					} else if (typeName.endsWith("Function")) {
						if (arrayComponent) {
							getPrinter().print("(");
						}
						getPrinter().print("(");
						printArguments(typeApply.arguments.subList(0, typeApply.arguments.length() - 1));
						getPrinter().print(") => ");
						substituteAndPrintType(typeApply.arguments.get(typeApply.arguments.length() - 1), arrayComponent, inTypeParameters, completeRawTypes,
								false);
						if (arrayComponent) {
							getPrinter().print(")");
						}
						return getPrinter();
					} else if (typeName.endsWith("Supplier")) {
						if (arrayComponent) {
							getPrinter().print("(");
						}
						getPrinter().print("(");
						getPrinter().print(") => ");
						substituteAndPrintType(typeApply.arguments.get(0), arrayComponent, inTypeParameters, completeRawTypes, false);
						if (arrayComponent) {
							getPrinter().print(")");
						}
						return getPrinter();
					} else if (typeName.endsWith("Predicate")) {
						if (arrayComponent) {
							getPrinter().print("(");
						}
						getPrinter().print("(");
						printArguments(typeApply.arguments);
						getPrinter().print(") => boolean");
						if (arrayComponent) {
							getPrinter().print(")");
						}
						return getPrinter();
					} else if (typeName.endsWith("Operator")) {
						if (arrayComponent) {
							getPrinter().print("(");
						}
						getPrinter().print("(");
						printArgument(typeApply.arguments.head, 1);
						if (typeName.startsWith("Binary")) {
							printArgument(typeApply.arguments.head, 2);
						}
						printArguments(typeApply.arguments);
						getPrinter().print(") => ");
						substituteAndPrintType(typeApply.arguments.head, arrayComponent, inTypeParameters, completeRawTypes, false);
						if (arrayComponent) {
							getPrinter().print(")");
						}
						return getPrinter();
					}
				}
				if (typeFullName.startsWith(Class.class.getName() + "<")) {
					if (typeApply.arguments.head.type.tsym instanceof TypeVariableSymbol || typeApply.arguments.head.toString().startsWith("?")) {
						return getPrinter().print("any");
					} else {
						getPrinter().print("typeof ");
						if (langTypesMapping.containsKey(typeApply.arguments.head.type.tsym.getQualifiedName().toString())) {
							return getPrinter().print(langTypesMapping.get(typeApply.arguments.head.type.tsym.getQualifiedName().toString()));
						} else {
							return substituteAndPrintType(typeApply.arguments.head, arrayComponent, inTypeParameters, completeRawTypes, disableSubstitution);
						}
					}
				}
			} else {
				if (typesMapping.containsKey(typeFullName)) {
					return getPrinter().print(typesMapping.get(typeFullName));
				}
			}
		}
		return super.substituteAndPrintType(typeTree, arrayComponent, inTypeParameters, completeRawTypes, disableSubstitution);
	}

	@Override
	public boolean substituteIdentifier(JCIdent identifier) {
		AnnotationMirror annotation;
		if ((annotation = Util.getAnnotation(identifier.sym, ANNOTATION_STRING_TYPE)) != null) {
			getPrinter().print("\"");
			getPrinter().printIdentifier(getFirstAnnotationValue(annotation, identifier).toString());
			getPrinter().print("\"");
			return true;
		}
		if (identifier.sym.owner.getQualifiedName().toString().endsWith("." + GLOBALS_CLASS_NAME)) {
			getPrinter().printIdentifier(identifier.toString());
			return true;
		}
		if (langTypesSimpleNames.contains(identifier.toString()) && langTypesMapping.containsKey(identifier.type.toString())) {
			getPrinter().print(langTypesMapping.get(identifier.type.toString()));
			return true;
		}
		if (identifier.type.toString().startsWith("java.lang.")) {
			if (("java.lang." + identifier.toString()).equals(identifier.type.toString())) {
				// it is a java.lang class being referenced, so we expand
				// its name
				getPrinter().print(identifier.type.toString());
				return true;
			}
		}
		return super.substituteIdentifier(identifier);
	}

	@Override
	public boolean needsTypeCast(JCTypeCast cast) {
		if (Util.hasAnnotationType(cast.clazz.type.tsym, ANNOTATION_ERASED, ANNOTATION_OBJECT_TYPE, ANNOTATION_FUNCTIONAL_INTERFACE)) {
			return false;
		} else {
			return super.needsTypeCast(cast);
		}
	}

	@Override
	public String getIdentifier(String identifier) {
		return JSweetConfig.toJsIdentifier(identifier);
	}

	@Override
	public String getQualifiedTypeName(TypeSymbol type, boolean globals) {
		String qualifiedName = super.getQualifiedTypeName(type, globals);
		if (langTypesMapping.containsKey(type.getQualifiedName().toString())) {
			qualifiedName = langTypesMapping.get(type.getQualifiedName().toString());
		} else {
			if (getPrinter().getContext().useModules) {
				int dotIndex = qualifiedName.lastIndexOf(".");
				qualifiedName = qualifiedName.substring(dotIndex + 1);
			}
			if (globals) {
				int dotIndex = qualifiedName.lastIndexOf(".");
				if (dotIndex == -1) {
					qualifiedName = "";
				} else {
					qualifiedName = qualifiedName.substring(0, dotIndex);
				}
			}
		}
		return qualifiedName;
	}

	@Override
	public Set<String> getErasedTypes() {
		return langTypesMapping.keySet();
	}
}
