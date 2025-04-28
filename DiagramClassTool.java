import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

interface DiagramFormatter 
{
    List<String> format(List<String> classInfo);
}

abstract class DiagramPreProcessor implements DiagramFormatter
{
    boolean methods;
    boolean attributes;
    boolean name;
    List<String> ignoreClasses;
    List<String> classInfo;

    public DiagramPreProcessor(List<String> classInfo, boolean methods, boolean attributes, boolean name, List<String> ignoreClasses) 
    {
        this.methods = methods;
        this.attributes = attributes;
        this.name = name;
        this.ignoreClasses = ignoreClasses;
        this.classInfo = classInfo;
    }

    List<String> ProcessInfo()
    {
        List<String> processedInfo = new ArrayList<>();

        String currentClass;

        int statesFlag = 0;


        for (String line : classInfo) 
        {
            if(line.startsWith("Class:"))
            {
                currentClass = extractSimpleName(line.substring(6).trim());

                if(ignoreClasses.contains(currentClass))
                {
                    statesFlag = 1;
                }
            }else if(line.startsWith("Fields:"))
            {
                statesFlag = 2;
    
            }else if(line.startsWith("Methods"))
            {
                statesFlag = 3;
    
            }

            switch (statesFlag) {
                case 0:
                {
                    processedInfo.add(line);
                    break;
                }
                case 1:
                {
                    if(!line.startsWith("----"))  break;
                    statesFlag = 0;
                    break;
                }
                case 2:
                {
                    if(!line.startsWith("Methods:") || !line.startsWith("Constructors:") || !line.startsWith("Class:") ||
                    !line.startsWith("Interface:") || !line.startsWith(line) || !line.startsWith("Extends:"))
                    {
                        break;
                    }
                    statesFlag = 0;
                    break;
                }
                case 3:
                {
                    if(!line.startsWith("Fields:") || !line.startsWith("Constructors:") || !line.startsWith("Class:") ||
                    !line.startsWith("Interface:") || !line.startsWith(line) || !line.startsWith("Extends:"))
                    {
                        break;
                    }

                    statesFlag = 0;
                    break;
                   
                }
                default:
                    processedInfo.add(line);
            }   
            
        }


        return processedInfo;
    }

    private String extractSimpleName(String fullName) 
    {
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }
    
}

class PlantUMLFormatter implements DiagramFormatter 
{

    @Override
    public List<String> format(List<String> classInfos) 
    {
        List<String> result = new ArrayList<>();
        List<String> relationList = new ArrayList<>();
        result.add("@startuml");

        String currentClass = null;
        boolean insideClass = false;
        int statusFlag = 0;

        for (String line : classInfos) 
        {
            line = line.trim();

            if (line.startsWith("Class:") || (line.startsWith("Interface:"))) 
            {
                statusFlag = 0;
                if (insideClass) 
                {
                    result.add("}");
                }
                insideClass = true;
                
                if(line.startsWith("Class:"))
                {
                    currentClass = extractSimpleName(line.substring(6).trim());
                    result.add("class " + currentClass + " {");
                } 
                else  
                {
                    currentClass = extractSimpleName(line.substring(10).trim());
                    result.add("interface " + currentClass + " {");
                }                               
                

            } else if (line.startsWith("Extends:")) 
            {
                String parentFullName = line.substring(8).trim();
                if (!parentFullName.equals("java.lang.Object")) 
                {
                    String parentClass = extractSimpleName(parentFullName);
                    relationList.add(parentClass + " <|-- " + currentClass);
                }

            } else if (line.startsWith("Implements:")) 
            {
                statusFlag = 1;

            } else if (line.startsWith("Constructors:")) 
            {
                statusFlag = 2;

            } else if (line.startsWith("Methods:")) 
            {
                statusFlag = 3;
                
            }else if(line.startsWith("Fields:"))
            {
                statusFlag = 4;

            } else if (line.contains("----")) {
                if (insideClass) {
                    result.add("}");
                    insideClass = false;
                    statusFlag = 0;
                }
                continue;
            }


            switch (statusFlag) {
                case 1: //interface
                {
                    if(line.startsWith("Implements:")) break;
                   
                    relationList.add(line.substring(1) + " <|.. " + currentClass);
                    break;
                }
                case 2: //constructors
                {   
                    if(line.startsWith("Constructors")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");
                    result.add(line);
                    break;
                }
                case 3: // methods
                {
                    if(line.startsWith("Methods")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");
                    line = line.replace("abstract", "{abstract}");
                    result.add(line);
                    break;
                }
                case 4: //fields
                {
                    if(line.startsWith("Fields")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");
                    StringTokenizer st = new StringTokenizer(line);
                    String accMod = st.nextToken(" ");
                    String theType = st.nextToken(" ");
                    String theNewLine =  st.nextToken(" ");
                    if(Character.isUpperCase(theType.charAt(0)))
                    {
                        if(theType.endsWith(">"))
                        {
                            int start = theType.indexOf("<");
                            relationList.add(currentClass + " ---> " + theType.substring(start+1, theType.length()-1));
                        }
                        else
                        {
                            relationList.add(currentClass + " ---> " + theType);
                        }
                    }
                    String inverse = accMod + theNewLine + ":" + theType;
                    
                    result.add(inverse);
                    break;
                }

                default:
                    ;
            }
        }

        if (insideClass) 
        {
            result.add("}");
        }

        result.addAll(relationList);

        result.add("@enduml");
        
        return result;
    }

    private String extractSimpleName(String fullName) 
    {
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }
}

class YumlFormatter implements DiagramFormatter
{
    @Override
    public List<String> format(List<String> classInfos) 
    {
        List<String> result = new ArrayList<>();
        List<String> relationList = new ArrayList<>();

        String currentClass = null;
        boolean insideClass = false;
        int statusFlag = 0;
        List<String> attributesAndMethods = new ArrayList<>();

        for (String line : classInfos) 
        {
            line = line.trim();

            if (line.startsWith("Class:") || (line.startsWith("Interface:"))) 
            {
                statusFlag = 0;
                if (insideClass) 
                {
                    // output previous class block
                    result.add(buildYumlClassBlock(currentClass, attributesAndMethods));
                    attributesAndMethods.clear();
                }
                insideClass = true;
                
                if(line.startsWith("Class:"))
                {
                    currentClass = extractSimpleName(line.substring(6).trim());
                } 
                else  
                {
                    currentClass = extractSimpleName(line.substring(10).trim());
                }                               
                

            } else if (line.startsWith("Extends:")) 
            {
                String parentFullName = line.substring(8).trim();
                if (!parentFullName.equals("java.lang.Object")) 
                {
                    String parentClass = extractSimpleName(parentFullName);
                    relationList.add("[" + currentClass + "]^-["
                                    + parentClass + "]");
                }

            } else if (line.startsWith("Implements:")) 
            {
                statusFlag = 1;

            } else if (line.startsWith("Constructors:")) 
            {
                statusFlag = 2;

            } else if (line.startsWith("Methods:")) 
            {
                statusFlag = 3;
                
            } else if(line.startsWith("Fields:")) 
            {
                statusFlag = 4;

            } else if (line.contains("----")) 
            {
                if (insideClass) 
                {
                    result.add(buildYumlClassBlock(currentClass, attributesAndMethods));
                    attributesAndMethods.clear();
                    insideClass = false;
                    statusFlag = 0;
                }
                continue;
            }

            switch (statusFlag) 
            {
                case 1: // Interfaces
                {
                    if(line.startsWith("Implements:")) break;

                    String interfaceName = extractSimpleName(line.substring(1).trim());
                    relationList.add("[" + currentClass + "]^.-[" + interfaceName + "]");
                    break;
                }
                case 2: // Constructors
                {   
                    if(line.startsWith("Constructors")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");
                    attributesAndMethods.add(line.substring(2)); // remove "- " prefix
                    break;
                }
                case 3: // Methods
                {
                    if(line.startsWith("Methods")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");
                    //line = line.replace("abstract", "{abstract}");
                    attributesAndMethods.add(line.substring(2)); // remove "- " prefix
                    break;
                }
                case 4: // Fields
                {
                    if(line.startsWith("Fields")) break;
                    line = line.replace("- private", "-");
                    line = line.replace("- public", "+");
                    line = line.replace("- protected", "#");

                    StringTokenizer st = new StringTokenizer(line);
                    String accMod = st.nextToken(" ");
                    String theType = st.nextToken(" ");
                    String theNewLine = st.nextToken(" ");

                    // Detect composition relations for fields
                    if(Character.isUpperCase(theType.charAt(0)))
                    {
                        if(theType.endsWith(">"))
                        {
                            int start = theType.indexOf("<");
                            relationList.add("[" + currentClass + "]->[" + theType.substring(start+1, theType.length()-1) + "]");
                        }
                        else
                        {
                            relationList.add("[" + currentClass + "]->[" + theType + "]");
                        }
                    }
                    String inverse = accMod + " " + theNewLine + " : " + theType;
                    attributesAndMethods.add(inverse);
                    break;
                }

                default:
                    ;
            }
        }

        if (insideClass) 
        {
            result.add(buildYumlClassBlock(currentClass, attributesAndMethods));
        }

        result.addAll(relationList);

        return result;
    }

    private String buildYumlClassBlock(String className, List<String> members) 
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(className);
        if (!members.isEmpty()) {
            sb.append("|");
            for (int i = 0; i < members.size(); i++) {
                sb.append(members.get(i));
                if (i < members.size() - 1) {
                    sb.append(";");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractSimpleName(String fullName) 
    {
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }

    }


    class DiagramFormatterFactory 
    {
    
    
    public static DiagramFormatter getFormatter(String type) 
    {
            
            
        if ("plantuml".equalsIgnoreCase(type)) 
        {
            return new PlantUMLFormatter();

        }else if ("yuml".equalsIgnoreCase(type)) 
        {
            return new YumlFormatter();

        }else 
        {
            throw new IllegalArgumentException("Unknown formatter type: " + type);
        }
    }
}


public class DiagramClassTool 
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

        JarInfoExtractor extractor = new JarInfoExtractor(jarFile);

        extractor.extractInfo();
        extractor.printExtracted();

        List<String> classInfo = extractor.getInfo();

        DiagramFormatter formatter = DiagramFormatterFactory.getFormatter("yuml");
        List<String> output = formatter.format(classInfo);

        for (String line : output) 
        {
            System.out.println(line);
        }
    }  
}
