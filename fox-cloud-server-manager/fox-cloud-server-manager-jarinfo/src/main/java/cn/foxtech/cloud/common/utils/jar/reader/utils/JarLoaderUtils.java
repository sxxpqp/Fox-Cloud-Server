package cn.foxtech.cloud.common.utils.jar.reader.utils;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarLoaderUtils {
    public JarLoaderUtils() {
    }

    public static Set<Class<?>> getClasses(String pack) {
        Set<Class<?>> classes = new LinkedHashSet();
        boolean recursive = true;
        String packageName = pack;
        String packageDirName = pack.replace('.', '/');
        Enumeration<URL> dirs = null;

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            dirs = classLoader.getResources(packageDirName);

            while (true) {
                label71:
                while (dirs.hasMoreElements()) {
                    URL url = dirs.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                        findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                    } else if ("jar".equals(protocol)) {
                        try {
                            JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                            Enumeration<JarEntry> entries = jar.entries();

                            while (true) {
                                JarEntry entry;
                                String name;
                                int idx;
                                do {
                                    do {
                                        if (!entries.hasMoreElements()) {
                                            continue label71;
                                        }

                                        entry = entries.nextElement();
                                        name = entry.getName();
                                        if (name.charAt(0) == '/') {
                                            name = name.substring(1);
                                        }
                                    } while (!name.startsWith(packageDirName));

                                    idx = name.lastIndexOf(47);
                                    if (idx != -1) {
                                        packageName = name.substring(0, idx).replace('/', '.');
                                    }
                                } while (idx == -1 && !recursive);

                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    String className = name.substring(packageName.length() + 1, name.length() - 6);

                                    try {
                                        classes.add(classLoader.loadClass(packageName + '.' + className));
                                    } catch (ClassNotFoundException var16) {
                                    //    var16.printStackTrace();
                                    }
                                }
                            }
                        } catch (IOException var17) {
                        //    var17.printStackTrace();
                        }
                    }
                }

                return classes;
            }
        } catch (IOException var18) {
       //     var18.printStackTrace();
        } catch (Exception var19) {
       //     var19.printStackTrace();
        } catch (Throwable var20) {
       //     var20.printStackTrace();
        }

        return classes;
    }

    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (dir.exists() && dir.isDirectory()) {
            File[] dirfiles = dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return recursive && file.isDirectory() || file.getName().endsWith(".class");
                }
            });
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File[] var7 = dirfiles;
            int var8 = dirfiles.length;

            for (int var9 = 0; var9 < var8; ++var9) {
                File file = var7[var9];
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
                } else {
                    String className = file.getName().substring(0, file.getName().length() - 6);

                    try {
                        classes.add(classLoader.loadClass(packageName + '.' + className));
                    } catch (ClassNotFoundException var13) {
                    //    var13.printStackTrace();
                    }
                }
            }

        }
    }

    public static void loadJar(String jarFilePathName) {
        File jarFile = new File(jarFilePathName);
        loadJar(jarFile);
    }

    public static void loadJar(File jarFile) {
        Method method = null;

        try {
            method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        } catch (SecurityException | NoSuchMethodException var11) {
        }

        boolean accessible = method.isAccessible();

        try {
            method.setAccessible(true);
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL url = jarFile.toURI().toURL();
            method.invoke(classLoader, url);
        } catch (Exception var9) {
        } finally {
            method.setAccessible(accessible);
        }

    }
}