c:
del /s /f /q Generated
cd \Users\iain\Documents\Git\SL\src\net\coagulate\GPHUD\Modules\Scripting\Language
"C:\Program Files\Java\jdk-11.0.2\bin\java" -cp "c:\Program Files\javacc\bin\javacc.jar" jjtree GSParser.jjtree
"C:\Program Files\Java\jdk-11.0.2\bin\java" -cp "c:\Program Files\javacc\bin\javacc.jar" javacc Generated/GSParser.jj