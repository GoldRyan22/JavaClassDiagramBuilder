import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassDiagramBuilder 
{

    public static void main(String[] args) 
    {
        if (args.length != 1) 
        {
            System.out.println("Args: /the/path/idk.jar");
            return;
        }

        String jarPath = args[0];
        File jarFile = new File(jarPath);

        if (!jarFile.exists()) 
        {
            System.out.println("Invalid JAR file path.");
            return;
        }

        try 
        {
            JarFile jar = new JarFile(jarFile);
            URL url = jarFile.toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[]{url});

            Enumeration<JarEntry> jarEntries = jar.entries();
            while (jarEntries.hasMoreElements()) 
            {
                JarEntry entry = jarEntries.nextElement();

                if (entry.getName().endsWith(".class")) 
                {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    try 
                    {
                        Class<?> cls = classLoader.loadClass(className);
                        printClassInfo(cls);
                    } catch (ClassNotFoundException  e) 
                    {
                        System.out.println("Could not load class: " + className);
                    }
                }
            }
            jar.close();
            

        } catch (IOException e) 
        {
            System.out.println("Error reading JAR file: ");
            e.printStackTrace();
        }
    }

    static void printClassInfo(Class<?> cls) 
    {
        System.out.println("-----------------------------------------------------");

        if(cls.isAnnotation())
        {
            System.out.println("Annotation: " + cls.getName());
        }
        else
        {
            if(cls.isInterface())
            {
                System.out.println("Interface: " + cls.getName());
            }
            else
            {
                System.out.println("Class: " + cls.getName());
            }
        }

        if (cls.getSuperclass() != null) 
        {
            System.out.println("Extends: " + cls.getSuperclass().getName());
        }

        Class<?>[] interfaces = cls.getInterfaces();
        if (interfaces.length > 0) 
        {
            System.out.println("Implements:");
            for (Class<?> theInterface : interfaces) 
            {
                System.out.println("  - " + theInterface.getName());
            }
        }

        Field[] fset1 = cls.getDeclaredFields();
        Field[] fset2 = cls.getFields();

        ArrayList<Field> allFields = new ArrayList();

        for(Field f : fset1)
        {
            if(!allFields.contains(f)) allFields.add(f);
        }

        for(Field f : fset2)
        {
            if(!allFields.contains(f)) allFields.add(f);
        }

        
        if (allFields.size() > 0) 
        {
            System.out.println("Fields:");
            for (Field field : allFields) 
            {
                System.out.println("  - " + Modifier.toString(field.getModifiers()) + " " + field.getType().getSimpleName() + " " + field.getName());
            }
        }



        Constructor<?>[] cset1 = cls.getDeclaredConstructors();
        Constructor<?>[] cset2 = cls.getConstructors();
        
        ArrayList<Constructor<?>> allConstructors = new ArrayList<>();
        
        for (Constructor<?> c : cset1) 
        {
            if (!allConstructors.contains(c)) allConstructors.add(c);
        }
        for (Constructor<?> c : cset2) 
        {
            if (!allConstructors.contains(c)) allConstructors.add(c);
        }
        
        if (allConstructors.size() > 0) 
        {
            System.out.println("Constructors:");
            for (Constructor<?> constructor : allConstructors) 
            {
                System.out.print("  - " + Modifier.toString(constructor.getModifiers()) + " " + cls.getSimpleName() + "(");
                Class<?>[] params = constructor.getParameterTypes();
                for (int i = 0; i < params.length; i++) 
                {
                    System.out.print(params[i].getSimpleName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }
        }

        Method[] set1 = cls.getDeclaredMethods();
        Method[] set2 = cls.getMethods();

        ArrayList<Method> allMethods = new ArrayList<>();

        for(Method m : set1)
        {
           if(!allMethods.contains(m)) allMethods.add(m);
        }

        for(Method m : set2)
        {
            if(!allMethods.contains(m)) allMethods.add(m);
        }

        if (allMethods.size() > 0) 
        {
            System.out.println("Methods:");
            for (Method method : allMethods) 
            {
                System.out.print("  - " + Modifier.toString(method.getModifiers()) + " " + method.getReturnType().getSimpleName() + " " + method.getName() + "(");
                Class<?>[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) 
                {
                    System.out.print(params[i].getSimpleName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }
        }
        System.out.println("----------------------------------------------------\n");
    }
}
