/*******************************************************************************
* Copyright (c) 2011 Luaj.org. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.lualoader.vm2.lib.jse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

import com.nepqneko.lualoader.lualoader;
import org.luaj.lualoader.vm2.Lua;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.VarArgFunction;

/**
 * LuaValue that represents a Java class.
 * <p>
 * Will respond to get() and set() by returning field values, or java methods. 
 * <p>
 * This class is not used directly.  
 * It is returned by calls to {@link CoerceJavaToLua#coerce(Object)} 
 * when a Class is supplied.
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
public class JavaClass extends JavaInstance implements CoerceJavaToLua.Coercion {

	static final Map classes = Collections.synchronizedMap(new HashMap());

	static final LuaValue NEW = valueOf("new");
	
	Map fields;
	Map methods;
	Map innerclasses;

	public static JavaClass forClass(Class c) {
		JavaClass j = (JavaClass) classes.get(c);
		if ( j == null )
			classes.put( c, j = new JavaClass(c) );
		return j;
	}
	
	JavaClass(Class c) {
		super(c);
		this.jclass = this;
	}

	public LuaValue coerce(Object javaValue,String type) {
		return this;
	}
		
	Field getField(LuaValue key) {
		if ( fields == null ) {
			Map m = new HashMap();
			Field[] f = ((Class)m_instance).getFields();
			for ( int i=0; i<f.length; i++ ) {
				Field fi = f[i];
				if ( Modifier.isPublic(fi.getModifiers()) ) {
					m.put(LuaValue.valueOf(fi.getName()), fi);
					try {
						if (!fi.isAccessible())
							fi.setAccessible(true);
					} catch (SecurityException s) {
					}
				}
			}
			fields = m;
		}

		Class obj = (Class)m_instance;
		LuaValue ct = lualoader.mcptosrg.get(obj.getName());

		if (ct.istable()){
			boolean isfind = false;

			if (!fields.containsKey(key) && lualoader.mcptosrg != null){
				LuaValue nkey = ct.get(key);

				if (!nkey.isnil()){
					key = nkey.get("name");
					isfind = true;
				}
			}

			if (!isfind && !fields.containsKey(key) && lualoader.mcptosrg != null){
				Class obj2 = (Class)m_instance;

				for (int i = 0;i < 10;i++){
					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;

					LuaValue tbl = lualoader.mcptosrg.get(obj2.getName());

					if (tbl.istable()){
						LuaValue nkey = tbl.get(key);

						if (!nkey.isnil()){
							key = nkey.get("name");
							isfind = true;

							break;
						}
					}
				}
			}

			if (!isfind && !fields.containsKey(key) && lualoader.mcptosrg != null){
				Class obj2 = (Class)m_instance;

				for (int i = 0;i < 10;i++){
					for (Class obji:obj2.getInterfaces()){
						LuaValue tbl = lualoader.mcptosrg.get(obji.getName());

						if (tbl.istable()){
							LuaValue nkey = tbl.get(key);

							if (!nkey.isnil()){
								key = nkey.get("name");

								break;
							}
						}

						for (Class obji2:obji.getInterfaces()){
							LuaValue tbl2 = lualoader.mcptosrg.get(obji2.getName());

							if (tbl2.istable()){
								LuaValue nkey = tbl2.get(key);

								if (!nkey.isnil()){
									key = nkey.get("name");

									break;
								}
							}

							for (Class obji3:obji2.getInterfaces()){
								LuaValue tbl3 = lualoader.mcptosrg.get(obji3.getName());

								if (tbl3.istable()){
									LuaValue nkey = tbl3.get(key);

									if (!nkey.isnil()){
										key = nkey.get("name");

										break;
									}
								}

								for (Class obji4:obji3.getInterfaces()){
									LuaValue tbl4 = lualoader.mcptosrg.get(obji4.getName());

									if (tbl4.istable()){
										LuaValue nkey = tbl4.get(key);

										if (!nkey.isnil()){
											key = nkey.get("name");

											break;
										}
									}

									for (Class obji5:obji4.getInterfaces()){
										LuaValue tbl5 = lualoader.mcptosrg.get(obji5.getName());

										if (tbl5.istable()){
											LuaValue nkey = tbl5.get(key);

											if (!nkey.isnil()){
												key = nkey.get("name");

												break;
											}
										}

										for (Class obji6:obji5.getInterfaces()){
											LuaValue tbl6 = lualoader.mcptosrg.get(obji6.getName());

											if (tbl6.istable()){
												LuaValue nkey = tbl6.get(key);

												if (!nkey.isnil()){
													key = nkey.get("name");

													break;
												}
											}

											for (Class obji7:obji6.getInterfaces()){
												LuaValue tbl7 = lualoader.mcptosrg.get(obji7.getName());

												if (tbl7.istable()){
													LuaValue nkey = tbl7.get(key);

													if (!nkey.isnil()){
														key = nkey.get("name");

														break;
													}
												}

												for (Class obji8:obji7.getInterfaces()){
													LuaValue tbl8 = lualoader.mcptosrg.get(obji8.getName());

													if (tbl8.istable()){
														LuaValue nkey = tbl8.get(key);

														if (!nkey.isnil()){
															key = nkey.get("name");

															break;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}

					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;
				}
			}
		}

		if (key.tojstring().equals("isRemote")) return null;

		return (Field)fields.get(key);
	}
	
	LuaValue getMethod(LuaValue key) {
		if ( methods == null ) {
			Map namedlists = new HashMap();
			Method[] m = ((Class)m_instance).getMethods();
			for ( int i=0; i<m.length; i++ ) {
				Method mi = m[i];
				if ( Modifier.isPublic( mi.getModifiers()) ) {
					String name = mi.getName();
					List list = (List) namedlists.get(name);
					if ( list == null )
						namedlists.put(name, list = new ArrayList());
					list.add( JavaMethod.forMethod(mi) );
				}
			}
			Map map = new HashMap();
			Constructor[] c = ((Class)m_instance).getConstructors();
			List list = new ArrayList();
			for ( int i=0; i<c.length; i++ )
				if ( Modifier.isPublic(c[i].getModifiers()) )
					list.add( JavaConstructor.forConstructor(c[i]) );
			switch ( list.size() ) {
			case 0: break;
			case 1: map.put(NEW, list.get(0)); break;
			default: map.put(NEW, JavaConstructor.forConstructors( (JavaConstructor[])list.toArray(new JavaConstructor[list.size()]) ) ); break;
			}

			for ( Iterator it=namedlists.entrySet().iterator(); it.hasNext(); ) {
				Entry e = (Entry) it.next();
				String name = (String) e.getKey();
				List methods = (List) e.getValue();
				map.put( LuaValue.valueOf(name),
					methods.size()==1?
						methods.get(0):
						JavaMethod.forMethods( (JavaMethod[])methods.toArray(new JavaMethod[methods.size()])) );
			}
			methods = map;
		}

		Class obj = (Class)m_instance;
		LuaValue ct = lualoader.mcptosrg.get(obj.getName());

		if (ct.istable()){
			if (!methods.containsKey(key) && lualoader.mcptosrg != null){
				LuaValue nkey = ct.get(key);

				if (!nkey.isnil()){
					LuaValue ft = lualoader.functable.get(obj.getName());

					if (!ft.isnil()){
						LuaValue ftk = ft.get(key);

						if (!ftk.isnil()){
							return new fixfunc(methods,ftk,key,(Class)m_instance);
						}
					}

					return new fixfunc2(methods,key,nkey,(Class)m_instance);
				}
			}

			if (!methods.containsKey(key) && lualoader.mcptosrg != null){
				Class obj2 = (Class)m_instance;

				for (int i = 0;i < 10;i++){
					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;

					LuaValue tbl = lualoader.mcptosrg.get(obj2.getName());

					if (tbl.istable()){
						LuaValue nkey = tbl.get(key);

						if (!nkey.isnil()){
							LuaValue ft = lualoader.functable.get(obj2.getName());

							if (!ft.isnil()){
								LuaValue ftk = ft.get(key);

								if (!ftk.isnil()){
									return new fixfunc(methods,ftk,key,(Class)m_instance);
								}
							}

							return new fixfunc2(methods,key,nkey,(Class)m_instance);
						}
					}
				}
			}

			if (!methods.containsKey(key) && lualoader.mcptosrg != null){
				Class obj2 = (Class)m_instance;

				for (int i = 0;i < 10;i++){
					for (Class obji:obj2.getInterfaces()){
						LuaValue tbl = lualoader.mcptosrg.get(obji.getName());

						if (tbl.istable()){
							LuaValue nkey = tbl.get(key);

							if (!nkey.isnil()){
								LuaValue ft = lualoader.functable.get(obji.getName());

								if (!ft.isnil()){
									LuaValue ftk = ft.get(key);

									if (!ftk.isnil()){
										return new fixfunc(methods,ftk,key,(Class)m_instance);
									}
								}

								return new fixfunc2(methods,key,nkey,(Class)m_instance);
							}
						}

						for (Class obji2:obji.getInterfaces()){
							LuaValue tbl2 = lualoader.mcptosrg.get(obji2.getName());

							if (tbl2.istable()){
								LuaValue nkey = tbl2.get(key);

								if (!nkey.isnil()){
									LuaValue ft = lualoader.functable.get(obji2.getName());

									if (!ft.isnil()){
										LuaValue ftk = ft.get(key);

										if (!ftk.isnil()){
											return new fixfunc(methods,ftk,key,(Class)m_instance);
										}
									}

									return new fixfunc2(methods,key,nkey,(Class)m_instance);
								}
							}

							for (Class obji3:obji2.getInterfaces()){
								LuaValue tbl3 = lualoader.mcptosrg.get(obji3.getName());

								if (tbl3.istable()){
									LuaValue nkey = tbl3.get(key);

									if (!nkey.isnil()){
										LuaValue ft = lualoader.functable.get(obji3.getName());

										if (!ft.isnil()){
											LuaValue ftk = ft.get(key);

											if (!ftk.isnil()){
												return new fixfunc(methods,ftk,key,(Class)m_instance);
											}
										}

										return new fixfunc2(methods,key,nkey,(Class)m_instance);
									}
								}

								for (Class obji4:obji3.getInterfaces()){
									LuaValue tbl4 = lualoader.mcptosrg.get(obji4.getName());

									if (tbl4.istable()){
										LuaValue nkey = tbl4.get(key);

										if (!nkey.isnil()){
											LuaValue ft = lualoader.functable.get(obji4.getName());

											if (!ft.isnil()){
												LuaValue ftk = ft.get(key);

												if (!ftk.isnil()){
													return new fixfunc(methods,ftk,key,(Class)m_instance);
												}
											}

											return new fixfunc2(methods,key,nkey,(Class)m_instance);
										}
									}

									for (Class obji5:obji4.getInterfaces()){
										LuaValue tbl5 = lualoader.mcptosrg.get(obji5.getName());

										if (tbl5.istable()){
											LuaValue nkey = tbl5.get(key);

											if (!nkey.isnil()){
												LuaValue ft = lualoader.functable.get(obji5.getName());

												if (!ft.isnil()){
													LuaValue ftk = ft.get(key);

													if (!ftk.isnil()){
														return new fixfunc(methods,ftk,key,(Class)m_instance);
													}
												}

												return new fixfunc2(methods,key,nkey,(Class)m_instance);
											}
										}

										for (Class obji6:obji5.getInterfaces()){
											LuaValue tbl6 = lualoader.mcptosrg.get(obji6.getName());

											if (tbl6.istable()){
												LuaValue nkey = tbl6.get(key);

												if (!nkey.isnil()){
													LuaValue ft = lualoader.functable.get(obji6.getName());

													if (!ft.isnil()){
														LuaValue ftk = ft.get(key);

														if (!ftk.isnil()){
															return new fixfunc(methods,ftk,key,(Class)m_instance);
														}
													}

													return new fixfunc2(methods,key,nkey,(Class)m_instance);
												}
											}

											for (Class obji7:obji6.getInterfaces()){
												LuaValue tbl7 = lualoader.mcptosrg.get(obji7.getName());

												if (tbl7.istable()){
													LuaValue nkey = tbl7.get(key);

													if (!nkey.isnil()){
														LuaValue ft = lualoader.functable.get(obji7.getName());

														if (!ft.isnil()){
															LuaValue ftk = ft.get(key);

															if (!ftk.isnil()){
																return new fixfunc(methods,ftk,key,(Class)m_instance);
															}
														}

														return new fixfunc2(methods,key,nkey,(Class)m_instance);
													}
												}

												for (Class obji8:obji7.getInterfaces()){
													LuaValue tbl8 = lualoader.mcptosrg.get(obji8.getName());

													if (tbl8.istable()){
														LuaValue nkey = tbl8.get(key);

														if (!nkey.isnil()){
															LuaValue ft = lualoader.functable.get(obji8.getName());

															if (!ft.isnil()){
																LuaValue ftk = ft.get(key);

																if (!ftk.isnil()){
																	return new fixfunc(methods,ftk,key,(Class)m_instance);
																}
															}

															return new fixfunc2(methods,key,nkey,(Class)m_instance);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}

					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;
				}
			}
		}

		return (LuaValue)methods.get(key);
	}
	
	Class getInnerClass(LuaValue key) {
		if ( innerclasses == null ) {
			Map m = new HashMap();
			Class[] c = ((Class)m_instance).getClasses();
			for ( int i=0; i<c.length; i++ ) {
				Class ci = c[i];
				String name = ci.getName();
				String stub = name.substring(Math.max(name.lastIndexOf('$'), name.lastIndexOf('.'))+1);
				m.put(LuaValue.valueOf(stub), ci);
			}
			innerclasses = m;
		}

		return (Class)innerclasses.get(key);
	}

	final class fixfunc extends VarArgFunction {
		Map methods;
		LuaValue ftk;
		LuaValue key;
		Class m_instance;

		fixfunc(Map methods, LuaValue ftk, LuaValue key, Class m_instance) {
			this.methods = methods;
			this.ftk = ftk;
			this.key = key;
			this.m_instance = m_instance;
		}

		public Varargs invoke(Varargs args) {
			LuaTable tbl = ftk.checktable();
			LuaValue k = NIL;

			while (true) {
				Varargs n = tbl.next(k);

				if ((k = n.arg1()).isnil()) break;

				LuaValue v = n.arg(2);

				try {
					LuaValue func = (LuaValue)methods.get(v.get("name"));

					return func.invoke(args);
				}
				catch (Exception e){

				}

//				if (v.get("argn").toint() == args.narg() - 1 && (checkargs(args.subargs(2),v.get("type")) || (v.get("argn").toint() == 0 && (args.narg()-1) == 0))) {
//					LuaValue func = (LuaValue)methods.get(v.get("name"));
//
//					return func.invoke(args);
//				}
			}

			if (!methods.containsKey(key) && lualoader.mcptosrg != null){
				Class obj2 = m_instance;

				for (int i = 0;i < 10;i++){
					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;

					LuaValue tbl2 = lualoader.mcptosrg.get(obj2.getName());

					if (tbl2.istable()){
						LuaValue nkey = tbl2.get(key);

						if (!nkey.isnil()){
							LuaValue ft = lualoader.functable.get(obj2.getName());

							if (!ft.isnil()){
								LuaValue ftk = ft.get(key);

								if (!ftk.isnil() && !ftk.equals(this.ftk)){
									LuaValue func = new fixfunc(methods,ftk,key,m_instance);

									return func.invoke(args);
								}
							}

							LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

							return func.invoke(args);
						}
					}
				}
			}

			if (!methods.containsKey(key) && lualoader.mcptosrg != null) {
				Class obj2 = m_instance;

				for (int i = 0; i < 10; i++) {
					for (Class obji : obj2.getInterfaces()) {
						LuaValue tbl2 = lualoader.mcptosrg.get(obji.getName());

						if (tbl2.istable()) {
							LuaValue nkey = tbl2.get(key);

							if (!nkey.isnil()) {
								LuaValue ft = lualoader.functable.get(obji.getName());

								if (!ft.isnil()) {
									LuaValue ftk = ft.get(key);

									if (!ftk.isnil() && !ftk.equals(this.ftk)) {
										LuaValue func = new fixfunc(methods,ftk,key,m_instance);

										return func.invoke(args);
									}
								}

								LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

								return func.invoke(args);
							}
						}

						for (Class obji2 : obji.getInterfaces()) {
							LuaValue tbl3 = lualoader.mcptosrg.get(obji2.getName());

							if (tbl3.istable()) {
								LuaValue nkey = tbl3.get(key);

								if (!nkey.isnil()) {
									LuaValue ft = lualoader.functable.get(obji2.getName());

									if (!ft.isnil()) {
										LuaValue ftk = ft.get(key);

										if (!ftk.isnil() && !ftk.equals(this.ftk)) {
											LuaValue func = new fixfunc(methods,ftk,key,m_instance);

											return func.invoke(args);
										}
									}

									LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

									return func.invoke(args);
								}
							}

							for (Class obji3 : obji2.getInterfaces()) {
								LuaValue tbl4 = lualoader.mcptosrg.get(obji3.getName());

								if (tbl4.istable()) {
									LuaValue nkey = tbl4.get(key);

									if (!nkey.isnil()) {
										LuaValue ft = lualoader.functable.get(obji3.getName());

										if (!ft.isnil()) {
											LuaValue ftk = ft.get(key);

											if (!ftk.isnil() && !ftk.equals(this.ftk)) {
												LuaValue func = new fixfunc(methods,ftk,key,m_instance);

												return func.invoke(args);
											}
										}

										LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

										return func.invoke(args);
									}
								}

								for (Class obji4 : obji3.getInterfaces()) {
									LuaValue tbl5 = lualoader.mcptosrg.get(obji4.getName());

									if (tbl5.istable()) {
										LuaValue nkey = tbl5.get(key);

										if (!nkey.isnil()) {
											LuaValue ft = lualoader.functable.get(obji4.getName());

											if (!ft.isnil()) {
												LuaValue ftk = ft.get(key);

												if (!ftk.isnil() && !ftk.equals(this.ftk)) {
													LuaValue func = new fixfunc(methods,ftk,key,m_instance);

													return func.invoke(args);
												}
											}

											LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

											return func.invoke(args);
										}
									}

									for (Class obji5 : obji4.getInterfaces()) {
										LuaValue tbl6 = lualoader.mcptosrg.get(obji5.getName());

										if (tbl6.istable()) {
											LuaValue nkey = tbl6.get(key);

											if (!nkey.isnil()) {
												LuaValue ft = lualoader.functable.get(obji5.getName());

												if (!ft.isnil()) {
													LuaValue ftk = ft.get(key);

													if (!ftk.isnil() && !ftk.equals(this.ftk)) {
														LuaValue func = new fixfunc(methods,ftk,key,m_instance);

														return func.invoke(args);
													}
												}

												LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

												return func.invoke(args);
											}
										}

										for (Class obji6 : obji5.getInterfaces()) {
											LuaValue tbl7 = lualoader.mcptosrg.get(obji6.getName());

											if (tbl7.istable()) {
												LuaValue nkey = tbl7.get(key);

												if (!nkey.isnil()) {
													LuaValue ft = lualoader.functable.get(obji6.getName());

													if (!ft.isnil()) {
														LuaValue ftk = ft.get(key);

														if (!ftk.isnil() && !ftk.equals(this.ftk)) {
															LuaValue func = new fixfunc(methods,ftk,key,m_instance);

															return func.invoke(args);
														}
													}

													LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

													return func.invoke(args);
												}
											}

											for (Class obji7 : obji6.getInterfaces()) {
												LuaValue tbl8 = lualoader.mcptosrg.get(obji7.getName());

												if (tbl8.istable()) {
													LuaValue nkey = tbl8.get(key);

													if (!nkey.isnil()) {
														LuaValue ft = lualoader.functable.get(obji7.getName());

														if (!ft.isnil()) {
															LuaValue ftk = ft.get(key);

															if (!ftk.isnil() && !ftk.equals(this.ftk)) {
																LuaValue func = new fixfunc(methods,ftk,key,m_instance);

																return func.invoke(args);
															}
														}

														LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

														return func.invoke(args);
													}
												}

												for (Class obji8 : obji7.getInterfaces()) {
													LuaValue tbl9 = lualoader.mcptosrg.get(obji8.getName());

													if (tbl9.istable()) {
														LuaValue nkey = tbl9.get(key);

														if (!nkey.isnil()) {
															LuaValue ft = lualoader.functable.get(obji8.getName());

															if (!ft.isnil()) {
																LuaValue ftk = ft.get(key);

																if (!ftk.isnil() && !ftk.equals(this.ftk)) {
																	LuaValue func = new fixfunc(methods,ftk,key,m_instance);

																	return func.invoke(args);
																}
															}

															LuaValue func = new fixfunc2(methods,key,nkey,m_instance);

															return func.invoke(args);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}

					obj2 = obj2.getSuperclass();

					if (obj2.getName().equals("java.lang.Object")) break;
				}
			}

			LuaValue func = (LuaValue) methods.get(ftk.get(1).get("name"));

			return func.invoke(args);
		}

		private boolean checkargs(Varargs args, LuaValue type) {
			LuaTable tbl = type.checktable();
			LuaValue k = NIL;

			while (true) {
				Varargs n = tbl.next(k);

				if ((k = n.arg1()).isnil()) break;

				LuaValue v = n.arg(2);

				switch (v.tojstring()) {
					case "double":
					case "float":
					case "long": {
						boolean b = args.arg(k.toint()).isnumber();

						if (!b) return false;
					}
					case "int": {
						boolean b = args.arg(k.toint()).isint();

						if (!b) return false;
					}
					case "boolean": {
						boolean b = args.arg(k.toint()).isboolean();

						if (!b) return false;
					}
					default: {
//						if (args.arg(k.toint()).isuserdata()){
//							Object o = args.arg(k.toint()).touserdata();
//							Class c = o.getClass();
//
//							return c.getName().equals(v.tojstring());
//						}
//						else {
//							return false;
//						}
					}
				}
			}

			return true;
		}
	}

	final class fixfunc2 extends VarArgFunction {
		Map methods;
		LuaValue key;
		LuaValue nkey;
		Class m_instance;

		fixfunc2(Map methods,LuaValue key,LuaValue nkey,Class m_instance){
			this.methods = methods;
			this.key = key;
			this.nkey = nkey;
			this.m_instance = m_instance;
		}

		public Varargs invoke(Varargs args){
			Varargs ars = args.subargs(2);

			if (ars.narg() == nkey.get("argn").toint()){
				LuaValue func = (LuaValue)methods.get(nkey.get("name"));

				return func.invoke(args);
			}
			else {
				int dargn = nkey.get("argn").toint();

				if (!methods.containsKey(key) && lualoader.mcptosrg != null){
					Class obj2 = m_instance;

					for (int i = 0;i < 10;i++){
						obj2 = obj2.getSuperclass();

						if (obj2.getName().equals("java.lang.Object")) break;

						LuaValue tbl = lualoader.mcptosrg.get(obj2.getName());

						if (tbl.istable()){
							LuaValue nkey = tbl.get(key);

							if (!nkey.isnil() && nkey.get("argn").toint() != dargn){
								LuaValue ft = lualoader.functable.get(obj2.getName());

								if (!ft.isnil()){
									LuaValue ftk = ft.get(key);

									if (!ftk.isnil()){
										return new fixfunc(methods,ftk,key,m_instance);
									}
								}

								LuaValue func = (LuaValue)methods.get(nkey.get("name"));

								return func.invoke(args);
							}
						}
					}
				}

				if (!methods.containsKey(key) && lualoader.mcptosrg != null) {
					Class obj2 = m_instance;

					for (int i = 0; i < 10; i++) {
						for (Class obji : obj2.getInterfaces()) {
							LuaValue tbl = lualoader.mcptosrg.get(obji.getName());

							if (tbl.istable()) {
								LuaValue nkey = tbl.get(key);

								if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
									LuaValue ft = lualoader.functable.get(obji.getName());

									if (!ft.isnil()) {
										LuaValue ftk = ft.get(key);

										if (!ftk.isnil()) {
											return new fixfunc(methods,ftk,key,m_instance);
										}
									}

									LuaValue func = (LuaValue)methods.get(nkey.get("name"));

									return func.invoke(args);
								}
							}

							for (Class obji2 : obji.getInterfaces()) {
								LuaValue tbl2 = lualoader.mcptosrg.get(obji2.getName());

								if (tbl2.istable()) {
									LuaValue nkey = tbl2.get(key);

									if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
										LuaValue ft = lualoader.functable.get(obji2.getName());

										if (!ft.isnil()) {
											LuaValue ftk = ft.get(key);

											if (!ftk.isnil()) {
												return new fixfunc(methods,ftk,key,m_instance);
											}
										}

										LuaValue func = (LuaValue)methods.get(nkey.get("name"));

										return func.invoke(args);
									}
								}

								for (Class obji3 : obji2.getInterfaces()) {
									LuaValue tbl3 = lualoader.mcptosrg.get(obji3.getName());

									if (tbl3.istable()) {
										LuaValue nkey = tbl3.get(key);

										if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
											LuaValue ft = lualoader.functable.get(obji3.getName());

											if (!ft.isnil()) {
												LuaValue ftk = ft.get(key);

												if (!ftk.isnil()) {
													return new fixfunc(methods,ftk,key,m_instance);
												}
											}

											LuaValue func = (LuaValue)methods.get(nkey.get("name"));

											return func.invoke(args);
										}
									}

									for (Class obji4 : obji3.getInterfaces()) {
										LuaValue tbl4 = lualoader.mcptosrg.get(obji4.getName());

										if (tbl4.istable()) {
											LuaValue nkey = tbl4.get(key);

											if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
												LuaValue ft = lualoader.functable.get(obji4.getName());

												if (!ft.isnil()) {
													LuaValue ftk = ft.get(key);

													if (!ftk.isnil()) {
														return new fixfunc(methods,ftk,key,m_instance);
													}
												}

												LuaValue func = (LuaValue)methods.get(nkey.get("name"));

												return func.invoke(args);
											}
										}

										for (Class obji5 : obji4.getInterfaces()) {
											LuaValue tbl5 = lualoader.mcptosrg.get(obji5.getName());

											if (tbl5.istable()) {
												LuaValue nkey = tbl5.get(key);

												if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
													LuaValue ft = lualoader.functable.get(obji5.getName());

													if (!ft.isnil()) {
														LuaValue ftk = ft.get(key);

														if (!ftk.isnil()) {
															return new fixfunc(methods,ftk,key,m_instance);
														}
													}

													LuaValue func = (LuaValue)methods.get(nkey.get("name"));

													return func.invoke(args);
												}
											}

											for (Class obji6 : obji5.getInterfaces()) {
												LuaValue tbl6 = lualoader.mcptosrg.get(obji6.getName());

												if (tbl6.istable()) {
													LuaValue nkey = tbl6.get(key);

													if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
														LuaValue ft = lualoader.functable.get(obji6.getName());

														if (!ft.isnil()) {
															LuaValue ftk = ft.get(key);

															if (!ftk.isnil()) {
																return new fixfunc(methods,ftk,key,m_instance);
															}
														}

														LuaValue func = (LuaValue)methods.get(nkey.get("name"));

														return func.invoke(args);
													}
												}

												for (Class obji7 : obji6.getInterfaces()) {
													LuaValue tbl7 = lualoader.mcptosrg.get(obji7.getName());

													if (tbl7.istable()) {
														LuaValue nkey = tbl7.get(key);

														if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
															LuaValue ft = lualoader.functable.get(obji7.getName());

															if (!ft.isnil()) {
																LuaValue ftk = ft.get(key);

																if (!ftk.isnil()) {
																	return new fixfunc(methods,ftk,key,m_instance);
																}
															}

															LuaValue func = (LuaValue)methods.get(nkey.get("name"));

															return func.invoke(args);
														}
													}

													for (Class obji8 : obji7.getInterfaces()) {
														LuaValue tbl8 = lualoader.mcptosrg.get(obji8.getName());

														if (tbl8.istable()) {
															LuaValue nkey = tbl8.get(key);

															if (!nkey.isnil() && nkey.get("argn").toint() != dargn) {
																LuaValue ft = lualoader.functable.get(obji8.getName());

																if (!ft.isnil()) {
																	LuaValue ftk = ft.get(key);

																	if (!ftk.isnil()) {
																		return new fixfunc(methods,ftk,key,m_instance);
																	}
																}

																LuaValue func = (LuaValue)methods.get(nkey.get("name"));

																return func.invoke(args);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}

						obj2 = obj2.getSuperclass();

						if (obj2.getName().equals("java.lang.Object")) break;
					}
				}
			}

			error("Could not find any method");

			return NIL;
		}
	}

	public LuaValue getConstructor() {
		return getMethod(NEW);
	}
}
