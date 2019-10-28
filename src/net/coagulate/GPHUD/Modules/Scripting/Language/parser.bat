c:
del /s /f /q Generated
cd \Users\iain\Documents\Git\SL\src\net\coagulate\GPHUD\Modules\Scripting\Language
java -cp "c:\Program Files\javacc\bin\javacc.jar" jjtree GSParser.jjtree
java -cp "c:\Program Files\javacc\bin\javacc.jar" javacc Generated/GSParser.jj