@echo off
echo Starting Ourania (Windows Edition)...
rem Relative to this file rather than to a path typed once. The Desktop moved into OneDrive
rem folder backup and this line did not, so the launcher was pointing at a husk with no
rem classes in it and the app would not start. %~dp0 is the folder this .bat sits in.
cd /d "%~dp0OuraniaWindows"
rem lib\* carries OpenPDF, which the PDF report needs. Without it on the classpath the app still
rem starts and every other export works, but Save Reading as PDF can only explain why it cannot.
"C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -cp "src\main\java;lib\*" com.zodiacomputing.ourania.gui.OuraniaWindow
cmd /k
